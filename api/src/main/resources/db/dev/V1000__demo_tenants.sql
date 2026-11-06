-- Demo tenants for local development and tests only (loaded when the dev or test profile is active).
-- Open http://radhagovinda.localhost:4200 or http://gurunanak.localhost:4200 in Chrome or Firefox.
insert into tenant (id, slug, name, short_name, greeting, tagline, primary_color, accent_color, soft_color,
                    currency, locale, time_zone, tax_registration, terms, whatsapp_number)
values
('11111111-1111-1111-1111-111111111111', 'radhagovinda', 'Sri Radha Govinda Mandir', 'RG', 'Hare Krishna',
 'Serving the devotee community with love and devotion.', '#8A1C2B', '#E8890C', '#FFF4E3',
 'INR', 'en', 'Asia/Kolkata', '80G: AAATR0000X/DEMO',
 '{"term.priest":"Pujari","term.priests":"Pujaris","term.seva":"Seva"}', '919800000000'),
('22222222-2222-2222-2222-222222222222', 'gurunanak', 'Guru Nanak Gurdwara Sahib', 'GN', 'Waheguru Ji Ka Khalsa',
 'Langar served daily to all, in the spirit of seva and sangat.', '#1F3A93', '#F28C28', '#EEF2FF',
 'INR', 'en', 'Asia/Kolkata', null,
 '{"term.priest":"Granthi","term.priests":"Granthis","term.seva":"Path"}', '919800000001');
