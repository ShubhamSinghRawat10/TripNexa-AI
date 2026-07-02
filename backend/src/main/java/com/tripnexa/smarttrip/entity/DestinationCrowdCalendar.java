package com.tripnexa.smarttrip.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "destination_crowd_calendar")
public class DestinationCrowdCalendar {
    @EmbeddedId
    private DestinationCrowdCalendarId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "crowd_level", nullable = false, length = 20)
    private CrowdLevel crowdLevel;

    @Column(nullable = false, length = 160)
    private String reason;

    protected DestinationCrowdCalendar() {}

    public DestinationCrowdCalendarId getId() { return id; }
    public CrowdLevel getCrowdLevel() { return crowdLevel; }
    public String getReason() { return reason; }
}
