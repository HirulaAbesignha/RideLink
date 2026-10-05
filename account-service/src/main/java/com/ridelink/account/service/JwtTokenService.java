package com.ridelink.account.service;

import com.ridelink.account.domain.Account;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirySeconds;

    public JwtTokenService(JwtEncoder jwtEncoder,
                           @Value("${security.jwt.issuer}") String issuer,
                           @Value("${security.jwt.expiry-seconds}") long expirySeconds) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirySeconds = expirySeconds;
    }

    public String createAccessToken(Account account) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(account.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(expirySeconds))
                .id(UUID.randomUUID().toString())
                .claim("role", account.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }
}
