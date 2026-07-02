package com.tripnexa.smarttrip.controller;

import com.tripnexa.smarttrip.dto.trip.ShouldITravelResponse;
import com.tripnexa.smarttrip.service.TripService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Standalone "Should I Travel Now?" endpoint — no login required.
 * Returns crowd level, price comparison, and recommendation without generating an itinerary.
 */
@Validated
@RestController
@RequestMapping("/api/should-i-travel")
public class ShouldITravelController {

    private final TripService tripService;

    public ShouldITravelController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    public ShouldITravelResponse check(
        @RequestParam @NotBlank String destination,
        @RequestParam @Min(1) long budget,
        @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return tripService.shouldITravel(destination, budget, date);
    }
}
