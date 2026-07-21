DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'organizations'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE organizations
            ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'organizations'
          AND column_name = 'updated_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE organizations
            ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';
    END IF;
END $$;

UPDATE organizations
SET
    legal_name = COALESCE(NULLIF(TRIM(legal_name), ''), NULLIF(TRIM(name), ''), 'Not provided'),
    display_name = COALESCE(NULLIF(TRIM(display_name), ''), NULLIF(TRIM(name), ''), 'Not provided'),
    tax_registration_number = COALESCE(NULLIF(TRIM(tax_registration_number), ''), NULLIF(TRIM(gst_number), '')),
    address_line1 = COALESCE(NULLIF(TRIM(address_line1), ''), NULLIF(TRIM(address), ''), 'Not provided'),
    address_city = COALESCE(NULLIF(TRIM(address_city), ''), 'Not provided'),
    address_state_or_region = COALESCE(NULLIF(TRIM(address_state_or_region), ''), 'Not provided'),
    address_postal_code = COALESCE(NULLIF(TRIM(address_postal_code), ''), '00000'),
    address_country_code = UPPER(COALESCE(NULLIF(TRIM(address_country_code), ''), 'US')),
    contact_email = LOWER(COALESCE(NULLIF(TRIM(contact_email), ''), NULLIF(TRIM(email), ''), 'unknown@example.com')),
    contact_phone = COALESCE(NULLIF(TRIM(contact_phone), ''), NULLIF(TRIM(phone), ''), '0000000000'),
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

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_organizations_status'
          AND conrelid = 'public.organizations'::regclass
    ) THEN
        ALTER TABLE organizations
            ADD CONSTRAINT chk_organizations_status
                CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_organizations_country_code'
          AND conrelid = 'public.organizations'::regclass
    ) THEN
        ALTER TABLE organizations
            ADD CONSTRAINT chk_organizations_country_code
                CHECK (address_country_code ~ '^[A-Z]{2}$');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_organizations_currency_code'
          AND conrelid = 'public.organizations'::regclass
    ) THEN
        ALTER TABLE organizations
            ADD CONSTRAINT chk_organizations_currency_code
                CHECK (currency_code ~ '^[A-Z]{3}$');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_organizations_contact_email'
          AND conrelid = 'public.organizations'::regclass
    ) THEN
        ALTER TABLE organizations
            ADD CONSTRAINT chk_organizations_contact_email
                CHECK (contact_email LIKE '%@%');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_organizations_date_format'
          AND conrelid = 'public.organizations'::regclass
    ) THEN
        ALTER TABLE organizations
            ADD CONSTRAINT chk_organizations_date_format
                CHECK (date_format IN ('DD_MM_YYYY', 'MM_DD_YYYY', 'YYYY_MM_DD'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_organizations_time_format'
          AND conrelid = 'public.organizations'::regclass
    ) THEN
        ALTER TABLE organizations
            ADD CONSTRAINT chk_organizations_time_format
                CHECK (time_format IN ('TWELVE_HOUR', 'TWENTY_FOUR_HOUR'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_organizations_week_start'
          AND conrelid = 'public.organizations'::regclass
    ) THEN
        ALTER TABLE organizations
            ADD CONSTRAINT chk_organizations_week_start
                CHECK (week_start IN ('MONDAY', 'SUNDAY', 'SATURDAY'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_organizations_audit_chronology'
          AND conrelid = 'public.organizations'::regclass
    ) THEN
        ALTER TABLE organizations
            ADD CONSTRAINT chk_organizations_audit_chronology
                CHECK (updated_at >= created_at);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_organizations_version'
          AND conrelid = 'public.organizations'::regclass
    ) THEN
        ALTER TABLE organizations
            ADD CONSTRAINT chk_organizations_version
                CHECK (version >= 0);
    END IF;
END $$;
