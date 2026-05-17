package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greengo.domain.Booking;
import com.greengo.domain.FaultReportChatResponse;
import com.greengo.domain.FaultReportMessage;
import com.greengo.domain.FeedbackIssue;
import com.greengo.domain.PricingPlan;
import com.greengo.domain.Scooter;
import com.greengo.domain.Store;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.PricingPlanMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.mapper.StoreMapper;
import com.greengo.service.BookingService;
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

    private static final String FAULT_SUBMIT_MARKER = "---FAULT SUBMIT---";
    private static final Pattern FAULT_JSON_BLOCK_PATTERN = Pattern.compile(
            "---FAULT SUBMIT---\\s*\\n?\\s*(\\{[\\s\\S]*?\\})\\s*$"
    );
    private static final String BOOKING_SUBMIT_MARKER = "---BOOKING SUBMIT---";
    private static final Pattern BOOKING_JSON_BLOCK_PATTERN = Pattern.compile(
            "---BOOKING SUBMIT---\\s*\\n?\\s*(\\{[\\s\\S]*?\\})\\s*$"
    );
    private static final Pattern SCOOTER_CODE_PATTERN = Pattern.compile(
            "SC\\d+", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile(
            "\\b\\d{1,10}\\b"
    );
    private static final Pattern STORE_ID_PATTERN = Pattern.compile(
            "(?:store|网点|门店|店)\\s*(\\d+)|(\\d+)\\s*(?:号店|号网点|号门店|个网点)"
    );
    private static final Pattern HIRE_PERIOD_PATTERN = Pattern.compile(
            "HOUR_\\d+|DAY_\\d+|WEEK_\\d+", Pattern.CASE_INSENSITIVE
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

            --- BOOKING MODE ---
            ONLY when the user explicitly mentions wanting to book or reserve a scooter at a store (e.g., "订车", "预定", "预约", "租车", "我想租", "book a scooter", "reserve", "rent a scooter", "store pickup") should you initiate the booking process. This is DIFFERENT from scan-and-ride (随扫随骑) — the user is making an advance reservation at a specific store.

            When booking is triggered, you need to collect THREE pieces of information, ONE AT A TIME:
            1. appointmentStart — the date and time the user wants to start using the scooter. Format as "yyyy-MM-dd HH:mm" (e.g., "2026-05-20 14:00"). Ask the user for their preferred start time.
            2. hiredPeriod — how long they want to rent the scooter. Available periods are: HOUR_1 (1 hour), HOUR_4 (4 hours), DAY_1 (1 day), WEEK_1 (1 week). Present these options to the user and let them choose.
            3. storeId — which store/station (网点) they want to pick up from. The backend will validate the store. If the user doesn't know the store, the backend system message will list available stores.

            Backend Validation (during booking):
            - The backend will automatically detect store references and hire period codes in the user's message and validate them.
            - You will see system messages with validation results like: "[SYSTEM VALIDATION: ...] [BOOKING VALIDATION: Store 1 — FOUND (Xipu North Hub, bookable: 5); ...]"
            - If a store is NOT FOUND, ask the user to pick another store.
            - If a store has no bookable inventory, tell the user and suggest trying another store.
            - The backend will list available stores when needed.

            Booking conversation rules:
            - Ask only ONE question at a time, starting with the appointment start time.
            - Collect information in order: start time → hired period → store.
            - ALWAYS confirm the information back to the user in a clear format.
            - NEVER guess any information. If the user is unclear, ask for clarification.
            - Once the user confirms, move on to the next question.
            - If the user mentions fault-related keywords during booking, stay in booking mode unless they clearly want to switch.

            When and ONLY when ALL THREE pieces of booking information are collected AND the store has been validated as FOUND with bookable inventory, you MUST output the following JSON marker at the END of your reply, on its own line:

            ---BOOKING SUBMIT---
            {"appointmentStart": "2026-05-20 14:00", "hiredPeriod": "HOUR_1", "storeId": 1}

            appointmentStart must be a string in "yyyy-MM-dd HH:mm" format. hiredPeriod must be one of HOUR_1, HOUR_4, DAY_1, WEEK_1. storeId must be a number without quotes.
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

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private PricingPlanMapper pricingPlanMapper;

    @Autowired
    private BookingService bookingService;

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
            return buildAvailableStoresSection();
        }

        StringBuilder ctx = new StringBuilder();
        Long userId = getCurrentUserId();

        // Fault reporting validation: scooter codes
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

        // Fault reporting validation: booking IDs
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

        // Booking validation: store references
        Matcher storeMatcher = STORE_ID_PATTERN.matcher(userMessage);
        boolean storeFound = false;
        while (storeMatcher.find()) {
            String storeIdStr = storeMatcher.group(1) != null ? storeMatcher.group(1) : storeMatcher.group(2);
            if (storeIdStr != null) {
                try {
                    long sid = Long.parseLong(storeIdStr);
                    Store store = storeMapper.selectById(sid);
                    if (store != null && "ENABLED".equals(store.getStatus())) {
                        // Count bookable inventory
                        int total = countStorePickupScooters(sid);
                        int overlapping = countOverlappingBookings(sid);
                        int bookable = Math.max(0, total - overlapping);
                        ctx.append("Store ").append(sid).append(" — FOUND (").append(store.getName())
                                .append(", ").append(store.getAddress())
                                .append(", bookable: ").append(bookable).append(")\n");
                    } else if (store != null) {
                        ctx.append("Store ").append(sid).append(" — FOUND but DISABLED (").append(store.getName()).append(")\n");
                    } else {
                        ctx.append("Store ").append(sid).append(" — NOT FOUND in database\n");
                    }
                    storeFound = true;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Also check for store name keywords
        if (!storeFound) {
            String lowerMsg = userMessage.toLowerCase();
            for (Store store : storeMapper.selectList(
                    new LambdaQueryWrapper<Store>().eq(Store::getStatus, "ENABLED"))) {
                String lowerName = store.getName().toLowerCase();
                if ((lowerName.contains("north") && lowerMsg.contains("north"))
                        || (lowerName.contains("south") && lowerMsg.contains("south"))
                        || (lowerName.contains("student") && lowerMsg.contains("student"))
                        || (lowerName.contains("北") && lowerMsg.contains("北"))
                        || (lowerName.contains("南") && lowerMsg.contains("南"))
                        || (lowerName.contains("学生") && lowerMsg.contains("学生"))) {
                    int total = countStorePickupScooters(store.getId());
                    int overlapping = countOverlappingBookings(store.getId());
                    int bookable = Math.max(0, total - overlapping);
                    ctx.append("Store ").append(store.getId()).append(" — FOUND (").append(store.getName())
                            .append(", ").append(store.getAddress())
                            .append(", bookable: ").append(bookable).append(")\n");
                }
            }
        }

        // Booking validation: hire period codes
        Matcher periodMatcher = HIRE_PERIOD_PATTERN.matcher(userMessage);
        while (periodMatcher.find()) {
            String periodCode = periodMatcher.group().toUpperCase();
            PricingPlan plan = pricingPlanMapper.selectOne(
                    new LambdaQueryWrapper<PricingPlan>().eq(PricingPlan::getHirePeriod, periodCode));
            if (plan != null) {
                ctx.append("hiredPeriod ").append(periodCode).append(" — VALID (price: ").append(plan.getPrice()).append(")\n");
            } else {
                ctx.append("hiredPeriod ").append(periodCode).append(" — INVALID (not an available period)\n");
            }
        }

        StringBuilder result = new StringBuilder();
        if (ctx.length() > 0) {
            result.append("[SYSTEM VALIDATION]\n").append(ctx);
        } else {
            result.append("No scooter code, booking ID, store reference, or hire period detected in the user's message yet.\n");
        }

        result.append(buildAvailableStoresSection());
        return result.toString();
    }

    private String buildAvailableStoresSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n[AVAILABLE STORES]\n");
        java.util.List<Store> stores = storeMapper.selectList(
                new LambdaQueryWrapper<Store>().eq(Store::getStatus, "ENABLED"));
        if (stores.isEmpty()) {
            sb.append("No stores currently available.\n");
        } else {
            for (Store store : stores) {
                int total = countStorePickupScooters(store.getId());
                int overlapping = countOverlappingBookings(store.getId());
                int bookable = Math.max(0, total - overlapping);
                sb.append("Store ").append(store.getId()).append(": ").append(store.getName())
                        .append(" — ").append(store.getAddress())
                        .append(" (bookable: ").append(bookable).append(")\n");
            }
        }
        return sb.toString();
    }

    private int countStorePickupScooters(Long storeId) {
        Long count = scooterMapper.selectCount(
                new LambdaQueryWrapper<Scooter>()
                        .eq(Scooter::getStoreId, storeId)
                        .eq(Scooter::getRentalMode, "STORE_PICKUP")
                        .notIn(Scooter::getStatus, "DISABLED", "MAINTENANCE"));
        return count != null ? count.intValue() : 0;
    }

    private int countOverlappingBookings(Long storeId) {
        // Count bookings for this store that overlap with "now" (current active/pending)
        // This is a simplified count — the actual booking creation does precise time-window checks
        Long count = bookingMapper.selectCount(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getStoreId, storeId)
                        .in(Booking::getStatus, "RESERVED", "IN_PROGRESS", "OVERDUE"));
        return count != null ? count.intValue() : 0;
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

    protected String callDashScope(List<Map<String, String>> messages) throws Exception {
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
        // Check for booking submission first
        Matcher bookingMatcher = BOOKING_JSON_BLOCK_PATTERN.matcher(llmResponse);
        if (bookingMatcher.find()) {
            return handleBookingSubmit(llmResponse, bookingMatcher);
        }

        // Check for fault submission
        Matcher faultMatcher = FAULT_JSON_BLOCK_PATTERN.matcher(llmResponse);
        if (faultMatcher.find()) {
            return handleFaultSubmit(llmResponse, faultMatcher);
        }

        // No marker found — plain reply
        String reply = llmResponse;
        if (reply.contains(FAULT_SUBMIT_MARKER)) {
            reply = reply.substring(0, reply.indexOf(FAULT_SUBMIT_MARKER)).trim();
        }
        if (reply.contains(BOOKING_SUBMIT_MARKER)) {
            reply = reply.substring(0, reply.indexOf(BOOKING_SUBMIT_MARKER)).trim();
        }
        return new FaultReportChatResponse(reply);
    }

    private FaultReportChatResponse handleFaultSubmit(String llmResponse, Matcher matcher) {
        String reply = llmResponse.substring(0, matcher.start()).trim();
        String jsonStr = matcher.group(1).trim();

        try {
            JsonNode json = objectMapper.readTree(jsonStr);
            String scooterCode = json.path("scooterCode").asText();
            long bookingId = json.path("bookingId").asLong();
            String faultDescription = json.path("faultDescription").asText();

            if (scooterCode.isBlank() || faultDescription.isBlank() || bookingId <= 0) {
                log.warn("LLM returned incomplete fault JSON: {}", jsonStr);
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
            log.error("Failed to parse fault JSON: {}", jsonStr, e);
            return new FaultReportChatResponse(reply + "\n\n(There was an error processing the fault information. Please try describing the issue again.)");
        }
    }

    private FaultReportChatResponse handleBookingSubmit(String llmResponse, Matcher matcher) {
        String reply = llmResponse.substring(0, matcher.start()).trim();
        String jsonStr = matcher.group(1).trim();

        try {
            JsonNode json = objectMapper.readTree(jsonStr);
            String appointmentStart = json.path("appointmentStart").asText();
            String hiredPeriod = json.path("hiredPeriod").asText();
            long storeId = json.path("storeId").asLong();

            if (appointmentStart.isBlank() || hiredPeriod.isBlank() || storeId <= 0) {
                log.warn("LLM returned incomplete booking JSON: {}", jsonStr);
                return new FaultReportChatResponse(reply + "\n\n(The system could not process the booking information. Please continue providing the details.)");
            }

            // Validate hire period
            PricingPlan plan = pricingPlanMapper.selectOne(
                    new LambdaQueryWrapper<PricingPlan>().eq(PricingPlan::getHirePeriod, hiredPeriod.toUpperCase()));
            if (plan == null) {
                return new FaultReportChatResponse(reply + "\n\nInvalid hire period: " + hiredPeriod + ". Available periods are HOUR_1, HOUR_4, DAY_1, WEEK_1. Please try again.");
            }

            // Validate store exists and is enabled
            Store store = storeMapper.selectById(storeId);
            if (store == null || !"ENABLED".equals(store.getStatus())) {
                return new FaultReportChatResponse(reply + "\n\nStore #" + storeId + " is not available. Please choose a different store.");
            }

            // Parse appointment time
            java.time.LocalDateTime appointmentTime;
            try {
                appointmentTime = java.time.LocalDateTime.parse(appointmentStart,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (Exception e) {
                return new FaultReportChatResponse(reply + "\n\nInvalid date format for appointment start. Please use yyyy-MM-dd HH:mm format (e.g., 2026-05-20 14:00).");
            }

            log.info("Creating booking via agent: storeId={}, appointmentStart={}, hiredPeriod={}",
                    storeId, appointmentTime, hiredPeriod.toUpperCase());

            Booking booking = bookingService.createStoreBooking(storeId, appointmentTime, hiredPeriod.toUpperCase());

            FaultReportChatResponse.BookingResult result =
                    new FaultReportChatResponse.BookingResult(
                            booking.getId(),
                            store.getName(),
                            appointmentStart,
                            hiredPeriod.toUpperCase()
                    );

            return new FaultReportChatResponse(
                    reply + "\n\nBooking created successfully! Your booking ID is #" + booking.getId()
                            + ". Store: " + store.getName()
                            + ", Start: " + appointmentStart
                            + ", Duration: " + hiredPeriod.toUpperCase(),
                    result, true);

        } catch (IllegalArgumentException e) {
            log.warn("Booking creation rejected: {}", e.getMessage());
            return new FaultReportChatResponse(reply + "\n\n" + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to parse booking JSON or create booking: {}", jsonStr, e);
            return new FaultReportChatResponse(reply + "\n\n(There was an error creating your booking. Please try again. " + e.getMessage() + ")");
        }
    }
}
