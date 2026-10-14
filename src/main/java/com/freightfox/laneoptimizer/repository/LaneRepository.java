package com.freightfox.laneoptimizer.repository;

import com.freightfox.laneoptimizer.entity.LaneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for trade lanes.
 */
@Repository
public interface LaneRepository extends JpaRepository<LaneEntity, Long> {
}
