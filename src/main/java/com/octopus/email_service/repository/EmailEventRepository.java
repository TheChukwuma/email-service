package com.octopus.email_service.repository;

import com.octopus.email_service.entity.EmailEvent;
import com.octopus.email_service.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailEventRepository extends JpaRepository<EmailEvent, Long> {
    
    List<EmailEvent> findByEmailIdOrderByCreatedAtDesc(Long emailId);
    
    @Query("SELECT ee FROM EmailEvent ee WHERE ee.email.id = :emailId AND ee.eventType = :eventType")
    List<EmailEvent> findByEmailIdAndEventType(@Param("emailId") Long emailId, 
                                              @Param("eventType") EventType eventType);
    
    @Query("SELECT ee FROM EmailEvent ee WHERE ee.eventType = :eventType AND ee.createdAt >= :since")
    Page<EmailEvent> findByEventTypeAndCreatedAtAfter(@Param("eventType") EventType eventType,
                                                     @Param("since") LocalDateTime since,
                                                     Pageable pageable);
    
    @Query("SELECT COUNT(ee) FROM EmailEvent ee WHERE ee.eventType = :eventType AND ee.createdAt >= :since")
    long countByEventTypeAndCreatedAtAfter(@Param("eventType") EventType eventType,
                                          @Param("since") LocalDateTime since);
    
    @Query("SELECT ee FROM EmailEvent ee WHERE ee.email.id = :emailId ORDER BY ee.createdAt DESC")
    Page<EmailEvent> findByEmailIdOrderByCreatedAtDesc(@Param("emailId") Long emailId, Pageable pageable);
}
