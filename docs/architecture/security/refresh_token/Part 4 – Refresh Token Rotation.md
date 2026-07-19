# Refresh Token Rotation
## Part 4 – Refresh Token Rotation

> **Project:** OdinSync
>
> **Module:** Identity & Access Management
>
> **Document:** Refresh Token Rotation
>
> **Part:** 4 – Refresh Token Rotation

---

# Overview

In the previous chapter, we learned how a refresh token is issued, stored, validated, and eventually expires.

A natural question is:

> Why not keep using the same refresh token until it expires?

Although this seems simpler, it introduces significant security risks.

To address these risks, OdinSync implements **Refresh Token Rotation**, where every refresh token is **single-use**.

---

# What is Refresh Token Rotation?

Refresh Token Rotation is the process of replacing a refresh token with a brand-new refresh token every time it is successfully used.

Instead of extending the life of the existing token, the server:

1. Validates the current refresh token.
2. Generates a new access token.
3. Generates a new refresh token.
4. Revokes the previous refresh token.
5. Returns the new token pair to the client.

```
        Login

          │

          ▼

         R1

          │

          ▼

 Refresh Request

          │

          ▼

Generate R2

          │

          ▼

Revoke R1

          │

          ▼

Return R2
```

Each refresh token is valid only **once**.

---

# Why Rotation is Necessary

Consider a refresh token that remains valid for 30 days.

```
Login

↓

Refresh Token R1

↓

Valid for 30 Days
```

If an attacker steals this token, both the legitimate user and the attacker can continue refreshing access tokens until it expires.

```
             R1

        /          \

User            Attacker

      Both Can Refresh
```

The server cannot distinguish between them.

This makes token theft extremely dangerous.

---

# How Rotation Solves This Problem

With rotation enabled:

```
R1

↓

Used Successfully

↓

Generate R2

↓

R1 Revoked
```

If anyone later attempts to use **R1** again:

```
Receive R1

↓

Already Revoked

↓

Reject Request
```

The server immediately knows that something unexpected has occurred.

---

# Single-Use Refresh Tokens

In OdinSync, a refresh token may only be used once.

```
R1

✓ First Request

↓

Revoked

↓

✗ Second Request

↓

Rejected
```

This property allows OdinSync to detect replay attacks that would otherwise go unnoticed.

---

# Rotation Flow

The complete rotation process is:

```
Client

↓

POST /auth/refresh

↓

Validate Refresh Token

↓

Generate JWT

↓

Generate Refresh Token

↓

Save New Refresh Token

↓

Revoke Previous Token

↓

Return New Token Pair
```

The client replaces both tokens after every successful refresh.

---

# Why Not Extend the Existing Token?

Instead of creating R2, imagine simply updating R1's expiration.

```
R1

↓

Expiration Updated

↓

Continue Using R1
```

Problems:

- Stolen token remains usable.
- Replay detection becomes impossible.
- Session history is lost.
- Token compromise cannot be identified.

Rotation avoids all of these issues.

---

# Refresh Token Chain

Over time, a session forms a chain of refresh tokens.

```
Login

↓

R1

↓

R2

↓

R3

↓

R4

↓

R5
```

Only the latest token is active.

All previous tokens remain in the database for auditing and replay detection.

---

# Token States

A refresh token can exist in one of several states.

| State | Description |
|--------|-------------|
| Active | Can be used to refresh |
| Revoked | Replaced by another token |
| Expired | Lifetime exceeded |
| Reused | Previously revoked token presented again |

Only **Active** tokens are accepted.

---

# Database Example

```
+------+---------+-----------+
| Token| Status  | ReplacedBy|
+------+---------+-----------+
| R1   | Revoked | R2        |
| R2   | Revoked | R3        |
| R3   | Active  | NULL      |
+------+---------+-----------+
```

This history allows OdinSync to understand the evolution of a session.

---

# Benefits of Rotation

Refresh token rotation provides several security advantages.

### Shorter Exposure Window

A stolen refresh token becomes useless immediately after the legitimate user refreshes.

---

### Replay Detection

Previously revoked tokens should never appear again.

If they do, the server can treat this as suspicious activity.

---

### Better Session Tracking

Each refresh operation is recorded.

This creates a complete history of the session.

---

### Stronger Security

Attackers cannot continue using old refresh tokens indefinitely.

---

# Client Responsibility

After a successful refresh:

```
Old Tokens

↓

Delete

↓

Store New Tokens
```

The client should never continue using an older refresh token.

---

# Server Responsibility

During rotation, the server must:

- Validate the token.
- Check expiration.
- Check revocation.
- Create a new token.
- Persist the new token.
- Revoke the previous token.
- Return the new pair.

These operations should occur within a single database transaction to avoid inconsistent state.

---

# Design Decisions in OdinSync

The refresh token implementation follows these principles:

- Every refresh token is single-use.
- Every successful refresh issues a new refresh token.
- Previous refresh tokens remain stored.
- Revoked tokens are never accepted again.
- Refresh operations are transactional.
- Session history is preserved for auditing and replay detection.

---

# Looking Ahead

Rotation alone is not enough.

Consider the following situation:

```
R1

↓

User Refreshes

↓

R2 Created

↓

Attacker Uses R1 Again
```

How should the server respond?

Should it reject only R1?

Should it revoke R2?

Should it terminate the entire session?

Answering these questions requires introducing the concept of a **Token Family**.

---

# Summary

Refresh Token Rotation is a core security feature of OdinSync.

Instead of allowing the same refresh token to be reused, every successful refresh creates a new refresh token and permanently revokes the previous one.

This approach:

- Limits the usefulness of stolen tokens.
- Enables replay attack detection.
- Preserves session history.
- Improves overall session security.

In the next chapter, we will introduce **Token Families**, explain the purpose of `family_id` and `replaced_by_token_id`, and see how they enable replay detection and secure session revocation.