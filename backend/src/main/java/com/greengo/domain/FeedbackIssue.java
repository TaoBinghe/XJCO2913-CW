package com.greengo.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("feedback_issue")
public class FeedbackIssue {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookingId;

    private Long scooterId;

    private String category;

    private String content;

    private String priority;

    private String status;

    private String resolutionNote;

    private Long handledByUserId;

    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String userEmail;

    @TableField(exist = false)
    private String bookingStatus;

    @TableField(exist = false)
    private String rentalType;

    @TableField(exist = false)
    private String scooterCode;

    @TableField(exist = false)
    private String handledByUsername;
}
