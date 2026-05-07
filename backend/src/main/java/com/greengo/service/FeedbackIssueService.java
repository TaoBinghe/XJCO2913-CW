package com.greengo.service;

import com.greengo.domain.AdminFeedbackIssueUpdateRequest;
import com.greengo.domain.FeedbackIssue;
import com.greengo.domain.FeedbackIssueCreateRequest;

import java.util.List;

public interface FeedbackIssueService {

    FeedbackIssue createIssue(FeedbackIssueCreateRequest request);

    List<FeedbackIssue> listMyIssues(Long bookingId);

    List<FeedbackIssue> listAdminIssues(String priority, String status, String keyword);

    List<FeedbackIssue> listHighPriorityIssues();

    FeedbackIssue updateAdminIssue(Long issueId, AdminFeedbackIssueUpdateRequest request);
}
