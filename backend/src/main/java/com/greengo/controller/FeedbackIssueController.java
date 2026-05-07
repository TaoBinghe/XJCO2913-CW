package com.greengo.controller;

import com.greengo.domain.FeedbackIssue;
import com.greengo.domain.FeedbackIssueCreateRequest;
import com.greengo.domain.Result;
import com.greengo.service.FeedbackIssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/feedback/issues")
public class FeedbackIssueController {

    @Autowired
    private FeedbackIssueService feedbackIssueService;

    @PostMapping
    public Result<?> createIssue(@RequestBody FeedbackIssueCreateRequest request) {
        try {
            FeedbackIssue issue = feedbackIssueService.createIssue(request);
            return Result.success(issue);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/my")
    public Result<?> listMyIssues(@RequestParam(required = false) Long bookingId) {
        try {
            List<FeedbackIssue> issues = feedbackIssueService.listMyIssues(bookingId);
            return Result.success(issues);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
