package com.windforecast.backend.Model;

import java.time.Instant;

public record ChartDataPoint(
    Instant startTime,
    Double  actual,
    Double  forecast,
    Instant forecastPublishTime
) {}