package com.careerpilot.app.repository;

import com.careerpilot.app.entity.InterviewSession;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    Optional<InterviewSession> findByIdAndUser(Long id, User user);

    List<InterviewSession> findByUserOrderByCreatedAtDesc(User user);

    List<InterviewSession> findTop10ByUserOrderByCreatedAtDesc(User user);
}
