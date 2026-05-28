package com.jva.ERP.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message Entity - Represents internal messaging/notifications in the system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages")
public class Message extends BaseEntity {

    @Column(name = "sender_id")          // nullable — NULL means system-generated message
    private Long senderId;

    @Column(name = "sender_name", nullable = false, length = 100)
    private String senderName;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "message_type", nullable = false, length = 30)
    private String messageType; // Notification, Alert, General, etc.

    @Column(name = "priority", nullable = false, length = 20)
    private String priority; // Low, Medium, High, Urgent

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private java.time.LocalDateTime readAt;

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;


    @Column(name = "attachment_url", length = 255)
    private String attachmentUrl;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType; // Employee, Payslip, Leave, etc.

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "sent_at", nullable = false)
    private java.time.LocalDateTime sentAt;

    @Override
    public String toString() {
        return "Message{" +
                "id=" + this.getId() +
                ", senderName='" + senderName + '\'' +
                ", receiverName='" + receiverName + '\'' +
                ", subject='" + subject + '\'' +
                ", priority='" + priority + '\'' +
                ", isRead=" + isRead +
                ", sentAt=" + sentAt +
                '}';
    }
}

