package com.careerpilot.app.repository;

import com.careerpilot.app.entity.AssistantConversation;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssistantConversationRepository extends JpaRepository<AssistantConversation, Long> {

    List<AssistantConversation> findByUserOrderByUpdatedAtDesc(User user);

    Optional<AssistantConversation> findByIdAndUser(Long id, User user);

    void deleteByUser(User user);
}
