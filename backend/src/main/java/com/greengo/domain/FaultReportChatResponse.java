package com.greengo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FaultReportChatResponse {
    private String reply;
    private FaultReportIssueResult issue;

    public FaultReportChatResponse(String reply) {
        this.reply = reply;
        this.issue = null;
    }

    public FaultReportChatResponse(String reply, FaultReportIssueResult issue) {
        this.reply = reply;
        this.issue = issue;
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
}
