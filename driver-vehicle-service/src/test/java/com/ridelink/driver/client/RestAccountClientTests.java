package com.ridelink.driver.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RestAccountClientTests {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void retriesOnceAfterServerError() throws IOException {
        UUID accountId = UUID.randomUUID();
        AtomicInteger requests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/v1/accounts/" + accountId + "/summary", exchange -> {
            int attempt = requests.incrementAndGet();
            if (attempt == 1) {
                exchange.sendResponseHeaders(500, -1);
            } else {
                byte[] body = ("""
                        {"accountId":"%s","role":"DRIVER","status":"ACTIVE"}
                        """).formatted(accountId).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();

        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .build();
        RestAccountClient client = new RestAccountClient(restClient,
                "test-service-token-with-at-least-32-characters");

        AccountSummary summary = client.getAccountSummary(accountId, UUID.randomUUID().toString());

        assertThat(summary.accountId()).isEqualTo(accountId);
        assertThat(summary.role()).isEqualTo("DRIVER");
        assertThat(requests).hasValue(2);
    }
}
