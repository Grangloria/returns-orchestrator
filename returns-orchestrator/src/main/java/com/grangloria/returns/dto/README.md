# Data Transfer Objects (DTOs)
This package holds the request and response models used specifically for the public API.

### Why separate from Domain?
- **Decoupling:** Allows the internal domain logic to evolve without breaking the external API contract.
- **Security:** Prevents "over-posting" by only exposing fields that should be editable by the client.