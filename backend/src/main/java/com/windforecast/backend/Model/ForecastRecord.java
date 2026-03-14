package com.windforecast.backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastRecord(
    Instant publishTime,
    Instant startTime,
    Double  generation
) {}