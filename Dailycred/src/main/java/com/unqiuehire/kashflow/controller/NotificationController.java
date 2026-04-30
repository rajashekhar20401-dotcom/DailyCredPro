package com.unqiuehire.kashflow.controller;

import com.unqiuehire.kashflow.constant.NotificationChannelType;
import com.unqiuehire.kashflow.constant.NotificationTargetType;
import com.unqiuehire.kashflow.dto.requestdto.NotificationCreateRequestDto;
import com.unqiuehire.kashflow.dto.responsedto.NotificationResponseDto;
import com.unqiuehire.kashflow.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public NotificationResponseDto createNotification(@RequestBody NotificationCreateRequestDto requestDto) {
        return notificationService.createNotification(
                requestDto.getTargetType(),
                requestDto.getTargetId(),
                requestDto.getChannelType() == null ? NotificationChannelType.IN_APP : requestDto.getChannelType(),
                requestDto.getTitle(),
                requestDto.getMessage()
        );
    }

    @GetMapping("/{targetType}/{targetId}")
    public List<NotificationResponseDto> getNotifications(@PathVariable NotificationTargetType targetType,
                                                          @PathVariable Long targetId) {
        return notificationService.getNotifications(targetType, targetId);
    }

    @PostMapping("/{notificationId}/read")
    public NotificationResponseDto markAsRead(@PathVariable Long notificationId) {
        return notificationService.markAsRead(notificationId);
    }
}