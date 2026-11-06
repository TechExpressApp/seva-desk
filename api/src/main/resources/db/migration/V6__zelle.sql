-- US-dollar temples use Zelle, not UPI (an Indian payment system). Other temples keep their records as they are.
-- Runs as the schema owner, so it updates every temple's rows.
update donation d
set payment_mode = 'ZELLE', updated_at = now()
from tenant t
where t.id = d.tenant_id and d.payment_mode = 'UPI' and t.currency = 'USD';
