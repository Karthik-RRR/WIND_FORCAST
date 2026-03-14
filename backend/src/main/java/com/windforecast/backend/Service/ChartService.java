package com.windforecast.backend.Service;


import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.windforecast.backend.Model.ActualRecord;
import com.windforecast.backend.Model.ChartDataPoint;
import com.windforecast.backend.Model.ChartResponse;
import com.windforecast.backend.Model.ForecastRecord;


@Service
public class ChartService {

    private final ElexonService elexonService;

    public ChartService(ElexonService elexonService) {
        this.elexonService = elexonService;
    }


    public ChartResponse buildChartData(Instant start, Instant end, double horizonHrs) {

        List<ActualRecord>   actuals   = elexonService.getActuals();
        List<ForecastRecord> forecasts = elexonService.getForecasts();

        Map<String, Double> actualsMap = new HashMap<>();
        for (ActualRecord a : actuals) {
            actualsMap.put(slotKey(a.startTime()), a.generation());
        }

        Map<String, List<ForecastRecord>> forecastMap = forecasts.stream()
            .filter(f -> {
                double h = horizonHours(f.startTime(), f.publishTime());
                return h >= 0 && h <= 48;
            })
            .collect(Collectors.groupingBy(f -> slotKey(f.startTime())));

        forecastMap.values().forEach(list ->
            list.sort(Comparator.comparing(ForecastRecord::publishTime).reversed())
        );

        List<ChartDataPoint> points = new ArrayList<>();
        Instant current = floorTo30Min(start);

        while (!current.isAfter(end)) {
            String  key    = slotKey(current);
            Instant cutoff = current.minus((long)(horizonHrs * 60), ChronoUnit.MINUTES);

            Double  actual         = actualsMap.get(key);
            Double  forecastVal    = null;
            Instant forecastPubTime = null;

            // Find latest forecast published at or before cutoff
            List<ForecastRecord> candidates = forecastMap.getOrDefault(key, List.of());
            for (ForecastRecord f : candidates) {
                if (!f.publishTime().isAfter(cutoff)) {
                    forecastVal     = f.generation();
                    forecastPubTime = f.publishTime();
                    break;   // list is sorted newest-first → first match = latest valid
                }
            }

            points.add(new ChartDataPoint(current, actual, forecastVal, forecastPubTime));
            current = current.plus(30, ChronoUnit.MINUTES);
        }

        Map<String, Object> stats = computeStats(points);
        return new ChartResponse(points, stats);
    }
    private Map<String, Object> computeStats(List<ChartDataPoint> points) {
        List<Double> errors = points.stream()
            .filter(p -> p.actual() != null && p.forecast() != null)
            .map(p -> p.forecast() - p.actual())
            .toList();

        if (errors.isEmpty()) return Map.of("n", 0);

        int    n          = errors.size();
        double mae        = errors.stream().mapToDouble(Math::abs).average().orElse(0);
        double rmse       = Math.sqrt(errors.stream().mapToDouble(e -> e*e).average().orElse(0));
        double bias       = errors.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        List<Double> abs  = errors.stream().map(Math::abs).sorted().toList();
        double p50        = abs.get((int)(0.50 * n));
        double p99        = abs.get(Math.min((int)(0.99 * n), n - 1));

        return Map.of(
            "n",    n,
            "mae",  round(mae),
            "rmse", round(rmse),
            "bias", round(bias),
            "p50",  round(p50),
            "p99",  round(p99)
        );
    }


    private String slotKey(Instant t) {
        return t.truncatedTo(ChronoUnit.MINUTES)
                .toString().substring(0, 16);
    }

    private double horizonHours(Instant target, Instant published) {
        return Duration.between(published, target).toMinutes() / 60.0;
    }

    private Instant floorTo30Min(Instant t) {
        long epochMin = t.toEpochMilli() / 60_000;
        long floored  = (epochMin / 30) * 30;
        return Instant.ofEpochSecond(floored * 60);
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}