"""Kategori & subkategori bawaan Kasiva (mengikuti Ollo 1.7.9) — satu sumber data.

Menghasilkan:
  - composeApp/.../core/model/DefaultCategoryData.kt  (id, induk, kunci moko, ikon, warna)
  - string moko 9 bahasa untuk nama yang belum ada (lewat add_strings.py)
  - baris ikon Hugeicons yang dibutuhkan di tools/hugeicons/icons.txt

Jalankan:  python -I kotlinApp/tools/i18n/gen_categories.py
Lalu jalankan ulang gen_default_names.py dan generator Hugeicons (lihat tools/hugeicons/README.md).
Id yang sudah pernah dipakai TIDAK boleh diganti: akun lama menyimpannya di Firestore.
"""
import io, json, os, subprocess, sys

here = os.path.dirname(os.path.abspath(__file__))
root = os.path.join(here, '..', '..')
LANGS = ['base', 'en', 'ms', 'de', 'fr', 'ja', 'es', 'hi', 'zh']

# Nama baru: kunci -> teks (urutan LANGS). Kunci yang sudah ada di moko tidak perlu ditulis di sini.
N = {
 'core_category_housing': 'Rumah|Housing|Rumah|Wohnen|Logement|住居|Vivienda|आवास|住房',
 'core_category_personal': 'Pribadi|Personal|Peribadi|Persönlich|Personnel|個人|Personal|निजी|个人',
 'core_category_financial': 'Keuangan|Financial|Kewangan|Finanzen|Finances|金融|Finanzas|वित्तीय|财务',
 'core_category_family': 'Keluarga|Family|Keluarga|Familie|Famille|家族|Familia|परिवार|家庭',
 'core_category_unrecorded': 'Tidak tercatat|Unrecorded|Tidak direkod|Nicht erfasst|Non enregistré|未記録|Sin registrar|अदर्ज|未记录',
 'core_category_friend': 'Teman|Friend|Kawan|Freunde|Amis|友人|Amigos|दोस्त|朋友',
 'core_category_pets': 'Hewan peliharaan|Pets|Haiwan peliharaan|Haustiere|Animaux|ペット|Mascotas|पालतू जानवर|宠物',
 'core_category_investments': 'Investasi|Investments|Pelaburan|Investitionen|Investissements|投資|Inversiones|निवेश|投资',
 'core_category_gifts': 'Hadiah|Gifts|Hadiah|Geschenke|Cadeaux|ギフト|Regalos|उपहार|礼物',
 'core_sub_snacks': 'Camilan|Snacks|Snek|Snacks|Snacks|おやつ|Snacks|स्नैक्स|零食',
 'core_sub_drinks': 'Minuman|Drinks|Minuman|Getränke|Boissons|飲み物|Bebidas|पेय|饮品',
 'core_sub_food_groceries': 'Bahan makanan|Groceries|Barang dapur|Lebensmittel|Courses|食料品|Comestibles|किराना|食品杂货',
 'core_sub_delivery': 'Pesan antar|Delivery|Penghantaran|Lieferung|Livraison|デリバリー|A domicilio|डिलीवरी|外卖',
 'core_sub_alcohol': 'Alkohol|Alcohol|Alkohol|Alkohol|Alcool|お酒|Alcohol|शराब|酒类',
 'core_sub_fruits': 'Buah|Fruits|Buah-buahan|Obst|Fruits|果物|Frutas|फल|水果',
 'core_sub_coffee': 'Kopi|Coffee|Kopi|Kaffee|Café|コーヒー|Café|कॉफ़ी|咖啡',
 'core_sub_street_food': 'Jajanan kaki lima|Street Food|Makanan jalanan|Streetfood|Cuisine de rue|屋台料理|Comida callejera|स्ट्रीट फ़ूड|街头小吃',
 'core_sub_bus': 'Bus|Bus|Bas|Bus|Bus|バス|Autobús|बस|公交',
 'core_sub_train': 'Kereta|Train|Kereta api|Zug|Train|電車|Tren|ट्रेन|火车',
 'core_sub_taxi': 'Taksi|Taxi|Teksi|Taxi|Taxi|タクシー|Taxi|टैक्सी|出租车',
 'core_sub_maintenance': 'Perawatan|Maintenance|Penyelenggaraan|Wartung|Entretien|メンテナンス|Mantenimiento|रखरखाव|保养',
 'core_sub_insurance': 'Asuransi|Insurance|Insurans|Versicherung|Assurance|保険|Seguro|बीमा|保险',
 'core_sub_toll': 'Tol|Toll|Tol|Maut|Péage|有料道路|Peaje|टोल|过路费',
 'core_sub_online_transport': 'Transportasi online|Online Transport|Pengangkutan dalam talian|Fahrdienste|Transport en ligne|配車アプリ|Transporte por app|ऑनलाइन परिवहन|网约车',
 'core_sub_flight': 'Pesawat|Flight|Penerbangan|Flug|Vol|航空券|Vuelo|उड़ान|机票',
 'core_sub_home_goods': 'Perlengkapan rumah|Home|Kelengkapan rumah|Haushalt|Maison|日用品|Hogar|घरेलू सामान|家居',
 'core_sub_beauty': 'Kecantikan|Beauty|Kecantikan|Schönheit|Beauté|美容|Belleza|सौंदर्य|美妆',
 'core_sub_gifts': 'Kado|Gifts|Hadiah|Geschenke|Cadeaux|贈り物|Regalos|उपहार|礼品',
 'core_sub_software': 'Software|Software|Perisian|Software|Logiciels|ソフトウェア|Software|सॉफ़्टवेयर|软件',
 'core_sub_tools': 'Perkakas|Tools|Peralatan|Werkzeuge|Outils|工具|Herramientas|औज़ार|工具',
 'core_sub_shoes': 'Sepatu|Shoes|Kasut|Schuhe|Chaussures|靴|Zapatos|जूते|鞋子',
 'core_sub_online_shopping': 'Belanja online|Online Shopping|Beli-belah dalam talian|Online-Shopping|Achats en ligne|ネット通販|Compras en línea|ऑनलाइन शॉपिंग|网购',
 'core_sub_skincare': 'Perawatan kulit|Skincare|Penjagaan kulit|Hautpflege|Soins de la peau|スキンケア|Cuidado de la piel|त्वचा देखभाल|护肤',
 'core_sub_mortgage': 'Cicilan rumah|Mortgage|Pinjaman rumah|Hypothek|Prêt immobilier|住宅ローン|Hipoteca|गृह ऋण|房贷',
 'core_sub_utilities': 'Utilitas|Utilities|Utiliti|Nebenkosten|Charges|光熱費|Servicios básicos|उपयोगिताएँ|水电煤',
 'core_sub_internet_only': 'Internet|Internet|Internet|Internet|Internet|インターネット|Internet|इंटरनेट|网络',
 'core_sub_furniture': 'Perabot|Furniture|Perabot|Möbel|Meubles|家具|Muebles|फ़र्नीचर|家具',
 'core_sub_services': 'Jasa|Services|Perkhidmatan|Dienstleistungen|Services|サービス|Servicios|सेवाएँ|服务',
 'core_sub_laundry': 'Laundry|Laundry|Dobi|Wäsche|Blanchisserie|クリーニング|Lavandería|लॉन्ड्री|洗衣',
 'core_sub_mobile_data': 'Pulsa & data|Pulsa & Data|Kredit & data|Guthaben & Daten|Crédit & données|通信費|Saldo y datos|रिचार्ज और डेटा|话费与流量',
 'core_sub_cleaning': 'Alat kebersihan|Cleaning Supplies|Bekalan pembersihan|Putzmittel|Produits ménagers|掃除用品|Artículos de limpieza|सफ़ाई सामग्री|清洁用品',
 'core_sub_events': 'Acara|Events|Acara|Veranstaltungen|Événements|イベント|Eventos|इवेंट|活动',
 'core_sub_travel': 'Liburan|Travel|Melancong|Reisen|Voyages|旅行|Viajes|यात्रा|旅行',
 'core_sub_music': 'Musik|Music|Muzik|Musik|Musique|音楽|Música|संगीत|音乐',
 'core_sub_gym': 'Gym|Gym|Gim|Fitnessstudio|Salle de sport|ジム|Gimnasio|जिम|健身房',
 'core_sub_mental_health': 'Kesehatan mental|Mental Health|Kesihatan mental|Psychische Gesundheit|Santé mentale|メンタルヘルス|Salud mental|मानसिक स्वास्थ्य|心理健康',
 'core_sub_tuition': 'Uang sekolah|Tuition|Yuran pengajian|Studiengebühren|Frais de scolarité|学費|Matrícula|ट्यूशन फ़ीस|学费',
 'core_sub_supplies': 'Perlengkapan sekolah|Supplies|Bekalan|Schulbedarf|Fournitures|学用品|Material escolar|सामग्री|用品',
 'core_sub_haircut': 'Potong rambut|Haircut|Gunting rambut|Friseur|Coiffeur|ヘアカット|Corte de pelo|बाल कटाई|理发',
 'core_sub_spa': 'Spa|Spa|Spa|Spa|Spa|スパ|Spa|स्पा|水疗',
 'core_sub_cosmetics': 'Kosmetik|Cosmetics|Kosmetik|Kosmetik|Cosmétiques|化粧品|Cosméticos|सौंदर्य प्रसाधन|化妆品',
 'core_sub_taxes': 'Pajak|Taxes|Cukai|Steuern|Impôts|税金|Impuestos|कर|税费',
 'core_sub_fees': 'Biaya admin|Fees|Yuran|Gebühren|Frais|手数料|Comisiones|शुल्क|手续费',
 'core_sub_fines': 'Denda|Fines|Denda|Bußgelder|Amendes|罰金|Multas|जुर्माना|罚款',
 'core_sub_donation': 'Donasi|Donation|Derma|Spende|Don|寄付|Donación|दान|捐款',
 'core_sub_zakat': 'Zakat|Zakat|Zakat|Zakat|Zakat|ザカート|Zakat|ज़कात|天课',
 'core_sub_childcare': 'Pengasuhan anak|Childcare|Penjagaan anak|Kinderbetreuung|Garde d\'enfants|保育|Cuidado infantil|बाल देखभाल|育儿',
 'core_sub_toys': 'Mainan|Toys|Mainan|Spielzeug|Jouets|おもちゃ|Juguetes|खिलौने|玩具',
 'core_sub_school': 'Sekolah anak|School|Sekolah|Schule|École|学校|Escuela|स्कूल|学校',
 'core_sub_baby_gear': 'Perlengkapan bayi|Baby Gear|Kelengkapan bayi|Babyausstattung|Articles pour bébé|ベビー用品|Artículos para bebé|बेबी सामान|婴儿用品',
 'core_sub_adjustment': 'Penyesuaian|Adjustment|Pelarasan|Korrektur|Ajustement|調整|Ajuste|समायोजन|调整',
 'core_sub_friend_transfer': 'Kirim uang|Transfer|Pindahan|Überweisung|Virement|送金|Transferencia|ट्रांसफ़र|转账',
 'core_sub_treat': 'Traktir|Treat|Belanja kawan|Einladung|Invitation|おごり|Invitación|ट्रीट|请客',
 'core_sub_refund': 'Pengembalian dana|Refund|Bayaran balik|Erstattung|Remboursement|返金|Reembolso|धनवापसी|退款',
 'core_sub_loan': 'Pinjaman|Loan|Pinjaman|Darlehen|Prêt|貸付|Préstamo|उधार|借款',
 'core_sub_pet_food': 'Makanan hewan|Pet Food|Makanan haiwan|Tierfutter|Nourriture pour animaux|ペットフード|Comida para mascotas|पालतू भोजन|宠物食品',
 'core_sub_vet': 'Dokter hewan|Vet|Doktor haiwan|Tierarzt|Vétérinaire|動物病院|Veterinario|पशु चिकित्सक|兽医',
 'core_sub_accessories': 'Aksesori|Accessories|Aksesori|Zubehör|Accessoires|アクセサリー|Accesorios|सहायक सामान|配件',
 'core_sub_grooming': 'Perawatan bulu|Grooming|Dandanan|Fellpflege|Toilettage|トリミング|Peluquería|ग्रूमिंग|宠物美容',
 'core_sub_monthly': 'Bulanan|Monthly|Bulanan|Monatlich|Mensuel|月給|Mensual|मासिक|月薪',
 'core_sub_weekly': 'Mingguan|Weekly|Mingguan|Wöchentlich|Hebdomadaire|週給|Semanal|साप्ताहिक|周薪',
 'core_sub_bonus': 'Bonus|Bonus|Bonus|Bonus|Prime|ボーナス|Bono|बोनस|奖金',
 'core_sub_overtime': 'Lembur|Overtime|Kerja lebih masa|Überstunden|Heures sup.|残業代|Horas extra|ओवरटाइम|加班费',
 'core_sub_sales': 'Penjualan|Sales|Jualan|Verkäufe|Ventes|売上|Ventas|बिक्री|销售',
 'core_sub_profit': 'Keuntungan|Profit|Keuntungan|Gewinn|Bénéfice|利益|Ganancia|मुनाफ़ा|利润',
 'core_sub_dividends': 'Dividen|Dividends|Dividen|Dividenden|Dividendes|配当|Dividendos|लाभांश|股息',
 'core_sub_interest': 'Bunga|Interest|Faedah|Zinsen|Intérêts|利息|Intereses|ब्याज|利息',
 'core_sub_crypto': 'Kripto|Crypto|Kripto|Krypto|Crypto|暗号資産|Cripto|क्रिप्टो|加密货币',
 'core_sub_stocks': 'Saham|Stocks|Saham|Aktien|Actions|株式|Acciones|शेयर|股票',
 'core_sub_real_estate': 'Properti|Real Estate|Hartanah|Immobilien|Immobilier|不動産|Inmuebles|रियल एस्टेट|房地产',
 'core_sub_birthday': 'Ulang tahun|Birthday|Hari jadi|Geburtstag|Anniversaire|誕生日|Cumpleaños|जन्मदिन|生日',
 'core_sub_holiday': 'Hari raya|Holiday|Hari raya|Feiertag|Fêtes|祝日|Festividades|त्योहार|节日',
 'core_sub_allowance': 'Uang saku|Allowance|Elaun|Taschengeld|Argent de poche|お小遣い|Paga|जेब खर्च|零花钱',
 'core_sub_grants': 'Hibah|Grants|Geran|Zuschüsse|Subventions|助成金|Subvenciones|अनुदान|补助',
 'core_sub_lottery': 'Undian|Lottery|Loteri|Lotterie|Loterie|宝くじ|Lotería|लॉटरी|彩票',
 'core_sub_selling': 'Jual barang|Selling|Jualan barang|Verkauf|Vente|売却|Venta|बिक्री|出售',
}

# Ikon Hugeicons: nama hi_* -> nama ikon paket (ditambahkan ke icons.txt bila belum ada).
ICONS = {}
def ic(name):
    hi = 'hi_k_' + ''.join('_' + c.lower() if c.isupper() and i else c.lower() for i, c in enumerate(name)).replace('__', '_')
    ICONS[hi] = name
    return hi

# (id, tipe, induk, kunci, ikon, warna). Urutan = urutan chip (sama dengan Ollo).
E, I = 'expense', 'income'
COLORS = {'food': 0xFFFF9800, 'transport': 0xFF7E57C2, 'shopping': 0xFF29B6F6, 'housing': 0xFF3F51B5, 'entertainment': 0xFFC0CA33,
          'health': 0xFF43A047, 'education': 0xFF1E88E5, 'personal': 0xFFEC407A, 'financial': 0xFF00897B, 'family': 0xFFFF7043,
          'unrecorded': 0xFF78909C, 'friend': 0xFF26A69A, 'pets': 0xFF8D6E63, 'bills': 0xFFE53935, 'other_expense': 0xFF757575,
          'salary': 0xFF2E7D32, 'business': 0xFF1565C0, 'investments': 0xFF6A1B9A, 'gifts': 0xFFD81B60, 'other_income': 0xFF757575}
TREE = [
 ('food', E, 'core_category_food', 'Restaurant01', [
   ('food-breakfast', 'core_sub_breakfast', 'Cookie'), ('food-lunch', 'core_sub_lunch', 'Restaurant02'),
   ('food-dinner', 'core_sub_dinner', 'Pizza01'), ('food-eat-out', 'core_sub_eat_out', 'RestaurantTable'),
   ('food-snacks', 'core_sub_snacks', 'Hamburger01'), ('food-drinks', 'core_sub_drinks', 'SoftDrink01'),
   ('food-groceries', 'core_sub_food_groceries', 'ShoppingBasket01'), ('food-delivery', 'core_sub_delivery', 'Motorbike01'),
   ('food-alcohol', 'core_sub_alcohol', 'Beer'), ('food-fruits', 'core_sub_fruits', 'Apple01'),
   ('food-coffee', 'core_sub_coffee', 'Coffee02'), ('food-street', 'core_sub_street_food', 'Noodles')]),
 ('transport', E, 'core_category_transport', 'Bus01', [
   ('transport-bus', 'core_sub_bus', 'Bus02'), ('transport-train', 'core_sub_train', 'Train01'), ('transport-taxi', 'core_sub_taxi', 'Taxi'),
   ('transport-fuel', 'core_sub_fuel', 'FuelStation'), ('transport-parking', 'core_sub_parking', 'CircleParking'),
   ('transport-maintenance', 'core_sub_maintenance', 'Wrench01'), ('transport-insurance', 'core_sub_insurance', 'Shield01'),
   ('transport-toll', 'core_sub_toll', 'Road01'), ('transport-online', 'core_sub_online_transport', 'Motorbike01'),
   ('transport-flight', 'core_sub_flight', 'Airplane01')]),
 ('shopping', E, 'core_category_shopping', 'ShoppingBag01', [
   ('shopping-clothing', 'core_sub_clothing', 'TShirt'), ('shopping-electronics', 'core_sub_electronics', 'Laptop'),
   ('shopping-home', 'core_sub_home_goods', 'Sofa01'), ('shopping-beauty', 'core_sub_beauty', 'HandBag01'),
   ('shopping-gifts', 'core_sub_gifts', 'Gift'), ('shopping-software', 'core_sub_software', 'Software'),
   ('shopping-tools', 'core_sub_tools', 'Wrench02'), ('shopping-shoes', 'core_sub_shoes', 'RunningShoes'),
   ('shopping-online', 'core_sub_online_shopping', 'ShoppingCart01'), ('shopping-skincare', 'core_sub_skincare', 'Droplet')]),
 ('housing', E, 'core_category_housing', 'House03', [
   ('housing-rent', 'core_sub_rent', 'Key01'), ('housing-mortgage', 'core_sub_mortgage', 'Building03'),
   ('housing-utilities', 'core_sub_utilities', 'Plug01'), ('housing-internet', 'core_sub_internet_only', 'HomeWifi'),
   ('housing-maintenance', 'core_sub_maintenance', 'Wrench01'), ('housing-furniture', 'core_sub_furniture', 'Sofa02'),
   ('housing-services', 'core_sub_services', 'UserGroup'), ('housing-laundry', 'core_sub_laundry', 'WashingMachine'),
   ('housing-mobile-data', 'core_sub_mobile_data', 'Simcard01'), ('housing-electricity', 'core_sub_electricity', 'Flash'),
   ('housing-cleaning', 'core_sub_cleaning', 'CleaningBucket')]),
 ('entertainment', E, 'core_category_entertainment', 'Film02', [
   ('entertainment-cinema', 'core_sub_cinema', 'Film01'), ('entertainment-games', 'core_sub_games', 'GameController01'),
   ('entertainment-streaming', 'core_sub_streaming', 'Tv01'), ('entertainment-events', 'core_sub_events', 'Ticket01'),
   ('entertainment-hobby', 'core_sub_hobby', 'Palette'), ('entertainment-travel', 'core_sub_travel', 'TravelBag'),
   ('entertainment-music', 'core_sub_music', 'MusicNote01')]),
 ('health', E, 'core_category_health', 'Stethoscope', [
   ('health-doctor', 'core_sub_doctor', 'Doctor01'), ('health-medicine', 'core_sub_medicine', 'Medicine02'),
   ('health-gym', 'core_sub_gym', 'Dumbbell01'), ('health-insurance', 'core_sub_insurance', 'Shield01'),
   ('health-mental', 'core_sub_mental_health', 'Brain'), ('health-sport', 'core_sub_sport', 'WorkoutRun')]),
 ('education', E, 'core_category_education', 'School01', [
   ('education-tuition', 'core_sub_tuition', 'School'), ('education-books', 'core_sub_books', 'Book01'),
   ('education-course', 'core_sub_course', 'Course'), ('education-supplies', 'core_sub_supplies', 'PencilRuler'),
   ('education-stationery', 'core_sub_stationery', 'Pencil')]),
 ('personal', E, 'core_category_personal', 'User', [
   ('personal-haircut', 'core_sub_haircut', 'Scissor01'), ('personal-spa', 'core_sub_spa', 'Droplets'),
   ('personal-cosmetics', 'core_sub_cosmetics', 'Perfume')]),
 ('financial', E, 'core_category_financial', 'Bank', [
   ('financial-taxes', 'core_sub_taxes', 'Taxes'), ('financial-fees', 'core_sub_fees', 'ReceiptDollar'),
   ('financial-fines', 'core_sub_fines', 'Alert02'), ('financial-insurance', 'core_sub_insurance', 'Shield01'),
   ('financial-donation', 'core_sub_donation', 'HandHeart'), ('financial-zakat', 'core_sub_zakat', 'Mosque01')]),
 ('family', E, 'core_category_family', 'UserMultiple', [
   ('family-childcare', 'core_sub_childcare', 'Baby01'), ('family-toys', 'core_sub_toys', 'ToyBrick'),
   ('family-school', 'core_sub_school', 'SchoolBus'), ('family-pets', 'core_category_pets', 'PawPrint'),
   ('family-baby-gear', 'core_sub_baby_gear', 'BabyBottle')]),
 ('unrecorded', E, 'core_category_unrecorded', 'HelpCircle', [
   ('unrecorded-adjustment', 'core_sub_adjustment', 'SlidersHorizontal'), ('unrecorded-fees', 'core_sub_fees', 'ReceiptDollar')]),
 ('friend', E, 'core_category_friend', 'Handshake', [
   ('friend-transfer', 'core_sub_friend_transfer', 'ArrowDataTransferHorizontal'), ('friend-treat', 'core_sub_treat', 'Drink'),
   ('friend-refund', 'core_sub_refund', 'ArrowTurnBackward'), ('friend-loan', 'core_sub_loan', 'HandCoins'),
   ('friend-gift', 'core_sub_gifts', 'Gift')]),
 ('pets', E, 'core_category_pets', 'PawPrint', [
   ('pets-general', 'core_category_pets', 'PawPrint'), ('pets-food', 'core_sub_pet_food', 'Bone01'),
   ('pets-vet', 'core_sub_vet', 'Stethoscope'), ('pets-toys', 'core_sub_toys', 'ToyBrick'),
   ('pets-accessories', 'core_sub_accessories', 'ShoppingBag02'), ('pets-grooming', 'core_sub_grooming', 'Scissor')]),
 # Bawaan lama Kasiva yang tetap ada (dipakai Tagihan, fallback AI, dan akun lama).
 ('bills', E, 'core_category_bills', 'Invoice01', []),
 ('other_expense', E, 'core_category_other', 'MoreHorizontalCircle01', []),
 ('salary', I, 'core_category_salary', 'Briefcase01', [
   ('salary-monthly', 'core_sub_monthly', 'Calendar03'), ('salary-weekly', 'core_sub_weekly', 'Calendar01'),
   ('salary-bonus', 'core_sub_bonus', 'Trophy'), ('salary-overtime', 'core_sub_overtime', 'Clock01')]),
 ('business', I, 'core_category_business', 'Store01', [
   ('business-sales', 'core_sub_sales', 'Store01'), ('business-services', 'core_sub_services', 'Briefcase02'),
   ('business-profit', 'core_sub_profit', 'ChartIncrease')]),
 ('investments', I, 'core_category_investments', 'ChartCandle', [
   ('investments-dividends', 'core_sub_dividends', 'Coins01'), ('investments-interest', 'core_sub_interest', 'PercentCircle'),
   ('investments-crypto', 'core_sub_crypto', 'Bitcoin01'), ('investments-stocks', 'core_sub_stocks', 'ChartCandle'),
   ('investments-real-estate', 'core_sub_real_estate', 'Building03')]),
 ('gifts', I, 'core_category_gifts', 'Gift', [
   ('gifts-birthday', 'core_sub_birthday', 'BirthdayCake'), ('gifts-holiday', 'core_sub_holiday', 'Calendar04'),
   ('gifts-allowance', 'core_sub_allowance', 'Money03')]),
 ('other_income', I, 'core_category_other', 'MoreHorizontalCircle01', [
   ('other_income-refunds', 'core_sub_refund', 'ArrowTurnBackward'), ('other_income-grants', 'core_sub_grants', 'Award01'),
   ('other_income-lottery', 'core_sub_lottery', 'Dice'), ('other_income-selling', 'core_sub_selling', 'Tag01')]),
]
# Subkategori bawaan versi lama: tidak dibuat lagi untuk akun baru, tetapi tetap dikenali (nama, ikon, restore).
LEGACY = [
 ('transport-public', E, 'transport', 'core_sub_public_transport', 'Bus02'), ('transport-ride', E, 'transport', 'core_sub_ride', 'Motorbike01'),
 ('shopping-groceries', E, 'shopping', 'core_sub_groceries', 'ShoppingBasket01'),
 ('bills-electricity', E, 'bills', 'core_sub_electricity', 'Flash'), ('bills-water', E, 'bills', 'core_sub_water', 'Droplet'),
 ('bills-internet', E, 'bills', 'core_sub_internet', 'HomeWifi'), ('bills-rent', E, 'bills', 'core_sub_rent', 'Key01'),
]

def main():
    pkg = os.path.join(root, 'composeApp', 'src', 'commonMain')
    res = os.path.join(pkg, 'moko-resources')
    existing = set()
    import re
    existing.update(re.findall(r'<string name="([a-z0-9_]+)">', io.open(os.path.join(res, 'base', 'strings.xml'), encoding='utf-8').read()))
    rows, legacy = [], []
    for pid, typ, key, icon, subs in TREE:
        rows.append((pid, typ, None, key, ic(icon), COLORS[pid]))
        for sid, skey, sicon in subs:
            rows.append((sid, typ, pid, skey, ic(sicon), COLORS[pid]))
    for sid, typ, pid, skey, sicon in LEGACY:
        legacy.append((sid, typ, pid, skey, ic(sicon), COLORS[pid]))
    needed = {k for r in rows + legacy for k in [r[3]]}
    missing = sorted(k for k in needed if k not in existing and k not in N)
    if missing: sys.exit('kunci tanpa teks: ' + ', '.join(missing))
    ids = [r[0] for r in rows + legacy]
    assert len(ids) == len(set(ids)), 'id ganda'
    # 1) string moko
    new = {k: dict(zip(LANGS, v.split('|'))) for k, v in N.items() if k not in existing}
    for k, v in new.items(): assert len(v) == 9, k
    if new:
        tmp = os.path.join(root, 'build', 'gen_categories_strings.json')
        os.makedirs(os.path.dirname(tmp), exist_ok=True)
        json.dump(new, io.open(tmp, 'w', encoding='utf-8'), ensure_ascii=False)
        subprocess.run([sys.executable, '-I', os.path.join(here, 'add_strings.py'), tmp], check=True)
    # 2) Kotlin
    def spec(r):
        sid, typ, pid, key, icon, color = r
        return f'    DefaultCategorySpec("{sid}", "{typ}", {"null" if pid is None else chr(34) + pid + chr(34)}, "{key}", MR.strings.{key}, "{icon}", 0x{color:08X}),'
    kt = ['// Dibuat oleh tools/i18n/gen_categories.py — jangan diedit manual.', 'package com.clonical.kasiva.core.model', '',
          'import com.clonical.kasiva.i18n.*', 'import dev.icerock.moko.resources.StringResource', '',
          '/** Satu kategori bawaan: id tetap, kunci moko nama, ikon Hugeicons, dan warna chip (Ollo). */',
          'class DefaultCategorySpec(val id: String, val type: String, val parentId: String?, val key: String, val name: StringResource, val icon: String, val color: Long)', '',
          '/** Kategori + subkategori bawaan untuk akun baru dan pelengkap akun lama, urut seperti chip Ollo. */',
          'val defaultCategorySpecs: List<DefaultCategorySpec> get() = listOf('] + [spec(r) for r in rows] + [')', '',
          '/** Subkategori bawaan versi lama: tidak dibuat lagi, tetapi tetap dikenali (nama, ikon, restore). */',
          'val legacyCategorySpecs: List<DefaultCategorySpec> get() = listOf('] + [spec(r) for r in legacy] + [')', '']
    out = os.path.join(pkg, 'kotlin', 'com', 'clonical', 'kasiva', 'core', 'model', 'DefaultCategoryData.kt')
    io.open(out, 'w', encoding='utf-8', newline='\n').write('\n'.join(kt))
    # 3) ikon
    icons_txt = os.path.join(root, 'tools', 'hugeicons', 'icons.txt')
    lines = [l.strip() for l in io.open(icons_txt, encoding='utf-8') if l.strip()]
    have = {l.split('=')[0] for l in lines}
    lines += [f'{k}={v}' for k, v in sorted(ICONS.items()) if k not in have]
    io.open(icons_txt, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')
    print(f'{len(rows)} kategori bawaan, {len(legacy)} lama, {len(new)} string baru, {len(ICONS)} ikon')

if __name__ == '__main__':
    main()
