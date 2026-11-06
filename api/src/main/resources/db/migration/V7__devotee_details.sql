-- Devotee details on each contact: initiated name, spiritual master, and where they are on the path.
alter table contact
    add column initiated_name   varchar(100),
    add column spiritual_master varchar(100),
    add column devotee_status   varchar(20)
        check (devotee_status in ('VISITOR', 'PRACTICING', 'FIRST_INITIATED', 'SECOND_INITIATED'));

-- Until now the website's subscribe form kept these two in custom_fields; move them into the new columns.
update contact
set initiated_name   = nullif(left(trim(custom_fields ->> 'initiatedName'), 100), ''),
    spiritual_master = nullif(left(trim(custom_fields ->> 'spiritualMaster'), 100), ''),
    custom_fields    = custom_fields - 'initiatedName' - 'spiritualMaster'
where custom_fields ? 'initiatedName' or custom_fields ? 'spiritualMaster';

create index contact_tenant_status_idx on contact (tenant_id, devotee_status);
