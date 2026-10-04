package com.ridelink.ride.client;

import com.ridelink.ride.api.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class FareServiceClient {

    private final RestClient restClient;
    private final String serviceToken;

    public FareServiceClient(@Value("${services.fare.base-url}") String baseUrl,
                             @Value("${security.internal.service-token}") String serviceToken) {
        this.restClient = ServiceRestClientFactory.create(baseUrl);
        this.serviceToken = serviceToken;
    }

    public FareEstimateResponse getEstimate(UUID estimateId) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return restClient.get().uri("/internal/v1/fares/estimates/{estimateId}", estimateId)
                        .header("X-Service-Token", serviceToken)
                        .retrieve().body(FareEstimateResponse.class);
            } catch (HttpClientErrorException.NotFound exception) {
                throw new ApiException(HttpStatus.NOT_FOUND, "FARE_ESTIMATE_NOT_FOUND", "Fare estimate not found");
            } catch (HttpClientErrorException.UnprocessableEntity exception) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "FARE_ESTIMATE_EXPIRED", "The fare estimate has expired");
            } catch (HttpClientErrorException exception) {
                throw unavailable("Fare Service rejected the estimate request");
            } catch (HttpServerErrorException | ResourceAccessException exception) {
                if (attempt == 1) throw unavailable("Fare Service is unavailable");
            }
        }
        throw unavailable("Fare Service is unavailable");
    }

    public FinalFareResponse createFinalFare(UUID rideId, UUID passengerId, BigDecimal distanceKm) {
        Map<String, Object> request = Map.of(
                "rideId", rideId,
                "passengerId", passengerId,
                "distanceKm", distanceKm);
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return restClient.post().uri("/internal/v1/fares/final")
                        .header("X-Service-Token", serviceToken)
                        .body(request).retrieve().body(FinalFareResponse.class);
            } catch (HttpClientErrorException exception) {
                throw unavailable("Fare Service rejected the final fare request");
            } catch (HttpServerErrorException | ResourceAccessException exception) {
                if (attempt == 1) throw unavailable("Fare Service is unavailable");
            }
        }
        throw unavailable("Fare Service is unavailable");
    }

    private ApiException unavailable(String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "FARE_SERVICE_UNAVAILABLE", message);
    }
}
