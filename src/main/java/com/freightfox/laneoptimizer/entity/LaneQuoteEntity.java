package com.freightfox.laneoptimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * JPA entity representing a quote submitted by a transporter for a specific lane.
 */
@Entity
@Table(name = "lane_quotes")
public class LaneQuoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transporter_id", nullable = false)
    private Long transporterId;

    @Column(name = "lane_id", nullable = false)
    private Long laneId;

    @Column(name = "quote", nullable = false)
    private Long quote;

    public LaneQuoteEntity() {}

    public LaneQuoteEntity(Long transporterId, Long laneId, Long quote) {
        this.transporterId = transporterId;
        this.laneId = laneId;
        this.quote = quote;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTransporterId() {
        return transporterId;
    }

    public void setTransporterId(Long transporterId) {
        this.transporterId = transporterId;
    }

    public Long getLaneId() {
        return laneId;
    }

    public void setLaneId(Long laneId) {
        this.laneId = laneId;
    }

    public Long getQuote() {
        return quote;
    }

    public void setQuote(Long quote) {
        this.quote = quote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LaneQuoteEntity that = (LaneQuoteEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
