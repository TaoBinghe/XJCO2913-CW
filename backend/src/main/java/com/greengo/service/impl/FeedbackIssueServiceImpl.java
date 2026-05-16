package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.greengo.domain.AdminFeedbackIssueUpdateRequest;
import com.greengo.domain.Booking;
import com.greengo.domain.FeedbackIssue;
import com.greengo.domain.FeedbackIssueCreateRequest;
import com.greengo.domain.Scooter;
import com.greengo.domain.User;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.FeedbackIssueMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.mapper.UserMapper;
import com.greengo.service.FeedbackIssueService;
import com.greengo.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FeedbackIssueServiceImpl implements FeedbackIssueService {

    private static final String CATEGORY_SCOOTER_FAULT = "SCOOTER_FAULT";
    private static final String CATEGORY_BOOKING = "BOOKING";
    private static final String CATEGORY_PAYMENT = "PAYMENT";
    private static final String CATEGORY_OTHER = "OTHER";

    private static final String PRIORITY_LOW = "LOW";
    private static final String PRIORITY_HIGH = "HIGH";

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_RESOLVED = "RESOLVED";

    private static final Set<String> VALID_CATEGORIES = Set.of(
            CATEGORY_SCOOTER_FAULT,
            CATEGORY_BOOKING,
            CATEGORY_PAYMENT,
            CATEGORY_OTHER
    );
    private static final Set<String> VALID_PRIORITIES = Set.of(PRIORITY_LOW, PRIORITY_HIGH);
    private static final Set<String> VALID_STATUSES = Set.of(STATUS_OPEN, STATUS_IN_PROGRESS, STATUS_RESOLVED);
    private static final List<String> HIGH_PRIORITY_KEYWORDS = List.of(
            "brake",
            "broken",
            "crash",
            "accident",
            "injury",
            "danger",
            "smoke",
            "fire",
            "battery",
            "lock",
            "cannot stop",
            "won't stop",
            "unsafe",
            "刹车",
            "事故",
            "受伤",
            "危险",
            "冒烟",
            "着火",
            "电池",
            "锁",
            "失控"
    );

    @Autowired
    private FeedbackIssueMapper feedbackIssueMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private ScooterMapper scooterMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private Clock clock = Clock.systemDefaultZone();

    @Override
    @Transactional
    public FeedbackIssue createIssue(FeedbackIssueCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Feedback request is missing");
        }

        Long userId = currentUserId();
        Long bookingId = request.getBookingId();
        if (bookingId == null) {
            throw new IllegalArgumentException("Booking id is required");
        }

        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!Objects.equals(booking.getUserId(), userId)) {
            throw new IllegalArgumentException("Not your booking");
        }

        String category = normalizeCategory(request.getCategory());
        String content = normalizeContent(request.getContent());
        String priority = resolvePriority(content);

        FeedbackIssue issue = FeedbackIssue.builder()
                .userId(userId)
                .bookingId(booking.getId())
                .scooterId(booking.getScooterId())
                .category(category)
                .content(content)
                .priority(priority)
                .status(STATUS_OPEN)
                .resolvedAt(null)
                .build();

        if (feedbackIssueMapper.insert(issue) <= 0) {
            throw new IllegalArgumentException("Failed to submit feedback");
        }
        return getIssueDetail(issue.getId());
    }

    @Override
    @Transactional
    public FeedbackIssue createIssueFromAgent(String scooterCode, Long bookingId, String faultDescription) {
        Long userId = currentUserId();

        if (bookingId == null) {
            throw new IllegalArgumentException("Booking ID is required. Please provide your booking number.");
        }
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found. Please check and re-enter the booking ID.");
        }
        if (!Objects.equals(booking.getUserId(), userId)) {
            throw new IllegalArgumentException("This booking does not belong to you. Please check and re-enter.");
        }

        if (scooterCode == null || scooterCode.trim().isBlank()) {
            throw new IllegalArgumentException("Scooter code is required. Please provide the scooter code.");
        }
        Scooter scooter = scooterMapper.selectOne(
                new LambdaQueryWrapper<Scooter>().eq(Scooter::getScooterCode, scooterCode.trim())
        );
        if (scooter == null) {
            throw new IllegalArgumentException("Scooter code " + scooterCode.trim() + " not found. Please check and re-enter.");
        }

        if (faultDescription == null || faultDescription.trim().length() < 5) {
            throw new IllegalArgumentException("Fault description must be at least 5 characters. Please describe the issue in more detail.");
        }
        String content = faultDescription.trim();
        if (content.length() > 500) {
            content = content.substring(0, 500);
        }

        String priority = resolvePriority(content);

        FeedbackIssue issue = FeedbackIssue.builder()
                .userId(userId)
                .bookingId(booking.getId())
                .scooterId(scooter.getId())
                .category(CATEGORY_SCOOTER_FAULT)
                .content(content)
                .priority(priority)
                .status(STATUS_OPEN)
                .resolvedAt(null)
                .build();

        if (feedbackIssueMapper.insert(issue) <= 0) {
            throw new IllegalArgumentException("Failed to submit fault report. Please try again.");
        }
        return getIssueDetail(issue.getId());
    }

    @Override
    public List<FeedbackIssue> listMyIssues(Long bookingId) {
        Long userId = currentUserId();
        QueryWrapper<FeedbackIssue> wrapper = new QueryWrapper<FeedbackIssue>()
                .eq("user_id", userId)
                .orderByDesc("created_at");
        if (bookingId != null) {
            wrapper.eq("booking_id", bookingId);
        }
        List<FeedbackIssue> issues = feedbackIssueMapper.selectList(wrapper);
        enrichIssues(issues);
        return issues;
    }

    @Override
    public List<FeedbackIssue> listAdminIssues(String priority, String status, String keyword) {
        QueryWrapper<FeedbackIssue> wrapper = new QueryWrapper<>();
        String normalizedPriority = normalizeOptionalPriority(priority);
        String normalizedStatus = normalizeOptionalStatus(status);
        String normalizedKeyword = normalizeOptionalText(keyword, 100, "Keyword must be 100 characters or fewer");

        if (normalizedPriority != null) {
            wrapper.eq("priority", normalizedPriority);
        }
        if (normalizedStatus != null) {
            wrapper.eq("status", normalizedStatus);
        }
        if (normalizedKeyword != null) {
            wrapper.and(query -> query
                    .like("content", normalizedKeyword)
                    .or()
                    .like("category", normalizedKeyword));
        }
        wrapper.orderByDesc("created_at");

        List<FeedbackIssue> issues = feedbackIssueMapper.selectList(wrapper);
        enrichIssues(issues);
        return issues;
    }

    @Override
    public List<FeedbackIssue> listHighPriorityIssues() {
        return listAdminIssues(PRIORITY_HIGH, null, null);
    }

    @Override
    @Transactional
    public FeedbackIssue updateAdminIssue(Long issueId, AdminFeedbackIssueUpdateRequest request) {
        if (issueId == null) {
            throw new IllegalArgumentException("Feedback issue id is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Feedback update request is missing");
        }

        FeedbackIssue issue = feedbackIssueMapper.selectById(issueId);
        if (issue == null) {
            throw new IllegalArgumentException("Feedback issue not found");
        }

        String priority = normalizeOptionalPriority(request.getPriority());
        String status = normalizeOptionalStatus(request.getStatus());
        String resolutionNote = request.getResolutionNote() == null
                ? null
                : normalizeOptionalText(request.getResolutionNote(), 500, "Resolution note must be 500 characters or fewer");

        if (priority != null) {
            issue.setPriority(priority);
        }
        if (status != null) {
            issue.setStatus(status);
            issue.setResolvedAt(STATUS_RESOLVED.equals(status) ? LocalDateTime.now(clock) : null);
        }
        if (request.getResolutionNote() != null) {
            issue.setResolutionNote(resolutionNote);
        }
        issue.setHandledByUserId(currentUserId());

        if (feedbackIssueMapper.updateById(issue) <= 0) {
            throw new IllegalArgumentException("Failed to update feedback issue");
        }
        return getIssueDetail(issueId);
    }

    private FeedbackIssue getIssueDetail(Long issueId) {
        FeedbackIssue issue = feedbackIssueMapper.selectById(issueId);
        if (issue == null) {
            throw new IllegalArgumentException("Feedback issue not found");
        }
        enrichIssues(List.of(issue));
        return issue;
    }

    private Long currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || claims.get("id") == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        return ((Number) claims.get("id")).longValue();
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Feedback category is required");
        }
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if (!VALID_CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid feedback category");
        }
        return normalized;
    }

    private String normalizeContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException("Feedback content is required");
        }
        String normalized = content.trim();
        if (normalized.length() < 5 || normalized.length() > 500) {
            throw new IllegalArgumentException("Feedback content must be 5 to 500 characters");
        }
        return normalized;
    }

    private String normalizeOptionalPriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        String normalized = priority.trim().toUpperCase(Locale.ROOT);
        if (!VALID_PRIORITIES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid feedback priority");
        }
        return normalized;
    }

    private String normalizeOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid feedback status");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, int maxLength, String lengthMessage) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(lengthMessage);
        }
        return normalized;
    }

    private String resolvePriority(String content) {
        String normalized = content.toLowerCase(Locale.ROOT);
        for (String keyword : HIGH_PRIORITY_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return PRIORITY_HIGH;
            }
        }
        return PRIORITY_LOW;
    }

    private void enrichIssues(List<FeedbackIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return;
        }

        Map<Long, User> userMap = loadUsers(issues);
        Map<Long, Booking> bookingMap = loadBookings(issues);
        Map<Long, Scooter> scooterMap = loadScooters(issues);

        for (FeedbackIssue issue : issues) {
            User user = issue.getUserId() == null ? null : userMap.get(issue.getUserId());
            if (user != null) {
                issue.setUsername(user.getUsername());
                issue.setUserEmail(user.getEmail());
            }

            User handler = issue.getHandledByUserId() == null ? null : userMap.get(issue.getHandledByUserId());
            if (handler != null) {
                issue.setHandledByUsername(handler.getUsername());
            }

            Booking booking = issue.getBookingId() == null ? null : bookingMap.get(issue.getBookingId());
            if (booking != null) {
                issue.setBookingStatus(booking.getStatus());
                issue.setRentalType(booking.getRentalType());
            }

            Scooter scooter = issue.getScooterId() == null ? null : scooterMap.get(issue.getScooterId());
            if (scooter != null) {
                issue.setScooterCode(scooter.getScooterCode());
            }
        }
    }

    private Map<Long, User> loadUsers(List<FeedbackIssue> issues) {
        List<Long> userIds = issues.stream()
                .flatMap(issue -> Stream.of(issue.getUserId(), issue.getHandledByUserId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private Map<Long, Booking> loadBookings(List<FeedbackIssue> issues) {
        List<Long> bookingIds = issues.stream()
                .map(FeedbackIssue::getBookingId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (bookingIds.isEmpty()) {
            return Map.of();
        }
        return bookingMapper.selectBatchIds(bookingIds).stream()
                .collect(Collectors.toMap(Booking::getId, booking -> booking));
    }

    private Map<Long, Scooter> loadScooters(List<FeedbackIssue> issues) {
        List<Long> scooterIds = issues.stream()
                .map(FeedbackIssue::getScooterId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (scooterIds.isEmpty()) {
            return Map.of();
        }
        return scooterMapper.selectBatchIds(scooterIds).stream()
                .collect(Collectors.toMap(Scooter::getId, scooter -> scooter));
    }
}
