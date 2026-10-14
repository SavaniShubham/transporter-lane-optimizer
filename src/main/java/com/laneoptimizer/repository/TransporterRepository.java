package com.laneoptimizer.repository;

import com.laneoptimizer.entity.TransporterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for transporters.
 */
@Repository
public interface TransporterRepository extends JpaRepository<TransporterEntity, Long> {
}
