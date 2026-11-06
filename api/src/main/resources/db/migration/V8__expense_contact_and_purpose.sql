-- Expenses: who was paid is a contact, and the purpose comes from a fixed list.

alter table expense
    add column contact_id uuid references contact(id),
    add column purpose    varchar(30);

-- Existing expenses: derive the purpose from the old free-text category, and keep that text in the notes.
update expense set
    purpose = case
        when lower(category) like '%grocer%'                                   then 'GROCERY'
        when lower(category) like '%book%'                                     then 'BOOKS'
        when lower(category) like '%construct%'                                then 'CONSTRUCTION'
        when lower(category) like '%maint%' or lower(category) like '%repair%' then 'BUILDING_MAINTENANCE'
        when lower(category) like '%deity%' or lower(category) like '%worship%'
          or lower(category) like '%flower%' or lower(category) like '%puja%'  then 'DEITY_WORSHIP'
        when lower(category) like '%monk%' or lower(category) like '%sannyas%' then 'MONK_DONATION'
        else 'OTHER'
    end,
    notes = left(trim(both ' ·' from coalesce(category, '') || coalesce(' · ' || payee, '') || coalesce(' · ' || notes, '')), 500);

alter table expense
    alter column purpose set not null,
    add constraint expense_purpose_check check (purpose in
        ('BUILDING_MAINTENANCE', 'CONSTRUCTION', 'DEITY_WORSHIP', 'GROCERY', 'MONK_DONATION', 'BOOKS', 'OTHER')),
    drop column category,
    drop column payee;

create index expense_tenant_contact_idx on expense (tenant_id, contact_id);

-- Expenses can be corrected or removed by the treasurer.
grant delete on expense to ${appRole};
