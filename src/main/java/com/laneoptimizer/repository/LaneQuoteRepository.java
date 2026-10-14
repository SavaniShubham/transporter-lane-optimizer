package com.laneoptimizer.repository;

import com.laneoptimizer.entity.LaneQuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for transporter lane quotes.
 */
@Repository
public interface LaneQuoteRepository extends JpaRepository<LaneQuoteEntity, Long> {
}
