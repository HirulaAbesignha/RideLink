# RideLink shared API contract

Complete this document as a group before integrating services. Decisions recorded here must match the code, Swagger documentation and Postman collection.

## 1. Common choices

- Identifier type: UUID
- Time format: ISO-8601 UTC
- Content type: `application/json`
- External roles: `PASSENGER`, `DRIVER`, `ADMIN`
- Internal caller: service credential or agreed service token

## 2. Common error response

```json
{
  "timestamp": "2026-09-22T10:30:00Z",
  "status": 409,
  "code": "DRIVER_NOT_AVAILABLE",
  "message": "The selected driver is not available",
  "path": "/internal/drivers/{driverId}/reservations",
  "correlationId": "UUID"
}
```

## 3. Status codes

- `200`: successful read or update
- `201`: resource created
- `400`: invalid request data
- `401`: missing or invalid authentication
- `403`: authenticated caller lacks permission
- `404`: resource does not exist
- `409`: state, uniqueness or concurrency conflict
- `503`: required service is temporarily unavailable

## 4. JWT contract - Member 1 leads

Record the final issuer, subject, role claim name, expiry and signing-key approach here. Other services must validate tokens; they must not query the Account database.

## 5. Driver contract - Member 2 leads

Proposed internal operations:

- Find eligible drivers by service area.
- Reserve one driver atomically using `rideId`.
- Release the reservation only when the same `rideId` is supplied.

Record final request/response examples and availability states here.

## 6. Ride contract - Member 3 leads

Proposed lifecycle:

```text
REQUESTED -> ASSIGNED -> ACCEPTED -> IN_PROGRESS -> COMPLETED
```

Record legal cancellation transitions, responsible actors and invalid-transition behavior here.

## 7. Fare contract - Member 4 leads

Record the final LKR formula, rounding, distance limits, rule version, final-fare request and payment simulation behavior here.

## 8. Interservice failures

For every service call, record:

- timeout;
- retry behavior;
- idempotency key;
- expected `4xx` and `5xx` responses;
- compensation action if the caller saves data but a later step fails.

