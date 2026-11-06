-- Finding an existing contact for a donor by phone (digits only, last 10) or by full name, regardless of formatting.
create index contact_tenant_phone_digits_idx
    on contact (tenant_id, right(regexp_replace(coalesce(phone, ''), '[^0-9]', '', 'g'), 10));

create index contact_tenant_full_name_idx
    on contact (tenant_id, lower(regexp_replace(trim(first_name || ' ' || coalesce(last_name, '')), '\s+', ' ', 'g')));

-- Totals per donor
create index donation_tenant_contact_idx on donation (tenant_id, contact_id) where contact_id is not null;
