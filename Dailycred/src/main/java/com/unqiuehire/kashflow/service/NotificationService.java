package com.unqiuehire.kashflow.service;

import com.unqiuehire.kashflow.constant.NotificationChannelType;
import com.unqiuehire.kashflow.constant.NotificationTargetType;
import com.unqiuehire.kashflow.dto.responsedto.NotificationResponseDto;

import java.util.List;

public interface NotificationService {
    NotificationResponseDto createNotification(NotificationTargetType targetType, Long targetId, NotificationChannelType channelType, String title, String message);
    List<NotificationResponseDto> getNotifications(NotificationTargetType targetType, Long targetId);
    NotificationResponseDto markAsRead(Long notificationId);
}