package com.freightfox.laneoptimizer.repository;

import com.freightfox.laneoptimizer.entity.LaneQuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for transporter lane quotes.
 */
@Repository
public interface LaneQuoteRepository extends JpaRepository<LaneQuoteEntity, Long> {
}
