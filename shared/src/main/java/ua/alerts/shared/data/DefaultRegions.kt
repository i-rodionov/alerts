package ua.alerts.shared.data

import ua.alerts.shared.model.District
import ua.alerts.shared.model.Region

object DefaultRegions {
    val ALL_REGIONS: List<Region> = listOf(
        Region(
            id = "m_kyiv",
            nameUk = "м. Київ",
            nameEn = "Kyiv City",
            raions = emptyList()
        ),
        Region(
            id = "kyivska",
            nameUk = "Київська область",
            nameEn = "Kyiv Oblast",
            raions = listOf(
                District("kyivska:bilotserkivskyi", "Білоцерківський район", "Bila Tserkva District", "kyivska"),
                District("kyivska:boryspilskyi", "Бориспільський район", "Boryspil District", "kyivska"),
                District("kyivska:brovarskyi", "Броварський район", "Brovary District", "kyivska"),
                District("kyivska:buchanskyi", "Бучанський район", "Bucha District", "kyivska"),
                District("kyivska:fastivskyi", "Фастівський район", "Fastiv District", "kyivska"),
                District("kyivska:obukhivskyi", "Обухівський район", "Obukhiv District", "kyivska"),
                District("kyivska:vyshhorodskyi", "Вишгородський район", "Vyshhorod District", "kyivska")
            )
        ),
        Region(
            id = "vinnytska",
            nameUk = "Вінницька область",
            nameEn = "Vinnytsia Oblast",
            raions = listOf(
                District("vinnytska:vinnytskyi", "Вінницький район", "Vinnytsia District", "vinnytska"),
                District("vinnytska:haisynskyi", "Гайсинський район", "Haisyn District", "vinnytska"),
                District("vinnytska:zhmerynskyi", "Жмеринський район", "Zhmerynka District", "vinnytska"),
                District("vinnytska:mohyliv_podilskyi", "Могилів-Подільський район", "Mohyliv-Podilskyi District", "vinnytska"),
                District("vinnytska:tulchynskyi", "Тульчинський район", "Tulchyn District", "vinnytska"),
                District("vinnytska:khmilnytskyi", "Хмільницький район", "Khmilnyk District", "vinnytska")
            )
        ),
        Region(
            id = "volynska",
            nameUk = "Волинська область",
            nameEn = "Volyn Oblast",
            raions = listOf(
                District("volynska:volodymyrskyi", "Володимирський район", "Volodymyr District", "volynska"),
                District("volynska:kamin_kashyrskyi", "Камінь-Каширський район", "Kamin-Kashyrskyi District", "volynska"),
                District("volynska:kovelskyi", "Ковельський район", "Kovel District", "volynska"),
                District("volynska:lutskyi", "Луцький район", "Lutsk District", "volynska")
            )
        ),
        Region(
            id = "dnipropetrovska",
            nameUk = "Дніпропетровська область",
            nameEn = "Dnipropetrovsk Oblast",
            raions = listOf(
                District("dnipropetrovska:dniprovskyi", "Дніпровський район", "Dnipro District", "dnipropetrovska"),
                District("dnipropetrovska:kamianskyi", "Кам'янський район", "Kamianske District", "dnipropetrovska"),
                District("dnipropetrovska:kryvorizkyi", "Криворізький район", "Kryvyi Rih District", "dnipropetrovska"),
                District("dnipropetrovska:nikopolskyi", "Нікопольський район", "Nikopol District", "dnipropetrovska"),
                District("dnipropetrovska:novomoskovskyi", "Новомосковський район", "Novomoskovsk District", "dnipropetrovska"),
                District("dnipropetrovska:pavlohradskyi", "Павлоградський район", "Pavlohrad District", "dnipropetrovska"),
                District("dnipropetrovska:synelnykivskyi", "Синельниківський район", "Synelnykove District", "dnipropetrovska")
            )
        ),
        Region(
            id = "donetska",
            nameUk = "Донецька область",
            nameEn = "Donetsk Oblast",
            raions = listOf(
                District("donetska:bakhmutskyi", "Бахмутський район", "Bakhmut District", "donetska"),
                District("donetska:volnovaskyi", "Волноваський район", "Volnovakha District", "donetska"),
                District("donetska:horlivskyi", "Горлівський район", "Horlivka District", "donetska"),
                District("donetska:donetskyi", "Донецький район", "Donetsk District", "donetska"),
                District("donetska:kalmiuskyi", "Кальміуський район", "Kalmiuske District", "donetska"),
                District("donetska:kramatorskyi", "Краматорський район", "Kramatorsk District", "donetska"),
                District("donetska:mariupolskyi", "Маріупольський район", "Mariupol District", "donetska"),
                District("donetska:pokrovskyi", "Покровський район", "Pokrovsk District", "donetska")
            )
        ),
        Region(
            id = "zhytomyrska",
            nameUk = "Житомирська область",
            nameEn = "Zhytomyr Oblast",
            raions = listOf(
                District("zhytomyrska:berdychivskyi", "Бердичівський район", "Berdychiv District", "zhytomyrska"),
                District("zhytomyrska:zhytomyrskyi", "Житомирський район", "Zhytomyr District", "zhytomyrska"),
                District("zhytomyrska:korostenskyi", "Коростенський район", "Korosten District", "zhytomyrska"),
                District("zhytomyrska:zviahelskyi", "Звягельський район", "Zviahel District", "zhytomyrska")
            )
        ),
        Region(
            id = "zakarpatska",
            nameUk = "Закарпатська область",
            nameEn = "Zakarpattia Oblast",
            raions = listOf(
                District("zakarpatska:berehivskyi", "Берегівський район", "Berehove District", "zakarpatska"),
                District("zakarpatska:mukachivskyi", "Мукачівський район", "Mukachevo District", "zakarpatska"),
                District("zakarpatska:rakhivskyi", "Рахівський район", "Rakhiv District", "zakarpatska"),
                District("zakarpatska:tiachivskyi", "Тячівський район", "Tiachiv District", "zakarpatska"),
                District("zakarpatska:uzhhorodskyi", "Ужгородський район", "Uzhhorod District", "zakarpatska"),
                District("zakarpatska:khustskyi", "Хустський район", "Khust District", "zakarpatska")
            )
        ),
        Region(
            id = "zaporizka",
            nameUk = "Запорізька область",
            nameEn = "Zaporizhzhia Oblast",
            raions = listOf(
                District("zaporizka:berdianskyi", "Бердянський район", "Berdiansk District", "zaporizka"),
                District("zaporizka:vasylivskyi", "Василівський район", "Vasylivka District", "zaporizka"),
                District("zaporizka:zaporizkyi", "Запорізький район", "Zaporizhzhia District", "zaporizka"),
                District("zaporizka:melitopolskyi", "Мелітопольський район", "Melitopol District", "zaporizka"),
                District("zaporizka:polohivskyi", "Пологівський район", "Polohy District", "zaporizka")
            )
        ),
        Region(
            id = "ivano_frankivska",
            nameUk = "Івано-Франківська область",
            nameEn = "Ivano-Frankivsk Oblast",
            raions = listOf(
                District("ivano_frankivska:verkhovynskyi", "Верховинський район", "Verkhovyna District", "ivano_frankivska"),
                District("ivano_frankivska:ivano_frankivskyi", "Івано-Франківський район", "Ivano-Frankivsk District", "ivano_frankivska"),
                District("ivano_frankivska:kaluskyi", "Калуський район", "Kalush District", "ivano_frankivska"),
                District("ivano_frankivska:kolomyiskyi", "Коломийський район", "Kolomyia District", "ivano_frankivska"),
                District("ivano_frankivska:kosivskyi", "Косівський район", "Kosiv District", "ivano_frankivska"),
                District("ivano_frankivska:nadvirnianskyi", "Надвірнянський район", "Nadvirna District", "ivano_frankivska")
            )
        ),
        Region(
            id = "kirovohradska",
            nameUk = "Кіровоградська область",
            nameEn = "Kirovohrad Oblast",
            raions = listOf(
                District("kirovohradska:holovanivskyi", "Голованівський район", "Holovanivsk District", "kirovohradska"),
                District("kirovohradska:kropyvnytskyi", "Кропивницький район", "Kropyvnytskyi District", "kirovohradska"),
                District("kirovohradska:novoukrainskyi", "Новоукраїнський район", "Novoukrainka District", "kirovohradska"),
                District("kirovohradska:oleksandriiskyi", "Олександрійський район", "Oleksandriia District", "kirovohradska")
            )
        ),
        Region(
            id = "luhanska",
            nameUk = "Луганська область",
            nameEn = "Luhansk Oblast",
            raions = listOf(
                District("luhanska:alchevskyi", "Алчевський район", "Alchevsk District", "luhanska"),
                District("luhanska:dovzhanskyi", "Довжанський район", "Dovzhansk District", "luhanska"),
                District("luhanska:luhanskyi", "Луганський район", "Luhansk District", "luhanska"),
                District("luhanska:rovenkivskyi", "Ровеньківський район", "Rovenky District", "luhanska"),
                District("luhanska:svativskyi", "Сватівський район", "Svatove District", "luhanska"),
                District("luhanska:sievierodonetskyi", "Сєвєродонецький район", "Sievierodonetsk District", "luhanska"),
                District("luhanska:starobilskyi", "Старобільський район", "Starobilsk District", "luhanska"),
                District("luhanska:shchastynskyi", "Щастинський район", "Shchastia District", "luhanska")
            )
        ),
        Region(
            id = "lvivska",
            nameUk = "Львівська область",
            nameEn = "Lviv Oblast",
            raions = listOf(
                District("lvivska:drohobytskyi", "Дрогобицький район", "Drohobych District", "lvivska"),
                District("lvivska:zolochivskyi", "Золочівський район", "Zolochiv District", "lvivska"),
                District("lvivska:lvivskyi", "Львівський район", "Lviv District", "lvivska"),
                District("lvivska:sambirskyi", "Самбірський район", "Sambir District", "lvivska"),
                District("lvivska:stryiskyi", "Стрийський район", "Stryi District", "lvivska"),
                District("lvivska:chervonohradskyi", "Червоноградський район", "Chervonohrad District", "lvivska"),
                District("lvivska:yavorivskyi", "Яворівський район", "Yavoriv District", "lvivska")
            )
        ),
        Region(
            id = "mykolaivska",
            nameUk = "Миколаївська область",
            nameEn = "Mykolaiv Oblast",
            raions = listOf(
                District("mykolaivska:bashtanskyi", "Баштанський район", "Bashtanka District", "mykolaivska"),
                District("mykolaivska:voznesenskyi", "Вознесенський район", "Voznesensk District", "mykolaivska"),
                District("mykolaivska:mykolaivskyi", "Миколаївський район", "Mykolaiv District", "mykolaivska"),
                District("mykolaivska:pervomaiskyi", "Первомайський район", "Pervomaisk District", "mykolaivska")
            )
        ),
        Region(
            id = "odeska",
            nameUk = "Одеська область",
            nameEn = "Odesa Oblast",
            raions = listOf(
                District("odeska:berezivskyi", "Березівський район", "Berezivka District", "odeska"),
                District("odeska:bilhorod_dnistrovskyi", "Білгород-Дністровський район", "Bilhorod-Dnistrovskyi District", "odeska"),
                District("odeska:bolhradskyi", "Болградський район", "Bolhrad District", "odeska"),
                District("odeska:izmailskyi", "Ізмаїльський район", "Izmail District", "odeska"),
                District("odeska:odeskyi", "Одеський район", "Odesa District", "odeska"),
                District("odeska:podilskyi", "Подільський район", "Podilsk District", "odeska"),
                District("odeska:rozdilnianskyi", "Роздільнянський район", "Rozdilna District", "odeska")
            )
        ),
        Region(
            id = "poltavska",
            nameUk = "Полтавська область",
            nameEn = "Poltava Oblast",
            raions = listOf(
                District("poltavska:kremenchutskyi", "Кременчуцький район", "Kremenchuk District", "poltavska"),
                District("poltavska:lubenskyi", "Лубенський район", "Lubny District", "poltavska"),
                District("poltavska:myrhorodskyi", "Миргородський район", "Myrhorod District", "poltavska"),
                District("poltavska:poltavskyi", "Полтавський район", "Poltava District", "poltavska")
            )
        ),
        Region(
            id = "rivnenska",
            nameUk = "Рівненська область",
            nameEn = "Rivne Oblast",
            raions = listOf(
                District("rivnenska:varaskyi", "Вараський район", "Varash District", "rivnenska"),
                District("rivnenska:dubenskyi", "Дубенський район", "Dubno District", "rivnenska"),
                District("rivnenska:kostopilskyi", "Рівненський район", "Rivne District", "rivnenska"),
                District("rivnenska:sarnenskyi", "Сарненський район", "Sarny District", "rivnenska")
            )
        ),
        Region(
            id = "sumska",
            nameUk = "Сумська область",
            nameEn = "Sumy Oblast",
            raions = listOf(
                District("sumska:konotopskyi", "Конотопський район", "Konotop District", "sumska"),
                District("sumska:okhtyrskyi", "Охтирський район", "Okhtyrka District", "sumska"),
                District("sumska:romenskyi", "Роменський район", "Romny District", "sumska"),
                District("sumska:sumskyi", "Сумський район", "Sumy District", "sumska"),
                District("sumska:shostkynskyi", "Шосткинський район", "Shostka District", "sumska")
            )
        ),
        Region(
            id = "ternopilska",
            nameUk = "Тернопільська область",
            nameEn = "Ternopil Oblast",
            raions = listOf(
                District("ternopilska:kremenetskyi", "Кременецький район", "Kremenets District", "ternopilska"),
                District("ternopilska:ternopilskyi", "Тернопільський район", "Ternopil District", "ternopilska"),
                District("ternopilska:chortkivskyi", "Чортківський район", "Chortkiv District", "ternopilska")
            )
        ),
        Region(
            id = "kharkivska",
            nameUk = "Харківська область",
            nameEn = "Kharkiv Oblast",
            raions = listOf(
                District("kharkivska:bohodukhivskyi", "Богодухівський район", "Bohodukhiv District", "kharkivska"),
                District("kharkivska:iziumskyi", "Ізюмський район", "Izium District", "kharkivska"),
                District("kharkivska:krasnohradskyi", "Красноградський район", "Krasnohrad District", "kharkivska"),
                District("kharkivska:kupianskyi", "Куп'янський район", "Kupiansk District", "kharkivska"),
                District("kharkivska:lozivskyi", "Лозівський район", "Lozova District", "kharkivska"),
                District("kharkivska:kharkivskyi", "Харківський район", "Kharkiv District", "kharkivska"),
                District("kharkivska:chuhuivskyi", "Чугуївський район", "Chuhuiv District", "kharkivska")
            )
        ),
        Region(
            id = "khersonska",
            nameUk = "Херсонська область",
            nameEn = "Kherson Oblast",
            raions = listOf(
                District("khersonska:beryslavskyi", "Бериславський район", "Beryslav District", "khersonska"),
                District("khersonska:henicheskyi", "Генічеський район", "Henichesk District", "khersonska"),
                District("khersonska:kakhovskyi", "Каховський район", "Kakhovka District", "khersonska"),
                District("khersonska:skadovskyi", "Скадовський район", "Skadovsk District", "khersonska"),
                District("khersonska:khersonskyi", "Херсонський район", "Kherson District", "khersonska")
            )
        ),
        Region(
            id = "khmelnytska",
            nameUk = "Хмельницька область",
            nameEn = "Khmelnytskyi Oblast",
            raions = listOf(
                District("khmelnytska:kamianets_podilskyi", "Кам'янець-Подільський район", "Kamianets-Podilskyi District", "khmelnytska"),
                District("khmelnytska:khmelnytskyi", "Хмельницький район", "Khmelnytskyi District", "khmelnytska"),
                District("khmelnytska:shepetivskyi", "Шепетівський район", "Shepetivka District", "khmelnytska")
            )
        ),
        Region(
            id = "cherkaska",
            nameUk = "Черкаська область",
            nameEn = "Cherkasy Oblast",
            raions = listOf(
                District("cherkaska:zvenyhorodskyi", "Звенигородський район", "Zvenyhorodka District", "cherkaska"),
                District("cherkaska:zolotoniskyi", "Золотоніський район", "Zolotonosha District", "cherkaska"),
                District("cherkaska:umanskyi", "Уманський район", "Uman District", "cherkaska"),
                District("cherkaska:cherkaskyi", "Черкаський район", "Cherkasy District", "cherkaska")
            )
        ),
        Region(
            id = "chernivetska",
            nameUk = "Чернівецька область",
            nameEn = "Chernivtsi Oblast",
            raions = listOf(
                District("chernivetska:vyzhnytskyi", "Вижницький район", "Vyzhnytsia District", "chernivetska"),
                District("chernivetska:dnistrovskyi", "Дністровський район", "Dnistrovskyi District", "chernivetska"),
                District("chernivetska:chernivetskyi", "Чернівецький район", "Chernivtsi District", "chernivetska")
            )
        ),
        Region(
            id = "chernihivska",
            nameUk = "Чернігівська область",
            nameEn = "Chernihiv Oblast",
            raions = listOf(
                District("chernihivska:koriukivskyi", "Корюківський район", "Koriukivka District", "chernihivska"),
                District("chernihivska:nizhynskyi", "Ніжинський район", "Nizhyn District", "chernihivska"),
                District("chernihivska:novhorod_siverskyi", "Новгород-Сіверський район", "Novhorod-Siverskyi District", "chernihivska"),
                District("chernihivska:prylutskyi", "Прилуцький район", "Pryluky District", "chernihivska"),
                District("chernihivska:chernihivskyi", "Чернігівський район", "Chernihiv District", "chernihivska")
            )
        ),
        Region(
            id = "krym",
            nameUk = "АР Крим",
            nameEn = "Autonomous Republic of Crimea",
            raions = emptyList()
        ),
        Region(
            id = "sevastopol",
            nameUk = "м. Севастополь",
            nameEn = "Sevastopol City",
            raions = emptyList()
        )
    )

    fun findRegion(key: String): Region? {
        return ALL_REGIONS.firstOrNull { it.id == key }
    }

    fun findDistrict(key: String): District? {
        return ALL_REGIONS.flatMap { it.raions }.firstOrNull { it.id == key }
    }
}
