package com.greengo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FaultReportChatResponse {
    private String reply;
    private FaultReportIssueResult issue;
    private BookingResult booking;

    public FaultReportChatResponse(String reply) {
        this.reply = reply;
    }

    public FaultReportChatResponse(String reply, FaultReportIssueResult issue) {
        this.reply = reply;
        this.issue = issue;
    }

    public FaultReportChatResponse(String reply, BookingResult booking, boolean unused) {
        this.reply = reply;
        this.booking = booking;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaultReportIssueResult {
        private Long issueId;
        private String scooterCode;
        private Long bookingId;
        private String faultDescription;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingResult {
        private Long bookingId;
        private String storeName;
        private String appointmentStart;
        private String hiredPeriod;
    }
}
