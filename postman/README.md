# Postman demonstration

Import `RideLink-Local.postman_environment.json` and all four service collections. Replace the local service token and admin credentials with the values used when the services were started. JWTs and generated IDs are saved automatically by the numbered requests.

## Full workflow

1. Run Account requests 01 to 06 to register and log in the passenger, driver and administrator.
2. Run Driver requests 01 to 06 to create the profile and vehicle and make the driver available.
3. Run Fare requests 01 to 03 to create and validate an estimate.
4. Run Ride requests 01 to 08 to create, accept, start and complete a ride.
5. Run Fare requests 04 to 06 to view the final fare, record payment and retrieve the receipt.

Use fresh demo email addresses or reset the local database volumes before repeating the registration requests.

## Collections

- `RideLink-Account-Service.postman_collection.json`: registration, login, profiles, account state and internal account summary.
- `RideLink-Driver-Service.postman_collection.json`: driver profile, vehicle, location, availability and reservation.
- `RideLink-Ride-Service.postman_collection.json`: ride assignment, retrieval, lifecycle and internal summary.
- `RideLink-Fare-Service.postman_collection.json`: estimates, final fare, simulated payment and receipt.

Each collection also contains negative requests for role checks, validation or internal service authentication. Do not put real passwords, JWTs or service tokens in the exported environment file.

For the stateful negative cases, run Fare's negative estimate setup, Ride's negative ride setup, Ride's start-before-accept request, Fare's payment-before-completion request, and then Ride's cleanup request. To demonstrate no available driver, set the driver to `OFFLINE`, create another fresh estimate, and run Ride's no-driver request.
