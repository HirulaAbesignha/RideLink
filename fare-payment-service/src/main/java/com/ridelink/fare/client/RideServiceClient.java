package com.ridelink.fare.client;

import java.util.UUID;
import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.ridelink.fare.config.CorrelationIdFilter;

import com.ridelink.fare.dto.RideSummaryResponse;
import com.ridelink.fare.exception.FareApiException;

@Component
public class RideServiceClient {

    private final RestClient restClient;
    private final String serviceToken;

    public RideServiceClient(
            @Value("${ride.service.url:http://localhost:8083}") String rideServiceUrl,
            @Value("${service.token}") String serviceToken) {

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .baseUrl(rideServiceUrl)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                        Object correlationId = attributes.getRequest().getAttribute(CorrelationIdFilter.ATTRIBUTE);
                        if (correlationId instanceof String value) {
                            request.getHeaders().set("X-Correlation-Id", value);
                        }
                    }
                    return execution.execute(request, body);
                })
                .build();

        this.serviceToken = serviceToken;
    }

    public RideSummaryResponse getRideSummary(UUID rideId) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return restClient.get()
                        .uri("/internal/v1/rides/{rideId}/summary", rideId)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("X-Service-Token", serviceToken)
                        .retrieve()
                        .body(RideSummaryResponse.class);
            } catch (HttpClientErrorException.NotFound ex) {
                throw new FareApiException(404, "RIDE_NOT_FOUND", "Ride not found");
            } catch (HttpClientErrorException ex) {
                throw new FareApiException(503, "RIDE_SERVICE_UNAVAILABLE", "Ride Service rejected the internal request");
            } catch (HttpServerErrorException | ResourceAccessException ex) {
                if (attempt == 1) {
                    throw new FareApiException(503, "RIDE_SERVICE_UNAVAILABLE", "Ride Service is unavailable");
                }
            } catch (RestClientException ex) {
                throw new FareApiException(503, "RIDE_SERVICE_UNAVAILABLE", "Ride Service request failed");
            }
        }
        throw new FareApiException(503, "RIDE_SERVICE_UNAVAILABLE", "Ride Service is unavailable");
    }
}
