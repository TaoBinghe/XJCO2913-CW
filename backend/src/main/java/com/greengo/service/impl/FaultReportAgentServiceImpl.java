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
import com.greengo.utils.RentalConstants;
import com.greengo.utils.ThreadLocalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final Pattern BOOKING_INTENT_PATTERN = Pattern.compile(
            "订车|预订|预定|预约|租车|我想租|取车|租.*(小时|天|周|号店|门店|网点)|book\\s*(a\\s*)?scooter|reserve|reservation|rent\\s*(a\\s*)?scooter|rent.*(hour|day|week|store)|store\\s*pickup",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BOOKING_DETAIL_PATTERN = Pattern.compile(
            "今天|明天|后天|下周|上午|中午|下午|晚上|\\d+\\s*(小时|天|周)|\\d+\\s*(号店|号网点|号门店)|store\\s*\\d+|\\d+\\s*(hour|hours|day|days|week|weeks)|\\b\\d{1,2}\\s*(am|pm)\\b|\\b\\d{1,2}:\\d{2}\\b|HOUR_\\d+|DAY_\\d+|WEEK_\\d+",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FAULT_INTENT_PATTERN = Pattern.compile(
            "broken|fault|not\\s*working|damaged|issue\\s*with\\s*scooter|坏了|故障|有问题|报修|损坏|不能启动",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FAULT_INTENT_ONLY_PATTERN = Pattern.compile(
            "^(?i)(i\\s+)?(want|need)\\s+to\\s+report(\\s+a)?(\\s+scooter)?\\s*fault[.!?]*$|"
                    + "^(?i)i\\s+need\\s+to\\s+report\\s+a\\s+scooter\\s+fault[.!?]*$|"
                    + "^(?i)(want\\s+to\\s+)?report(\\s+a)?\\s*fault[.!?]*$|"
                    + "^(?i)fault\\s+report[.!?]*$|"
                    + "^(?i)scooter\\s+fault[.!?]*$|"
                    + "^报障$|^我要报障$|^报告故障$|^故障上报$",
            Pattern.CASE_INSENSITIVE
    );
    private static final int MIN_FAULT_DESCRIPTION_LENGTH = 5;
    private static final DateTimeFormatter BOOKING_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter BOOKING_TIME_WITH_SECONDS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String AI_UNAVAILABLE_REPLY = "AI Support is taking too long to respond right now. Please try again, or use manual booking and Feedback as a fallback.";
    private static final Pattern UNVERIFIED_BOOKING_CONFIRMATION_PATTERN = Pattern.compile(
            "\\b(booking|reservation|scooter)\\b[\\s\\S]{0,80}\\b(created|confirmed|reserved|booked)\\b|\\b(created|confirmed|reserved|booked)\\b[\\s\\S]{0,80}\\b(booking|reservation|scooter)\\b|\\u9884\\u8ba2\\u6210\\u529f|\\u5df2\\u9884\\u8ba2|\\u9884\\u7ea6\\u6210\\u529f",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern AFFIRMATION_PATTERN = Pattern.compile(
            "^(yes|yep|yeah|ok|okay|confirm|confirmed|sure|correct|looks good|go ahead|submit|book it|reserve it|\\u662f|\\u662f\\u7684|\\u597d|\\u597d\\u7684|\\u53ef\\u4ee5|\\u786e\\u8ba4|\\u63d0\\u4ea4|\\u9884\\u8ba2\\u5427)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern EXPLICIT_BOOKING_TIME_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{1,2}-\\d{1,2})[ T]+(\\d{1,2}:\\d{2})(?::\\d{2})?"
    );
    private static final Pattern CLOCK_TIME_PATTERN = Pattern.compile(
            "\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final int MAX_HISTORY_MESSAGES = 6;
    private static final int BOOKING_CONTEXT_HISTORY_MESSAGES = 12;
    private static final Duration DASHSCOPE_REQUEST_TIMEOUT = Duration.ofSeconds(14);

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
            1. appointmentStart — the date and time the user wants to start using the scooter. Users may express this naturally, such as "tomorrow at 2pm", "明天下午两点", "next Monday morning at 9", or "tonight at 7". Do NOT force the user to type a machine date format.
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
            - The backend system will provide the current local time in a [CURRENT TIME] message. Use it to resolve relative dates like "today", "tomorrow", "明天", "next Monday", and "下周一".
            - Convert natural-language times into the internal "yyyy-MM-dd HH:mm" value yourself. Never ask the user to use this exact format.
            - If the user's time is ambiguous, such as "afternoon" without an hour, ask a natural follow-up like "What time in the afternoon?" before submitting.
            - Only submit a booking when the resolved appointmentStart is a concrete clock time.
            - ALWAYS confirm the information back to the user in a clear format.
            - NEVER guess any information. If the user is unclear, ask for clarification.
            - Once the user confirms, move on to the next question.
            - If the user mentions fault-related keywords during booking, stay in booking mode unless they clearly want to switch.

            When and ONLY when ALL THREE pieces of booking information are collected AND the store has been validated as FOUND with bookable inventory, you MUST output the following JSON marker at the END of your reply, on its own line:

            ---BOOKING SUBMIT---
            {"appointmentStart": "2026-05-20 14:00", "hiredPeriod": "HOUR_1", "storeId": 1}

            appointmentStart in the JSON marker must be the resolved concrete local time in "yyyy-MM-dd HH:mm" format. This is only for the hidden JSON marker, not for the user's wording. hiredPeriod must be one of HOUR_1, HOUR_4, DAY_1, WEEK_1. storeId must be a number without quotes.
            """;

    private static final String FAULT_SYSTEM_PROMPT = """
            You are Green Go AI Support. Reply in the same language as the user.

            Fault reporting mode:
            - Only handle scooter fault reports here.
            - Collect scooterCode, bookingId, and a clear faultDescription.
            - Use the backend validation message to reject unknown scooters or orders that do not belong to this user.
            - Ask one short follow-up question at a time.
            - Never say the fault report is submitted unless you output the marker below.
            - When all three fields are collected and validated, end the reply with:

            ---FAULT SUBMIT---
            {"scooterCode":"SC001","bookingId":123,"faultDescription":"description"}
            """;

    private static final String BOOKING_SYSTEM_PROMPT = """
            You are Green Go AI Support. Reply in the same language as the user.

            Store booking mode:
            - Help the user reserve a scooter for pickup at a store (not scan-and-ride).
            - Collect appointmentStart (resolve natural language to a concrete local date and time), hiredPeriod (HOUR_1, HOUR_4, DAY_1, or WEEK_1), and storeId.
            - Use the backend validation message for store availability and hire period checks.
            - Ask one short follow-up question at a time.
            - Never say a booking is created, confirmed, or reserved unless you output the marker below.
            - When all three fields are collected and the store is valid with bookable inventory, end the reply with:

            ---BOOKING SUBMIT---
            {"appointmentStart":"2026-05-20 14:00","hiredPeriod":"HOUR_1","storeId":1}

            appointmentStart in the marker must use yyyy-MM-dd HH:mm. hiredPeriod must be HOUR_1, HOUR_4, DAY_1, or WEEK_1. storeId must be a number without quotes.
            """;

    private static final String GENERAL_SYSTEM_PROMPT = """
            You are Green Go AI Support for an e-scooter rental mini app. Reply in the same language as the user.
            Keep answers brief and helpful. If the user wants to book a scooter, tell them you can help if they provide start time, duration, and pickup store. If they report a fault, ask for scooter code, booking ID, and fault description.
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
            if (userMessage == null || userMessage.isBlank()) {
                return new FaultReportChatResponse("Please type a message so I can help with booking or fault reporting.");
            }

            if (shouldPreferBooking(userMessage, history)) {
                FaultReportChatResponse bookingResponse = tryProcessBookingWithoutAi(userMessage, history);
                if (bookingResponse != null) {
                    return bookingResponse;
                }
                if (isFaultIntent(userMessage)) {
                    FaultReportChatResponse faultResponse = tryProcessFaultWithoutAi(userMessage, history);
                    if (faultResponse != null) {
                        return faultResponse;
                    }
                }
            } else {
                FaultReportChatResponse faultResponse = tryProcessFaultWithoutAi(userMessage, history);
                if (faultResponse != null) {
                    return faultResponse;
                }

                FaultReportChatResponse bookingResponse = tryProcessBookingWithoutAi(userMessage, history);
                if (bookingResponse != null) {
                    return bookingResponse;
                }
            }

            if (!hasDashScopeConfig()) {
                log.warn("Skip AI call because DashScope configuration is incomplete");
                return new FaultReportChatResponse(AI_UNAVAILABLE_REPLY);
            }

            String validationContext = buildValidationContext(userMessage);
            List<Map<String, String>> messages = buildMessages(userMessage, history, validationContext);
            String llmResponse = callDashScope(messages);
            return parseResponse(llmResponse);
        } catch (HttpTimeoutException e) {
            log.warn("Fault report agent timed out while calling DashScope");
            return new FaultReportChatResponse(AI_UNAVAILABLE_REPLY);
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
        result.append("[CURRENT TIME]\n")
                .append(LocalDateTime.now().format(BOOKING_TIME_FORMAT))
                .append(" local server time. Use this to resolve natural booking times.\n\n");

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

    private FaultReportChatResponse tryProcessFaultWithoutAi(String userMessage, List<FaultReportMessage> history) {
        if (!isFaultConversation(userMessage, history)) {
            return null;
        }

        FaultDraft draft = extractFaultDraft(userMessage, history);
        if (draft.scooterCode == null) {
            return new FaultReportChatResponse(localizedFaultReply(userMessage,
                    "Please provide the scooter code, for example SC001.",
                    "\u8bf7\u63d0\u4f9b\u8f66\u8f86\u7f16\u53f7\uff0c\u4f8b\u5982 SC001\u3002"));
        }
        if (draft.bookingId == null) {
            return new FaultReportChatResponse(localizedFaultReply(userMessage,
                    "Please provide the related booking/order ID.",
                    "\u8bf7\u63d0\u4f9b\u76f8\u5173\u8ba2\u5355 ID\u3002"));
        }
        if (!isMeaningfulFaultDescription(draft.faultDescription)) {
            return new FaultReportChatResponse(localizedFaultReply(userMessage,
                    "Please describe what is wrong with the scooter in a little more detail.",
                    "\u8bf7\u7a0d\u5fae\u8be6\u7ec6\u63cf\u8ff0\u4e00\u4e0b\u8f66\u8f86\u7684\u6545\u969c\u60c5\u51b5\u3002"));
        }

        try {
            log.info("Creating fault report via deterministic agent path: scooterCode={}, bookingId={}, description={}",
                    draft.scooterCode, draft.bookingId, draft.faultDescription);
            FeedbackIssue issue = feedbackIssueService.createIssueFromAgent(
                    draft.scooterCode, draft.bookingId, draft.faultDescription);

            FaultReportChatResponse.FaultReportIssueResult result =
                    new FaultReportChatResponse.FaultReportIssueResult(
                            issue.getId(),
                            issue.getScooterCode(),
                            issue.getBookingId(),
                            issue.getContent()
                    );

            String reply = localizedFaultReply(userMessage,
                    "Fault report submitted successfully. Your report ID is #" + issue.getId() + ".",
                    "\u6545\u969c\u5de5\u5355\u5df2\u63d0\u4ea4\u6210\u529f\uff0c\u5de5\u5355\u53f7 #" + issue.getId() + "\u3002");
            return new FaultReportChatResponse(reply, result);
        } catch (IllegalArgumentException e) {
            log.warn("Issue creation rejected: {}", e.getMessage());
            return new FaultReportChatResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Deterministic fault report creation failed", e);
            return new FaultReportChatResponse(localizedFaultReply(userMessage,
                    "I could not submit the fault report right now. Please try again or use Feedback manually.",
                    "\u6682\u65f6\u65e0\u6cd5\u63d0\u4ea4\u6545\u969c\u5de5\u5355\u3002\u8bf7\u91cd\u8bd5\uff0c\u6216\u4f7f\u7528\u624b\u52a8 Feedback\u3002"));
        }
    }

    private boolean shouldPreferBooking(String userMessage, List<FaultReportMessage> history) {
        return isBookingIntent(userMessage) || isBookingConversation(userMessage, history);
    }

    private boolean isAttemptingNewBooking(String userMessage, List<FaultReportMessage> history) {
        if (isBookingIntent(userMessage) || isBookingConversation(userMessage, history)) {
            return true;
        }
        return isBookingDetail(userMessage) && hasUserBookingIntent(history);
    }

    private boolean hasUserBookingIntent(List<FaultReportMessage> history) {
        return validHistory(history, BOOKING_CONTEXT_HISTORY_MESSAGES).stream()
                .filter(message -> "user".equals(message.getRole()))
                .map(FaultReportMessage::getContent)
                .anyMatch(this::isBookingIntent);
    }

    private boolean isFaultConversation(String userMessage, List<FaultReportMessage> history) {
        return isFaultIntent(userMessage) || hasFaultContext(history);
    }

    private boolean hasFaultContext(List<FaultReportMessage> history) {
        return validHistory(history, BOOKING_CONTEXT_HISTORY_MESSAGES).stream()
                .anyMatch(message -> {
                    String content = message.getContent();
                    if ("user".equals(message.getRole())) {
                        return isFaultIntent(content);
                    }
                    if ("assistant".equals(message.getRole())) {
                        return looksLikeFaultAssistantPrompt(content);
                    }
                    return false;
                });
    }

    private boolean looksLikeFaultAssistantPrompt(String content) {
        if (content == null) {
            return false;
        }
        String lower = content.toLowerCase();
        return (lower.contains("fault") || lower.contains("scooter") || lower.contains("issue"))
                && (lower.contains("scooter code")
                || lower.contains("booking id")
                || lower.contains("order id")
                || lower.contains("describe"));
    }

    private FaultDraft extractFaultDraft(String userMessage, List<FaultReportMessage> history) {
        String text = buildUserFaultText(userMessage, history);
        FaultDraft draft = new FaultDraft();

        Matcher scooterMatcher = SCOOTER_CODE_PATTERN.matcher(text);
        if (scooterMatcher.find()) {
            draft.scooterCode = scooterMatcher.group().toUpperCase();
        }

        Matcher bookingMatcher = BOOKING_ID_PATTERN.matcher(removeScooterCodes(text));
        if (bookingMatcher.find()) {
            try {
                draft.bookingId = Long.parseLong(bookingMatcher.group());
            } catch (NumberFormatException ignored) {
            }
        }

        draft.faultDescription = extractFaultDescription(text);
        return draft;
    }

    private String buildUserFaultText(String userMessage, List<FaultReportMessage> history) {
        StringBuilder sb = new StringBuilder();
        for (FaultReportMessage msg : validHistory(history, BOOKING_CONTEXT_HISTORY_MESSAGES)) {
            if ("user".equals(msg.getRole())) {
                sb.append(msg.getContent()).append('\n');
            }
        }
        sb.append(userMessage == null ? "" : userMessage);
        return sb.toString();
    }

    private String extractFaultDescription(String text) {
        return normalizeFaultDescription(text);
    }

    private String normalizeFaultDescription(String text) {
        if (text == null) {
            return "";
        }
        String description = removeScooterCodes(text)
                .replaceAll("(?i)\\b(booking|order|bookingId|id)\\s*#?\\s*\\d{1,10}\\b", " ")
                .replaceAll("\\b\\d{1,10}\\b", " ")
                .replaceAll("(?i)\\b(i\\s+(want|need)\\s+to\\s+report(\\s+a)?(\\s+scooter)?\\s*fault|i\\s+need\\s+to\\s+report\\s+a\\s+scooter\\s+fault|want\\s+to\\s+report(\\s+a)?\\s*fault|report(\\s+a)?\\s*fault|fault\\s+report|scooter\\s+fault)\\b", " ")
                .replaceAll("(我要报障|报告故障|故障上报|报障)", " ")
                .replaceAll("\\s+[,，。.!?]\\s*", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return description;
    }

    private boolean isMeaningfulFaultDescription(String description) {
        String normalized = normalizeFaultDescription(description);
        if (normalized.length() < MIN_FAULT_DESCRIPTION_LENGTH) {
            return false;
        }
        if (FAULT_INTENT_ONLY_PATTERN.matcher(normalized).matches()) {
            return false;
        }
        String lower = normalized.toLowerCase();
        return !(lower.equals("fault") || lower.equals("broken") || lower.equals("issue"));
    }

    private String removeScooterCodes(String text) {
        return text == null ? "" : SCOOTER_CODE_PATTERN.matcher(text).replaceAll(" ");
    }

    private String localizedFaultReply(String userMessage, String english, String chinese) {
        return containsChinese(userMessage) ? chinese : english;
    }

    private FaultReportChatResponse tryProcessBookingWithoutAi(String userMessage, List<FaultReportMessage> history) {
        if (isFaultIntent(userMessage)) {
            return null;
        }

        if (hasUnfinishedBooking() && isAttemptingNewBooking(userMessage, history)) {
            return new FaultReportChatResponse(duplicateBookingReply(userMessage));
        }

        if (!isBookingConversation(userMessage, history)) {
            return null;
        }

        BookingDraft draft = extractBookingDraft(userMessage, history);
        if (draft.appointmentStart == null) {
            return new FaultReportChatResponse(localizedBookingReply(userMessage,
                    "Please tell me the pickup date and clock time, for example \"tomorrow at 2pm\".",
                    "\u8bf7\u544a\u8bc9\u6211\u53d6\u8f66\u65e5\u671f\u548c\u5177\u4f53\u65f6\u95f4\uff0c\u4f8b\u5982\u201c\u660e\u5929\u4e0b\u5348\u4e24\u70b9\u201d\u3002"));
        }
        if (draft.hiredPeriod == null) {
            return new FaultReportChatResponse(localizedBookingReply(userMessage,
                    "How long would you like to rent it? You can choose 1 hour, 4 hours, 1 day, or 1 week.",
                    "\u4f60\u60f3\u79df\u591a\u4e45\uff1f\u53ef\u4ee5\u9009 1 \u5c0f\u65f6\u30014 \u5c0f\u65f6\u30011 \u5929\u6216 1 \u5468\u3002"));
        }
        if (draft.storeId == null) {
            return new FaultReportChatResponse(localizedBookingReply(userMessage,
                    "Which pickup store would you like to use?\n" + buildFriendlyStoreList(),
                    "\u4f60\u60f3\u5728\u54ea\u4e2a\u95e8\u5e97\u53d6\u8f66\uff1f\n" + buildFriendlyStoreList()));
        }

        return createBookingFromDraft(draft, userMessage);
    }

    private BookingDraft extractBookingDraft(String userMessage, List<FaultReportMessage> history) {
        String text = buildUserBookingText(userMessage, history);
        BookingDraft draft = new BookingDraft();
        draft.appointmentStart = parseNaturalAppointmentStart(text);
        draft.hiredPeriod = parseHirePeriod(text);
        draft.storeId = parseStoreId(text);
        return draft;
    }

    private String buildUserBookingText(String userMessage, List<FaultReportMessage> history) {
        StringBuilder sb = new StringBuilder();
        for (FaultReportMessage msg : validHistory(history, BOOKING_CONTEXT_HISTORY_MESSAGES)) {
            if ("user".equals(msg.getRole())) {
                sb.append(msg.getContent()).append('\n');
            }
        }
        sb.append(userMessage == null ? "" : userMessage);
        return sb.toString();
    }

    private FaultReportChatResponse createBookingFromDraft(BookingDraft draft, String userMessage) {
        String hiredPeriod = draft.hiredPeriod.toUpperCase();
        String appointmentStart = draft.appointmentStart.format(BOOKING_TIME_FORMAT);

        try {
            PricingPlan plan = pricingPlanMapper.selectOne(
                    new LambdaQueryWrapper<PricingPlan>().eq(PricingPlan::getHirePeriod, hiredPeriod));
            if (plan == null) {
                return new FaultReportChatResponse(localizedBookingReply(userMessage,
                        "That duration is not available. Please choose 1 hour, 4 hours, 1 day, or 1 week.",
                        "\u8fd9\u4e2a\u79df\u8f66\u65f6\u957f\u4e0d\u53ef\u7528\u3002\u8bf7\u9009 1 \u5c0f\u65f6\u30014 \u5c0f\u65f6\u30011 \u5929\u6216 1 \u5468\u3002"));
            }

            Store store = storeMapper.selectById(draft.storeId);
            if (store == null || !"ENABLED".equals(store.getStatus())) {
                return new FaultReportChatResponse(localizedBookingReply(userMessage,
                        "Store #" + draft.storeId + " is not available. Please choose another pickup store.",
                        "\u95e8\u5e97 #" + draft.storeId + " \u6682\u4e0d\u53ef\u7528\uff0c\u8bf7\u9009\u62e9\u5176\u4ed6\u53d6\u8f66\u95e8\u5e97\u3002"));
            }

            log.info("Creating booking via deterministic agent path: storeId={}, appointmentStart={}, hiredPeriod={}",
                    draft.storeId, draft.appointmentStart, hiredPeriod);

            Booking booking = bookingService.createStoreBooking(draft.storeId, draft.appointmentStart, hiredPeriod);
            log.info("Booking created via deterministic agent path: bookingId={}, storeId={}, appointmentStart={}, hiredPeriod={}",
                    booking.getId(), draft.storeId, draft.appointmentStart, hiredPeriod);

            FaultReportChatResponse.BookingResult result =
                    new FaultReportChatResponse.BookingResult(
                            booking.getId(),
                            store.getName(),
                            appointmentStart,
                            hiredPeriod
                    );

            String reply = localizedBookingReply(userMessage,
                    "Booking created successfully! Your booking ID is #" + booking.getId()
                            + ". Store: " + store.getName()
                            + ", Start: " + appointmentStart
                            + ", Duration: " + hiredPeriod + ".",
                    "\u9884\u8ba2\u6210\u529f\uff01\u8ba2\u5355\u53f7 #" + booking.getId()
                            + "\u3002\u95e8\u5e97\uff1a" + store.getName()
                            + "\uff0c\u5f00\u59cb\u65f6\u95f4\uff1a" + appointmentStart
                            + "\uff0c\u65f6\u957f\uff1a" + hiredPeriod + "\u3002");
            return new FaultReportChatResponse(reply, result, true);
        } catch (IllegalArgumentException e) {
            log.warn("Booking creation rejected: {}", e.getMessage());
            return new FaultReportChatResponse(localizedBookingReply(userMessage,
                    friendlyBookingRejection(e.getMessage()),
                    friendlyBookingRejection(e.getMessage())));
        } catch (Exception e) {
            log.error("Deterministic booking creation failed", e);
            return new FaultReportChatResponse(localizedBookingReply(userMessage,
                    "I could not create the booking right now. Please try again or use Reserve at a Store.",
                    "\u6682\u65f6\u65e0\u6cd5\u521b\u5efa\u9884\u8ba2\u3002\u8bf7\u91cd\u8bd5\uff0c\u6216\u4f7f\u7528\u624b\u52a8\u95e8\u5e97\u9884\u8ba2\u3002"));
        }
    }

    private String localizedBookingReply(String userMessage, String english, String chinese) {
        return containsChinese(userMessage) ? chinese : english;
    }

    private String buildFriendlyStoreList() {
        List<Store> stores = storeMapper.selectList(
                new LambdaQueryWrapper<Store>().eq(Store::getStatus, "ENABLED"));
        if (stores == null || stores.isEmpty()) {
            return "No pickup stores are currently available.";
        }
        StringBuilder sb = new StringBuilder();
        for (Store store : stores) {
            sb.append("Store ").append(store.getId()).append(": ").append(store.getName());
            if (store.getAddress() != null && !store.getAddress().isBlank()) {
                sb.append(" - ").append(store.getAddress());
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    private LocalDateTime parseNaturalAppointmentStart(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher explicitMatcher = EXPLICIT_BOOKING_TIME_PATTERN.matcher(text);
        if (explicitMatcher.find()) {
            return parseExplicitDateTime(explicitMatcher.group(1), explicitMatcher.group(2));
        }

        LocalDate date = resolveRelativeDate(text);
        if (date == null) {
            return null;
        }

        TimeParts time = resolveClockTime(text);
        if (time == null) {
            return null;
        }

        return date.atTime(time.hour, time.minute);
    }

    private LocalDateTime parseExplicitDateTime(String dateText, String timeText) {
        String[] dateParts = dateText.split("-");
        String[] timeParts = timeText.split(":");
        String normalized = String.format("%04d-%02d-%02d %02d:%02d",
                Integer.parseInt(dateParts[0]),
                Integer.parseInt(dateParts[1]),
                Integer.parseInt(dateParts[2]),
                Integer.parseInt(timeParts[0]),
                Integer.parseInt(timeParts[1]));
        return LocalDateTime.parse(normalized, BOOKING_TIME_FORMAT);
    }

    private LocalDate resolveRelativeDate(String text) {
        String lower = text.toLowerCase();
        LocalDate today = LocalDate.now();
        if (lower.contains("today") || text.contains("\u4eca\u5929")) {
            return today;
        }
        if (lower.contains("day after tomorrow") || text.contains("\u540e\u5929")) {
            return today.plusDays(2);
        }
        if (lower.contains("tomorrow") || text.contains("\u660e\u5929")) {
            return today.plusDays(1);
        }
        return null;
    }

    private TimeParts resolveClockTime(String text) {
        TimeParts chineseTime = resolveChineseClockTime(text);
        if (chineseTime != null) {
            return chineseTime;
        }

        Matcher clockMatcher = CLOCK_TIME_PATTERN.matcher(text);
        while (clockMatcher.find()) {
            String raw = clockMatcher.group();
            String suffix = clockMatcher.group(3);
            String before = text.substring(Math.max(0, clockMatcher.start() - 8), clockMatcher.start()).toLowerCase();
            String after = text.substring(clockMatcher.end(), Math.min(text.length(), clockMatcher.end() + 8)).toLowerCase();
            if (suffix == null && !raw.contains(":") && looksLikeDurationOrStore(before, after)) {
                continue;
            }
            if (suffix == null && !raw.contains(":") && !before.contains("at ") && !before.endsWith("at")) {
                continue;
            }
            int hour = Integer.parseInt(clockMatcher.group(1));
            int minute = clockMatcher.group(2) == null ? 0 : Integer.parseInt(clockMatcher.group(2));
            hour = adjustHourForMeridiem(hour, suffix, text);
            if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                return new TimeParts(hour, minute);
            }
        }
        return null;
    }

    private TimeParts resolveChineseClockTime(String text) {
        Pattern chineseClockPattern = Pattern.compile(
                "(?:(\\u4e0a\\u5348|\\u4e2d\\u5348|\\u4e0b\\u5348|\\u665a\\u4e0a|\\u65e9\\u4e0a)\\s*)?([0-9]{1,2}|\\u96f6|\\u4e00|\\u4e8c|\\u4e24|\\u4e09|\\u56db|\\u4e94|\\u516d|\\u4e03|\\u516b|\\u4e5d|\\u5341|\\u5341\\u4e00|\\u5341\\u4e8c)\\s*(?:\\u70b9|\\u65f6)(?:\\s*([0-9]{1,2})\\s*\\u5206)?"
        );
        Matcher matcher = chineseClockPattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        int hour = parseChineseNumber(matcher.group(2));
        int minute = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        String period = matcher.group(1);
        if (period != null && (period.equals("\u4e0b\u5348") || period.equals("\u665a\u4e0a")) && hour < 12) {
            hour += 12;
        }
        if (period != null && period.equals("\u4e2d\u5348") && hour < 11) {
            hour += 12;
        }
        if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
            return new TimeParts(hour, minute);
        }
        return null;
    }

    private boolean looksLikeDurationOrStore(String before, String after) {
        return before.contains("store")
                || after.contains("hour")
                || after.contains("day")
                || after.contains("week")
                || after.contains("\u5c0f\u65f6")
                || after.contains("\u5929")
                || after.contains("\u5468")
                || after.contains("\u53f7\u5e97")
                || after.contains("\u7f51\u70b9")
                || after.contains("\u95e8\u5e97");
    }

    private int adjustHourForMeridiem(int hour, String suffix, String text) {
        String lower = text.toLowerCase();
        if (suffix != null) {
            if ("pm".equalsIgnoreCase(suffix) && hour < 12) {
                return hour + 12;
            }
            if ("am".equalsIgnoreCase(suffix) && hour == 12) {
                return 0;
            }
            return hour;
        }
        if ((lower.contains("afternoon") || lower.contains("evening") || lower.contains("tonight")) && hour < 12) {
            return hour + 12;
        }
        return hour;
    }

    private String parseHirePeriod(String text) {
        if (text == null) {
            return null;
        }
        Matcher codeMatcher = HIRE_PERIOD_PATTERN.matcher(text);
        if (codeMatcher.find()) {
            return codeMatcher.group().toUpperCase();
        }
        String lower = text.toLowerCase();
        if (lower.matches("(?s).*(\\b4\\s*h\\b|\\b4\\s*hours?\\b|\\bfour\\s*hours?\\b|4\\s*\\u5c0f\\u65f6|\\u56db\\s*\\u5c0f\\u65f6).*")) {
            return "HOUR_4";
        }
        if (lower.matches("(?s).*(\\b1\\s*h\\b|\\b1\\s*hours?\\b|\\bone\\s*hours?\\b|1\\s*\\u5c0f\\u65f6|\\u4e00\\s*\\u5c0f\\u65f6).*")) {
            return "HOUR_1";
        }
        if (lower.matches("(?s).*(\\b1\\s*days?\\b|\\bone\\s*days?\\b|1\\s*\\u5929|\\u4e00\\s*\\u5929).*")) {
            return "DAY_1";
        }
        if (lower.matches("(?s).*(\\b1\\s*weeks?\\b|\\bone\\s*weeks?\\b|1\\s*\\u5468|\\u4e00\\s*\\u5468|\\u4e00\\s*\\u661f\\u671f).*")) {
            return "WEEK_1";
        }
        return null;
    }

    private Long parseStoreId(String text) {
        if (text == null) {
            return null;
        }
        Pattern storePattern = Pattern.compile(
                "(?:store|station|site|pickup\\s*store|\\u7f51\\u70b9|\\u95e8\\u5e97|\\u5e97)\\s*#?\\s*(\\d+)|(\\d+)\\s*(?:\\u53f7\\u5e97|\\u53f7\\u7f51\\u70b9|\\u53f7\\u95e8\\u5e97|\\u4e2a\\u7f51\\u70b9)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = storePattern.matcher(text);
        if (matcher.find()) {
            String id = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            return Long.parseLong(id);
        }
        String lower = text.toLowerCase();
        for (Store store : storeMapper.selectList(new LambdaQueryWrapper<Store>().eq(Store::getStatus, "ENABLED"))) {
            String lowerName = store.getName() == null ? "" : store.getName().toLowerCase();
            if (!lowerName.isBlank() && lower.contains(lowerName)) {
                return store.getId();
            }
            if ((lowerName.contains("north") && lower.contains("north"))
                    || (lowerName.contains("south") && lower.contains("south"))
                    || (lowerName.contains("student") && lower.contains("student"))) {
                return store.getId();
            }
        }
        return null;
    }

    private int parseChineseNumber(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }
        if (text.matches("\\d+")) {
            return Integer.parseInt(text);
        }
        return switch (text) {
            case "\u96f6" -> 0;
            case "\u4e00" -> 1;
            case "\u4e8c", "\u4e24" -> 2;
            case "\u4e09" -> 3;
            case "\u56db" -> 4;
            case "\u4e94" -> 5;
            case "\u516d" -> 6;
            case "\u4e03" -> 7;
            case "\u516b" -> 8;
            case "\u4e5d" -> 9;
            case "\u5341" -> 10;
            case "\u5341\u4e00" -> 11;
            case "\u5341\u4e8c" -> 12;
            default -> -1;
        };
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

    private boolean hasDashScopeConfig() {
        return apiKey != null && !apiKey.isBlank()
                && baseUrl != null && !baseUrl.isBlank()
                && model != null && !model.isBlank();
    }

    private boolean isBookingIntent(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        String lower = userMessage.toLowerCase();
        return BOOKING_INTENT_PATTERN.matcher(userMessage).find()
                || userMessage.contains("\u8ba2\u8f66")
                || userMessage.contains("\u9884\u8ba2")
                || userMessage.contains("\u9884\u5b9a")
                || userMessage.contains("\u9884\u7ea6")
                || userMessage.contains("\u79df\u8f66")
                || userMessage.contains("\u6211\u60f3\u79df")
                || lower.contains("book a scooter")
                || lower.contains("reserve")
                || lower.contains("reservation")
                || lower.contains("rent a scooter");
    }

    private boolean isBookingDetail(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        String lower = userMessage.toLowerCase();
        return BOOKING_DETAIL_PATTERN.matcher(userMessage).find()
                || userMessage.contains("\u4eca\u5929")
                || userMessage.contains("\u660e\u5929")
                || userMessage.contains("\u540e\u5929")
                || userMessage.contains("\u4e0a\u5348")
                || userMessage.contains("\u4e0b\u5348")
                || userMessage.contains("\u665a\u4e0a")
                || lower.contains("today")
                || lower.contains("tomorrow")
                || lower.contains("hour")
                || lower.contains("store")
                || lower.contains("pickup");
    }

    private boolean isFaultIntent(String userMessage) {
        if (userMessage == null) {
            return false;
        }
        return FAULT_INTENT_PATTERN.matcher(userMessage).find()
                || userMessage.contains("\u574f\u4e86")
                || userMessage.contains("\u6545\u969c")
                || userMessage.contains("\u6709\u95ee\u9898")
                || userMessage.contains("\u62a5\u4fee")
                || userMessage.contains("\u4e0d\u80fd\u542f\u52a8");
    }

    private boolean isBookingConversation(String userMessage, List<FaultReportMessage> history) {
        if (isBookingIntent(userMessage)) {
            return true;
        }
        boolean hasBookingHistory = hasBookingContext(history);
        if (isAffirmation(userMessage) && hasBookingHistory) {
            return true;
        }
        if (isBookingDetail(userMessage) && hasBookingHistory) {
            return true;
        }
        return parseNaturalAppointmentStart(userMessage) != null
                && parseHirePeriod(userMessage) != null
                && parseStoreId(userMessage) != null;
    }

    private boolean isAffirmation(String userMessage) {
        return userMessage != null && AFFIRMATION_PATTERN.matcher(userMessage.trim()).matches();
    }

    private boolean hasBookingHistory(List<FaultReportMessage> history) {
        return validHistory(history, BOOKING_CONTEXT_HISTORY_MESSAGES).stream()
                .filter(message -> "user".equals(message.getRole()))
                .map(FaultReportMessage::getContent)
                .anyMatch(this::isBookingIntent);
    }

    private boolean hasBookingContext(List<FaultReportMessage> history) {
        return validHistory(history, BOOKING_CONTEXT_HISTORY_MESSAGES).stream()
                .anyMatch(message -> {
                    String content = message.getContent();
                    if ("user".equals(message.getRole())) {
                        return isBookingIntent(content) || isBookingDetail(content);
                    }
                    if ("assistant".equals(message.getRole())) {
                        return looksLikeBookingAssistantPrompt(content);
                    }
                    return false;
                });
    }

    private boolean looksLikeBookingAssistantPrompt(String content) {
        if (content == null) {
            return false;
        }
        String lower = content.toLowerCase();
        return lower.contains("how long would you like to rent")
                || lower.contains("which pickup store would you like")
                || lower.contains("please tell me the pickup date and clock time")
                || lower.contains("please confirm these booking details")
                || lower.contains("please confirm your booking details")
                || lower.contains("\u4f60\u60f3\u79df\u591a\u4e45")
                || lower.contains("\u4f60\u60f3\u5728\u54ea\u4e2a\u95e8\u5e97\u53d6\u8f66")
                || lower.contains("\u8bf7\u544a\u8bc9\u6211\u53d6\u8f66\u65e5\u671f");
    }

    private List<FaultReportMessage> recentValidHistory(List<FaultReportMessage> history) {
        return validHistory(history, MAX_HISTORY_MESSAGES);
    }

    private List<FaultReportMessage> validHistory(List<FaultReportMessage> history, int limit) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<FaultReportMessage> valid = history.stream()
                .filter(message -> message != null)
                .filter(message -> "user".equals(message.getRole()) || "assistant".equals(message.getRole()))
                .filter(message -> message.getContent() != null && !message.getContent().isBlank())
                .toList();
        int fromIndex = Math.max(0, valid.size() - limit);
        return valid.subList(fromIndex, valid.size());
    }

    private boolean hasUnfinishedBooking() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }
        Long count = bookingMapper.selectCount(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getUserId, userId)
                        .in(Booking::getStatus,
                                RentalConstants.BOOKING_STATUS_RESERVED,
                                RentalConstants.BOOKING_STATUS_IN_PROGRESS,
                                RentalConstants.BOOKING_STATUS_OVERDUE,
                                RentalConstants.BOOKING_STATUS_AWAITING_PAYMENT));
        return count != null && count > 0;
    }

    private String duplicateBookingReply(String userMessage) {
        if (containsChinese(userMessage)) {
            return "你已有未完成订单，不能重复预定。请先到 My Orders 完成、取消或支付当前订单后，再创建新的预约。";
        }
        return "You already have an unfinished booking, so you cannot create another reservation yet. Please open My Orders to finish, cancel, or pay the current order first.";
    }

    private boolean containsChinese(String text) {
        return text != null && text.chars().anyMatch(ch -> ch >= 0x4E00 && ch <= 0x9FFF);
    }

    private String resolveSystemPrompt(String userMessage, List<FaultReportMessage> history) {
        if (isFaultConversation(userMessage, history)) {
            return FAULT_SYSTEM_PROMPT;
        }
        if (isBookingConversation(userMessage, history)
                || isBookingIntent(userMessage)
                || isBookingDetail(userMessage)
                || hasBookingContext(history)) {
            return BOOKING_SYSTEM_PROMPT;
        }
        return GENERAL_SYSTEM_PROMPT;
    }

    private List<Map<String, String>> buildMessages(String userMessage, List<FaultReportMessage> history, String validationContext) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", resolveSystemPrompt(userMessage, history));
        messages.add(systemMsg);

        for (FaultReportMessage msg : recentValidHistory(history)) {
            Map<String, String> histMsg = new LinkedHashMap<>();
            histMsg.put("role", msg.getRole());
            histMsg.put("content", msg.getContent().trim());
            messages.add(histMsg);
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
        body.put("temperature", 0.2);
        body.put("max_tokens", 500);

        String jsonBody = objectMapper.writeValueAsString(body);

        URI uri = URI.create(baseUrl.replaceAll("/+$", "") + "/chat/completions");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(DASHSCOPE_REQUEST_TIMEOUT)
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
        if (looksLikeUnverifiedBookingConfirmation(reply)) {
            log.warn("Blocked unverified booking confirmation from AI: {}", reply);
            return new FaultReportChatResponse(unverifiedBookingReply(reply));
        }
        return new FaultReportChatResponse(reply);
    }

    private boolean looksLikeUnverifiedBookingConfirmation(String reply) {
        return reply != null && UNVERIFIED_BOOKING_CONFIRMATION_PATTERN.matcher(reply).find();
    }

    private String unverifiedBookingReply(String reply) {
        if (containsChinese(reply)) {
            return "\u6211\u8fd8\u6ca1\u6709\u771f\u6b63\u521b\u5efa\u8ba2\u5355\u3002\u8bf7\u7ee7\u7eed\u63d0\u4f9b\u53d6\u8f66\u65f6\u95f4\u3001\u79df\u8f66\u65f6\u957f\u548c\u95e8\u5e97\uff0c\u6216\u8005\u4f7f\u7528\u624b\u52a8 Reserve at a Store\u3002\u53ea\u6709\u51fa\u73b0 Booking ID \u65f6\u624d\u8868\u793a\u9884\u8ba2\u6210\u529f\u3002";
        }
        return "I have not created a real booking yet. Please continue with the pickup time, rental duration, and store, or use Reserve at a Store. A reservation is only confirmed when a Booking ID is shown.";
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
            if (!isMeaningfulFaultDescription(faultDescription)) {
                log.warn("LLM returned fault description without concrete issue details: {}", faultDescription);
                return new FaultReportChatResponse(reply + "\n\n"
                        + localizedFaultReply(faultDescription,
                        "Please describe what is wrong with the scooter in a little more detail.",
                        "\u8bf7\u7a0d\u5fae\u8be6\u7ec6\u63cf\u8ff0\u4e00\u4e0b\u8f66\u8f86\u7684\u6545\u969c\u60c5\u51b5\u3002"));
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
            LocalDateTime appointmentTime;
            try {
                appointmentTime = parseAppointmentStart(appointmentStart);
            } catch (Exception e) {
                return new FaultReportChatResponse(reply + "\n\nI could not turn that start time into a concrete booking time. Please tell me the date and clock time naturally, for example \"tomorrow at 2pm\" .");
            }

            log.info("Creating booking via agent: storeId={}, appointmentStart={}, hiredPeriod={}",
                    storeId, appointmentTime, hiredPeriod.toUpperCase());

            Booking booking = bookingService.createStoreBooking(storeId, appointmentTime, hiredPeriod.toUpperCase());
            log.info("Booking created via agent: bookingId={}, storeId={}, appointmentStart={}, hiredPeriod={}",
                    booking.getId(), storeId, appointmentTime, hiredPeriod.toUpperCase());

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
            return new FaultReportChatResponse(reply + "\n\n" + friendlyBookingRejection(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to parse booking JSON or create booking: {}", jsonStr, e);
            return new FaultReportChatResponse(reply + "\n\n(There was an error creating your booking. Please try again. " + e.getMessage() + ")");
        }
    }

    private LocalDateTime parseAppointmentStart(String appointmentStart) {
        String normalized = appointmentStart.trim().replace('T', ' ');
        if (normalized.length() == 16) {
            return LocalDateTime.parse(normalized, BOOKING_TIME_FORMAT);
        }
        if (normalized.length() == 19) {
            return LocalDateTime.parse(normalized, BOOKING_TIME_WITH_SECONDS_FORMAT);
        }
        return LocalDateTime.parse(appointmentStart);
    }

    private String friendlyBookingRejection(String message) {
        if (message == null || message.isBlank()) {
            return "I could not create the booking. Please check your booking details and try again.";
        }
        if (message.toLowerCase().contains("unfinished booking")) {
            return "You already have an unfinished booking. Please open My Orders to finish, cancel, or pay that order before creating a new reservation.";
        }
        return message;
    }

    private static class BookingDraft {
        private LocalDateTime appointmentStart;
        private String hiredPeriod;
        private Long storeId;
    }

    private static class TimeParts {
        private final int hour;
        private final int minute;

        private TimeParts(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
        }
    }

    private static class FaultDraft {
        private String scooterCode;
        private Long bookingId;
        private String faultDescription;
    }
}
