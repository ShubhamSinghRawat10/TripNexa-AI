package com.tripnexa.smarttrip;

import static org.assertj.core.api.Assertions.assertThat;

import com.tripnexa.smarttrip.entity.CrowdLevel;
import com.tripnexa.smarttrip.repository.DestinationCrowdCalendarRepository;
import com.tripnexa.smarttrip.repository.DestinationPriceBaselineRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DestinationDataIntegrationTest {
    @Autowired DestinationPriceBaselineRepository priceRepository;
    @Autowired DestinationCrowdCalendarRepository crowdRepository;

    @Test
    void migrationSeedsDestinationsAndHolidayCrowdRows() {
        assertThat(priceRepository.count()).isEqualTo(19);
        assertThat(crowdRepository.count()).isEqualTo(19L * 14L);

        var manali = priceRepository.findByDestinationIgnoreCase("manali").orElseThrow();
        assertThat(manali.getAverageHotelPrice()).isEqualTo(3500);
        assertThat(manali.priceFor(true)).isEqualTo(4900);

        var diwali = crowdRepository
            .findByIdDestinationIgnoreCaseAndIdTravelDate("MANALI", LocalDate.of(2026, 11, 8))
            .orElseThrow();
        assertThat(diwali.getCrowdLevel()).isEqualTo(CrowdLevel.HIGH);
        assertThat(diwali.getReason()).isEqualTo("Diwali");
    }
}
