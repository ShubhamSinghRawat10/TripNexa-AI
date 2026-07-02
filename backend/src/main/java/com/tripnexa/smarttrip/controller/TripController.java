package com.tripnexa.smarttrip.controller;

import com.tripnexa.smarttrip.dto.trip.GenerateResponse;
import com.tripnexa.smarttrip.dto.trip.TripRequest;
import com.tripnexa.smarttrip.dto.trip.TripResponse;
import com.tripnexa.smarttrip.security.UserPrincipal;
import com.tripnexa.smarttrip.service.TripService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trip")
public class TripController {
    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping("/create")
    public ResponseEntity<TripResponse> create(@AuthenticationPrincipal UserPrincipal user,
                                               @Valid @RequestBody TripRequest request) {
        TripResponse trip = tripService.create(user.id(), request);
        return ResponseEntity.created(URI.create("/api/trip/" + trip.id())).body(trip);
    }

    /** Runs the full multi-agent pipeline and returns itinerary + recommendation. */
    @PostMapping("/generate")
    public GenerateResponse generate(@AuthenticationPrincipal UserPrincipal user,
                                     @Valid @RequestBody TripRequest request) {
        return tripService.generate(user.id(), request);
    }

    @GetMapping("/history")
    public List<TripResponse> history(@AuthenticationPrincipal UserPrincipal user) {
        return tripService.history(user.id());
    }

    @GetMapping("/{id}")
    public TripResponse get(@AuthenticationPrincipal UserPrincipal user, @PathVariable UUID id) {
        return tripService.get(user.id(), id);
    }

    @PutMapping("/{id}")
    public TripResponse update(@AuthenticationPrincipal UserPrincipal user, @PathVariable UUID id,
                               @Valid @RequestBody TripRequest request) {
        return tripService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable UUID id) {
        tripService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
