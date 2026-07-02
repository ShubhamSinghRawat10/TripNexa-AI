package com.tripnexa.smarttrip.agent.result;

import java.util.List;

public record HotelResult(
    List<HotelOption> options,
    HotelOption recommended
) {}
