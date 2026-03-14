package com.windforecast.backend.Model;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ActualRecord(
    Instant startTime,
    String  fuelType,
    Double  generation
) {}