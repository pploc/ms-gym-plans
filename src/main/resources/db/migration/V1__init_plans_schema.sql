CREATE TABLE gym_locations (
    id VARCHAR(64) PRIMARY KEY,
    chain_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_gym_locations_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE INDEX idx_gym_locations_chain_city_status
    ON gym_locations (chain_id, city, status);

CREATE TABLE membership_plans (
    id VARCHAR(64) PRIMARY KEY,
    gym_id VARCHAR(64) NOT NULL REFERENCES gym_locations (id),
    name VARCHAR(100) NOT NULL,
    plan_type VARCHAR(20) NOT NULL,
    duration_days INT,
    price_vnd BIGINT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_membership_plans_type CHECK (plan_type IN ('MONTHLY', 'YEARLY', 'LIFETIME')),
    CONSTRAINT chk_membership_plans_price CHECK (price_vnd >= 0),
    CONSTRAINT chk_membership_plans_duration CHECK (
        (plan_type IN ('MONTHLY', 'YEARLY') AND duration_days IS NOT NULL AND duration_days > 0)
        OR (plan_type = 'LIFETIME' AND duration_days IS NULL)
    )
);

CREATE INDEX idx_membership_plans_gym_type_active
    ON membership_plans (gym_id, plan_type, active);
