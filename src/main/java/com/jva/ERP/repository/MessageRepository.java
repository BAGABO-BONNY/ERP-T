package com.jva.ERP.repository;

import com.jva.ERP.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Message Repository
 * Provides CRUD operations and custom query methods for Message entity
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find messages by receiver ID
     */
    List<Message> findByReceiverId(Long receiverId);

    /**
     * Find messages by sender ID
     */
    List<Message> findBySenderId(Long senderId);

    /**
     * Find unread messages for a receiver
     */
    @Query("SELECT m FROM Message m WHERE m.receiverId = :receiverId AND m.isRead = false ORDER BY m.sentAt DESC")
    List<Message> findUnreadMessagesByReceiver(@Param("receiverId") Long receiverId);

    /**
     * Find read messages for a receiver
     */
    @Query("SELECT m FROM Message m WHERE m.receiverId = :receiverId AND m.isRead = true ORDER BY m.sentAt DESC")
    List<Message> findReadMessagesByReceiver(@Param("receiverId") Long receiverId);

    /**
     * Count unread messages for a receiver
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiverId = :receiverId AND m.isRead = false")
    long countUnreadMessagesByReceiver(@Param("receiverId") Long receiverId);

    /**
     * Find messages between sender and receiver
     */
    @Query("SELECT m FROM Message m WHERE (m.senderId = :senderId AND m.receiverId = :receiverId) " +
            "OR (m.senderId = :receiverId AND m.receiverId = :senderId) ORDER BY m.sentAt DESC")
    List<Message> findConversation(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    /**
     * Find messages by priority
     */
    List<Message> findByPriority(String priority);

    /**
     * Find urgent messages
     */
    @Query("SELECT m FROM Message m WHERE m.priority = 'Urgent' AND m.isRead = false ORDER BY m.sentAt DESC")
    List<Message> findUrgentUnreadMessages();

    /**
     * Find urgent unread messages for a receiver
     */
    @Query("SELECT m FROM Message m WHERE m.receiverId = :receiverId AND m.priority = 'Urgent' AND m.isRead = false ORDER BY m.sentAt DESC")
    List<Message> findUrgentUnreadMessagesForReceiver(@Param("receiverId") Long receiverId);

    /**
     * Find messages by message type
     */
    List<Message> findByMessageType(String messageType);

    /**
     * Find archived messages for a receiver
     */
    @Query("SELECT m FROM Message m WHERE m.receiverId = :receiverId AND m.isArchived = true ORDER BY m.sentAt DESC")
    List<Message> findArchivedMessagesByReceiver(@Param("receiverId") Long receiverId);

    /**
     * Find messages by date range
     */
    @Query("SELECT m FROM Message m WHERE m.sentAt BETWEEN :startDate AND :endDate ORDER BY m.sentAt DESC")
    List<Message> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find messages by subject or content
     */
    @Query("SELECT m FROM Message m WHERE LOWER(m.subject) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.message) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY m.sentAt DESC")
    List<Message> searchMessages(@Param("search") String search);

    /**
     * Find messages related to a specific entity
     */
    @Query("SELECT m FROM Message m WHERE m.relatedEntityType = :entityType AND m.relatedEntityId = :entityId ORDER BY m.sentAt DESC")
    List<Message> findByRelatedEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    /**
     * Find deleted messages
     */
    @Query("SELECT m FROM Message m WHERE m.isDeleted = true ORDER BY m.sentAt DESC")
    List<Message> findDeletedMessages();

    /**
     * Find sent messages by sender
     */
    @Query("SELECT m FROM Message m WHERE m.senderId = :senderId ORDER BY m.sentAt DESC")
    List<Message> findSentMessagesBySender(@Param("senderId") Long senderId);
}

