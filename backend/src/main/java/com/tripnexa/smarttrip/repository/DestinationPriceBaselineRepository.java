package com.tripnexa.smarttrip.repository;

import com.tripnexa.smarttrip.entity.DestinationPriceBaseline;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationPriceBaselineRepository extends JpaRepository<DestinationPriceBaseline, String> {
    Optional<DestinationPriceBaseline> findByDestinationIgnoreCase(String destination);
}
