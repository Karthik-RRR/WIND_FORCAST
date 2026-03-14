package com.windforecast.backend.Controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.windforecast.backend.Model.ActualRecord;
import com.windforecast.backend.Model.ChartResponse;
import com.windforecast.backend.Model.ForecastRecord;
import com.windforecast.backend.Service.ChartService;
import com.windforecast.backend.Service.ElexonService;



@RestController
@RequestMapping("/api")
public class ChartController {

    private final ChartService chartService;
    private final ElexonService elexonService;

    public ChartController(ChartService chartService, com.windforecast.backend.Service.ElexonService elexonService) {
        this.chartService = chartService;
        this.elexonService = elexonService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"ok\"}");
    }

    @GetMapping("/meta")
    public ResponseEntity<?> meta() {
        return ResponseEntity.ok(java.util.Map.of(
                "dataFrom", "2024-01-01T00:00:00Z",
                "dataTo", "2024-01-31T23:30:00Z",
                "resolutionMins", 30,
                "horizonMinHrs", 0,
                "horizonMaxHrs", 48,
                "horizonDefaultHrs", 4));
    }

    @GetMapping("/chart")
    public ResponseEntity<ChartResponse> chart(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(defaultValue = "4.0") double horizon) {
        if (horizon < 0 || horizon > 48) {
            return ResponseEntity.badRequest().build();
        }

        Instant startDt = Instant.parse(start);
        Instant endDt = Instant.parse(end);

        if (!startDt.isBefore(endDt)) {
            return ResponseEntity.badRequest().build();
        }

        ChartResponse response = chartService.buildChartData(startDt, endDt, horizon);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/debug")
    public ResponseEntity<?> debug() {
        List<ActualRecord> actuals = elexonService.getActuals();
        List<ForecastRecord> forecasts = elexonService.getForecasts();
        return ResponseEntity.ok(Map.of(
                "actuals_count", actuals.size(),
                "forecasts_count", forecasts.size(),
                "forecast_sample", forecasts.stream().limit(3).toList()));
    }
}