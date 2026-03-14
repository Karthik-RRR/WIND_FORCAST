package com.windforecast.backend.Model;

import java.util.List;
import java.util.Map;

public record ChartResponse(
    List<ChartDataPoint> data,
    Map<String, Object>  stats
) {}