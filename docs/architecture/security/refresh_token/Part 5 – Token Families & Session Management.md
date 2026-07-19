# Refresh Token Rotation
## Part 5 – Token Families & Session Management

> **Project:** OdinSync
>
> **Module:** Identity & Access Management
>
> **Document:** Refresh Token Rotation
>
> **Part:** 5 – Token Families & Session Management

---

# Overview

In the previous chapter, we learned that refresh tokens are **single-use** and are replaced every time they are successfully used.

This naturally raises an important question:

> If every refresh creates a new token, how do we know which tokens belong to the same user session?

The answer is the **Token Family**.

A token family allows OdinSync to track the complete lifecycle of a login session, detect replay attacks, revoke sessions, and support multiple devices securely.

---

# What is a Token Family?

A **Token Family** is a collection of refresh tokens that originate from the same login session.

Example:

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
```

Although the refresh token changes over time, all of these tokens belong to **one logical session**.

Instead of treating them as independent tokens, OdinSync groups them into a single family.

---

# Why Do We Need Token Families?

Imagine the following timeline.

```
Day 1

Login

↓

R1
```

After 15 minutes:

```
Refresh

↓

R2
```

After another 15 minutes:

```
Refresh

↓

R3
```

Weeks later:

```
Refresh

↓

R25
```

Without a token family, the database would contain many unrelated refresh tokens.

The server would have no efficient way to determine:

- Which tokens belong to the same login?
- Which session should be revoked?
- Which token replaced another?
- Which device owns the session?

---

# Token Family Structure

Every login creates a unique family.

Example:

```
Family A

R1

↓

R2

↓

R3

↓

R4
```

A completely different login creates another family.

```
Family B

S1

↓

S2

↓

S3
```

Each family represents one authenticated session.

---

# Multiple Device Sessions

Users commonly log in from multiple devices.

Example:

```
Laptop

↓

Family A

↓

R1 → R2 → R3
```

```
Mobile

↓

Family B

↓

M1 → M2
```

```
Tablet

↓

Family C

↓

T1
```

Each device has its own refresh-token family.

Refreshing a token on the laptop does not affect the mobile or tablet sessions.

---

# Database Representation

A simplified table might look like this:

| Token | Family | Status |
|--------|--------|--------|
| R1 | F100 | Revoked |
| R2 | F100 | Revoked |
| R3 | F100 | Active |
| M1 | F200 | Revoked |
| M2 | F200 | Active |
| T1 | F300 | Active |

Notice that:

- Each family has exactly one active refresh token.
- Older tokens remain stored for audit and replay detection.

---

# The family_id

Each refresh token contains a `family_id`.

Example:

```
+-------------------------------+
| id            = R3            |
| family_id     = F100          |
| token_hash    = ...           |
| revoked_at    = NULL          |
+-------------------------------+
```

The `family_id` never changes throughout the lifetime of the session.

Every rotated token inherits the same value.

```
R1

family_id = F100

↓

R2

family_id = F100

↓

R3

family_id = F100
```

This makes it easy to identify all tokens that belong to the same session.

---

# The replaced_by_token_id

Rotation also records which token replaced the previous one.

```
R1

↓

R2

↓

R3

↓

R4
```

This relationship is stored using `replaced_by_token_id`.

Example:

| Token | Replaced By |
|--------|-------------|
| R1 | R2 |
| R2 | R3 |
| R3 | R4 |
| R4 | NULL |

This creates a linked chain representing the evolution of the session.

---

# Why Keep Both family_id and replaced_by_token_id?

A common question is:

> If we already have `replaced_by_token_id`, why do we also need `family_id`?

The two fields solve different problems.

### `replaced_by_token_id`

Describes the **relationship between two consecutive tokens**.

```
R1

↓

R2

↓

R3
```

It tells us:

- Which token replaced the current token.
- The sequence of rotation.

---

### `family_id`

Groups **all tokens belonging to the same session**.

```
Family F100

R1

↓

R2

↓

R3

↓

R4
```

It allows us to find every token in the session using a single query.

Example:

```sql
SELECT *
FROM refresh_tokens
WHERE family_id = 'F100';
```

Without `family_id`, the server would need to recursively traverse the chain of replacements, which is slower and more complex.

---

# Why Not Delete Old Tokens?

It might seem simpler to delete R1 after creating R2.

```
R1

↓

Delete

↓

R2
```

However, this removes valuable security information.

Keeping historical tokens allows OdinSync to:

- Detect replay attacks.
- Audit session history.
- Investigate suspicious activity.
- Understand how the session evolved.

Historical records are an important part of the security model.

---

# Session Revocation

Suppose an administrator wants to terminate a user's laptop session.

Because every refresh token in that session shares the same `family_id`, the server can revoke the entire session.

```
Family F100

↓

Revoke

↓

R1

R2

R3

R4
```

Other sessions remain unaffected.

```
Laptop

✗ Revoked
```

```
Mobile

✓ Active
```

```
Tablet

✓ Active
```

This provides fine-grained session management.

---

# Benefits of Token Families

Using token families enables OdinSync to:

- Track session history.
- Support multiple devices.
- Revoke individual sessions.
- Detect replay attacks.
- Audit authentication events.
- Maintain a complete rotation chain.

Without token families, these features become significantly more difficult to implement.

---

# Design Decisions in OdinSync

The refresh-token implementation follows these principles:

- Each login creates a new token family.
- Every rotated token inherits the same `family_id`.
- Every rotation records `replaced_by_token_id`.
- Previous tokens remain in the database.
- Exactly one active refresh token exists per family.
- Different devices maintain independent token families.

---

# Looking Ahead

Token families solve the problem of organizing refresh tokens into sessions.

The next challenge is handling an abnormal situation:

```
R1

↓

R2 Created

↓

Attacker Uses R1 Again
```

How should the server respond?

- Reject only R1?
- Revoke R2?
- Revoke the entire family?
- Force the user to log in again?

The next chapter introduces **Replay Detection**, where we'll examine these scenarios and explain why OdinSync revokes the entire token family when token reuse is detected.

---

# Summary

A **Token Family** represents one authenticated session.

Every refresh token created during that session shares the same `family_id`, while `replaced_by_token_id` records the order in which tokens were rotated.

Together, these fields allow OdinSync to:

- Manage sessions efficiently.
- Support multiple devices.
- Preserve authentication history.
- Revoke sessions safely.
- Prepare for replay attack detection.

In the next chapter, we will build upon this foundation to implement secure replay detection and explain how OdinSync responds to refresh token reuse.