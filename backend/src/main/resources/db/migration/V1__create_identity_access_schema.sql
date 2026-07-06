CREATE TABLE tenants (
    id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL,
    plan VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE organizations (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    legal_name VARCHAR(200),
    gst_number VARCHAR(30),
    email VARCHAR(150),
    phone VARCHAR(30),
    address TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_organizations_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE users (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_users_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE roles (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_roles_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE permissions (
    id UUID NOT NULL,
    code VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE refresh_tokens (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX uk_organizations_tenant_id ON organizations (tenant_id);

CREATE UNIQUE INDEX uk_users_email ON users (email);
CREATE INDEX idx_users_tenant_id ON users (tenant_id);

CREATE UNIQUE INDEX uk_roles_tenant_name ON roles (tenant_id, name);
CREATE INDEX idx_roles_tenant_id ON roles (tenant_id);

CREATE UNIQUE INDEX uk_permissions_code ON permissions (code);

CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

CREATE UNIQUE INDEX uk_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_tenant_id ON refresh_tokens (tenant_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);