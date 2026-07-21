ALTER TABLE organizations
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE organizations
    ADD COLUMN display_name VARCHAR(120) DEFAULT 'Not provided',
    ADD COLUMN tax_registration_number VARCHAR(50),
    ADD COLUMN address_line1 VARCHAR(200) DEFAULT 'Not provided',
    ADD COLUMN address_line2 VARCHAR(200),
    ADD COLUMN address_city VARCHAR(100) DEFAULT 'Not provided',
    ADD COLUMN address_state_or_region VARCHAR(100) DEFAULT 'Not provided',
    ADD COLUMN address_postal_code VARCHAR(20) DEFAULT '00000',
    ADD COLUMN address_country_code VARCHAR(2) DEFAULT 'US',
    ADD COLUMN contact_email VARCHAR(254) DEFAULT 'unknown@example.com',
    ADD COLUMN contact_phone VARCHAR(30) DEFAULT '0000000000',
    ADD COLUMN contact_website VARCHAR(500),
    ADD COLUMN currency_code VARCHAR(3) DEFAULT 'USD',
    ADD COLUMN time_zone VARCHAR(100) DEFAULT 'UTC',
    ADD COLUMN locale VARCHAR(20) DEFAULT 'en-US',
    ADD COLUMN date_format VARCHAR(30) DEFAULT 'MM_DD_YYYY',
    ADD COLUMN time_format VARCHAR(20) DEFAULT 'TWENTY_FOUR_HOUR',
    ADD COLUMN week_start VARCHAR(15) DEFAULT 'MONDAY',
    ADD COLUMN status VARCHAR(30) DEFAULT 'ACTIVE',
    ADD COLUMN created_by UUID DEFAULT '00000000-0000-0000-0000-000000000000',
    ADD COLUMN updated_by UUID DEFAULT '00000000-0000-0000-0000-000000000000',
    ADD COLUMN version BIGINT DEFAULT 0;

UPDATE organizations
SET
    legal_name = COALESCE(NULLIF(TRIM(legal_name), ''), NULLIF(TRIM(name), ''), 'Not provided'),
    display_name = COALESCE(NULLIF(TRIM(name), ''), NULLIF(TRIM(display_name), ''), 'Not provided'),
    tax_registration_number = COALESCE(NULLIF(TRIM(tax_registration_number), ''), NULLIF(TRIM(gst_number), '')),
    address_line1 = COALESCE(NULLIF(TRIM(address), ''), NULLIF(TRIM(address_line1), ''), 'Not provided'),
    address_city = COALESCE(NULLIF(TRIM(address_city), ''), 'Not provided'),
    address_state_or_region = COALESCE(NULLIF(TRIM(address_state_or_region), ''), 'Not provided'),
    address_postal_code = COALESCE(NULLIF(TRIM(address_postal_code), ''), '00000'),
    address_country_code = UPPER(COALESCE(NULLIF(TRIM(address_country_code), ''), 'US')),
    contact_email = LOWER(COALESCE(NULLIF(TRIM(email), ''), NULLIF(TRIM(contact_email), ''), 'unknown@example.com')),
    contact_phone = COALESCE(NULLIF(TRIM(phone), ''), NULLIF(TRIM(contact_phone), ''), '0000000000'),
    currency_code = UPPER(COALESCE(NULLIF(TRIM(currency_code), ''), 'USD')),
    time_zone = COALESCE(NULLIF(TRIM(time_zone), ''), 'UTC'),
    locale = COALESCE(NULLIF(TRIM(locale), ''), 'en-US'),
    date_format = COALESCE(NULLIF(TRIM(date_format), ''), 'MM_DD_YYYY'),
    time_format = COALESCE(NULLIF(TRIM(time_format), ''), 'TWENTY_FOUR_HOUR'),
    week_start = COALESCE(NULLIF(TRIM(week_start), ''), 'MONDAY'),
    status = COALESCE(NULLIF(TRIM(status), ''), 'ACTIVE'),
    created_by = COALESCE(created_by, '00000000-0000-0000-0000-000000000000'),
    updated_by = COALESCE(updated_by, '00000000-0000-0000-0000-000000000000'),
    version = COALESCE(version, 0);

ALTER TABLE organizations
    ALTER COLUMN legal_name SET NOT NULL,
    ALTER COLUMN display_name SET NOT NULL,
    ALTER COLUMN address_line1 SET NOT NULL,
    ALTER COLUMN address_city SET NOT NULL,
    ALTER COLUMN address_state_or_region SET NOT NULL,
    ALTER COLUMN address_postal_code SET NOT NULL,
    ALTER COLUMN address_country_code SET NOT NULL,
    ALTER COLUMN contact_email SET NOT NULL,
    ALTER COLUMN contact_phone SET NOT NULL,
    ALTER COLUMN currency_code SET NOT NULL,
    ALTER COLUMN time_zone SET NOT NULL,
    ALTER COLUMN locale SET NOT NULL,
    ALTER COLUMN date_format SET NOT NULL,
    ALTER COLUMN time_format SET NOT NULL,
    ALTER COLUMN week_start SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN updated_by SET NOT NULL,
    ALTER COLUMN version SET NOT NULL;

ALTER TABLE organizations
    ADD CONSTRAINT chk_organizations_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED')),
    ADD CONSTRAINT chk_organizations_country_code
        CHECK (address_country_code ~ '^[A-Z]{2}$'),
    ADD CONSTRAINT chk_organizations_currency_code
        CHECK (currency_code ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT chk_organizations_contact_email
        CHECK (contact_email LIKE '%@%'),
    ADD CONSTRAINT chk_organizations_date_format
        CHECK (date_format IN ('DD_MM_YYYY', 'MM_DD_YYYY', 'YYYY_MM_DD')),
    ADD CONSTRAINT chk_organizations_time_format
        CHECK (time_format IN ('TWELVE_HOUR', 'TWENTY_FOUR_HOUR')),
    ADD CONSTRAINT chk_organizations_week_start
        CHECK (week_start IN ('MONDAY', 'SUNDAY', 'SATURDAY')),
    ADD CONSTRAINT chk_organizations_audit_chronology
        CHECK (updated_at >= created_at),
    ADD CONSTRAINT chk_organizations_version
        CHECK (version >= 0);
