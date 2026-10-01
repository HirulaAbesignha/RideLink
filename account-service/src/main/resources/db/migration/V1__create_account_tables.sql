CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_account_role CHECK (role IN ('PASSENGER', 'DRIVER', 'ADMIN')),
    CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE account_status_audit (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    changed_by UUID NOT NULL,
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_audit_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_audit_admin FOREIGN KEY (changed_by) REFERENCES accounts(id),
    CONSTRAINT chk_previous_status CHECK (previous_status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT chk_new_status CHECK (new_status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_account_status ON accounts (status);
CREATE INDEX idx_account_audit_account ON account_status_audit (account_id, changed_at);
