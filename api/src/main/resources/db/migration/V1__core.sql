-- SevaDesk core schema.
-- Every tenant-scoped table has tenant_id and a row-level-security policy keyed on the
-- session setting app.tenant_id, which TenantAwareDataSource sets on every connection checkout.
-- The application connects as ${appRole} (not the owner, not a superuser), so RLS always applies.

create table tenant (
    id                  uuid primary key default gen_random_uuid(),
    slug                varchar(60)  not null unique,          -- radhagovinda -> radhagovinda.sevadesk.app
    custom_domain       varchar(255) unique,                   -- admin.mytemple.org
    name                varchar(200) not null,
    short_name          varchar(10)  not null,                 -- used in receipt numbers and the logo monogram
    greeting            varchar(120) not null default 'Welcome',
    tagline             varchar(300),
    primary_color       varchar(9)   not null default '#8A1C2B',
    accent_color        varchar(9)   not null default '#E8890C',
    soft_color          varchar(9)   not null default '#FFF4E3',
    logo_url            varchar(500),
    currency            varchar(3)      not null default 'INR',
    locale              varchar(10)  not null default 'en',
    time_zone           varchar(60)  not null default 'Asia/Kolkata',
    fiscal_year_start   integer      not null default 4 check (fiscal_year_start between 1 and 12),
    tax_registration    varchar(100),                          -- e.g. 80G / 501(c)(3) number for receipts
    terms               jsonb        not null default '{}'::jsonb,   -- vocabulary overrides: {"term.priest":"Archaka"}
    enabled_modules     jsonb        not null default '["contacts","donors","finance","events","seva","guesthouse","members","volunteers"]'::jsonb,
    whatsapp_number     varchar(20),
    active              boolean      not null default true,
    created_at          timestamptz  not null default now()
);

create table app_user (
    id              uuid primary key default gen_random_uuid(),
    tenant_id       uuid not null references tenant(id),
    email           varchar(200) not null,
    password_hash   varchar(200) not null,
    display_name    varchar(120) not null,
    roles           varchar(200) not null,          -- comma separated: ADMIN,TREASURER,FRONT_DESK,...
    active          boolean not null default true,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    unique (tenant_id, email)
);

create table contact (
    id              uuid primary key default gen_random_uuid(),
    tenant_id       uuid not null references tenant(id),
    first_name      varchar(100) not null,
    last_name       varchar(100),
    email           varchar(200),
    phone           varchar(30),
    city            varchar(100),
    gotra           varchar(60),
    nakshatra       varchar(60),
    birth_date      date,
    custom_fields   jsonb not null default '{}'::jsonb,
    consent_email   boolean not null default false,
    consent_whatsapp boolean not null default false,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);
create index contact_tenant_name_idx on contact (tenant_id, lower(first_name), lower(last_name));
create index contact_tenant_phone_idx on contact (tenant_id, phone);

create table fund (
    id              uuid primary key default gen_random_uuid(),
    tenant_id       uuid not null references tenant(id),
    name            varchar(100) not null,                 -- Annadanam, Gau Seva, Building Fund
    tax_deductible  boolean not null default true,
    active          boolean not null default true,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    unique (tenant_id, name)
);

create table receipt_counter (
    tenant_id       uuid not null references tenant(id),
    fiscal_year     varchar(9) not null,                   -- 2026-27
    last_no         integer not null,
    primary key (tenant_id, fiscal_year)
);

create table donation (
    id              uuid primary key default gen_random_uuid(),
    tenant_id       uuid not null references tenant(id),
    contact_id      uuid references contact(id),
    fund_id         uuid not null references fund(id),
    amount          numeric(12,2) not null check (amount > 0),
    currency        varchar(3) not null,
    payment_mode    varchar(20) not null,                  -- CASH, CHEQUE, UPI, CARD, BANK_TRANSFER, ONLINE, IN_KIND
    reference       varchar(100),
    donated_at      timestamptz not null default now(),
    receipt_no      varchar(40) not null,
    status          varchar(10) not null default 'RECORDED',   -- RECORDED, VOIDED (never deleted)
    void_reason     varchar(300),
    notes           varchar(500),
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    unique (tenant_id, receipt_no)
);
create index donation_tenant_date_idx on donation (tenant_id, donated_at desc);

create table expense (
    id              uuid primary key default gen_random_uuid(),
    tenant_id       uuid not null references tenant(id),
    category        varchar(60) not null,                  -- Annadanam, Electricity, Flowers, Priest honorarium
    fund_id         uuid references fund(id),
    amount          numeric(12,2) not null check (amount > 0),
    spent_on        date not null,
    payee           varchar(150),
    notes           varchar(500),
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);
create index expense_tenant_date_idx on expense (tenant_id, spent_on desc);

-- Row-level security ------------------------------------------------------------------------
do $$
declare t text;
begin
  foreach t in array array['app_user','contact','fund','receipt_counter','donation','expense'] loop
    execute format('alter table %I enable row level security', t);
    execute format('alter table %I force row level security', t);
    execute format($p$create policy tenant_isolation on %I
        using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
        with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)$p$, t);
  end loop;
end $$;

-- Application role privileges (the role itself is created outside Flyway, see infra/db/init) --
grant usage on schema public to ${appRole};
grant select on tenant to ${appRole};
grant select, insert, update on app_user, contact, fund, receipt_counter, donation, expense to ${appRole};
grant delete on contact to ${appRole};
