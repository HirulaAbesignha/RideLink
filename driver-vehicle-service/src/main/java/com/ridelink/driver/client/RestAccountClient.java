package com.ridelink.driver.client;

import com.ridelink.driver.api.error.ApiException;
import com.ridelink.driver.config.CorrelationIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class RestAccountClient implements AccountClient {

    private final RestClient restClient;
    private final String serviceToken;

    public RestAccountClient(RestClient accountRestClient,
                             @Value("${security.internal.service-token}") String serviceToken) {
        this.restClient = accountRestClient;
        this.serviceToken = serviceToken;
    }

    @Override
    public AccountSummary getAccountSummary(UUID accountId, String correlationId) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                AccountSummary summary = restClient.get()
                        .uri("/internal/v1/accounts/{accountId}/summary", accountId)
                        .header("X-Service-Token", serviceToken)
                        .header(CorrelationIdFilter.HEADER_NAME, correlationId)
                        .retrieve()
                        .body(AccountSummary.class);
                if (summary == null) {
                    throw unavailable();
                }
                return summary;
            } catch (RestClientResponseException exception) {
                if (!exception.getStatusCode().is5xxServerError()) {
                    throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "ACCOUNT_NOT_ACTIVE_DRIVER",
                            "The account is not an active driver");
                }
                if (attempt == 2) {
                    throw unavailable();
                }
            } catch (RestClientException exception) {
                if (attempt == 2) {
                    throw unavailable();
                }
            }
        }
        throw unavailable();
    }

    private ApiException unavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "ACCOUNT_SERVICE_UNAVAILABLE", "Account Service is unavailable");
    }
}
