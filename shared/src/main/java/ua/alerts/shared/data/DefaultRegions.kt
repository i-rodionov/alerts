package ua.alerts.shared.data

import ua.alerts.shared.model.AlertStatus
import ua.alerts.shared.model.District
import ua.alerts.shared.model.NeptunAlertsResponse
import ua.alerts.shared.model.Region

object DefaultRegions {
    val ALL_REGIONS: List<Region> = listOf(
        Region(
            id = "m_kyiv",
            nameUk = "м. Київ",
            nameEn = "Kyiv City",
            neptunKey = "м. київ",
            raions = emptyList()
        ),
        Region(
            id = "kyivska",
            nameUk = "Київська область",
            nameEn = "Kyiv Oblast",
            neptunKey = "київська",
            raions = listOf(
                District("kyivska:bilotserkivskyi", "Білоцерківський район", "Bila Tserkva District", "kyivska", "білоцерківський"),
                District("kyivska:boryspilskyi", "Бориспільський район", "Boryspil District", "kyivska", "бориспільський"),
                District("kyivska:brovarskyi", "Броварський район", "Brovary District", "kyivska", "броварський"),
                District("kyivska:buchanskyi", "Бучанський район", "Bucha District", "kyivska", "бучанський"),
                District("kyivska:fastivskyi", "Фастівський район", "Fastiv District", "kyivska", "фастівський"),
                District("kyivska:obukhivskyi", "Обухівський район", "Obukhiv District", "kyivska", "обухівський"),
                District("kyivska:vyshhorodskyi", "Вишгородський район", "Vyshhorod District", "kyivska", "вишгородський")
            )
        ),
        Region(
            id = "vinnytska",
            nameUk = "Вінницька область",
            nameEn = "Vinnytsia Oblast",
            neptunKey = "вінницька",
            raions = listOf(
                District("vinnytska:vinnytskyi", "Вінницький район", "Vinnytsia District", "vinnytska", "вінницький"),
                District("vinnytska:haisynskyi", "Гайсинський район", "Haisyn District", "vinnytska", "гайсинський"),
                District("vinnytska:zhmerynskyi", "Жмеринський район", "Zhmerynka District", "vinnytska", "жмеринський"),
                District("vinnytska:mohyliv_podilskyi", "Могилів-Подільський район", "Mohyliv-Podilskyi District", "vinnytska", "могилів-подільський"),
                District("vinnytska:tulchynskyi", "Тульчинський район", "Tulchyn District", "vinnytska", "тульчинський"),
                District("vinnytska:khmilnytskyi", "Хмільницький район", "Khmilnyk District", "vinnytska", "хмільницький")
            )
        ),
        Region(
            id = "volynska",
            nameUk = "Волинська область",
            nameEn = "Volyn Oblast",
            neptunKey = "волинська",
            raions = listOf(
                District("volynska:volodymyrskyi", "Володимирський район", "Volodymyr District", "volynska", "володимир-волинський"),
                District("volynska:kamin_kashyrskyi", "Камінь-Каширський район", "Kamin-Kashyrskyi District", "volynska", "камінь-каширський"),
                District("volynska:kovelskyi", "Ковельський район", "Kovel District", "volynska", "ковельський"),
                District("volynska:lutskyi", "Луцький район", "Lutsk District", "volynska", "луцький")
            )
        ),
        Region(
            id = "dnipropetrovska",
            nameUk = "Дніпропетровська область",
            nameEn = "Dnipropetrovsk Oblast",
            neptunKey = "дніпропетровська",
            raions = listOf(
                District("dnipropetrovska:dniprovskyi", "Дніпровський район", "Dnipro District", "dnipropetrovska", "дніпровський"),
                District("dnipropetrovska:kamianskyi", "Кам'янський район", "Kamianske District", "dnipropetrovska", "кам'янський"),
                District("dnipropetrovska:kryvorizkyi", "Криворізький район", "Kryvyi Rih District", "dnipropetrovska", "криворізький"),
                District("dnipropetrovska:nikopolskyi", "Нікопольський район", "Nikopol District", "dnipropetrovska", "нікопольський"),
                District("dnipropetrovska:novomoskovskyi", "Новомосковський район", "Novomoskovsk District", "dnipropetrovska", "новомосковський"),
                District("dnipropetrovska:pavlohradskyi", "Павлоградський район", "Pavlohrad District", "dnipropetrovska", "павлоградський"),
                District("dnipropetrovska:synelnykivskyi", "Синельниківський район", "Synelnykove District", "dnipropetrovska", "синельниківський")
            )
        ),
        Region(
            id = "donetska",
            nameUk = "Донецька область",
            nameEn = "Donetsk Oblast",
            neptunKey = "донецька",
            raions = listOf(
                District("donetska:bakhmutskyi", "Бахмутський район", "Bakhmut District", "donetska", "бахмутський"),
                District("donetska:volnovaskyi", "Волноваський район", "Volnovakha District", "donetska", "волноваський"),
                District("donetska:horlivskyi", "Горлівський район", "Horlivka District", "donetska", "горлівський"),
                District("donetska:donetskyi", "Донецький район", "Donetsk District", "donetska", "донецький"),
                District("donetska:kalmiuskyi", "Кальміуський район", "Kalmiuske District", "donetska", "кальміуський"),
                District("donetska:kramatorskyi", "Краматорський район", "Kramatorsk District", "donetska", "краматорський"),
                District("donetska:mariupolskyi", "Маріупольський район", "Mariupol District", "donetska", "маріупольський"),
                District("donetska:pokrovskyi", "Покровський район", "Pokrovsk District", "donetska", "покровський")
            )
        ),
        Region(
            id = "zhytomyrska",
            nameUk = "Житомирська область",
            nameEn = "Zhytomyr Oblast",
            neptunKey = "житомирська",
            raions = listOf(
                District("zhytomyrska:berdychivskyi", "Бердичівський район", "Berdychiv District", "zhytomyrska", "бердичівський"),
                District("zhytomyrska:zhytomyrskyi", "Житомирський район", "Zhytomyr District", "zhytomyrska", "житомирський"),
                District("zhytomyrska:korostenskyi", "Коростенський район", "Korosten District", "zhytomyrska", "коростенський"),
                District("zhytomyrska:zviahelskyi", "Звягельський район", "Zviahel District", "zhytomyrska", "новоград-волинський")
            )
        ),
        Region(
            id = "zakarpatska",
            nameUk = "Закарпатська область",
            nameEn = "Zakarpattia Oblast",
            neptunKey = "закарпатська",
            raions = listOf(
                District("zakarpatska:berehivskyi", "Берегівський район", "Berehove District", "zakarpatska", "берегівський"),
                District("zakarpatska:mukachivskyi", "Мукачівський район", "Mukachevo District", "zakarpatska", "мукачівський"),
                District("zakarpatska:rakhivskyi", "Рахівський район", "Rakhiv District", "zakarpatska", "рахівський"),
                District("zakarpatska:tiachivskyi", "Тячівський район", "Tiachiv District", "zakarpatska", "тячівський"),
                District("zakarpatska:uzhhorodskyi", "Ужгородський район", "Uzhhorod District", "zakarpatska", "ужгородський"),
                District("zakarpatska:khustskyi", "Хустський район", "Khust District", "zakarpatska", "хустський")
            )
        ),
        Region(
            id = "zaporizka",
            nameUk = "Запорізька область",
            nameEn = "Zaporizhzhia Oblast",
            neptunKey = "запорізька",
            raions = listOf(
                District("zaporizka:berdianskyi", "Бердянський район", "Berdiansk District", "zaporizka", "бердянський"),
                District("zaporizka:vasylivskyi", "Василівський район", "Vasylivka District", "zaporizka", "василівський"),
                District("zaporizka:zaporizkyi", "Запорізький район", "Zaporizhzhia District", "zaporizka", "запорізький"),
                District("zaporizka:melitopolskyi", "Мелітопольський район", "Melitopol District", "zaporizka", "мелітопольський"),
                District("zaporizka:polohivskyi", "Пологівський район", "Polohy District", "zaporizka", "пологівський")
            )
        ),
        Region(
            id = "ivano_frankivska",
            nameUk = "Івано-Франківська область",
            nameEn = "Ivano-Frankivsk Oblast",
            neptunKey = "івано-франківська",
            raions = listOf(
                District("ivano_frankivska:verkhovynskyi", "Верховинський район", "Verkhovyna District", "ivano_frankivska", "верховинський"),
                District("ivano_frankivska:ivano_frankivskyi", "Івано-Франківський район", "Ivano-Frankivsk District", "ivano_frankivska", "івано-франківський"),
                District("ivano_frankivska:kaluskyi", "Калуський район", "Kalush District", "ivano_frankivska", "калуський"),
                District("ivano_frankivska:kolomyiskyi", "Коломийський район", "Kolomyia District", "ivano_frankivska", "коломийський"),
                District("ivano_frankivska:kosivskyi", "Косівський район", "Kosiv District", "ivano_frankivska", "косівський"),
                District("ivano_frankivska:nadvirnianskyi", "Надвірнянський район", "Nadvirna District", "ivano_frankivska", "надвірнянський")
            )
        ),
        Region(
            id = "kirovohradska",
            nameUk = "Кіровоградська область",
            nameEn = "Kirovohrad Oblast",
            neptunKey = "кіровоградська",
            raions = listOf(
                District("kirovohradska:holovanivskyi", "Голованівський район", "Holovanivsk District", "kirovohradska", "голованівський"),
                District("kirovohradska:kropyvnytskyi", "Кропивницький район", "Kropyvnytskyi District", "kirovohradska", "кропивницький"),
                District("kirovohradska:novoukrainskyi", "Новоукраїнський район", "Novoukrainka District", "kirovohradska", "новоукраїнський"),
                District("kirovohradska:oleksandriiskyi", "Олександрійський район", "Oleksandriia District", "kirovohradska", "олександрійський")
            )
        ),
        Region(
            id = "luhanska",
            nameUk = "Луганська область",
            nameEn = "Luhansk Oblast",
            neptunKey = "луганська",
            raions = listOf(
                District("luhanska:alchevskyi", "Алчевський район", "Alchevsk District", "luhanska", "алчевський"),
                District("luhanska:dovzhanskyi", "Довжанський район", "Dovzhansk District", "luhanska", "довжанський"),
                District("luhanska:luhanskyi", "Луганський район", "Luhansk District", "luhanska", "луганський"),
                District("luhanska:rovenkivskyi", "Ровеньківський район", "Rovenky District", "luhanska", "ровеньківський"),
                District("luhanska:svativskyi", "Сватівський район", "Svatove District", "luhanska", "сватівський"),
                District("luhanska:sievierodonetskyi", "Сєвєродонецький район", "Sievierodonetsk District", "luhanska", "сєвєродонецький"),
                District("luhanska:starobilskyi", "Старобільський район", "Starobilsk District", "luhanska", "старобільський"),
                District("luhanska:shchastynskyi", "Щастинський район", "Shchastia District", "luhanska", "щастинський")
            )
        ),
        Region(
            id = "lvivska",
            nameUk = "Львівська область",
            nameEn = "Lviv Oblast",
            neptunKey = "львівська",
            raions = listOf(
                District("lvivska:drohobytskyi", "Дрогобицький район", "Drohobych District", "lvivska", "дрогобицький"),
                District("lvivska:zolochivskyi", "Золочівський район", "Zolochiv District", "lvivska", "золочівський"),
                District("lvivska:lvivskyi", "Львівський район", "Lviv District", "lvivska", "львівський"),
                District("lvivska:sambirskyi", "Самбірський район", "Sambir District", "lvivska", "самбірський"),
                District("lvivska:stryiskyi", "Стрийський район", "Stryi District", "lvivska", "стрийський"),
                District("lvivska:chervonohradskyi", "Червоноградський район", "Chervonohrad District", "lvivska", "червоноградський"),
                District("lvivska:yavorivskyi", "Яворівський район", "Yavoriv District", "lvivska", "яворівський")
            )
        ),
        Region(
            id = "mykolaivska",
            nameUk = "Миколаївська область",
            nameEn = "Mykolaiv Oblast",
            neptunKey = "миколаївська",
            raions = listOf(
                District("mykolaivska:bashtanskyi", "Баштанський район", "Bashtanka District", "mykolaivska", "баштанський"),
                District("mykolaivska:voznesenskyi", "Вознесенський район", "Voznesensk District", "mykolaivska", "вознесенський"),
                District("mykolaivska:mykolaivskyi", "Миколаївський район", "Mykolaiv District", "mykolaivska", "миколаївський"),
                District("mykolaivska:pervomaiskyi", "Первомайський район", "Pervomaisk District", "mykolaivska", "первомайський")
            )
        ),
        Region(
            id = "odeska",
            nameUk = "Одеська область",
            nameEn = "Odesa Oblast",
            neptunKey = "одеська",
            raions = listOf(
                District("odeska:berezivskyi", "Березівський район", "Berezivka District", "odeska", "березівський"),
                District("odeska:bilhorod_dnistrovskyi", "Білгород-Дністровський район", "Bilhorod-Dnistrovskyi District", "odeska", "білгород-дністровський"),
                District("odeska:bolhradskyi", "Болградський район", "Bolhrad District", "odeska", "болградський"),
                District("odeska:izmailskyi", "Ізмаїльський район", "Izmail District", "odeska", "ізмаїльський"),
                District("odeska:odeskyi", "Одеський район", "Odesa District", "odeska", "одеський"),
                District("odeska:podilskyi", "Подільський район", "Podilsk District", "odeska", "подільський"),
                District("odeska:rozdilnianskyi", "Роздільнянський район", "Rozdilna District", "odeska", "роздільнянський")
            )
        ),
        Region(
            id = "poltavska",
            nameUk = "Полтавська область",
            nameEn = "Poltava Oblast",
            neptunKey = "полтавська",
            raions = listOf(
                District("poltavska:kremenchutskyi", "Кременчуцький район", "Kremenchuk District", "poltavska", "кременчуцький"),
                District("poltavska:lubenskyi", "Лубенський район", "Lubny District", "poltavska", "лубенський"),
                District("poltavska:myrhorodskyi", "Миргородський район", "Myrhorod District", "poltavska", "миргородський"),
                District("poltavska:poltavskyi", "Полтавський район", "Poltava District", "poltavska", "полтавський")
            )
        ),
        Region(
            id = "rivnenska",
            nameUk = "Рівненська область",
            nameEn = "Rivne Oblast",
            neptunKey = "рівненська",
            raions = listOf(
                District("rivnenska:varaskyi", "Вараський район", "Varash District", "rivnenska", "вараський"),
                District("rivnenska:dubenskyi", "Дубенський район", "Dubno District", "rivnenska", "дубенський"),
                District("rivnenska:kostopilskyi", "Рівненський район", "Rivne District", "rivnenska", "рівненський"),
                District("rivnenska:sarnenskyi", "Сарненський район", "Sarny District", "rivnenska", "сарненський")
            )
        ),
        Region(
            id = "sumska",
            nameUk = "Сумська область",
            nameEn = "Sumy Oblast",
            neptunKey = "сумська",
            raions = listOf(
                District("sumska:konotopskyi", "Конотопський район", "Konotop District", "sumska", "конотопський"),
                District("sumska:okhtyrskyi", "Охтирський район", "Okhtyrka District", "sumska", "охтирський"),
                District("sumska:romenskyi", "Роменський район", "Romny District", "sumska", "роменський"),
                District("sumska:sumskyi", "Сумський район", "Sumy District", "sumska", "сумський"),
                District("sumska:shostkynskyi", "Шосткинський район", "Shostka District", "sumska", "шосткинський")
            )
        ),
        Region(
            id = "ternopilska",
            nameUk = "Тернопільська область",
            nameEn = "Ternopil Oblast",
            neptunKey = "тернопільська",
            raions = listOf(
                District("ternopilska:kremenetskyi", "Кременецький район", "Kremenets District", "ternopilska", "кременецький"),
                District("ternopilska:ternopilskyi", "Тернопільський район", "Ternopil District", "ternopilska", "тернопільський"),
                District("ternopilska:chortkivskyi", "Чортківський район", "Chortkiv District", "ternopilska", "чортківський")
            )
        ),
        Region(
            id = "kharkivska",
            nameUk = "Харківська область",
            nameEn = "Kharkiv Oblast",
            neptunKey = "харківська",
            raions = listOf(
                District("kharkivska:bohodukhivskyi", "Богодухівський район", "Bohodukhiv District", "kharkivska", "богодухівський"),
                District("kharkivska:iziumskyi", "Ізюмський район", "Izium District", "kharkivska", "ізюмський"),
                District("kharkivska:krasnohradskyi", "Красноградський район", "Krasnohrad District", "kharkivska", "красноградський"),
                District("kharkivska:kupianskyi", "Куп'янський район", "Kupiansk District", "kharkivska", "куп'янський"),
                District("kharkivska:lozivskyi", "Лозівський район", "Lozova District", "kharkivska", "лозівський"),
                District("kharkivska:kharkivskyi", "Харківський район", "Kharkiv District", "kharkivska", "харківський"),
                District("kharkivska:chuhuivskyi", "Чугуївський район", "Chuhuiv District", "kharkivska", "чугуївський")
            )
        ),
        Region(
            id = "khersonska",
            nameUk = "Херсонська область",
            nameEn = "Kherson Oblast",
            neptunKey = "херсонська",
            raions = listOf(
                District("khersonska:beryslavskyi", "Бериславський район", "Beryslav District", "khersonska", "бериславський"),
                District("khersonska:henicheskyi", "Генічеський район", "Henichesk District", "khersonska", "генічеський"),
                District("khersonska:kakhovskyi", "Каховський район", "Kakhovka District", "khersonska", "каховський"),
                District("khersonska:skadovskyi", "Скадовський район", "Skadovsk District", "khersonska", "скадовський"),
                District("khersonska:khersonskyi", "Херсонський район", "Kherson District", "khersonska", "херсонський")
            )
        ),
        Region(
            id = "khmelnytska",
            nameUk = "Хмельницька область",
            nameEn = "Khmelnytskyi Oblast",
            neptunKey = "хмельницька",
            raions = listOf(
                District("khmelnytska:kamianets_podilskyi", "Кам'янець-Подільський район", "Kamianets-Podilskyi District", "khmelnytska", "кам'янець-подільський"),
                District("khmelnytska:khmelnytskyi", "Хмельницький район", "Khmelnytskyi District", "khmelnytska", "хмельницький"),
                District("khmelnytska:shepetivskyi", "Шепетівський район", "Shepetivka District", "khmelnytska", "шепетівський")
            )
        ),
        Region(
            id = "cherkaska",
            nameUk = "Черкаська область",
            nameEn = "Cherkasy Oblast",
            neptunKey = "черкаська",
            raions = listOf(
                District("cherkaska:zvenyhorodskyi", "Звенигородський район", "Zvenyhorodka District", "cherkaska", "звенигородський"),
                District("cherkaska:zolotoniskyi", "Золотоніський район", "Zolotonosha District", "cherkaska", "золотоніський"),
                District("cherkaska:umanskyi", "Уманський район", "Uman District", "cherkaska", "уманський"),
                District("cherkaska:cherkaskyi", "Черкаський район", "Cherkasy District", "cherkaska", "черкаський")
            )
        ),
        Region(
            id = "chernivetska",
            nameUk = "Чернівецька область",
            nameEn = "Chernivtsi Oblast",
            neptunKey = "чернівецька",
            raions = listOf(
                District("chernivetska:vyzhnytskyi", "Вижницький район", "Vyzhnytsia District", "chernivetska", "вижницький"),
                District("chernivetska:dnistrovskyi", "Дністровський район", "Dnistrovskyi District", "chernivetska", "дністровський"),
                District("chernivetska:chernivetskyi", "Чернівецький район", "Chernivtsi District", "chernivetska", "чернівецький")
            )
        ),
        Region(
            id = "chernihivska",
            nameUk = "Чернігівська область",
            nameEn = "Chernihiv Oblast",
            neptunKey = "чернігівська",
            raions = listOf(
                District("chernihivska:koriukivskyi", "Корюківський район", "Koriukivka District", "chernihivska", "корюківський"),
                District("chernihivska:nizhynskyi", "Ніжинський район", "Nizhyn District", "chernihivska", "ніжинський"),
                District("chernihivska:novhorod_siverskyi", "Новгород-Сіверський район", "Novhorod-Siverskyi District", "chernihivska", "новгород-сіверський"),
                District("chernihivska:prylutskyi", "Прилуцький район", "Pryluky District", "chernihivska", "прилуцький"),
                District("chernihivska:chernihivskyi", "Чернігівський район", "Chernihiv District", "chernihivska", "чернігівський")
            )
        ),
        Region(
            id = "krym",
            nameUk = "АР Крим",
            nameEn = "Autonomous Republic of Crimea",
            neptunKey = "автономна республіка крим",
            raions = listOf(
                District("krym:bakhchysaraiskyi", "Бахчисарайський район", "Bakhchysarai District", "krym", "бахчисарайський"),
                District("krym:bilohirskyi", "Білогірський район", "Bilohirsk District", "krym", "білогірський"),
                District("krym:dzhankoiskyi", "Джанкойський район", "Dzhankoi District", "krym", "джанкойський"),
                District("krym:yevpatoriiskyi", "Євпаторійський район", "Yevpatoriia District", "krym", "євпаторійський"),
                District("krym:kerchenskyi", "Керченський район", "Kerch District", "krym", "керченський"),
                District("krym:kurmanskyi", "Курманський район", "Kurman District", "krym", "курманський"),
                District("krym:perekopskyi", "Перекопський район", "Perekop District", "krym", "перекопський"),
                District("krym:simferopolskyi", "Сімферопольський район", "Simferopol District", "krym", "сімферопольський"),
                District("krym:feodosiiskyi", "Феодосійський район", "Feodosiia District", "krym", "феодосійський"),
                District("krym:yaltynskyi", "Ялтинський район", "Yalta District", "krym", "ялтинський")
            )
        ),
        Region(
            id = "sevastopol",
            nameUk = "м. Севастополь",
            nameEn = "Sevastopol City",
            neptunKey = "севастополь",
            raions = emptyList()
        )
    )

    fun findRegion(key: String): Region? {
        return ALL_REGIONS.firstOrNull { it.id == key || it.neptunKey == key }
    }

    fun findDistrict(key: String): District? {
        return ALL_REGIONS.flatMap { it.raions }.firstOrNull { it.id == key || it.neptunKey == key }
    }

    fun computeAlertStatus(
        alerts: NeptunAlertsResponse,
        regionId: String,
        regionName: String,
        districtId: String?,
        districtName: String?
    ): AlertStatus {
        val region = findRegion(regionId)
        val district = districtId?.let { findDistrict(it) }
        val neptunRegionKey = region?.neptunKey
        val neptunDistrictKey = district?.neptunKey

        // Workaround for API bug: entries without a non-blank "level" are NOT active alerts
        val activeOblasts = alerts.oblasts.filter { !it.level.isNullOrBlank() }
        val activeRaions = alerts.raions.filter { !it.level.isNullOrBlank() }

        // 1. Check if entire oblast has an active alert
        val matchingOblast = activeOblasts.firstOrNull { oblast ->
            oblast.key == regionId ||
            (neptunRegionKey != null && oblast.key.equals(neptunRegionKey, ignoreCase = true)) ||
            oblast.name.equals(regionName, ignoreCase = true) ||
            oblast.oblast.equals(regionName, ignoreCase = true) ||
            regionName.contains(oblast.key, ignoreCase = true)
        }

        var isAlarm = false
        var sinceTime: String? = null
        var alertLevel: String? = null
        val alertReasons = mutableListOf<String>()

        if (districtId != null) {
            // 2. Specific district selected
            val matchingRaion = activeRaions.firstOrNull { raion ->
                val keyMatches = (neptunDistrictKey != null && raion.key.equals(neptunDistrictKey, ignoreCase = true)) ||
                                 raion.key == districtId

                val sameOblast = raion.oblast.isBlank() ||
                                 raion.oblast.contains(regionName, ignoreCase = true) ||
                                 regionName.contains(raion.oblast, ignoreCase = true)

                val nameMatches = sameOblast && (
                    raion.name.equals(districtName, ignoreCase = true) ||
                    (districtName != null && (
                        raion.key.equals(districtName.substringBefore(" ").trim(), ignoreCase = true) ||
                        districtName.contains(raion.key, ignoreCase = true)
                    ))
                )

                keyMatches || nameMatches
            }

            if (matchingRaion != null || matchingOblast != null) {
                isAlarm = true
                val isRed = matchingRaion?.level.equals("red", ignoreCase = true) ||
                            matchingOblast?.level.equals("red", ignoreCase = true)
                alertLevel = if (isRed) "red" else "yellow"
                sinceTime = matchingRaion?.since ?: matchingOblast?.since

                matchingRaion?.reasons?.let { alertReasons.addAll(it) }
                matchingOblast?.reasons?.let { alertReasons.addAll(it) }
            }
        } else {
            // 3. Entire oblast selected
            val matchingRaions = activeRaions.filter { raion ->
                raion.key.startsWith("$regionId:") ||
                (neptunRegionKey != null && raion.key.startsWith("$neptunRegionKey:")) ||
                raion.oblast.contains(regionName, ignoreCase = true) ||
                regionName.contains(raion.oblast, ignoreCase = true)
            }

            if (matchingOblast != null || matchingRaions.isNotEmpty()) {
                isAlarm = true
                val isRed = matchingOblast?.level.equals("red", ignoreCase = true) ||
                            matchingRaions.any { it.level.equals("red", ignoreCase = true) }
                alertLevel = if (isRed) "red" else "yellow"
                sinceTime = matchingOblast?.since ?: matchingRaions.firstOrNull()?.since

                matchingOblast?.reasons?.let { alertReasons.addAll(it) }
                matchingRaions.forEach { alertReasons.addAll(it.reasons) }
            }
        }

        return AlertStatus(
            isAlarm = isAlarm,
            regionKey = regionId,
            regionName = regionName,
            districtKey = districtId,
            districtName = districtName,
            since = sinceTime,
            updatedAt = System.currentTimeMillis(),
            level = alertLevel,
            reasons = alertReasons.distinct()
        )
    }
}
