# Postman

The service collections contain each member's demonstration flow and negative requests. Import the collection you need together with `RideLink-Local.postman_environment.json`.

- `RideLink-Account-Service.postman_collection.json`: Member 1 registration, login, profile, admin and internal summary flow.
- `RideLink-Driver-Service.postman_collection.json`: Member 2 profile, vehicle, availability and reservation flow.

The environment contains local demonstration placeholders only. Use the same local service token and admin bootstrap credentials used by the applications. Never commit real passwords, JWTs or tokens.

Suggested variables:

- `accountBaseUrl`
- `driverBaseUrl`
- `rideBaseUrl`
- `fareBaseUrl`
- `passengerToken`
- `driverToken`
- `adminToken`
- generated account, driver, ride and payment IDs

