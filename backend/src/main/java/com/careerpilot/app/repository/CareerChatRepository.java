package com.careerpilot.app.repository;

import com.careerpilot.app.entity.CareerChat;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CareerChatRepository extends JpaRepository<CareerChat, Long> {

    List<CareerChat> findByUserOrderByUpdatedAtDesc(User user);

    Optional<CareerChat> findByIdAndUser(Long id, User user);
}
