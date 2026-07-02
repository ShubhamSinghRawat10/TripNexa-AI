package com.tripnexa.smarttrip.dto.trip;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

public record TripRequest(
    @NotBlank @Size(max = 120) String destination,
    @Min(1) long budget,
    @Min(1) @Max(30) int days,
    @Min(1) @Max(50) int people,
    @NotNull @FutureOrPresent LocalDate travelDate,
    @NotNull @Size(max = 10) Set<@NotBlank @Size(max = 50) String> interests
) {}
