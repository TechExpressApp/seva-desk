-- Online gifts (Stripe) are recorded from webhooks, which Stripe may send more than once.
-- One donation per Stripe payment/invoice per temple, so a repeated webhook can't record a gift twice.
create unique index donation_online_reference_uq
    on donation (tenant_id, reference)
    where payment_mode = 'ONLINE' and reference is not null;

create index contact_tenant_email_idx on contact (tenant_id, lower(email));
