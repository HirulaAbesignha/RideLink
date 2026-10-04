package com.ridelink.ride.client;

import com.ridelink.ride.config.CorrelationIdFilter;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.http.HttpClient;
import java.time.Duration;

final class ServiceRestClientFactory {

    private ServiceRestClientFactory() {
    }

    static RestClient create(String baseUrl) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                        Object value = attributes.getRequest().getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
                        if (value != null) {
                            request.getHeaders().set(CorrelationIdFilter.HEADER_NAME, value.toString());
                        }
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
