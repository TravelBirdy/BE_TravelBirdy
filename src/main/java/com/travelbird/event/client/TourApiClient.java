package com.travelbird.event.client;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * TourAPI 4.0 축제·행사 정보(searchFestival2) 연동.
 * 서비스키는 커밋되는 설정 파일에 값이 들어가지 않고 TOUR_API_SERVICE_KEY 환경변수로만 주입된다.
 */
@Component
public class TourApiClient {

    private static final int TIMEOUT_MILLIS = 5000;
    private static final DateTimeFormatter TOUR_API_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestClient restClient;
    private final String baseUri;
    private final String serviceKey;

    public TourApiClient(
            @Value("${tour-api.base-uri}") String baseUri,
            @Value("${tour-api.service-key:}") String serviceKey
    ) {
        this.baseUri = baseUri;
        this.serviceKey = decode(serviceKey);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(TIMEOUT_MILLIS);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isConfigured() {
        return serviceKey != null && !serviceKey.isBlank();
    }

    public List<TourApiFestivalResponse.Item> searchFestivals(LocalDate eventStartDate, LocalDate eventEndDate, int pageNo, int numOfRows) {
        if (!isConfigured()) {
            throw new IllegalStateException("TOUR_API_SERVICE_KEY가 설정되지 않았습니다.");
        }

        TourApiFestivalResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(baseUri.replaceFirst("^https?://", "").split("/")[0])
                        .path(baseUri.replaceFirst("^https?://[^/]+", "") + "/searchFestival2")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("MobileOS", "ETC")
                        .queryParam("MobileApp", "TravelBird")
                        .queryParam("_type", "json")
                        .queryParam("arrange", "A")
                        .queryParam("listYN", "Y")
                        .queryParam("eventStartDate", eventStartDate.format(TOUR_API_DATE))
                        .queryParam("eventEndDate", eventEndDate.format(TOUR_API_DATE))
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows)
                        .build())
                .retrieve()
                .body(TourApiFestivalResponse.class);

        if (response == null || response.response() == null || response.response().body() == null
                || response.response().body().items() == null || response.response().body().items().item() == null) {
            return List.of();
        }
        return response.response().body().items().item();
    }

    private String decode(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return value;
        }
    }
}
