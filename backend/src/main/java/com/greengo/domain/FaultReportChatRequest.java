package com.greengo.domain;

import lombok.Data;

import java.util.List;

@Data
public class FaultReportChatRequest {
    private String message;
    private List<FaultReportMessage> history;
}
