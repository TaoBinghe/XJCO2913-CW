package com.greengo.controller;

import com.greengo.domain.FaultReportChatRequest;
import com.greengo.domain.FaultReportChatResponse;
import com.greengo.domain.Result;
import com.greengo.service.FaultReportAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fault-report")
public class FaultReportController {

    @Autowired
    private FaultReportAgentService faultReportAgentService;

    @PostMapping("/chat")
    public Result<?> chat(@RequestBody FaultReportChatRequest request) {
        try {
            if (request == null) {
                return Result.error("Message is required");
            }
            FaultReportChatResponse response = faultReportAgentService.processMessage(
                    request.getMessage(),
                    request.getHistory() != null ? request.getHistory() : List.of()
            );
            return Result.success(response);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
