# Login & JWT Authentication

## Goal

Allow registered users to login and receive a JWT access token for authenticated API access.

## API

POST /api/v1/auth/login

## Request

- email
- password

## Response

- accessToken
- refreshToken
- tokenType
- expiresIn
- tenantId
- userId
- roles

## Business Rules

1. Email must exist.
2. Password must match stored BCrypt hash.
3. User status must be ACTIVE.
4. Tenant status must be ACTIVE.
5. JWT must contain userId, tenantId, and roles.
6. Refresh token must be stored as a hash.
7. Raw password and raw refresh token must never be stored.
8. Login endpoint must be public.
9. All protected APIs require JWT.

## Login Flow

User submits email/password  
→ System finds user by email  
→ System validates password  
→ System validates user status  
→ System validates tenant status  
→ System loads roles  
→ System generates access token  
→ System generates refresh token  
→ System stores refresh token hash  
→ System returns token response

## Security Flow

HTTP Request  
→ JWT Authentication Filter  
→ Extract Bearer Token  
→ Validate Signature  
→ Extract Claims  
→ Build Authentication  
→ Set SecurityContext  
→ Continue to Controller

## Future Enhancements

- Refresh token rotation
- Logout
- Token revocation
- Account lockout
- MFA
- Password reset