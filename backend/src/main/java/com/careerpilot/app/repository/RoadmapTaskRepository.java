package com.careerpilot.app.repository;

import com.careerpilot.app.entity.CareerRoadmap;
import com.careerpilot.app.entity.RoadmapTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadmapTaskRepository extends JpaRepository<RoadmapTask, Long> {

    List<RoadmapTask> findByRoadmapOrderByWeekNumberAscIdAsc(CareerRoadmap roadmap);

    Optional<RoadmapTask> findByIdAndRoadmap(Long id, CareerRoadmap roadmap);
}
