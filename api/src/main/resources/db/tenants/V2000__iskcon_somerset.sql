-- ISKCON Somerset, NJ. Served at https://www.iskconsomerset.org (admin at /admin).
-- The first admin login is created at deploy time from BOOTSTRAP_* environment variables (see AdminBootstrap).
insert into tenant (slug, custom_domain, name, short_name, greeting, tagline,
                    primary_color, accent_color, soft_color,
                    currency, locale, time_zone, fiscal_year_start, tax_registration, terms)
values ('iskconsomerset', 'iskconsomerset.org', 'ISKCON Somerset', 'ISK', 'Hare Krishna',
        'Enhancing lives through bhakti yoga in Somerset, New Jersey.',
        '#B5462B', '#E8890C', '#FFF4E3',
        'USD', 'en', 'America/New_York', 1, null,
        '{"term.priest":"Pujari","term.priests":"Pujaris","term.seva":"Seva"}')
on conflict (slug) do nothing;
