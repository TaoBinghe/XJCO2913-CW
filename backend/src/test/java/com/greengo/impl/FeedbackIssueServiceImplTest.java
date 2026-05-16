package com.greengo.impl;

import com.greengo.domain.Booking;
import com.greengo.domain.FeedbackIssue;
import com.greengo.domain.Scooter;
import com.greengo.mapper.BookingMapper;
import com.greengo.mapper.FeedbackIssueMapper;
import com.greengo.mapper.ScooterMapper;
import com.greengo.mapper.UserMapper;
import com.greengo.service.impl.FeedbackIssueServiceImpl;
import com.greengo.utils.ThreadLocalUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackIssueServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long BOOKING_ID = 10L;
    private static final Long SCOOTER_ID = 3L;

    @Mock
    private FeedbackIssueMapper feedbackIssueMapper;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private ScooterMapper scooterMapper;

    @Mock
    private UserMapper userMapper;

    private FeedbackIssueServiceImpl feedbackIssueService;

    @BeforeEach
    void setUp() {
        feedbackIssueService = new FeedbackIssueServiceImpl();
        ReflectionTestUtils.setField(feedbackIssueService, "feedbackIssueMapper", feedbackIssueMapper);
        ReflectionTestUtils.setField(feedbackIssueService, "bookingMapper", bookingMapper);
        ReflectionTestUtils.setField(feedbackIssueService, "scooterMapper", scooterMapper);
        ReflectionTestUtils.setField(feedbackIssueService, "userMapper", userMapper);
        ThreadLocalUtil.set(Map.of("id", USER_ID));
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void createIssueFromAgentCreatesScooterFaultIssue() {
        Booking booking = ownedBooking();
        Scooter scooter = scooter();
        AtomicReference<FeedbackIssue> storedIssue = new AtomicReference<>();

        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(booking);
        when(scooterMapper.selectOne(any())).thenReturn(scooter);
        when(feedbackIssueMapper.insert(any(FeedbackIssue.class))).thenAnswer(invocation -> {
            FeedbackIssue issue = invocation.getArgument(0);
            issue.setId(99L);
            storedIssue.set(issue);
            return 1;
        });
        when(feedbackIssueMapper.selectById(99L)).thenAnswer(invocation -> storedIssue.get());
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());
        when(bookingMapper.selectBatchIds(any())).thenReturn(List.of(booking));
        when(scooterMapper.selectBatchIds(any())).thenReturn(List.of(scooter));

        FeedbackIssue issue = feedbackIssueService.createIssueFromAgent(
                " SC001 ",
                BOOKING_ID,
                "Brake is broken during the ride"
        );

        assertEquals(99L, issue.getId());
        assertEquals(USER_ID, issue.getUserId());
        assertEquals(BOOKING_ID, issue.getBookingId());
        assertEquals(SCOOTER_ID, issue.getScooterId());
        assertEquals("SCOOTER_FAULT", issue.getCategory());
        assertEquals("HIGH", issue.getPriority());
        assertEquals("OPEN", issue.getStatus());
        assertEquals("SC001", issue.getScooterCode());
    }

    @Test
    void createIssueFromAgentRejectsBookingOwnedByAnotherUser() {
        Booking booking = ownedBooking();
        booking.setUserId(2L);
        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(booking);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> feedbackIssueService.createIssueFromAgent(
                        "SC001",
                        BOOKING_ID,
                        "Brake is broken"
                ));

        assertEquals("该订单不属于您，请检查订单编号是否正确", error.getMessage());
        verify(feedbackIssueMapper, never()).insert(any(FeedbackIssue.class));
    }

    @Test
    void createIssueFromAgentRejectsUnknownScooterCode() {
        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(ownedBooking());
        when(scooterMapper.selectOne(any())).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> feedbackIssueService.createIssueFromAgent(
                        "SC404",
                        BOOKING_ID,
                        "Brake is broken"
                ));

        assertEquals("未找到车辆编码为 SC404 的车辆，请检查后重新输入", error.getMessage());
        verify(feedbackIssueMapper, never()).insert(any(FeedbackIssue.class));
    }

    @Test
    void createIssueFromAgentRejectsShortFaultDescription() {
        when(bookingMapper.selectById(BOOKING_ID)).thenReturn(ownedBooking());
        when(scooterMapper.selectOne(any())).thenReturn(scooter());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> feedbackIssueService.createIssueFromAgent(
                        "SC001",
                        BOOKING_ID,
                        "bad"
                ));

        assertEquals("故障描述不能少于5个字符，请详细描述您遇到的问题", error.getMessage());
        verify(feedbackIssueMapper, never()).insert(any(FeedbackIssue.class));
    }

    private Booking ownedBooking() {
        return Booking.builder()
                .id(BOOKING_ID)
                .userId(USER_ID)
                .scooterId(SCOOTER_ID)
                .build();
    }

    private Scooter scooter() {
        return Scooter.builder()
                .id(SCOOTER_ID)
                .scooterCode("SC001")
                .build();
    }
}
