package com.careerpilot.app.repository;

import com.careerpilot.app.entity.Resume;
import com.careerpilot.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserOrderByUploadedAtDesc(User user);

    Optional<Resume> findByIdAndUser(Long id, User user);

    /**
     * Used for ownership check without loading the full entity.
     */
    @Query("SELECT COUNT(r) > 0 FROM Resume r WHERE r.id = :id AND r.user = :user")
    boolean existsByIdAndUser(@Param("id") Long id, @Param("user") User user);

    /**
     * Find the most recent resume for a user (for 1-resume-per-user display).
     */
    Optional<Resume> findFirstByUserOrderByUploadedAtDesc(User user);

    long countByUser(User user);
}
