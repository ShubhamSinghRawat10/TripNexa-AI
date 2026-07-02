package com.tripnexa.smarttrip.repository;

import com.tripnexa.smarttrip.entity.DestinationCrowdCalendar;
import com.tripnexa.smarttrip.entity.DestinationCrowdCalendarId;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationCrowdCalendarRepository
    extends JpaRepository<DestinationCrowdCalendar, DestinationCrowdCalendarId> {
    Optional<DestinationCrowdCalendar> findByIdDestinationIgnoreCaseAndIdTravelDate(
        String destination, LocalDate travelDate);
}
