# OdinSync User Stories

## Epic 1: Identity & Access

### Story 1.1: Register Organization

As a business owner,  
I want to register my company on OdinSync,  
so that my business can start using the platform.

#### Acceptance Criteria

- User can submit organization name, owner name, email, and password.
- System creates a tenant.
- System creates an organization under that tenant.
- System creates the first user as Tenant Owner.
- System assigns OWNER role to the first user.
- System does not allow duplicate owner email.
- System returns a successful registration response.

---

### Story 1.2: Login

As a registered user,  
I want to login securely,  
so that I can access my organization's data.

#### Acceptance Criteria

- User can login using email and password.
- System validates credentials.
- System returns JWT access token.
- JWT contains userId, tenantId, and roles.
- Invalid credentials return 401 Unauthorized.