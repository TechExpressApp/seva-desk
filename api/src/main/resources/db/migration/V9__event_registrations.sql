-- Event registrations: who signed up for which event. Each registrant is also saved (or matched) as a contact,
-- and the temple's notification address gets an email for every registration.

alter table tenant add column notification_email varchar(254);   -- where registration emails go

create table event_registration (
    id          uuid primary key default gen_random_uuid(),
    tenant_id   uuid not null references tenant(id),
    event_id    uuid references event(id),                      -- set when the name matches one of our events
    event_name  varchar(200) not null,
    contact_id  uuid references contact(id),
    first_name  varchar(100) not null,
    last_name   varchar(100) not null,
    email       varchar(254) not null,
    whatsapp    varchar(40)  not null,
    city        varchar(100) not null,
    state       varchar(100) not null,
    age         integer      not null check (age between 0 and 120),
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now()
);
create index event_registration_tenant_event_idx on event_registration (tenant_id, lower(event_name), created_at desc);
create index event_registration_tenant_contact_idx on event_registration (tenant_id, contact_id);

alter table event_registration enable row level security;
alter table event_registration force row level security;
create policy tenant_isolation on event_registration
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

grant select, insert, delete on event_registration to ${appRole};
