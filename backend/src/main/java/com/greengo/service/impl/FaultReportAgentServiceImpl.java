package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greengo.domain.Booking;
import com.greengo.domain.FaultReportChatResponse;
import com.greengo.domain.FaultReportMessage;
import com.greengo.domain.FeedbackIssue;
import com.greengo.domain.Scooter;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.service.FaultReportAgentService;
import com.greengo.service.FeedbackIssueService;
import com.greengo.utils.ThreadLocalUtil;
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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FaultReportAgentServiceImpl implements FaultReportAgentService {

    private static final Logger log = LoggerFactory.getLogger(FaultReportAgentServiceImpl.class);

    private static final String SUBMIT_MARKER = "---FAULT SUBMIT---";
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "---FAULT SUBMIT---\\s*\\n?\\s*(\\{[\\s\\S]*?\\})\\s*$"
    );
    private static final Pattern SCOOTER_CODE_PATTERN = Pattern.compile(
            "SC\\d+", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile(
            "\\b\\d{1,10}\\b"
    );

    private static final String SYSTEM_PROMPT = """
            You are a helpful customer service assistant for Green Go, an e-scooter rental service. Your primary role is to help users with general inquiries. Respond to their questions in a helpful, professional manner. Do NOT start with a greeting — the user will send the first message.

            Reply in the SAME LANGUAGE the user is using. If they speak English, reply in English. If they speak Chinese, reply in Chinese. Be friendly and professional.

            --- FAULT REPORTING MODE ---
            ONLY when the user explicitly mentions a vehicle problem (e.g., "broken", "fault", "not working", "damaged", "issue with scooter", "坏了", "故障", "有问题", "报修") should you initiate the fault reporting process.

            When fault reporting is triggered, you need to collect THREE pieces of information:
            1. scooterCode — the scooter's code (e.g. SC001). Must be explicitly provided by the user. Do NOT guess.
            2. bookingId — the booking/order ID related to this fault. Must be explicitly provided by the user. Do NOT guess.
            3. faultDescription — a clear description of what is wrong with the scooter. Must be explicitly provided by the user. Do NOT guess.

            Backend Validation (during fault reporting):
            - After the user provides a scooter code or booking ID, the backend system will automatically validate it against the database.
            - You will see a system message with validation results like: "[SYSTEM VALIDATION: scooterCode SC001 — FOUND / NOT FOUND; bookingId 123 — FOUND (your booking) / NOT FOUND / NOT YOURS]"
            - If validation says NOT FOUND, the scooter code or booking ID is invalid. Tell the user it was not found and ask them to double-check.
            - If validation says NOT YOURS, the booking does not belong to the current user. Tell the user to check again.
            - Only consider scooterCode and bookingId as confirmed when validation says FOUND.

            Fault reporting conversation rules:
            - Ask only ONE question at a time, focusing on the next missing piece of information.
            - If the user's input is unclear or incomplete, ask for clarification.
            - NEVER guess any information. If you don't understand what the user means, ask them to explain further.

            When and ONLY when ALL THREE pieces of fault information are collected AND the backend has validated scooterCode and bookingId as FOUND, you MUST output the following JSON marker at the END of your reply, on its own line:

            ---FAULT SUBMIT---
            {"scooterCode": "SC001", "bookingId": 123, "faultDescription": "description of the fault"}

            Do NOT output this marker before all information is collected and validated. bookingId must be a number type without quotes in the JSON.
            """;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${dashscope.model:qwen-plus}")
    private String model;

    @Autowired
    private FeedbackIssueService feedbackIssueService;

    @Autowired
    private ScooterMapper scooterMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Override
    public FaultReportChatResponse processMessage(String userMessage, List<FaultReportMessage> history) {
        try {
            String validationContext = buildValidationContext(userMessage);
            List<Map<String, String>> messages = buildMessages(userMessage, history, validationContext);
            String llmResponse = callDashScope(messages);
            return parseResponse(llmResponse);
        } catch (Exception e) {
            log.error("Fault report agent error", e);
            return new FaultReportChatResponse("The system encountered an error processing your request. Please try again later.");
        }
    }

    private String buildValidationContext(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "";
        }

        StringBuilder ctx = new StringBuilder();
        Long userId = getCurrentUserId();

        Matcher scooterMatcher = SCOOTER_CODE_PATTERN.matcher(userMessage);
        while (scooterMatcher.find()) {
            String code = scooterMatcher.group().toUpperCase();
            Scooter scooter = scooterMapper.selectOne(
                    new LambdaQueryWrapper<Scooter>().eq(Scooter::getScooterCode, code));
            if (scooter != null) {
                ctx.append("scooterCode ").append(code).append(" — FOUND (status: ").append(scooter.getStatus()).append(")\n");
            } else {
                ctx.append("scooterCode ").append(code).append(" — NOT FOUND in database\n");
            }
        }

        Matcher bookingMatcher = BOOKING_ID_PATTERN.matcher(userMessage);
        while (bookingMatcher.find()) {
            String numStr = bookingMatcher.group();
            try {
                long bid = Long.parseLong(numStr);
                Booking booking = bookingMapper.selectById(bid);
                if (booking != null) {
                    if (userId != null && Objects.equals(booking.getUserId(), userId)) {
                        ctx.append("bookingId ").append(bid).append(" — FOUND (your booking, status: ").append(booking.getStatus()).append(")\n");
                    } else {
                        ctx.append("bookingId ").append(bid).append(" — FOUND but NOT YOURS (belongs to userId=").append(booking.getUserId()).append(")\n");
                    }
                } else {
                    ctx.append("bookingId ").append(bid).append(" — NOT FOUND in database\n");
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (ctx.length() == 0) {
            return "No scooter code or booking ID detected in the user's message yet.\n";
        }
        return "[SYSTEM VALIDATION]\n" + ctx.toString();
    }

    private Long getCurrentUserId() {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            if (claims != null && claims.get("id") != null) {
                return ((Number) claims.get("id")).longValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private List<Map<String, String>> buildMessages(String userMessage, List<FaultReportMessage> history, String validationContext) {
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

        if (validationContext != null && !validationContext.isBlank()) {
            Map<String, String> ctxMsg = new LinkedHashMap<>();
            ctxMsg.put("role", "system");
            ctxMsg.put("content", validationContext);
            messages.add(ctxMsg);
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
            throw new RuntimeException("AI service returned error status: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("AI service returned an empty response");
        }

        String content = choices.get(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new RuntimeException("AI service returned empty content");
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
                return new FaultReportChatResponse(reply + "\n\n(The system could not process the fault information. Please continue describing the issue.)");
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
                    reply + "\n\nFault report submitted successfully. Your report ID is #" + issue.getId(),
                    result);

        } catch (IllegalArgumentException e) {
            log.warn("Issue creation rejected: {}", e.getMessage());
            return new FaultReportChatResponse(reply + "\n\n" + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to parse LLM JSON: {}", jsonStr, e);
            return new FaultReportChatResponse(reply + "\n\n(There was an error processing the fault information. Please try describing the issue again.)");
        }
    }
}
