-- Staff logins: forced password change, session invalidation, last sign-in, and one-time password links.

alter table app_user
    add column must_change_password boolean not null default false,
    -- Bumped on password change, role change or deactivation; sign-in tokens carrying an older value stop working.
    add column token_version integer not null default 0,
    add column last_login_at timestamptz;

-- One-time links for "forgot password" (1 hour) and new-user invitations (24 hours). Only a SHA-256 hash is stored.
create table password_token (
    id          uuid primary key default gen_random_uuid(),
    tenant_id   uuid not null references tenant(id),
    user_id     uuid not null references app_user(id),
    token_hash  varchar(64) not null unique,
    purpose     varchar(10) not null,            -- RESET, INVITE
    expires_at  timestamptz not null,
    used_at     timestamptz,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);
create index password_token_user_idx on password_token (tenant_id, user_id, created_at desc);

alter table password_token enable row level security;
alter table password_token force row level security;
create policy tenant_isolation on password_token
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

grant select, insert, update on password_token to ${appRole};
