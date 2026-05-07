package com.greengo.domain;

import lombok.Data;

@Data
public class FeedbackIssueCreateRequest {

    private Long bookingId;

    private String category;

    private String content;
}
