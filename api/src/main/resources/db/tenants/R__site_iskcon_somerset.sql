-- Public website settings for ISKCON Somerset: menu (same pages and URLs as the previous site), contact details,
-- home page text and page content. Edit this file and restart the app: Flyway re-applies it whenever it changes.
-- Page text below is placeholder copy; replace it with the temple's own wording. Body values are HTML.
update tenant set site = $json$
{
  "heroTitle": "Welcome to ISKCON Somerset",
  "heroText": "A home for bhakti yoga in central New Jersey: kirtan, Bhagavad Gita study, festivals and prasadam, open to everyone.",
  "mantra": "Hare Krishna Hare Krishna Krishna Krishna Hare Hare · Hare Rama Hare Rama Rama Rama Hare Hare ·",
  "gatherings": {
    "eyebrow": "Come together for",
    "title": "Weekly Gatherings",
    "items": [
      {
        "title": "Kirtan & Meditation",
        "text": "Chant the Hare Krishna maha-mantra together in uplifting kirtan and quiet japa meditation. No experience needed; bring your friends and family.",
        "href": "/events",
        "button": "Join us"
      },
      {
        "title": "Bhagavad Gita Classes",
        "text": "Read and discuss the Bhagavad Gita As It Is each week, in person at the temple or online, followed by kirtan and prasadam.",
        "href": "/bg-inperson",
        "button": "Join us"
      },
      {
        "title": "Bhakti Vriksha & Families",
        "text": "Small home groups where families, youth and children share kirtan, stories and friendship, and grow in bhakti together.",
        "href": "/bhakti-vriksha",
        "button": "Join us"
      }
    ]
  },
  "founder": {
    "eyebrow": "Founder-Acharya of ISKCON · His Divine Grace",
    "title": "Srila Prabhupada",
    "verse": "harer nāma harer nāma harer nāmaiva kevalam",
    "verseMeaning": "The holy name, the holy name, only the holy name of the Lord",
    "body": "<p>His Divine Grace A.C. Bhaktivedanta Swami Prabhupada arrived in New York in 1965, at the age of 69, carrying his spiritual master's instruction to share Krishna consciousness in the English-speaking world. In 1966, from a small storefront on the Lower East Side, he founded the International Society for Krishna Consciousness.</p><p>In the eleven years that followed he travelled around the world many times, opened more than a hundred temples, farms and schools, and translated <b>Bhagavad-gītā As It Is</b>, <b>Śrīmad-Bhāgavatam</b> and <b>Śrī Caitanya-caritāmṛta</b> with his own commentary. Every kirtan, class and plate of prasadam at our temple continues the work he began.</p>",
    "href": "/srila-prabhupada",
    "button": "Learn more"
  },
  "menu": [
    {
      "label": "Home",
      "href": "/"
    },
    {
      "label": "Bhakti Vriksha",
      "href": "/bhakti-vriksha"
    },
    {
      "label": "Donate",
      "href": "/donate",
      "children": [
        {
          "label": "Nitya Sevak",
          "href": "/nityasevak"
        },
        {
          "label": "1000 Fortunate Souls",
          "href": "/1000fortunesouls"
        }
      ]
    },
    {
      "label": "Festivals",
      "href": "/events",
      "children": [
        {
          "label": "Upcoming events",
          "href": "/events"
        },
        {
          "label": "Janmastami",
          "href": "/janmastami"
        },
        {
          "label": "Yogini Ekadasi",
          "href": "/yogini-ekadasi"
        },
        {
          "label": "Sayana Ekadasi",
          "href": "/sayana-ekadasi"
        },
        {
          "label": "Putrada Ekadasi",
          "href": "/putrada-ekadasi"
        },
        {
          "label": "Pandava Nirjala Ekadasi",
          "href": "/pandava-nirjala-ekadasi"
        }
      ]
    },
    {
      "label": "Vaishnava Songs",
      "href": "/vaishnav-songs",
      "children": [
        {
          "label": "Guru Vandana",
          "href": "/guru-vandana"
        },
        {
          "label": "Gaura Arati",
          "href": "/gaura-arati"
        },
        {
          "label": "Narasimha Arati",
          "href": "/narasimha-arati"
        },
        {
          "label": "Raj Bhog Arati",
          "href": "/raj-bhog-arati"
        },
        {
          "label": "Tulasi Arati",
          "href": "/tulasi-arati"
        }
      ]
    },
    {
      "label": "Contact Us",
      "href": "/contact-us"
    },
    {
      "label": "New Temple",
      "href": "/new-temple-project"
    }
  ],
  "contact": {
    "phone": "+1-201-208-8976",
    "email": "info@iskconsomerset.org",
    "address": "Somerset, New Jersey, USA"
  },
  "social": [],
  "calendarUrl": "https://1drv.ms/b/c/c1f56f15d95d5ae8/IQByoJPlg2PNT5RUURoBCqJ-AdzphP6dtkficmzRsx5_1EU?e=ObaeCx",
  "pages": {
    "srila-prabhupada": {
      "title": "Srila Prabhupada",
      "description": "His Divine Grace A.C. Bhaktivedanta Swami Prabhupada, Founder-Acharya of ISKCON.",
      "heading": "Srila Prabhupada",
      "body": "<p>His Divine Grace A.C. Bhaktivedanta Swami Prabhupada (1896–1977) is the Founder-Acharya of the International Society for Krishna Consciousness (ISKCON).</p><h2>A life of service</h2><p>Born Abhay Charan De in Calcutta, he met his spiritual master, Srila Bhaktisiddhanta Sarasvati Thakura, in 1922 and was asked to bring the teachings of Lord Chaitanya to the English-speaking world. He took up that instruction for the rest of his life: writing, publishing and, in 1959, accepting the renounced order of sannyasa.</p><h2>Bringing bhakti to the West</h2><p>In 1965, at the age of 69, he travelled by cargo ship to New York with little more than a trunk of his books. A year later he founded ISKCON in a small storefront on the Lower East Side, and chanting the Hare Krishna maha-mantra soon spread to cities around the world.</p><h2>His books</h2><p>He translated and explained the great texts of the bhakti tradition, including <b>Bhagavad-gītā As It Is</b>, <b>Śrīmad-Bhāgavatam</b> and <b>Śrī Caitanya-caritāmṛta</b>. His books remain at the heart of every class and study group at our temple.</p><p><a href=\"/bg-inperson\">Join a Bhagavad Gita class</a> or <a href=\"/contact-us\">contact us</a> to learn more.</p>"
    },
    "bhakti-vriksha": {
      "title": "Bhakti Vriksha",
      "description": "Bhakti Vriksha home programmes: weekly kirtan, discussion and friendship in small groups.",
      "heading": "Bhakti Vriksha – Home Programme",
      "body": "<p>Bhakti Vriksha (\"the tree of devotion\") is a small-group programme that meets each week in a devotee's home. An evening usually includes kirtan, a short reading, open discussion and a shared meal.</p><h2>Join a group</h2><p>Groups meet in several towns around Somerset. <a href=\"/contact-us\">Contact us</a> and we will connect you with the one nearest you.</p>"
    },
    "donate": {
      "title": "Donate",
      "description": "Donate to ISKCON Somerset by card, Zelle or check: sponsor the Sunday Feast, give monthly, or support the new temple.",
      "heading": "Support the temple",
      "body": "<p>Your gifts keep the daily worship, festivals, classes and prasadam distribution going.</p><h2>Ways to give</h2><ul><li><a href=\"/nityasevak\">Nitya Sevak</a> – a monthly pledge</li><li><a href=\"/1000fortunesouls\">1000 Fortunate Souls</a> – the new temple appeal</li><li>In person at the temple</li></ul><p>For questions about donations or tax receipts, please <a href=\"/contact-us\">contact us</a>.</p>"
    },
    "nityasevak": {
      "title": "Nitya Sevak",
      "description": "Become a Nitya Sevak with a regular monthly offering to ISKCON Somerset.",
      "heading": "Nitya Sevak – monthly offering",
      "body": "<p>Nitya Sevaks support the temple with a regular monthly offering, so the community can plan worship and programmes with confidence.</p><p><a class=\"btn\" href=\"/donate#nitya-sevak\">Become a Nitya Sevak</a></p>"
    },
    "1000fortunesouls": {
      "title": "1000 Fortunate Souls",
      "description": "The 1000 Fortunate Souls appeal for the new ISKCON Somerset temple.",
      "heading": "1000 Fortunate Souls",
      "body": "<p>We are inviting 1000 families to each pledge $200 a month for 25 months towards building the new temple.</p><p><a class=\"btn\" href=\"/donate#1000-fortunate-souls\">Pledge now</a> <a href=\"/new-temple-project\">Read about the new temple project</a></p>"
    },
    "janmastami": {
      "title": "Janmastami",
      "description": "Celebrate Sri Krishna Janmastami, the appearance day of Lord Krishna, at ISKCON Somerset.",
      "heading": "Sri Krishna Janmastami",
      "body": "<p>Janmastami celebrates the appearance of Lord Krishna. The festival includes kirtan, abhishekam, a midnight arati and a feast for everyone.</p><p>See <a href=\"/events\">upcoming events</a> for this year's date and schedule.</p>"
    },
    "yogini-ekadasi": {
      "title": "Yogini Ekadasi",
      "description": "Yogini Ekadasi: fasting, chanting and hearing at ISKCON Somerset.",
      "heading": "Yogini Ekadasi",
      "body": "<p>Ekadasi, the eleventh day of each lunar fortnight, is a day for extra chanting and hearing. Devotees traditionally avoid grains and beans.</p><p>Check the <a href=\"/events\">events page</a> for the date and evening programme.</p>"
    },
    "sayana-ekadasi": {
      "title": "Sayana Ekadasi",
      "description": "Sayana (Devashayani) Ekadasi, the start of Chaturmasya, at ISKCON Somerset.",
      "heading": "Sayana Ekadasi",
      "body": "<p>Sayana Ekadasi begins the four months of Chaturmasya, a period many devotees observe with extra vows and study.</p><p>Check the <a href=\"/events\">events page</a> for the date and programme.</p>"
    },
    "putrada-ekadasi": {
      "title": "Putrada Ekadasi",
      "description": "Putrada Ekadasi at ISKCON Somerset.",
      "heading": "Putrada Ekadasi",
      "body": "<p>Putrada Ekadasi is observed with fasting from grains, kirtan and hearing about the Lord.</p><p>Check the <a href=\"/events\">events page</a> for the date and programme.</p>"
    },
    "pandava-nirjala-ekadasi": {
      "title": "Pandava Nirjala Ekadasi",
      "description": "Pandava Nirjala Ekadasi at ISKCON Somerset.",
      "heading": "Pandava Nirjala Ekadasi",
      "body": "<p>On Nirjala Ekadasi some devotees fast even from water, following the example of Bhima. Please fast only as your health allows.</p><p>Check the <a href=\"/events\">events page</a> for the date and programme.</p>"
    },
    "bg-inperson": {
      "title": "Bhagavad Gita – In Person",
      "description": "In-person Bhagavad Gita classes at ISKCON Somerset.",
      "heading": "Bhagavad Gita – in person",
      "body": "<p>A friendly weekly class that reads the Bhagavad Gita chapter by chapter, with time for questions. No background is needed.</p><p>See <a href=\"/events\">upcoming events</a> for the next session or <a href=\"/contact-us\">contact us</a> to register.</p>"
    },
    "bg-online": {
      "title": "Bhagavad Gita Online",
      "description": "Online Bhagavad Gita classes from ISKCON Somerset.",
      "heading": "Bhagavad Gita online",
      "body": "<p>Join the Bhagavad Gita study group online from anywhere. Sessions are live with time for discussion.</p><p><a href=\"/contact-us\">Contact us</a> for the joining link.</p>"
    },
    "category/all-products": {
      "title": "Gift Shop",
      "description": "Books, deity items and gifts from the ISKCON Somerset gift shop.",
      "heading": "Gift Shop",
      "body": "<p>Books, japa beads, deity paraphernalia and gifts are available at the temple. Online ordering is coming soon.</p><p>To ask about an item, please <a href=\"/contact-us\">contact us</a>.</p>"
    },
    "vaishnav-songs": {
      "title": "Vaishnava Songs",
      "description": "Words and meanings of the songs sung in daily programmes at ISKCON Somerset.",
      "heading": "Vaishnava songs",
      "body": "<p>The prayers and songs sung during the temple's daily programmes:</p><ul><li><a href=\"/guru-vandana\">Guru Vandana</a></li><li><a href=\"/gaura-arati\">Gaura Arati</a></li><li><a href=\"/narasimha-arati\">Narasimha Arati</a></li><li><a href=\"/raj-bhog-arati\">Raj Bhog Arati</a></li><li><a href=\"/tulasi-arati\">Tulasi Arati</a></li></ul>"
    },
    "guru-vandana": {
      "title": "Guru Vandana",
      "description": "Guru Vandana: prayers to the spiritual master sung in the morning programme.",
      "heading": "Guru Vandana",
      "body": "<p>Sung in the morning programme in glorification of the spiritual master.</p><p><em>The words and meaning will be added here.</em></p>"
    },
    "gaura-arati": {
      "title": "Gaura Arati",
      "description": "Gaura Arati: the evening song glorifying Sri Chaitanya Mahaprabhu.",
      "heading": "Gaura Arati",
      "body": "<p>Sung at the evening arati, describing the worship of Sri Chaitanya Mahaprabhu.</p><p><em>The words and meaning will be added here.</em></p>"
    },
    "narasimha-arati": {
      "title": "Narasimha Arati",
      "description": "Prayers to Lord Narasimha sung after arati.",
      "heading": "Narasimha Arati",
      "body": "<p>Prayers to Lord Narasimhadeva for protection, sung after the arati.</p><p><em>The words and meaning will be added here.</em></p>"
    },
    "raj-bhog-arati": {
      "title": "Raj Bhog Arati",
      "description": "The song sung during the midday Raj Bhog offering.",
      "heading": "Raj Bhog Arati",
      "body": "<p>Sung during the midday offering of food to the deities.</p><p><em>The words and meaning will be added here.</em></p>"
    },
    "tulasi-arati": {
      "title": "Tulasi Arati",
      "description": "Tulasi Arati: prayers to Srimati Tulasi Devi.",
      "heading": "Tulasi Arati",
      "body": "<p>Sung each morning while devotees offer worship to Srimati Tulasi Devi.</p><p><em>The words and meaning will be added here.</em></p>"
    },
    "contact-us": {
      "title": "Contact Us",
      "description": "Contact ISKCON Somerset and subscribe for event notifications.",
      "heading": "Contact us",
      "body": "<p>We'd love to hear from you. Call or email us, or subscribe to get festival and programme announcements.</p>"
    },
    "new-temple-project": {
      "title": "New Temple",
      "description": "The new ISKCON Somerset temple project: plans, progress and how to help.",
      "heading": "New temple project",
      "body": "<p>The community is working towards a permanent temple in Somerset with space for worship, classes, festivals and prasadam.</p><h2>How you can help</h2><ul><li>Pledge through <a href=\"/1000fortunesouls\">1000 Fortunate Souls</a></li><li>Become a <a href=\"/nityasevak\">Nitya Sevak</a></li><li>Volunteer your skills: <a href=\"/contact-us\">contact us</a></li></ul>"
    }
  },
  "donate": {
    "heading": "Give from the heart",
    "intro": "<p>Your gifts become seva: they keep the Sunday Love Feast and its prasadam going, bring our festivals to the whole community, and support Vedic classes for children and adults.</p><p>Sponsor a feast in honor of a loved one, or choose a weekly, monthly or yearly gift with the <b>Frequency</b> option. Every offering, large or small, helps build a spiritual home for everyone.</p>",
    "offline": [
      {
        "label": "Zelle",
        "detail": "201-208-8976 / donate@iskconsomerset.org"
      },
      {
        "label": "Check",
        "detail": "Payable to \"ISKCON Somerset\""
      }
    ],
    "frequencies": [
      "ONE_TIME",
      "WEEKLY",
      "MONTHLY",
      "YEARLY"
    ],
    "options": [
      {
        "label": "Sunday Feast Flower Sponsor",
        "amount": 108,
        "fund": "Sunday Feast"
      },
      {
        "label": "Sunday Feast Prasadam Sponsor",
        "amount": 151,
        "fund": "Sunday Feast"
      },
      {
        "label": "Sunday Feast Co-sponsor",
        "amount": 251,
        "fund": "Sunday Feast"
      },
      {
        "label": "Full Sunday Feast Sponsor",
        "amount": 551,
        "fund": "Sunday Feast"
      }
    ],
    "defaultFund": "General",
    "allowOther": true,
    "coverFees": true,
    "campaigns": [
      {
        "key": "1000-fortunate-souls",
        "title": "1000 Fortunate Souls",
        "text": "Be part of the new temple construction by pledging $200 a month for 25 months.",
        "fund": "New Temple",
        "amount": 200,
        "frequency": "MONTHLY",
        "installments": 25,
        "button": "Pledge $200 a month"
      },
      {
        "key": "nitya-sevak",
        "title": "Nitya Sevak",
        "text": "Sudama Seva: support the temple's monthly running costs as a regular monthly donor. Choose any amount.",
        "fund": "Nitya Sevak",
        "frequency": "MONTHLY",
        "button": "Become a Nitya Sevak"
      }
    ]
  }
}
$json$::jsonb
where slug = 'iskconsomerset';
