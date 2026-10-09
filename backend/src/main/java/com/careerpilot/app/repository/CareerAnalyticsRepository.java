package com.careerpilot.app.repository;

import com.careerpilot.app.entity.CareerAnalytics;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CareerAnalyticsRepository extends JpaRepository<CareerAnalytics, Long> {

    Optional<CareerAnalytics> findByUser(User user);

    Optional<CareerAnalytics> findByUserId(Long userId);

    void deleteByUser(User user);
}
