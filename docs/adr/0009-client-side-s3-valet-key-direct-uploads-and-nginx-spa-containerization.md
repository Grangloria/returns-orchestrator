# ADR 0009: Client-Side S3 Valet Key Direct Uploads and Nginx SPA Containerization

- **Status:** Accepted
- **Date:** 2026-10-11
- **Deciders:** James Hayes (Staff Engineer)
- **Technical Domain:** Frontend Architecture / Cloud Storage / Microservice Security

---

## 1. Context & Problem Statement

The `customer-portal` React SPA allows customers to attach photo evidence of damaged items when submitting a return request.

Initially, streaming multi-megabyte binary image files through `api-gateway` and `returns-service` created significant JVM memory overhead, consumed gateway thread pools, and introduced `OutOfMemoryError` risks under high concurrent upload traffic. Additionally, browser HTTP `OPTIONS` preflight requests were failing with `401 Unauthorized` in `api-gateway` because Spring Security blocked unauthenticated preflight calls before CORS evaluation. Finally, running the SPA via Vite's development server was unviable for production container orchestration within `docker-compose`.

---

## 2. Decision Drivers & Forces

- **Resource Preservation & Scalability:** Eliminate binary file buffering on Java microservices to preserve JVM heap memory and API Gateway throughput under high upload concurrency.
- **Security & Scope Granularity:** Allow client photo uploads directly to object storage without exposing long-term AWS IAM credentials or granting open bucket write access.
- **Gateway Security Compliance:** Permit browser CORS preflight checks (`OPTIONS`) globally across public customer endpoints without requiring JWT Bearer tokens.
- **Production Containerization:** Package the React SPA into a lightweight, standalone container co-located with Keycloak, LocalStack, Kafka, and backend microservices in `docker-compose`.

---

## 3. Options Considered

1. **Option 1 — Backend Proxy File Upload (Spring `MultipartFile`):** SPA posts binary payloads to `returns-service` via `api-gateway`, which buffers the stream into JVM memory before uploading to S3. **Pros:** Simple single-endpoint API design. **Cons:** Severe backend heap memory consumption; risk of gateway thread starvation and timeout errors on large files.
2. **Option 2 — Direct S3 Upload with Public Bucket Access:** SPA uploads directly to a publicly writable S3 bucket. **Pros:** Complete bypass of backend services. **Cons:** Severe security risk; allows unauthenticated storage consumption, asset overwrites, and malicious bucket manipulation.
3. **Option 3 — Valet Key Pattern (S3 Presigned URLs) + Spring Security Preflight Permit + Multi-Stage Nginx Containerization:** SPA requests a short-lived presigned upload URL from `returns-service`, executes a direct HTTP `PUT` to LocalStack S3, and submits the resulting `s3Key`. `api-gateway` explicitly permits `OPTIONS` preflight requests and delegates CORS to global configuration. SPA is compiled via a multi-stage Node.js build into an Alpine Nginx container. **Pros:** Zero backend memory impact for storage I/O, strict time-bound write permissions, lightweight ~14 MB runtime container footprint, and unblocked browser CORS. **Cons:** Requires a two-step upload execution flow on the client.

---

## 4. Decision Outcome

**Chosen Option:** Option 3 — Valet Key Pattern (S3 Presigned URLs) + Spring Security Preflight Permit + Multi-Stage Nginx Containerization

### Justification
Option 3 offloads heavy binary I/O directly to object storage (LocalStack S3 in local dev) while retaining `returns-service` as an authoritative access broker. Configuring Spring Security in `api-gateway` with `.cors(Customizer.withDefaults())` and `.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()` allows browsers to complete preflight checks without authentication. Building `customer-portal` with a multi-stage Docker build served via Alpine Nginx reduces container memory usage to ~14 MB while guaranteeing proper SPA client-side routing (`try_files $uri $uri/ /index.html`).

---

## 5. System Consequences & Trade-offs

### Positive
- **Zero Backend Memory Overhead:** Microservice heap space and thread pools remain completely unburdened during evidence photo uploads.
- **Strict Access Control:** Presigned URLs expire after 15 minutes and limit write access strictly to specified object keys (`returns/{orderId}/evidence.jpg`).
- **Unblocked Preflight Requests:** Global CORS and `OPTIONS` authorization allow seamless cross-origin communication between `localhost:5173` (SPA), `localhost:8080` (Gateway), and `localhost:4566` (LocalStack S3).
- **Production Efficiency:** Compiling Vite static assets to an Nginx container drops SPA container RAM consumption to ~14.2 MB.

### Negative & Mitigation
- **Drawback:** Native platform binary dependencies (e.g., `lightningcss` in Tailwind CSS v4) cause `npm ci` lockfile mismatches inside Alpine Linux containers during Docker builds.
    - **Mitigation:** Utilized `npm install` within the Docker build stage and added a `.dockerignore` file excluding host `node_modules` and build artifacts.
- **Drawback:** Client SPA requires two sequential asynchronous network requests per image upload (`POST /presigned-url` followed by binary `PUT`).
    - **Mitigation:** Network overhead is negligible (~5–10 ms) compared to proxying large binary streams through Spring WebFlux and Spring Boot microservices.

---

## 6. Observability & Verification

- **Network Verification:** DevTools Network tab confirms successful preflight (200 OK on `OPTIONS`), presigned URL issuance (`POST /api/v1/returns/evidence/presigned-url`), and binary payload upload (`PUT http://localhost:4566/grangloria-return-photos/...`).
- **Container Verification:** Confirmed via `lsof -i :5173` that port 5173 is actively bound to the Docker desktop daemon (`com.docker`) running Nginx.
- **End-to-End Execution:** Verified complete event-driven pipeline execution—from SPA submission through Kafka messaging to asynchronous return label email generation in Mailpit (`http://localhost:8025`).