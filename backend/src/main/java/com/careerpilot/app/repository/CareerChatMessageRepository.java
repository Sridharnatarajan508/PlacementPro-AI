package com.careerpilot.app.repository;

import com.careerpilot.app.entity.CareerChat;
import com.careerpilot.app.entity.CareerChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareerChatMessageRepository extends JpaRepository<CareerChatMessage, Long> {

    List<CareerChatMessage> findByChatOrderByCreatedAtAsc(CareerChat chat);

    List<CareerChatMessage> findTop10ByChatOrderByCreatedAtDesc(CareerChat chat);
}
