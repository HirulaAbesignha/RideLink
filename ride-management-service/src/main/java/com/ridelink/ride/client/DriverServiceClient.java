package com.ridelink.ride.client;

import com.ridelink.ride.api.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DriverServiceClient {

    private final RestClient restClient;
    private final String serviceToken;

    public DriverServiceClient(@Value("${services.driver.base-url}") String baseUrl,
                               @Value("${security.internal.service-token}") String serviceToken) {
        this.restClient = ServiceRestClientFactory.create(baseUrl);
        this.serviceToken = serviceToken;
    }

    public List<EligibleDriver> findEligible(String serviceArea, int seatCount) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                EligibleDriversResponse response = restClient.get()
                        .uri(uri -> uri.path("/internal/v1/drivers/eligible")
                                .queryParam("serviceArea", serviceArea)
                                .queryParam("seatCount", seatCount).build())
                        .header("X-Service-Token", serviceToken)
                        .retrieve().body(EligibleDriversResponse.class);
                return response == null || response.drivers() == null ? List.of() : response.drivers();
            } catch (HttpClientErrorException exception) {
                throw unavailable("Driver Service rejected the eligibility request");
            } catch (HttpServerErrorException | ResourceAccessException exception) {
                if (attempt == 1) throw unavailable("Driver Service is unavailable");
            }
        }
        throw unavailable("Driver Service is unavailable");
    }

    public boolean reserve(UUID driverId, UUID rideId) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                restClient.post().uri("/internal/v1/drivers/{driverId}/reservations", driverId)
                        .header("X-Service-Token", serviceToken)
                        .body(Map.of("rideId", rideId))
                        .retrieve().toBodilessEntity();
                return true;
            } catch (HttpClientErrorException.Conflict exception) {
                return false;
            } catch (HttpClientErrorException exception) {
                throw unavailable("Driver Service rejected the reservation request");
            } catch (HttpServerErrorException | ResourceAccessException exception) {
                if (attempt == 1) throw unavailable("Driver Service is unavailable");
            }
        }
        throw unavailable("Driver Service is unavailable");
    }

    public void release(UUID driverId, UUID rideId) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                restClient.delete().uri("/internal/v1/drivers/{driverId}/reservations/{rideId}", driverId, rideId)
                        .header("X-Service-Token", serviceToken)
                        .retrieve().toBodilessEntity();
                return;
            } catch (HttpClientErrorException exception) {
                throw unavailable("Driver Service rejected the release request");
            } catch (HttpServerErrorException | ResourceAccessException exception) {
                if (attempt == 2) throw unavailable("Driver Service is unavailable");
            }
        }
    }

    private ApiException unavailable(String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DRIVER_SERVICE_UNAVAILABLE", message);
    }
}
