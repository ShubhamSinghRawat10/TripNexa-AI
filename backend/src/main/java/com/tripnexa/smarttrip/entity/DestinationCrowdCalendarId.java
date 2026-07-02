package com.tripnexa.smarttrip.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class DestinationCrowdCalendarId implements Serializable {
    @Column(length = 120)
    private String destination;

    @Column(name = "travel_date")
    private LocalDate travelDate;

    protected DestinationCrowdCalendarId() {}

    public DestinationCrowdCalendarId(String destination, LocalDate travelDate) {
        this.destination = destination;
        this.travelDate = travelDate;
    }

    public String getDestination() { return destination; }
    public LocalDate getTravelDate() { return travelDate; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DestinationCrowdCalendarId that)) return false;
        return Objects.equals(destination, that.destination) && Objects.equals(travelDate, that.travelDate);
    }

    @Override
    public int hashCode() { return Objects.hash(destination, travelDate); }
}
