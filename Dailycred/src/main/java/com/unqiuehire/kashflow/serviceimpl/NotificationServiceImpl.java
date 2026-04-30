package com.unqiuehire.kashflow.serviceimpl;

import com.unqiuehire.kashflow.constant.NotificationChannelType;
import com.unqiuehire.kashflow.constant.NotificationStatus;
import com.unqiuehire.kashflow.constant.NotificationTargetType;
import com.unqiuehire.kashflow.dto.responsedto.NotificationResponseDto;
import com.unqiuehire.kashflow.entity.AppNotification;
import com.unqiuehire.kashflow.repository.AppNotificationRepository;
import com.unqiuehire.kashflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final AppNotificationRepository appNotificationRepository;

    @Override
    @Transactional
    public NotificationResponseDto createNotification(NotificationTargetType targetType, Long targetId, NotificationChannelType channelType, String title, String message) {
        AppNotification notification = new AppNotification();
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setChannelType(channelType);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setStatus(NotificationStatus.CREATED);
        notification.setReadFlag(false);
        notification.setCreatedAt(LocalDateTime.now());

        AppNotification saved = appNotificationRepository.save(notification);

        // for current MVP, mark IN_APP as SENT immediately
        if (channelType == NotificationChannelType.IN_APP) {
            saved.setStatus(NotificationStatus.SENT);
            saved.setSentAt(LocalDateTime.now());
            saved = appNotificationRepository.save(saved);
        }

        return map(saved);
    }

    @Override
    public List<NotificationResponseDto> getNotifications(NotificationTargetType targetType, Long targetId) {
        return appNotificationRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponseDto markAsRead(Long notificationId) {
        AppNotification notification = appNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setReadFlag(true);
        notification.setReadAt(LocalDateTime.now());
        notification.setStatus(NotificationStatus.READ);

        return map(appNotificationRepository.save(notification));
    }

    private NotificationResponseDto map(AppNotification notification) {
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.setNotificationId(notification.getNotificationId());
        dto.setTargetType(notification.getTargetType());
        dto.setTargetId(notification.getTargetId());
        dto.setChannelType(notification.getChannelType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setStatus(notification.getStatus());
        dto.setReadFlag(notification.getReadFlag());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setSentAt(notification.getSentAt());
        dto.setReadAt(notification.getReadAt());
        return dto;
    }
}