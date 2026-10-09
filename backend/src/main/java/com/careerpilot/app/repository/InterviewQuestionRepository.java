package com.careerpilot.app.repository;

import com.careerpilot.app.entity.InterviewQuestion;
import com.careerpilot.app.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findBySessionOrderByQuestionNumberAsc(InterviewSession session);

    Optional<InterviewQuestion> findByIdAndSession(Long id, InterviewSession session);

    Optional<InterviewQuestion> findBySessionAndQuestionNumber(InterviewSession session, Integer questionNumber);
}
