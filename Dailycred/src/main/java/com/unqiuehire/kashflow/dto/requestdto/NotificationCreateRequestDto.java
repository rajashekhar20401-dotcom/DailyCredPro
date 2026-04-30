package com.unqiuehire.kashflow.dto.requestdto;

import com.unqiuehire.kashflow.constant.NotificationChannelType;
import com.unqiuehire.kashflow.constant.NotificationTargetType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationCreateRequestDto {
    private NotificationTargetType targetType;
    private Long targetId;
    private NotificationChannelType channelType;
    private String title;
    private String message;
}