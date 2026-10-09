package com.careerpilot.app.repository;

import com.careerpilot.app.entity.CareerRoadmap;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CareerRoadmapRepository extends JpaRepository<CareerRoadmap, Long> {

    Optional<CareerRoadmap> findTopByUserOrderByUpdatedAtDesc(User user);

    List<CareerRoadmap> findByUserOrderByUpdatedAtDesc(User user);

    Optional<CareerRoadmap> findTopByUserOrderByCreatedAtDesc(User user);

    List<CareerRoadmap> findByUserOrderByCreatedAtDesc(User user);

    Optional<CareerRoadmap> findByIdAndUser(Long id, User user);
}
