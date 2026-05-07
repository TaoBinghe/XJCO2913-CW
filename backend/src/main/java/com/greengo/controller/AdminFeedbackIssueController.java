package com.greengo.controller;

import com.greengo.domain.AdminFeedbackIssueUpdateRequest;
import com.greengo.domain.FeedbackIssue;
import com.greengo.domain.Result;
import com.greengo.service.FeedbackIssueService;
import com.greengo.utils.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/feedback/issues")
public class AdminFeedbackIssueController {

    @Autowired
    private FeedbackIssueService feedbackIssueService;

    @GetMapping
    public Result<?> listIssues(@RequestParam(required = false) String priority,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String keyword) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        try {
            List<FeedbackIssue> issues = feedbackIssueService.listAdminIssues(priority, status, keyword);
            return Result.success(issues);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/high-priority")
    public Result<?> listHighPriorityIssues() {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        return Result.success(feedbackIssueService.listHighPriorityIssues());
    }

    @PutMapping("/{id}")
    public Result<?> updateIssue(@PathVariable Long id,
                                 @RequestBody AdminFeedbackIssueUpdateRequest request) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        try {
            FeedbackIssue issue = feedbackIssueService.updateAdminIssue(id, request);
            return Result.success(issue);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
