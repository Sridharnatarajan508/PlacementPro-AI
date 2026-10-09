package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "career_chat_messages",
    indexes = {
        @Index(name = "idx_chat_msg_chat", columnList = "chat_id"),
        @Index(name = "idx_chat_msg_created", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private CareerChat chat;

    @Column(nullable = false, length = 20)
    private String role; // "USER" or "ASSISTANT"

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String message;

    @Column(columnDefinition = "LONGTEXT")
    private String suggestedActionsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String relevantSkillsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String relevantJobsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String preparationSuggestionsJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
