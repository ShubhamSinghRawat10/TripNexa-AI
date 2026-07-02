package com.tripnexa.smarttrip.repository;

import com.tripnexa.smarttrip.entity.Trip;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    List<Trip> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Trip> findByIdAndUserId(UUID id, UUID userId);
}
