package com.unqiuehire.kashflow.repository;

import com.unqiuehire.kashflow.constant.NotificationTargetType;
import com.unqiuehire.kashflow.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(NotificationTargetType targetType, Long targetId);
}