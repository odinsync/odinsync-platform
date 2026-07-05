# Register Organization Use Case

## Goal

Allow a new company to register on OdinSync and become a new tenant.

---

## Actor

Business Owner

---

## Preconditions

- Email must not already exist.
- Company name must be provided.
- Password must satisfy security policy.

---

## Input

- Organization Name
- Legal Name (optional)
- Owner Name
- Email
- Password

---

## Business Rules

1. Email must be unique.
2. Password must be hashed using BCrypt.
3. A new tenant must be created.
4. A new organization must be linked to the tenant.
5. A new owner user must be created.
6. OWNER role must be assigned.
7. Default subscription plan is FREE.
8. Tenant status is ACTIVE.
9. User status is ACTIVE.

---

## Success Flow

1. Validate request.
2. Check email uniqueness.
3. Create tenant.
4. Create organization.
5. Create user.
6. Assign OWNER role.
7. Commit transaction.
8. Return success response.

---

## Failure Scenarios

- Email already exists.
- Invalid password.
- Database error.
- Unexpected system failure.

---

## Response

HTTP 201 Created

Returns:

- Tenant ID
- Organization ID
- User ID
- Success message

---

## Transaction Boundary

The entire registration process executes in one database transaction.

If any step fails:

- Roll back tenant creation.
- Roll back organization creation.
- Roll back user creation.
- Roll back role assignment.