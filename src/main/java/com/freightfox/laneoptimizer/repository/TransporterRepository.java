package com.freightfox.laneoptimizer.repository;

import com.freightfox.laneoptimizer.entity.TransporterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for transporters.
 */
@Repository
public interface TransporterRepository extends JpaRepository<TransporterEntity, Long> {
}
