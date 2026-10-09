package com.careerpilot.app.repository;

import com.careerpilot.app.entity.LearningRecommendation;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface LearningRecommendationRepository extends JpaRepository<LearningRecommendation, Long> {

    List<LearningRecommendation> findByUserOrderByIdAsc(User user);

    List<LearningRecommendation> findByUserOrderByCreatedAtDesc(User user);

    @Modifying
    @Transactional
    void deleteByUser(User user);
}
