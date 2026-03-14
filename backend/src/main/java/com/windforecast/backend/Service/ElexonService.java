package com.windforecast.backend.Service;

import com.windforecast.backend.Model.ActualRecord;
import com.windforecast.backend.Model.ForecastRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class ElexonService {

    private final WebClient webClient;

    @Value("${elexon.base-url}")
    private String baseUrl;

    @Value("${elexon.data-from}")
    private String dataFrom;

    @Value("${elexon.data-to}")
    private String dataTo;

    public ElexonService(WebClient webClient) {
        this.webClient = webClient;
    }

    @Cacheable("actuals")
    public List<ActualRecord> getActuals() {
        String url = baseUrl + "/FUELHH/stream"
                   + "?settlementDateFrom=" + dataFrom
                   + "&settlementDateTo="   + dataTo
                   + "&fuelType=WIND";

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(ActualRecord.class)
                .filter(r -> r.fuelType() != null && r.fuelType().equalsIgnoreCase("WIND"))
                .filter(r -> r.generation() != null && r.startTime() != null)
                .collectList()
                .block();
    }

    @Cacheable("forecasts")
    public List<ForecastRecord> getForecasts() {

        String[][] weeks = {
            {"2024-01-01", "2024-01-07"},
            {"2024-01-08", "2024-01-14"},
            {"2024-01-15", "2024-01-21"},
            {"2024-01-22", "2024-01-31"},
        };

        List<ForecastRecord> all = new ArrayList<>();

        for (String[] week : weeks) {
            String url = baseUrl + "/WINDFOR/stream"
                       + "?publishDateTimeFrom=" + week[0] + "T00:00:00Z"
                       + "&publishDateTimeTo="   + week[1] + "T23:59:59Z";

            List<ForecastRecord> chunk = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToFlux(ForecastRecord.class)
                    .filter(r -> r.startTime() != null && r.publishTime() != null
                              && r.generation() != null)
                    .collectList()
                    .block();

            if (chunk != null) {
                all.addAll(chunk);
            }
        }

        return all;
    }
}