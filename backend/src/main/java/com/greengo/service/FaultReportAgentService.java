package com.greengo.service;

import com.greengo.domain.FaultReportChatResponse;
import com.greengo.domain.FaultReportMessage;

import java.util.List;

public interface FaultReportAgentService {
    FaultReportChatResponse processMessage(String userMessage, List<FaultReportMessage> history);
}
