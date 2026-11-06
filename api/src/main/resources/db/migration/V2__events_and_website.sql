-- Events (managed in SevaDesk, shown on the public website) and per-temple website settings.

create table event (
    id              uuid primary key default gen_random_uuid(),
    tenant_id       uuid not null references tenant(id),
    slug            varchar(120) not null,                 -- URL: /events/{slug}
    title           varchar(200) not null,
    summary         varchar(400),
    description     text,                                  -- simple HTML written by staff
    starts_at       timestamptz not null,
    ends_at         timestamptz,
    location        varchar(200),
    image_url       varchar(500),
    registration_url varchar(500),
    status          varchar(12) not null default 'DRAFT',  -- DRAFT, PUBLISHED, CANCELLED
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    unique (tenant_id, slug)
);
create index event_tenant_start_idx on event (tenant_id, starts_at);

alter table event enable row level security;
alter table event force row level security;
create policy tenant_isolation on event
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

grant select, insert, update, delete on event to ${appRole};

-- Public website settings per temple: menu, contact details, social links, page content.
-- Shape: see com.sevadesk.website.SiteSettings
alter table tenant add column site jsonb not null default '{}'::jsonb;
