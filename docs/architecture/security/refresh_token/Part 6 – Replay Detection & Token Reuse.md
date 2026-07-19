# Refresh Token Rotation
## Part 6 – Replay Detection & Token Reuse

> **Project:** OdinSync
>
> **Module:** Identity & Access Management
>
> **Document:** Refresh Token Rotation
>
> **Part:** 6 – Replay Detection & Token Reuse

---

# Overview

In the previous chapter, we introduced **Token Families** and learned how every refresh token belongs to a single authenticated session.

This chapter answers one of the most important security questions:

> **What happens if a previously revoked refresh token is used again?**

This situation is called **Refresh Token Reuse** or a **Replay Attack**.

A secure authentication system must detect this behavior and respond appropriately.

---

# What is a Replay Attack?

A replay attack occurs when a previously valid token is captured and later presented again.

For refresh tokens, this means an attacker (or another client) attempts to use a refresh token that has already been replaced.

Example:

```
Login

↓

R1

↓

Refresh

↓

R2

↓

Attacker Uses R1 Again
```

Since **R1** was already replaced by **R2**, it should never appear again.

Its reuse is considered suspicious.

---

# Why Is Token Reuse Dangerous?

Imagine the following scenario.

```
Day 1

User Logs In

↓

Refresh Token R1
```

Later:

```
User Refreshes

↓

R2 Created

↓

R1 Revoked
```

Unknown to the user, an attacker has already copied R1.

```
Attacker

↓

Uses R1
```

If the server simply ignores the reuse and only rejects R1, the attacker may already possess other valid credentials or may continue probing the session.

A reused refresh token is evidence that:

- the token may have been stolen,
- another device is using copied credentials,
- or the client is behaving unexpectedly.

---

# Replay Detection Flow

```
Receive Refresh Token
          │
          ▼
Find Matching Session
          │
          ▼
Is Token Active?
      │         │
     Yes        No
      │         │
      ▼         ▼
 Continue   Possible Replay
                  │
                  ▼
      Revoke Entire Token Family
                  │
                  ▼
        Force User Login
```

---

# Why Rejecting Only the Token Is Not Enough

Suppose we only reject R1.

```
R1

↓

Rejected
```

Meanwhile:

```
R2

↓

Still Active
```

The server has already detected suspicious behavior, but it allows the session to continue.

This leaves open the possibility that the attacker has already compromised the session.

Rejecting only the old token is therefore not sufficient.

---

# Recommended Response

When a revoked refresh token is presented:

1. Reject the request.
2. Revoke every refresh token in the same family.
3. Require the user to authenticate again.

```
Family F100

R1

↓

R2

↓

R3

↓

Replay Detected

↓

Revoke Entire Family
```

This immediately terminates the compromised session.

---

# Why Revoke the Entire Family?

Consider this timeline.

```
User

↓

R1

↓

Refresh

↓

R2
```

Before the user receives R2, an attacker copies R1.

Later:

```
Attacker

↓

Uses R1
```

The server cannot determine:

- whether the attacker already obtained R2,
- whether the user's device is compromised,
- or whether another client is impersonating the user.

The safest assumption is that the session can no longer be trusted.

Therefore, the entire session is revoked.

---

# Example Timeline

### Step 1

```
Login

↓

R1
```

---

### Step 2

```
Refresh

↓

R2

↓

R1 Revoked
```

---

### Step 3

```
Attacker Uses R1
```

---

### Step 4

```
Replay Detected
```

---

### Step 5

```
Revoke

R2

↓

Session Ends
```

The next refresh attempt using R2 will also fail.

The user must log in again.

---

# Can Replay Happen Without an Attacker?

Yes.

Although replay often indicates token theft, there are legitimate situations where the same refresh token may be submitted twice.

Examples include:

- Client retry after a network timeout
- Browser sending duplicate requests
- Multiple browser tabs refreshing simultaneously
- Mobile application retrying automatically
- Load balancer retrying a request

Example:

```
Request A

↓

Refresh R1

↓

Success
```

At nearly the same time:

```
Request B

↓

Refresh R1

↓

Already Revoked
```

Without proper concurrency handling, this appears identical to a replay attack.

For this reason, OdinSync performs refresh operations inside a transaction with database locking (covered in a later chapter).

---

# Logging Replay Events

Replay detection should always be recorded.

Example log:

```text
Replay detected

User ID      : 12345
Family ID    : F100
Token ID     : R1
IP Address   : 192.168.10.25
User Agent   : Chrome 138
Timestamp    : 2026-07-19T12:15:30Z
```

These logs help:

- investigate security incidents,
- detect suspicious patterns,
- support audit requirements.

Sensitive token values should **never** be written to logs.

---

# User Experience

When replay detection occurs:

```
Client

↓

401 Unauthorized

↓

Display

"Your session has expired. Please sign in again."
```

Avoid exposing security details such as:

- "Replay attack detected"
- "Refresh token stolen"

These messages provide unnecessary information to potential attackers.

---

# Design Decisions in OdinSync

The replay detection strategy follows these principles:

- Refresh tokens are single-use.
- Reusing a revoked refresh token is considered suspicious.
- Replay detection terminates the affected session.
- All tokens in the same family are revoked.
- Users authenticate again to establish a trusted session.
- Replay events are logged for auditing.

---

# Looking Ahead

Replay detection relies on the server knowing:

- which refresh token was presented,
- which session it belongs to,
- whether it has already been revoked,
- and how tokens are stored.

The next chapter introduces the database schema that supports these operations, including:

- `token_hash`
- `family_id`
- `replaced_by_token_id`
- `revoked_at`
- `expires_at`

and explains why each field exists.

---

# Summary

Replay detection protects OdinSync from stolen or duplicated refresh tokens.

When a revoked refresh token is used again:

- the request is rejected,
- the entire token family is revoked,
- the user must authenticate again,
- and the event is recorded for auditing.

This approach favors security over convenience and ensures that potentially compromised sessions cannot continue.