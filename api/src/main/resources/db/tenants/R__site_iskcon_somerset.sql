-- Public website settings for ISKCON Somerset: menu (same pages and URLs as the previous site), contact details,
-- home page text and page content. Edit this file and restart the app: Flyway re-applies it whenever it changes.
-- Page text below is placeholder copy; replace it with the temple's own wording. Body values are HTML.
update tenant set site = $json$
{
  "heroTitle": "Welcome to ISKCON Somerset",
  "heroText": "A home for bhakti yoga in central New Jersey: kirtan, Bhagavad Gita study, festivals and prasadam, open to everyone.",
  "logoFull": "/site/iskcon-somerset-logo.png",
  "heroImage": "/site/hare-krishna-banner.webp",
  "heroImageAlt": "Hare Krishna maha-mantra: Hare Krishna Hare Krishna Krishna Krishna Hare Hare, Hare Rama Hare Rama Rama Rama Hare Hare. Chant and be happy.",
  "mantra": "Hare Krishna Hare Krishna Krishna Krishna Hare Hare · Hare Rama Hare Rama Rama Rama Hare Hare ·",
  "gatherings": {
    "eyebrow": "Come together for",
    "title": "Weekly Gatherings",
    "items": [
      {
        "title": "Sunday Love Feast",
        "when": "Every Sunday",
        "text": "Join us for ecstatic kirtan, ārati to Their Lordships, an enlightening class from the Bhagavad-gītā or Śrīmad-Bhāgavatam, and a sumptuous feast of Krishna prasādam. Everyone is welcome; bring your family and friends!",
        "image": "/site/sunday-love-feast.jpg",
        "href": "/register?event=Sunday%20Love%20Feast",
        "button": "Join us"
      },
      {
        "title": "Srimad Bhagavatam & Bhagavad Gita Classes",
        "when": "Every day",
        "text": "Each day, relish Śrīmad-Bhāgavatam, the ripened fruit of Vedic knowledge, and hear Lord Krishna's own words in Bhagavad-gītā As It Is, as presented by Srila Prabhupada. Discover who you are beyond the body and how to live a life of joyful devotion.",
        "image": "/site/scripture-classes.jpg",
        "href": "/register?event=Srimad%20Bhagavatam%20%26%20Bhagavad%20Gita%20Classes",
        "button": "Join us"
      },
      {
        "title": "Bhakti Vriksha & Families",
        "when": "Every Friday Evening, 7:30 PM",
        "text": "Small home groups where families, youth and children share kirtan, stories and friendship, and grow in bhakti together.",
        "image": "/site/bhakti-vriksha.jpg",
        "href": "/register?event=Bhakti%20Vriksha%20%26%20Families",
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
    "image": "/site/srila-prabhupada.jpg",
    "imageAlt": "Srila Prabhupada smiling on a morning walk with devotees, Germany, 1974",
    "imageCredit": "Photo: Christian Jansen, CC BY 3.0",
    "imageCreditUrl": "https://commons.wikimedia.org/wiki/File:Prabhupada_on_a_morning_walk_in_Germany_1974.jpg",
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
      "label": "Gita Course",
      "href": "/bhagavad-gita-course"
    },
    {
      "label": "Donate",
      "href": "/donate"
    },
    {
      "label": "Festivals",
      "href": "/events"
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
    "bhagavad-gita-course": {
      "title": "Bhagavad-gita 18-Day Certificate Course",
      "description": "An 18-day Bhagavad-gita certificate course by ISKCON Somerset, based on Bhagavad-gita As It Is. Online classes Monday to Thursday at 7:30 PM Eastern Time in English, Telugu, Hindi, Tamil, Odia and Kannada.",
      "heading": "Discover the Wisdom of the Bhagavad-gita",
      "body": "<p class=\"course-kicker\">An 18-Day Online Certificate Course · Based on <i>Bhagavad-gita As It Is</i></p><p class=\"intro\">In a fast-paced world, many of us search for lasting happiness, peace of mind, clarity and a deeper sense of purpose. The Bhagavad-gita, spoken by Lord Sri Krishna Himself, offers timeless wisdom to guide us through every challenge of life.</p><p>Hare Krishna! We warmly invite you and your family to join our <b>18-Day Bhagavad-gita Certificate Course</b>, taught by experienced ISKCON preachers and based on <i>Bhagavad-gita As It Is</i> by His Divine Grace A.C. Bhaktivedanta Swami Srila Prabhupada.</p><h2>Course at a glance</h2><dl class=\"course-facts\"><div><dt>Starts</dt><dd>Monday, November 30th</dd></div><div><dt>Time</dt><dd>7:30 PM Eastern Time (ET)</dd></div><div><dt>Class days</dt><dd>Monday to Thursday</dd></div><div><dt>Duration</dt><dd>18 days of classes</dd></div><div><dt>Format</dt><dd>Online classes</dd></div></dl><h2>Available in six languages</h2><ul class=\"course-languages\"><li>English</li><li>Telugu</li><li>Hindi</li><li>Tamil</li><li>Odia (Oriya)</li><li>Kannada</li></ul><h2>Who should join</h2><p>Whether you are new to spiritual life or wish to deepen your understanding of Krishna consciousness, this course is a wonderful opportunity to nourish the soul and progress in devotional service. We encourage you to invite your family members, friends and colleagues to join you.</p><h2>How to register</h2><ol class=\"course-steps\"><li><b>Complete the registration form</b> below with your details and preferred language.</li><li><b>Join the WhatsApp group.</b> After you submit the form, you will be taken to the WhatsApp group for your language.</li><li><b>Stay informed.</b> Course updates and instructions are shared in the group, so please be sure to join it.</li></ol><p><a class=\"btn\" href=\"#signup\">Register now</a></p><blockquote class=\"course-quote\">Invest 18 days in spiritual learning and discover divine wisdom that will guide you throughout your life.</blockquote><h2>Questions?</h2><div class=\"card course-contact\"><p class=\"muted small\">For further information, please contact</p><p><b>Gopi Kantha Krishna Das</b><br><a href=\"tel:+17327071616\">732-707-1616</a></p></div><p class=\"course-motto\">Read the Gita. Live the Gita. Share the Gita.</p>",
      "courseSignup": {
        "course": "Bhagavad-gita 18-Day Certificate Course",
        "heading": "Register for the course",
        "intro": "Complete the form below and choose your preferred language. After you submit it, you will be taken directly to that language’s WhatsApp group.",
        "languages": [
          {
            "name": "English",
            "whatsapp": "https://chat.whatsapp.com/HsVMATaeVbfLql4nUmTJZA?mode=gi_t"
          },
          {
            "name": "Telugu",
            "whatsapp": "https://chat.whatsapp.com/BioMZqzkxnY8ifRwxhcNuk?mode=gi_t"
          },
          {
            "name": "Hindi",
            "whatsapp": "https://chat.whatsapp.com/L2c7mdVpEJ3GSlqA3KwTqx?mode=gi_t"
          },
          {
            "name": "Oriya",
            "whatsapp": "https://chat.whatsapp.com/GE5yQEbbTf1HvRpP4GTHSN?mode=gi_t"
          },
          {
            "name": "Tamil",
            "whatsapp": "https://chat.whatsapp.com/BBiEiMsmsx53si3nhUJAod?mode=gi_t"
          },
          {
            "name": "Kannada",
            "whatsapp": "https://chat.whatsapp.com/HE8pKZxNa03KisYXdRrxsn?mode=gi_t"
          }
        ]
      }
    },
    "bhakti-vriksha": {
      "title": "Bhakti Vriksha",
      "description": "Bhakti Vriksha, “the tree of devotion”: ISKCON Somerset’s weekly small groups for kirtan, Bhagavad-gītā wisdom, friendship and prasādam. Free and open to everyone.",
      "heading": "Bhakti Vriksha: The Tree of Devotion",
      "body": "<p class=\"intro\">In Sanskrit, <b>bhakti</b> means loving devotion to Lord Krishna and <b>vṛkṣa</b> means tree. <b>Bhakti Vriksha</b>, “the tree of devotion,” is ISKCON’s small-group program: a warm circle of friends and families who meet every Friday evening, usually in someone’s home, to chant, learn and grow together in Krishna consciousness.</p><p>Just as a tree grows from a small seed, sends out roots and gives shade and fruit to everyone around it, devotion grows best when it is nurtured among caring friends. Each group is guided by experienced devotees, and as a group grows, new groups branch out from it, so the tree keeps spreading.</p><blockquote class=\"verse-block\"><p lang=\"sa-Latn\">mac-cittā mad-gata-prāṇā bodhayantaḥ parasparam<br>kathayantaś ca māṁ nityaṁ tuṣyanti ca ramanti ca</p><cite>Bhagavad-gītā 10.9</cite><span> Lord Krishna explains that His devotees, with their minds fixed on Him, find deep satisfaction and joy in enlightening one another and speaking about Him. That is exactly what happens in a Bhakti Vriksha gathering.</span></blockquote><h2>What happens at a gathering</h2><p>Each meeting lasts about two hours and is simple, relaxed and family-friendly:</p><ol class=\"step-list\"><li><b>Welcome and kirtan</b><span> We begin by singing the Hare Krishna mahā-mantra together, with simple melodies anyone can join.</span></li><li><b>Japa meditation</b><span> A few minutes of quiet chanting on beads, learning the practice that steadies the mind each day.</span></li><li><b>Wisdom from the scriptures</b><span> A short reading from Bhagavad-gītā As It Is or Śrīmad-Bhāgavatam, with open discussion on how it applies to work, family and everyday choices.</span></li><li><b>Sharing and caring</b><span> Friends share realizations and challenges, and support one another, like an extended spiritual family.</span></li><li><b>Kids’ corner</b><span> Children learn stories of Krishna, ślokas, drawing and drama while the adults discuss.</span></li><li><b>Prasādam</b><span> We close with a delicious vegetarian meal first offered to Krishna, shared with love.</span></li></ol><h2>How Bhakti Vriksha helps in daily life</h2><div class=\"benefit-grid\"><div><b>Peace of mind</b><span> Regular chanting and meditation calm anxiety and stress, and give a steady inner strength for life’s ups and downs.</span></div><div><b>A daily spiritual routine</b><span> Step by step you build simple habits: chanting, reading and offering your food to Krishna, at a pace that fits your life.</span></div><div><b>Practical wisdom</b><span> The Gītā’s teachings help with real questions: work-life balance, decision making, relationships and finding purpose.</span></div><div><b>Stronger families</b><span> Families who chant, pray and eat prasādam together grow closer, and children absorb good values naturally.</span></div><div><b>Real friendships</b><span> A circle of like-minded friends who care for you, celebrate festivals with you and stand by you in difficult times.</span></div><div><b>Healthy living</b><span> Learn to cook and enjoy wholesome vegetarian prasādam, and live a simpler, happier and more balanced life.</span></div><div><b>Joy of service</b><span> Discover the happiness of seva: helping at festivals, hosting a gathering, or caring for newcomers.</span></div><div><b>Steady spiritual growth</b><span> Grow from curious newcomer to sincere practitioner with friends and mentors to guide you along the way.</span></div></div><blockquote class=\"verse-block\"><p lang=\"sa-Latn\">harer nāma harer nāma harer nāmaiva kevalam<br>kalau nāsty eva nāsty eva nāsty eva gatir anyathā</p><cite>Bṛhan-nāradīya Purāṇa</cite><span> In this age, chanting the holy names of the Lord is the simplest and surest way to spiritual progress. Bhakti Vriksha makes that chanting a joyful part of everyday life, together with friends.</span></blockquote><h2>Frequently asked questions</h2><dl class=\"faq\"><dt>Who can join?</dt><dd>Everyone: individuals, couples, families, students and seniors, whatever your background. No experience is needed.</dd><dt>Is there a cost?</dt><dd>No. Bhakti Vriksha is free. Many members like to bring a dish or help with the prasādam.</dd><dt>Where and when do groups meet?</dt><dd>Groups meet <b>every Friday evening at 7:30 PM</b> in devotees’ homes in several towns around Somerset, New Jersey. Register below and we will connect you with the group nearest you.</dd><dt>Can I bring my children?</dt><dd>Yes, please do! Children are at the heart of Bhakti Vriksha, and many groups have activities just for them.</dd><dt>Do I have to commit to anything?</dt><dd>Not at all. Come, experience it and go at your own pace. Most people find they look forward to it every week.</dd></dl>",
      "registerEvent": "Bhakti Vriksha & Families"
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
      "description": "Guru Vandana (Śrī Guru-caraṇa-padma) by Narottama Dāsa Ṭhākura: the words of the song in Bengali with English translation, sung in worship of the spiritual master.",
      "heading": "Guru Vandana",
      "body": "<p class=\"intro\">Narottama Dāsa Ṭhākura wrote this song, <b>Śrī Guru-caraṇa-padma</b>. Its proper name is <b>Śrī Guru Vandanā</b> (“The Worship of Śrī Guru”), also sung as Guru Pūjā. It comes from his book <i>Prema-bhakti-candrikā</i>. In this song, Narottama Dāsa Ṭhākura glorifies the spiritual master, who is the ocean of mercy.</p><dl class=\"song-facts\"><div><dt>Song</dt><dd>Śrī Guru-caraṇa-padma</dd></div><div><dt>Author</dt><dd>Narottama Dāsa Ṭhākura</dd></div><div><dt>Book</dt><dd>Prema-bhakti-candrikā</dd></div><div><dt>Language</dt><dd>Bengali</dd></div></dl><h2>The song, with translation</h2><ol class=\"song\"><li><p class=\"song-verse\" lang=\"bn-Latn\">śrī-guru-caraṇa-padma, kevala-bhakati-sadma,<br>bando muñi sāvadhāna mate<br>jāhāra prasāde bhāi, e bhava toriyā jāi,<br>kṛṣṇa-prāpti hoy jāhā ha'te</p><p class=\"song-translation\">The lotus feet of our spiritual master are the only way by which we can attain pure devotional service. I bow to his lotus feet with great awe and reverence. By his grace one can cross the ocean of material suffering and obtain the mercy of Krsna.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">guru-mukha-padma-vākya, cittete koribo aikya,<br>ār nā koriho mane āśā<br>śrī-guru-caraṇe rati, ei se uttama-gati,<br>je prasāde pūre sarva āśā</p><p class=\"song-translation\">My only wish is to have my consciousness purified by the words emanating from his lotus mouth. Attachment to his lotus feet is the perfection that fulfills all desires.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">cakṣu-dān dilo jei, janme janme prabhu sei,<br>divya jñān hṛde prokāśito<br>prema-bhakti jāhā hoite, avidyā vināśa jāte,<br>vede gāy jāhāra carito</p><p class=\"song-translation\">He opens my darkened eyes and fills my heart with transcendental knowledge. He is my Lord birth after birth. From him ecstatic prema emanates; by him ignorance is destroyed. The Vedic scriptures sing of his character.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">śrī-guru karuṇā-sindhu, adhama janāra bandhu,<br>lokanāth lokera jīvana<br>hā hā prabhu koro doyā, deho more pada-chāyā,<br>ebe jaśa ghuṣuk tribhuvana</p><p class=\"song-translation\">Our spiritual master is the ocean of mercy, the friend of the poor, and the lord and master of the devotees. O Lokanatha Goswami! O master! Be merciful unto me. Give me the shade of your lotus feet. Your fame is spread all over the three worlds.</p></li></ol>"
    },
    "gaura-arati": {
      "title": "Gaura Arati",
      "description": "Gaura Ārati (Jaya Jaya Gorācānder Ārotik) by Bhaktivinoda Ṭhākura: the words of the song in Bengali with English translation, sung at the evening ārati of Lord Caitanya.",
      "heading": "Gaura Arati",
      "body": "<p class=\"intro\">Bhaktivinoda Ṭhākura’s song describes the beautiful evening ārati of Lord Caitanya (Gaurāṅga) in a grove on the banks of the Ganges, surrounded by His associates and the demigods, with conchshells, bells, karatālas and mṛdaṅgas resounding.</p><dl class=\"song-facts\"><div><dt>Song</dt><dd>Jaya Jaya Gorācānder Ārotik</dd></div><div><dt>Official name</dt><dd>Ārati Kīrtana Song 2 (Gaura Ārotik)</dd></div><div><dt>Author</dt><dd>Bhaktivinoda Ṭhākura</dd></div><div><dt>Book</dt><dd>Gītāvalī</dd></div><div><dt>Language</dt><dd>Bengali</dd></div></dl><h2>The song, with translation</h2><ol class=\"song\"><li><p class=\"song-verse\" lang=\"bn-Latn\">(kiba) jaya jaya gorācāńder āratiko śobhā<br>jāhnavī-taṭa-vane jaga-mana-lobhā<br>jaga-jana-mana-lobhā</p><p class=\"song-translation\">All glories, all glories to the beautiful arati ceremony of Lord Caitanya. This Gaura-arati is taking place in a grove on the banks of the Jahnavi (Ganges) and is attracting the minds of all living entities in the universe.</p><div class=\"song-refrain\"><span>Refrain</span><p class=\"song-verse\" lang=\"bn-Latn\">(gaurāńger ārotik śobhā jaga-janer-mana-lobhā)</p><p class=\"song-translation\">The splendor of Lord Gauranga’s (Caitanya) arati attracts the minds of all living entities of the universe!</p></div></li><li><p class=\"song-verse\" lang=\"bn-Latn\">dakhiṇe nitāicāńd, bāme gadādhara<br>nikaṭe advaita, śrīnivāsa chatra-dhara</p><p class=\"song-translation\">On Lord Caitanya's right side is Lord Nityananda and on His left is Sri Gadadhara. Nearby stands Sri Advaita, and Srivasa Thakura is holding an umbrella over Lord Caitanya's head.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">bosiyāche gorācāńd ratna-siḿhāsane<br>ārati koren brahmā-ādi deva-gaṇe</p><p class=\"song-translation\">Lord Caitanya has sat down on a jeweled throne, and the demigods, headed by Lord Brahma, perform the arati ceremony.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">narahari-ādi kori' cāmara dhulāya<br>sañjaya-mukunda-bāsu-ghoṣ-ādi gāya</p><p class=\"song-translation\">Narahari Sarakara and other associates of Lord Caitanya fan Him with camaras, and devotees headed by Sanjaya Pandita, Mukunda Datta, and Vasu Ghosa sing sweet kirtana.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">śańkha bāje ghaṇṭā bāje bāje karatāla<br>madhura mṛdańga bāje parama rasāla</p><p class=\"song-translation\">Conchshells, bells, and karatalas resound, and the mrdangas play very sweetly. This kirtana music is supremely sweet and relishable to hear.</p><div class=\"song-refrain\"><span>Refrain</span><p class=\"song-verse\" lang=\"bn-Latn\">(śankha bāje ghaṇṭā bāje madhur madhur madhur bāje)</p><p class=\"song-translation\">Conchshells resound! Bells resound! Sweetness, sweetness, and sweetness resounds!</p></div></li><li><p class=\"song-verse\" lang=\"bn-Latn\">bahu-koṭi candra jini' vadana ujjvala<br>gala-deśe bana-mālā kore jhalamala</p><p class=\"song-translation\">The brilliance of Lord Caitanya's face conquers millions upon millions of moons, and the garland of forest flowers around His neck shines.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">śiva-śuka-nārada preme gada-gada<br>bhakativinoda dekhe gorāra sampada</p><p class=\"song-translation\">Lord Siva, Sukadeva Gosvami, and Narada Muni are all there, and their voices are choked with the ecstasy of transcendental love. Thus Thakura Bhaktivinoda envisions the glory of Lord Sri Caitanya.</p></li></ol>"
    },
    "narasimha-arati": {
      "title": "Narasimha Arati",
      "description": "Narasiṁha Ārati (Śrī Nṛsiṁha Praṇāma): prayers to Lord Nṛsiṁhadeva in Sanskrit with English translation, sung after the ārati.",
      "heading": "Narasimha Arati",
      "body": "<p class=\"intro\">Kṛṣṇa Dvaipāyana Vyāsa wrote the prayer <b>“namas te narasiṁhāya”</b>. Its proper name is <b>Śrī Nṛsiṁha Praṇāma</b>, and it comes from the <i>Nṛsiṁha Purāṇa</i>. These prayers to Lord Nṛsiṁhadeva, the protector of His devotees, are sung after the ārati, and close with a verse from Śrī Jayadeva Gosvāmī’s Daśāvatāra-stotra.</p><dl class=\"song-facts\"><div><dt>Prayer</dt><dd>Śrī Nṛsiṁha Praṇāma</dd></div><div><dt>Verses 1–2</dt><dd>Kṛṣṇa Dvaipāyana Vyāsa, Nṛsiṁha Purāṇa</dd></div><div><dt>Verse 3</dt><dd>Śrī Jayadeva Gosvāmī, Gīta-govinda</dd></div><div><dt>Language</dt><dd>Sanskrit</dd></div></dl><h2>The prayers, with translation</h2><ol class=\"song\"><li><p class=\"song-verse\" lang=\"sa-Latn\">namas te narasimhaya<br>prahladahlada-dayine<br>hiranyakasipor vakshahsila-<br>tanka-nakhalaye</p><p class=\"song-translation\">I offer my obeisances to Lord Narasimha who gives joy to Prahlada Maharaja and whose nails are like chisels on the stonelike chest of the demon Hiranyakasipu.</p><p class=\"song-source\">Nṛsiṁha Purāṇa</p></li><li><p class=\"song-verse\" lang=\"sa-Latn\">ito nrisimhah parato nrisimho<br>yato yato yami tato nrisimhah<br>bahir nrisimho hridaye nrisimho<br>nrisimham adim saranam prapadye</p><p class=\"song-translation\">Lord Nrisimha is here and also there. Wherever I go Lord Nrisimha is there. He is in the heart and is outside as well. I surrender to Lord Nrisimha, the origin of all things and the supreme refuge.</p><p class=\"song-source\">Nṛsiṁha Purāṇa</p></li><li><p class=\"song-verse\" lang=\"sa-Latn\">tava kara-kamala-vare nakham adbhuta-sringam<br>dalita-hiranyakasipu-tanu-bhringam<br>kesava dhrita-narahari-rupa jaya jagadisa hare</p><p class=\"song-translation\">O Kesava! O Lord of the universe! O Lord Hari, who have assumed the form of half-man, half-lion! All glories to You! Just as one can easily crush a wasp between one’s fingernails, so in the same way the body of the wasplike demon Hiranyakasipu has been ripped apart by the wonderful pointed nails on Your beautiful lotus hands.</p><p class=\"song-source\">Śrī Jayadeva Gosvāmī, Daśāvatāra-stotra (Gīta-govinda)</p></li></ol>"
    },
    "raj-bhog-arati": {
      "title": "Raj Bhog Arati",
      "description": "Rāj Bhog Ārati (Yaśomatī-nandana) by Bhaktivinoda Ṭhākura: the words of the song in Bengali with English translation, glorifying the holy names of Lord Krishna.",
      "heading": "Raj Bhog Arati",
      "body": "<p class=\"intro\">In this song Bhaktivinoda Ṭhākura glorifies Lord Govinda, the son of Yaśodā and Nanda Mahārāja. He is the protector of all; He fulfills everyone’s wishes and gives pleasure to everyone’s senses. Krishna satisfies all living beings and appears in this world to perform His pastimes and liberate everyone. He is the lover of Śrī Rādhā and very dear to Her.</p><dl class=\"song-facts\"><div><dt>Song</dt><dd>Yaśomatī-nandana</dd></div><div><dt>Official name</dt><dd>Śrī Nāma-kīrtana (Chanting of the Holy Names)</dd></div><div><dt>Author</dt><dd>Bhaktivinoda Ṭhākura</dd></div><div><dt>Book</dt><dd>Gītāvalī (Nāma-kīrtana Song 1)</dd></div><div><dt>Language</dt><dd>Bengali</dd></div></dl><h2>The song, with translation</h2><ol class=\"song\"><li><p class=\"song-verse\" lang=\"bn-Latn\">yaśomatī-nandana, braja-baro-nāgara,<br>gokula-rañjana kāna<br>gopī-parāṇa-dhana, madana-manohara,<br>kāliya-damana-vidhāna</p><p class=\"song-translation\">Lord Krishna is the beloved son of mother Yasoda; the transcendental lover in the land of Vraja; the delight of Gokula; Kana [a nickname of Krishna]; the wealth of the lives of the gopis. He steals the mind of even Cupid and punishes the Kaliya serpent.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">amala harinām amiya-vilāsā<br>vipina-purandara, navīna nāgara-bora,<br>baṁśī-badana suvāsā</p><p class=\"song-translation\">These pure, holy names of Lord Hari are full of sweet, nectarean pastimes. Krishna is the Lord of the twelve forests of Vraja, He is ever-youthful and is the best of lovers. He is always playing on a flute, and He is an excellent dresser.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">braja-jana-pālana, asura-kula-nāśana<br>nanda-godhana-rākhowālā<br>govinda mādhava, navanīta-taskara,<br>sundara nanda-gopālā</p><p class=\"song-translation\">Krishna is the protector of the inhabitants of Vraja; the destroyer of various demoniac dynasties; the keeper and tender of Nanda Maharaja’s cows; the giver of pleasure to the cows, land, and spiritual senses; the husband of the goddess of fortune; the butter thief; and the beautiful cowherd boy of Nanda Maharaja.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">yāmuna-taṭa-cara, gopī-basana-hara,<br>rāsa-rasika, kṛpāmoya<br>śrī-rādhā-vallabha, bṛndābana-naṭabara,<br>bhakativinod-āśraya</p><p class=\"song-translation\">Krishna wanders along the banks of the River Yamuna. He stole the garments of the young damsels of Vraja who were bathing there. He delights in the mellows of the rasa dance; He is very merciful; the lover and beloved of Srimati Radharani; the great dancer of Vrindavana; and the shelter and only refuge of Thakura Bhaktivinoda.</p></li></ol>"
    },
    "tulasi-arati": {
      "title": "Tulasi Arati",
      "description": "Tulasī Ārati: Śrī Tulasī Praṇāma, Tulasī Kīrtana, the pradakṣiṇa mantra and the leaf-picking mantra, with English translations.",
      "heading": "Tulasi Arati",
      "body": "<p class=\"intro\">Śrīmatī Tulasī Devī is very dear to Lord Keśava (Krishna), and by her mercy one obtains devotional service. Devotees offer her these prayers during Tulasī ārati, walk around her (pradakṣiṇa), and chant a mantra when collecting her leaves for the Lord’s service.</p><h2>Śrī Tulasī Praṇāma</h2><blockquote class=\"verse-block\"><p lang=\"sa-Latn\">vṛndāyai tulasī-devyai priyāyai keśavasya ca<br>viṣṇu-bhakti-prade devī satya vatyai namo namaḥ</p><span>I offer my repeated obeisances unto Vrnda, Srimati Tulasi Devi, who is very dear to Lord Kesava. O goddess, you bestow devotional service to Lord Visnu and possess the highest truth.</span></blockquote><h2>Śrī Tulasī Kīrtana</h2><ol class=\"song\"><li><p class=\"song-verse\" lang=\"bn-Latn\">namo namaḥ tulasī kṛṣṇa-preyasi namo namaḥ<br>rādhā-kṛṣṇa-sevā pābo ei abilāṣī</p><p class=\"song-translation\">O Tulasi, beloved of Krishna, I bow before you again and again. My desire is to obtain the service of Sri Sri Radha and Krishna.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">ye tomāra śaraṇa loy, tara vāñchā pūrṇa hoy<br>kṛpā kori' koro tāre vṛndāvana-vāsi</p><p class=\"song-translation\">Whoever takes shelter of you has his wishes fulfilled. Bestowing your mercy on him, you make him a resident of Vrindavana.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">mora ei abhilāṣa, vilāsa kuñje dio vāsa<br>nayana heribo sadā yugala-rūpa-rāśi</p><p class=\"song-translation\">My desire is that you will also give me a residence in the pleasure groves of Sri Vrindavana-dhama. Thus within my vision I will always behold the beautiful pastimes of Radha and Krishna.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">ei nivedana dhara, sakhīra anugata koro<br>sevā-adhikāra diye koro nīja dāsī</p><p class=\"song-translation\">I beg you to make me a follower of the cowherd damsels of Vraja. Please give me the privilege of devotional service and make me your own maidservant.</p></li><li><p class=\"song-verse\" lang=\"bn-Latn\">dīna kṛṣṇa-dāse koy, ei yena mora hoy<br>śrī-rādhā-govinda-preme sadā yena bhāsi</p><p class=\"song-translation\">This very fallen and lowly servant of Krsna prays, “May I always swim in the love of Sri Radha and Govinda.”</p></li></ol><h2>Śrī Tulasī Pradakṣiṇa Mantra</h2><p>Chanted while walking around Tulasī Devī:</p><blockquote class=\"verse-block\"><p lang=\"sa-Latn\">yāni kāni ca pāpāni<br>brahma-hatyādikāni ca<br>tāni tāni praṇaśyanti<br>pradakṣiṇaḥ pade pade</p><span>By the circumambulation of Srimati Tulasi Devi all the sins that one may have committed are destroyed at every step, even the sin of killing a brahmana.</span></blockquote><h2>When collecting Tulasī leaves</h2><p>Chant this mantra when collecting leaves from the plant:</p><blockquote class=\"verse-block\"><p lang=\"sa-Latn\">tulasy amṛta-janmāsi<br>sadā tvaṁ keśava-priyā<br>keśavārthaṁ cinomi tvāṁ<br>varadā bhava śobhane</p><span>O Tulasī, you were born from nectar and are always dear to Lord Keśava. I am picking your leaves for the service of Keśava. O beautiful one, please bless me.</span><span class=\"song-note\">Translation added for this page.</span></blockquote>"
    },
    "contact-us": {
      "title": "Contact Us",
      "description": "Contact ISKCON Somerset and subscribe for event notifications.",
      "heading": "Contact us",
      "body": "<p>We'd love to hear from you. Call or email us, or subscribe to get festival and programme announcements.</p>"
    },
    "new-temple-project": {
      "title": "New Temple",
      "description": "Support ISKCON Somerset’s new temple: sponsor a brick or square feet by card, Zelle or check. Śrīmad-Bhāgavatam on the glory of building a temple for the Lord.",
      "heading": "Support our upcoming New Temple",
      "body": "<p class=\"intro\">ISKCON Somerset is building a permanent home for the Lord: a temple with space for daily worship, festivals, classes and prasādam for the whole community. Every brick and every square foot is an offering. Please join us in this seva.</p><blockquote class=\"verse-block\"><p lang=\"sa-Latn\">pūjādīnāṁ pravāhārthaṁ<br>mahā-parvasv athānv-aham<br>kṣetrāpaṇa-pura-grāmān<br>dattvā mat-sārṣṭitām iyāt</p><cite>Śrīmad-Bhāgavatam 11.27.51</cite><span>One who offers the Deity gifts of land, markets, cities and villages so that the regular daily worship and special festivals of the Deity may go on continually will achieve opulence equal to My own.</span></blockquote><blockquote class=\"verse-block\"><p lang=\"sa-Latn\">pratiṣṭhayā sārvabhaumaṁ<br>sadmanā bhuvana-trayam<br>pūjādinā brahma-lokaṁ<br>tribhir mat-sāmyatām iyāt</p><cite>Śrīmad-Bhāgavatam 11.27.52</cite><span>By installing the Deity of the Lord one becomes king of the entire earth, by building a temple for the Lord one becomes ruler of the three worlds, by worshiping and serving the Deity one goes to the planet of Lord Brahmā, and by performing all three of these activities one achieves a transcendental form like My own.</span></blockquote><h2>How to donate</h2><p>Give by card below: sponsor a brick or square feet of the temple, once, monthly or yearly. You can also give by Zelle or check, or join one of our ongoing programs:</p><ul><li><a href=\"/donate#1000-fortunate-souls\"><b>1000 Fortunate Souls</b></a>: be part of the new temple construction by sponsoring $200 per month for 25 months.</li><li><a href=\"/donate#nitya-sevak\"><b>Nitya Sevak</b></a>: Sudama Seva, supporting our monthly expenses as a regular monthly donor.</li></ul>",
      "donateCampaign": "new-temple"
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
        "key": "new-temple",
        "title": "Build the New Temple",
        "text": "Sponsor a brick or square feet of the new temple, once or every month or year. Every offering brings us closer.",
        "fund": "New Temple",
        "frequencies": [
          "ONE_TIME",
          "MONTHLY",
          "YEARLY"
        ],
        "button": "Donate to the New Temple",
        "levels": [
          {
            "label": "One Brick",
            "amount": 55
          },
          {
            "label": "One Square Foot",
            "amount": 333
          },
          {
            "label": "Ten Square Feet",
            "amount": 3330
          },
          {
            "label": "Fifteen Square Feet",
            "amount": 5000
          }
        ],
        "goal": 500000,
        "raisedBefore": 21041,
        "donationsBefore": 37
      },
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
        "text": "Sudama Seva: support the temple's monthly running costs as a regular monthly donor. Choose a patron level, or any amount you like.",
        "fund": "Nitya Sevak",
        "frequency": "MONTHLY",
        "button": "Become a Nitya Sevak",
        "levels": [
          {
            "label": "Platinum Patron",
            "amount": 216
          },
          {
            "label": "Diamond Patron",
            "amount": 151
          },
          {
            "label": "Gold Patron",
            "amount": 108
          },
          {
            "label": "Silver Patron",
            "amount": 54
          }
        ]
      }
    ]
  }
}
$json$::jsonb
where slug = 'iskconsomerset';

-- Colours: royal blue and gold, to match the Hare Krishna banner picture. Used by the website and the admin.
-- Logo: the lotus mark is used where space is small (menu bar, browser tab, admin); the full logo in the footer.
update tenant set primary_color = '#1F3C88', accent_color = '#D4A72C', soft_color = '#FBF3DC',
       logo_url = '/site/iskcon-somerset-mark.png',
       notification_email = 'iskcon.somerset@gmail.com'  -- event registrations are emailed here
where slug = 'iskconsomerset';
