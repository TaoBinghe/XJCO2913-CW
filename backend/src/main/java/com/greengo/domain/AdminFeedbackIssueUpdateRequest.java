package com.greengo.domain;

import lombok.Data;

@Data
public class AdminFeedbackIssueUpdateRequest {

    private String priority;

    private String status;

    private String resolutionNote;
}
