package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "assistant_messages",
    indexes = {
        @Index(name = "idx_asst_msg_conv", columnList = "conversation_id"),
        @Index(name = "idx_asst_msg_created", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssistantMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AssistantConversation conversation;

    @Column(nullable = false, length = 20)
    private String role; // "USER", "ASSISTANT", "SYSTEM"

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "LONGTEXT")
    private String suggestedActionsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String contextUsedJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
