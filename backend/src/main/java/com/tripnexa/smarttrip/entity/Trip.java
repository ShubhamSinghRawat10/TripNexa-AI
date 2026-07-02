package com.tripnexa.smarttrip.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String destination;

    @Column(nullable = false)
    private long budget;

    @Column(nullable = false)
    private int days;

    @Column(nullable = false)
    private int people;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trip_interests", joinColumns = @JoinColumn(name = "trip_id"))
    @Column(name = "interest", nullable = false, length = 50)
    private Set<String> interests = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Trip() {}

    public Trip(User user, String destination, long budget, int days, int people,
                LocalDate travelDate, Set<String> interests) {
        this.user = user;
        this.destination = destination;
        this.budget = budget;
        this.days = days;
        this.people = people;
        this.travelDate = travelDate;
        this.interests = new LinkedHashSet<>(interests);
        this.status = TripStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String destination, long budget, int days, int people,
                       LocalDate travelDate, Set<String> interests) {
        this.destination = destination;
        this.budget = budget;
        this.days = days;
        this.people = people;
        this.travelDate = travelDate;
        this.interests.clear();
        this.interests.addAll(interests);
        this.updatedAt = Instant.now();
    }

    public void markGenerated() {
        this.status = TripStatus.GENERATED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getDestination() { return destination; }
    public long getBudget() { return budget; }
    public int getDays() { return days; }
    public int getPeople() { return people; }
    public LocalDate getTravelDate() { return travelDate; }
    public Set<String> getInterests() { return Set.copyOf(interests); }
    public TripStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
