# Postman

`RideLink-Driver-Service.postman_collection.json` contains the complete Member 2 demonstration flow and required negative requests. Import it together with `RideLink-Local.postman_environment.json`.

The environment contains harmless placeholders only. Paste a current driver JWT and the same local service token used by the applications before running the collection. Never commit real passwords, JWTs or tokens.

Suggested variables:

- `accountBaseUrl`
- `driverBaseUrl`
- `rideBaseUrl`
- `fareBaseUrl`
- `passengerToken`
- `driverToken`
- `adminToken`
- generated account, driver, ride and payment IDs

