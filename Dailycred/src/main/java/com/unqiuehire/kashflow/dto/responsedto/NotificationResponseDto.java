package com.unqiuehire.kashflow.dto.responsedto;

import com.unqiuehire.kashflow.constant.NotificationChannelType;
import com.unqiuehire.kashflow.constant.NotificationStatus;
import com.unqiuehire.kashflow.constant.NotificationTargetType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationResponseDto {
    private Long notificationId;
    private NotificationTargetType targetType;
    private Long targetId;
    private NotificationChannelType channelType;
    private String title;
    private String message;
    private NotificationStatus status;
    private Boolean readFlag;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}