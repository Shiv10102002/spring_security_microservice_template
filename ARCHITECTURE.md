# 📖 Project Architecture — Explained Simply

This document explains **what this project is, how it works, and why every piece exists** — written so
that anyone (even without a microservices background) can read it top to bottom and understand the
whole system.

---

## 1. What is this project, in one sentence?

It's a **mini version of how big companies (Netflix, Amazon, Uber) build login/security systems** —
instead of one giant application doing everything, the work is split into small independent
programs ("microservices") that talk to each other over the network, each with one clear job.

Think of it like a **restaurant**:
- One person takes orders (API Gateway)
- One person cooks (Security Service)
- One person handles deliveries/SMS to customers (Notification Service)
- A manager's notebook tracks who is currently working today (Eureka - Service Registry)
- A central recipe book everyone reads from (Config Server)

No single person does everything — and if the delivery person goes on a break, the kitchen still
works (the rest of the system doesn't crash).

---

## 2. The Big Picture (Architecture Diagram)

```
                        ┌──────────────────────┐
                        │      Client App       │  (Postman / React / Mobile App)
                        └───────────┬───────────┘
                                    │  http://localhost:8085
                                    ▼
                        ┌──────────────────────┐
                        │      API Gateway       │  ← single entry door for everyone
                        └───────────┬───────────┘
                                    │  (looks up address in Eureka, then forwards)
                                    ▼
                        ┌──────────────────────┐
                        │   Security Service     │  ← login, JWT, roles, permissions
                        │ (springboot-security)  │
                        └──────┬─────────┬──────┘
                               │         │
                 reads/writes  │         │  calls over HTTP
                               ▼         ▼
                        ┌──────────┐  ┌────────────────────┐
                        │  MySQL   │  │ Notification Service │  ← sends emails (SendGrid)
                        │ (users,  │  └────────────────────┘
                        │  roles)  │
                        └──────────┘
                               ▲
                               │
                        ┌──────────┐
                        │  Redis   │  ← OTPs, login-attempt counters (fast, temporary data)
                        └──────────┘

        Behind the scenes, ALL services register themselves here so others can find them:
                        ┌──────────────────────┐
                        │     Eureka Server      │  ← the "phone book" of the system
                        └──────────────────────┘
                        ┌──────────────────────┐
                        │     Config Server      │  ← the shared settings/recipe book
                        └──────────────────────┘
```

---

## 3. Why microservices instead of one big application?

| Monolith (one big app)                              | Microservices (this project)                         |
|------------------------------------------------------|--------------------------------------------------------|
| One codebase does login, notifications, everything   | Each concern is its own small service                  |
| If one part crashes, the whole app can go down       | If Notification Service dies, login still works        |
| Hard to scale just the "busy" part                    | You can run 5 copies of just the Security Service       |
| One team, one deploy, tightly coupled                 | Independent teams/services can deploy separately        |

**Real example:** Imagine on Black Friday your login traffic spikes but nobody is resetting passwords.
In a microservices setup you can scale up *only* the Security Service (add more copies) without
touching the Notification Service at all. In a monolith, you'd have to scale the *entire* application,
wasting resources.

---

## 4. Component-by-Component — What, Why, and the Benefit

### 🧭 4.1 Eureka Server (Netflix Eureka) — Service Registry
**Port:** 8761

**What it does:** Every microservice, when it starts up, "checks in" with Eureka and says:
*"Hi, I'm `security-service`, my address is `10.0.0.5:8181`, I'm alive."* Eureka keeps this list
up to date (like a live phonebook), removing services that stop responding.

**Why we need it:** In Docker/Cloud, container IP addresses change every time you restart a
service. Without Eureka, the API Gateway would need a hardcoded IP address for the Security
Service — and that address breaks the moment the container restarts. Eureka solves this by letting
services find each other **by name** ("security-service") instead of by IP.

**Real world analogy:** It's like calling a company through their **main reception number**
instead of a specific employee's desk phone. If that employee moves desks (IP changes), you still
reach them by asking reception (Eureka) to connect you.

**Benefit:** Automatic service discovery + automatic removal of dead/unhealthy instances
(self-healing system).

---

### ⚙️ 4.2 Config Server — Centralized Configuration
**Port:** 8888

**What it does:** Instead of each service having its settings (timeouts, URLs, feature flags)
scattered in its own files, they can all pull shared configuration from one central place at
startup.

**Why we need it:** Imagine changing a setting (like a timeout value) that 5 services use. Without
a Config Server, you'd edit 5 different files and redeploy 5 services. With a Config Server, you
change it in **one place**.

**Real world analogy:** Like a company-wide policy handbook stored in one office, instead of every
department printing and keeping their own (possibly outdated) copy.

**Benefit:** Single source of truth for configuration; easier to manage multiple environments
(dev/staging/prod).

---

### 🚪 4.3 API Gateway — The Single Entry Door
**Port:** 8085 (mapped from container's 8080)

**What it does:** All client requests (Postman, frontend apps) hit the Gateway first. The Gateway
looks up the right service in Eureka and forwards ("routes") the request there. It also has a
**Circuit Breaker** — if the Security Service is down, it doesn't hang forever; it instantly
returns a friendly fallback message.

**Why we need it:** Without a gateway, your frontend would need to know the exact address of every
microservice (security-service:8181, notification-service:8182, ...). That's messy and insecure
(exposes internal services directly to the internet). The Gateway hides all of that behind **one
public URL**.

**Real world analogy:** Like a hotel receptionist — guests only ever talk to the front desk; the
receptionist figures out which department (housekeeping, room service) should handle the request.

**Benefit:**
- One public URL for everything
- Centralized routing, security, and failure handling
- If the Security Service is temporarily down, users get `"Security service is currently
  unavailable. Please try again later."` instead of a hung request or ugly stack trace
  (see `FallbackController`).

---

### 🔐 4.4 Security Service (`springboot-security-jwt-rbac-app4`) — The Core
**Port:** 8181

This is the heart of the system: handles registration, login, JWT tokens, roles & permissions
(RBAC), password reset, brute-force protection, and audit logging.

#### a) JWT (JSON Web Token) — "Digital ID Card"
**What it does:** When you log in successfully, the service gives you a signed token (JWT)
containing your user ID, role, and permissions. For every future request, you show this token
instead of your username/password again.

**Why:** Without JWT, the server would need to remember every logged-in user in memory/session
(stateful). With JWT, the server verifies the *token's signature* — it doesn't need to store
anything about your session. This makes the system **stateless**, which scales much better across
multiple servers.

**Real world analogy:** Like a **concert wristband**. Once security straps it on you (login), you
don't need to show your ID again at every gate — you just show the wristband (token). The staff
can verify it's genuine just by looking at it (signature check), without calling the ticket office
every time.

This project uses **two tokens**:
- **Access Token** (short-lived, ~15 min) — used for every API call
- **Refresh Token** (long-lived, ~7 days) — used only to get a new Access Token when it expires,
  without forcing the user to log in again

#### b) RBAC (Role-Based Access Control) — "Who can do what"
**What it does:** Every `User` has one `Role` (e.g., `ADMIN`, `EMPLOYEE`). Every `Role` has a set of
`Permission`s (e.g., `VIEW_AUDIT_LOGS`, `APPROVE_REGISTRATION`). Controllers protect endpoints with
`@PreAuthorize("hasAuthority('APPROVE_REGISTRATION')")` — meaning only users whose role includes
that permission can call that API.

**Why:** Without RBAC, you'd have to write custom "if user is admin" checks scattered everywhere in
code — messy and error-prone. RBAC centralizes "who can do what" into data (roles & permissions
tables), so granting/revoking access is a database change, not a code change.

**Real world analogy:** Like an **office keycard system** — a keycard (role) opens certain doors
(permissions). A cleaner's card opens storage rooms; a manager's card also opens the server room.
Nobody needs to personally check ID at every door — the card system does it.

#### c) Brute-force Protection (Redis-backed)
**What it does:** `RedisLoginAttemptService` counts failed login attempts per username in Redis. After
too many failures (configurable, e.g. 5), the account is locked for a period (e.g. 15 minutes).

**Why Redis and not MySQL for this?** Redis is an in-memory database — extremely fast for small,
temporary counters that expire automatically (`TTL`). Using MySQL for something that changes on
every failed login attempt and expires after 30 minutes would be slower and creates unnecessary
write load on your main database.

**Real world analogy:** Like a **bouncer at a club** who mentally keeps a quick tally of how many
times someone tried a fake ID tonight — he doesn't file a permanent paperwork record for it, just a
temporary note that resets in the morning.

#### d) OTP via Redis (`RedisOtpService`)
**What it does:** When registering or resetting a password, a One-Time-Password is generated,
stored in Redis with a 5-minute expiry, and emailed to the user (via Notification Service). The user
enters the OTP to prove they own that email.

**Why Redis:** OTPs are short-lived by nature (5 min) — Redis's built-in expiry (`TTL`) automatically
deletes them without needing a cleanup job.

#### e) Audit Logging
**What it does:** Every important action (login, approval, permission change...) is written to the
`audit_logs` table — who did it, when, from which IP/device, and whether it succeeded or failed.

**Why:** If something goes wrong (a security incident, or "who deleted this user?"), you need a
paper trail. Audit logs answer "who did what, when, from where."

**Real world analogy:** Like CCTV footage + a visitor sign-in book for a secure building — not used
day-to-day, but essential when investigating an incident.

#### f) Registration Approval Workflow
**What it does:** When an employee self-registers, their account sits in `PENDING_APPROVAL` status
with a `requestedRole`. An admin (with `APPROVE_REGISTRATION` permission) reviews and approves or
rejects it via `RegistrationApprovalController`.

**Why:** Prevents anyone from registering and immediately granting themselves a powerful role — a
human with the right permission must approve elevated access first.

---

### 📧 4.5 Notification Service — "The Messenger"
**Port:** 8182

**What it does:** A dedicated service whose only job is sending emails (via SendGrid) — OTPs,
registration approval notices, password-reset links, etc.

**Why a separate service instead of sending emails directly from Security Service?**
1. **Separation of concerns** — Security Service shouldn't need to know *how* email delivery
   works (SendGrid API, templates, retries).
2. **Reusability** — Any future service (e.g., an Orders service) could reuse the same
   Notification Service instead of re-implementing email logic.
3. **Resilience** — If SendGrid is slow/down, it only affects the Notification Service, not login/auth.

**Security between services:** The Security Service calls the Notification Service over plain HTTP
inside the Docker network, but it must include a secret `X-Internal-Api-Key` header
(`InternalApiKeyFilter`). This stops anyone else on the network from bombarding the Notification
Service with fake email requests — only trusted internal services carrying the shared secret key
can call it.

**Real world analogy:** Like using a **courier company** to deliver letters — you (Security
Service) don't manage trucks and delivery routes yourself, you just hand your letter (email
request) to the courier along with your company ID badge (API key) to prove you're a legitimate
client.

---

### 🐬 4.6 MySQL — The Permanent Record Keeper
**Port:** 3307 (mapped from container's 3306)

**What it stores:** Users, Roles, Permissions, Audit Logs, Notifications, Password Reset Tokens —
anything that must be **permanent and structured** (relationships between tables matter: a user
belongs to a role, a role has many permissions).

**Why a relational database:** Because RBAC is inherently relational — "which permissions does
this role have" is a classic many-to-many relationship, which SQL databases handle naturally with
joins.

---

### ⚡ 4.7 Redis — The Fast, Temporary Memory
**Port:** 6379

**What it stores:** OTPs (5 min expiry), failed login attempt counters (30 min expiry) — anything
**short-lived** that needs to be read/written extremely fast and doesn't need to survive forever.

**Why not just use MySQL for these too:** Redis lives in RAM, so reads/writes are near-instant
(microseconds vs milliseconds), and it has built-in automatic expiry (TTL) — no cron job needed to
clean up old OTPs. Using MySQL for high-frequency, short-lived data would be slower and add
unnecessary load/rows to your main database.

**Real world analogy:** Redis is like a **sticky note on your desk** (fast to write, fast to read,
gets thrown away after a while) versus MySQL, which is like a **filing cabinet** (slower to access,
but permanent and organized).

---

## 5. A Real Example — Full Request Flow

Let's trace what happens when a **new employee registers, gets approved, and logs in**:

### Step 1 — Registration
1. Client calls `POST /api/v1/users/send-registration-otp` with an email → **API Gateway** →
   routes to **Security Service**.
2. Security Service generates a 6-digit OTP, saves it in **Redis** (5-min expiry), and calls
   **Notification Service** (with the internal API key) to email the OTP via **SendGrid**.
3. User submits the OTP + registration details → `POST /api/v1/users/register`. Security Service
   verifies the OTP against Redis, creates a `User` row in **MySQL** with `status=PENDING_APPROVAL`.

### Step 2 — Admin Approval
4. An admin logs in (see Step 3 below) and calls `GET /api/v1/approvals/registrations/pending`
   (requires `VIEW_PENDING_REGISTRATIONS` permission).
5. Admin calls `POST /api/v1/approvals/registrations/{id}/approve` (requires
   `APPROVE_REGISTRATION` permission). The user's `status` becomes `ACTIVE` and gets their
   requested `Role` assigned. An audit log entry (`AuditLog`) records this approval action.

### Step 3 — Login
6. User calls `POST /api/v1/auth/login` with username/password → API Gateway → Security Service.
7. Security Service checks Redis for existing failed-attempt lockouts, verifies the password, and
   on success:
   - Generates an **Access Token** (15 min) and **Refresh Token** (7 days), both JWTs signed with
     `JWT_SECRET`.
   - Saves the tokens in MySQL (`UserToken` table) for session tracking (so tokens can be revoked).
   - Resets the Redis failed-attempt counter.
   - Writes an audit log entry: `action=LOGIN, status=SUCCESS`.

### Step 4 — Accessing a Protected Resource
8. User calls `GET /api/v1/admin/statistics/security` with header `Authorization: Bearer <accessToken>`.
9. Request hits **API Gateway** → forwarded to **Security Service**.
10. `JwtFilter` intercepts the request:
    - Verifies the JWT signature is valid and not expired.
    - Confirms it's an `ACCESS` token (not a refresh token).
    - Checks the token still exists and isn't revoked in MySQL (`UserTokenRepository`).
    - Extracts `userId`, `role`, `permissions` from the token and builds Spring Security
      "authorities" (e.g., `ROLE_ADMIN`, `VIEW_SECURITY_STATISTICS`).
11. `@PreAuthorize("hasAuthority('VIEW_SECURITY_STATISTICS')")` on `AdminController` checks the
    user has that specific permission — if yes, the request proceeds; if no, `403 Forbidden`.

### Step 5 — What if Security Service is temporarily down?
12. If Security Service crashes mid-deployment, the API Gateway's Circuit Breaker detects the
    failure and instantly returns:
    `{"success": false, "message": "Security service is currently unavailable. Please try again later.", "status": 503}`
    — instead of the client waiting 30+ seconds for a timeout.

---

## 6. How do services "find" each other? (Service Discovery in action)

1. Every service (Security, Notification, API Gateway) registers itself with **Eureka** on startup,
   under a logical name (e.g. `springboot-security-jwt-rbac-app1`).
2. The API Gateway's routes use `lb://springboot-security-jwt-rbac-app1` (note the `lb://` — "load
   balanced") instead of a fixed IP. At request time, it asks Eureka *"where is
   springboot-security-jwt-rbac-app1 right now?"*, gets the current address, and forwards the
   request there.
3. If you scaled Security Service to 3 running copies, Eureka would know about all 3, and the
   Gateway would automatically load-balance requests across them.

---

## 7. Quick Recap: Why Each Piece Exists (Cheat Sheet)

| Component | Problem it Solves | One-line Benefit |
|---|---|---|
| Eureka Server | Services' IPs change constantly in Docker/Cloud | Services find each other by name, not IP |
| Config Server | Config scattered across many services | One place to manage shared settings |
| API Gateway | Clients need to know every service's address | One public door; hides internals; handles failures gracefully |
| JWT | Server shouldn't need to remember every session | Stateless, scalable authentication |
| RBAC (Role/Permission) | Hardcoded "if admin" checks are messy | Access control lives in data, not code |
| Redis (OTP/login attempts) | Short-lived data doesn't belong in a permanent DB | Fast, auto-expiring temporary storage |
| MySQL | Users/roles/permissions need structured, permanent storage | Reliable relational data with integrity |
| Notification Service | Security Service shouldn't handle email delivery details | Separation of concerns + reusability |
| Internal API Key | Notification Service shouldn't accept requests from anyone | Only trusted internal services can call it |
| Audit Logs | Need to know "who did what, when" | Traceability for security investigations |
| Circuit Breaker / Fallback | A dead service shouldn't hang the whole request chain | Fast, graceful failure instead of timeouts |

---

## 8. Where to look in the code

| Concern | Key files |
|---|---|
| JWT creation/validation | `springboot-security-jwt-rbac-app4/.../utility/JwtUtility.java`, `filter/JwtFilter.java` |
| Login/Auth logic | `service/AuthServiceImpl.java`, `controller/AuthController.java` |
| Roles & Permissions | `entity/Role.java`, `entity/Permission.java`, `security/RoleInitializationService.java` |
| Brute-force protection | `service/RedisLoginAttemptService.java` |
| OTP handling | `service/RedisOtpService.java` |
| Registration approval | `controller/RegistrationApprovalController.java`, `service/AdminServiceImpl.java` |
| Audit logging | `entity/AuditLog.java`, `service/AuditService.java` |
| Calling Notification Service | `client/NotificationClientImpl.java`, `client/NotificationFacadeImpl.java` |
| Internal service auth | `notification-service/.../filter/InternalApiKeyFilter.java` |
| Gateway routing/fallback | `api-gateway/.../application.properties`, `FallbackController.java` |
| Service registry config | `eureka-server/.../application.properties` |
| Shared config | `config-server/.../config-repo/*.properties` |

---

*For setup/run instructions, see the main [README.md](./README.md).*
