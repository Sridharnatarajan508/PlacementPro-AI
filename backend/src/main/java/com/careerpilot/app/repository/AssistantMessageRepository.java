package com.careerpilot.app.repository;

import com.careerpilot.app.entity.AssistantConversation;
import com.careerpilot.app.entity.AssistantMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, Long> {

    List<AssistantMessage> findByConversationOrderByCreatedAtAsc(AssistantConversation conversation);

    List<AssistantMessage> findTop10ByConversationOrderByCreatedAtDesc(AssistantConversation conversation);

    void deleteByConversation(AssistantConversation conversation);
}
