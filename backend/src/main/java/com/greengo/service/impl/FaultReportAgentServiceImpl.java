package com.greengo.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greengo.domain.FaultReportChatResponse;
import com.greengo.domain.FaultReportMessage;
import com.greengo.domain.FeedbackIssue;
import com.greengo.service.FaultReportAgentService;
import com.greengo.service.FeedbackIssueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FaultReportAgentServiceImpl implements FaultReportAgentService {

    private static final Logger log = LoggerFactory.getLogger(FaultReportAgentServiceImpl.class);

    private static final String SUBMIT_MARKER = "---故障提交---";
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "---故障提交---\\s*\\n?\\s*(\\{[\\s\\S]*?\\})\\s*$"
    );

    private static final String SYSTEM_PROMPT = """
            你是一名Green Go电动滑板车故障上报助手。你的工作是通过对话收集用户的故障信息，并最终提交故障报告。

            你需要收集以下三项信息：
            1. scooterCode——车辆编码（例如SC001），必须由用户明确提供，不要猜测
            2. bookingId——订单ID，必须由用户明确提供，不要猜测
            3. faultDescription——故障描述，必须由用户明确提供，不要猜测

            对话规则：
            - 每次只询问一项缺失的信息，一次只问一个问题
            - 如果用户提供的信息不完整或不清楚，主动追问
            - 绝对不要猜测任何信息。如果用户说的内容你不理解，请用户进一步说明
            - 语气友好、专业，使用中文与用户交流
            - 如果用户一开始就提供了全部信息，直接确认后提交

            当且仅当全部三项信息都已收集并确认无误后，你必须在回复的末尾单独一段输出以下JSON标记：

            ---故障提交---
            {"scooterCode": "SC001", "bookingId": 123, "faultDescription": "车辆故障描述"}

            在收集到所有信息之前，绝对不要输出上述JSON标记。bookingId必须是数字类型，不要加引号。
            """;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.base-url}")
    private String baseUrl;

    @Value("${dashscope.model}")
    private String model;

    @Autowired
    private FeedbackIssueService feedbackIssueService;

    @Override
    public FaultReportChatResponse processMessage(String userMessage, List<FaultReportMessage> history) {
        try {
            List<Map<String, String>> messages = buildMessages(userMessage, history);
            String llmResponse = callDashScope(messages);
            return parseResponse(llmResponse);
        } catch (Exception e) {
            log.error("Fault report agent error", e);
            return new FaultReportChatResponse("系统处理故障报告时遇到问题，请稍后重试。");
        }
    }

    private List<Map<String, String>> buildMessages(String userMessage, List<FaultReportMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        messages.add(systemMsg);

        if (history != null) {
            for (FaultReportMessage msg : history) {
                if (msg.getRole() != null && msg.getContent() != null) {
                    Map<String, String> histMsg = new LinkedHashMap<>();
                    histMsg.put("role", msg.getRole());
                    histMsg.put("content", msg.getContent());
                    messages.add(histMsg);
                }
            }
        }

        if (userMessage != null && !userMessage.isBlank()) {
            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage.trim());
            messages.add(userMsg);
        }

        return messages;
    }

    private String callDashScope(List<Map<String, String>> messages) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 1000);

        String jsonBody = objectMapper.writeValueAsString(body);

        URI uri = URI.create(baseUrl + "/chat/completions");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        log.debug("Calling DashScope API with {} messages", messages.size());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("DashScope API returned status {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("AI服务返回错误状态: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("AI服务返回了空的响应");
        }

        String content = choices.get(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new RuntimeException("AI服务返回了空的回复内容");
        }

        return content.trim();
    }

    private FaultReportChatResponse parseResponse(String llmResponse) {
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(llmResponse);
        if (!matcher.find()) {
            String reply = llmResponse;
            if (reply.contains(SUBMIT_MARKER)) {
                reply = reply.substring(0, reply.indexOf(SUBMIT_MARKER)).trim();
            }
            return new FaultReportChatResponse(reply);
        }

        String reply = llmResponse.substring(0, matcher.start()).trim();
        String jsonStr = matcher.group(1).trim();

        try {
            JsonNode json = objectMapper.readTree(jsonStr);
            String scooterCode = json.path("scooterCode").asText();
            long bookingId = json.path("bookingId").asLong();
            String faultDescription = json.path("faultDescription").asText();

            if (scooterCode.isBlank() || faultDescription.isBlank() || bookingId <= 0) {
                log.warn("LLM returned incomplete JSON: {}", jsonStr);
                return new FaultReportChatResponse(reply + "\n\n（系统未能识别完整的故障信息，请继续与我沟通。）");
            }

            log.info("Creating fault report: scooterCode={}, bookingId={}, description={}",
                    scooterCode, bookingId, faultDescription);

            FeedbackIssue issue = feedbackIssueService.createIssueFromAgent(
                    scooterCode.trim(), bookingId, faultDescription.trim());

            FaultReportChatResponse.FaultReportIssueResult result =
                    new FaultReportChatResponse.FaultReportIssueResult(
                            issue.getId(),
                            issue.getScooterCode(),
                            issue.getBookingId(),
                            issue.getContent()
                    );

            return new FaultReportChatResponse(
                    reply + "\n\n✅ 故障报告已成功提交！您的报告编号是 #" + issue.getId(),
                    result);

        } catch (IllegalArgumentException e) {
            log.warn("Issue creation rejected: {}", e.getMessage());
            return new FaultReportChatResponse(reply + "\n\n" + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to parse LLM JSON: {}", jsonStr, e);
            return new FaultReportChatResponse(reply + "\n\n（系统处理故障信息时出错，请重新描述故障情况。）");
        }
    }
}
