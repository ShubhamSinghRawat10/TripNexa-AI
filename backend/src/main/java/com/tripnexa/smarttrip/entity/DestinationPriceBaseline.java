package com.tripnexa.smarttrip.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "destination_price_baseline")
public class DestinationPriceBaseline {
    @Id
    @Column(length = 120)
    private String destination;

    @Column(name = "avg_hotel_price", nullable = false)
    private long averageHotelPrice;

    @Column(name = "peak_multiplier", nullable = false, precision = 3, scale = 2)
    private BigDecimal peakMultiplier;

    protected DestinationPriceBaseline() {}

    public String getDestination() { return destination; }
    public long getAverageHotelPrice() { return averageHotelPrice; }
    public BigDecimal getPeakMultiplier() { return peakMultiplier; }

    public long priceFor(boolean peakDate) {
        if (!peakDate) {
            return averageHotelPrice;
        }
        return peakMultiplier.multiply(BigDecimal.valueOf(averageHotelPrice)).longValue();
    }
}
