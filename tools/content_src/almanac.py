"""Folk weather and garden almanac: almanac.tsv, one line for every day (01-01 … 12-31 with 02-29).

Russian folk calendar flavour: names of days are folklore about weather, nature and the garden.
Each line is «ru || en».
"""
from common import write

COLS = "md text_ru text_en".split()

MONTHS = [
# January
"""
Новый год — к весне поворот: день понемногу прибавляется, а мороз крепчает. || New Year turns toward spring: the days slowly lengthen while the frost grows harder.
Если второго января ясно и морозно, жди урожайного года на рожь. || If the second of January is clear and frosty, expect a good year for rye.
Ветер с юга в начале января — к грозовому лету. || A south wind early in January means a thundery summer.
Пушистый иней на деревьях — к ясной погоде, а в народе говорили, что и к хорошему мёду. || Fluffy hoarfrost on the trees means clear weather — and, folk said, good honey to come.
Снег глубок — хлеб хорош: под сугробом озимым тепло, как под одеялом. || Deep snow, good bread: winter crops lie warm under the drifts as if under a quilt.
Звёздная ночь в начале января — к урожаю гороха и ягод. || A starry night in early January promises a crop of peas and berries.
Метель в первую неделю года — к хорошему роению пчёл, говорили пасечники. || A blizzard in the first week of the year means the bees will swarm well, the beekeepers used to say.
Бабьи каши: в этот день варили кашу и угощали соседей. Удалась каша — жди урожая. || Granny's Porridge Day: porridge was cooked and shared with the neighbours. If it turned out well, a good harvest would follow.
Голубоватые облака в январе — к скорому теплу. || Bluish clouds in January mean warmer weather is on its way.
Дым столбом — к морозу, дым коромыслом — к оттепели. || Smoke rising straight up means frost; smoke bending like a yoke means a thaw.
Синицы с утра пищат — к морозной ночи. || Tits squeaking in the morning mean a frosty night.
Анисья-желудочница: хозяйки варили сытную еду и смотрели на зарю — красная заря к ветру. || Anisya the Hearty: housewives cooked filling food and watched the dawn — a red one means wind.
Щедрый вечер накануне старого Нового года: ясное звёздное небо — к урожаю ягод. || Generous Eve, the night before Old New Year: a clear starry sky foretells plenty of berries.
Васильев день, старый Новый год: ветер с юга сегодня — к жаркому и урожайному году. || Vasily's Day, the Old New Year: a south wind today promises a hot and fruitful year.
Синица запищала — зиму предвещает, воробьи дружно зачирикали — к оттепели. || A squeaking tit foretells more winter; sparrows chirping all together foretell a thaw.
Кошка прячет нос — к морозу, спит на спине, раскинув лапы, — к теплу. || A cat tucking its nose in means frost; a cat sprawled on its back means warmth.
Зимой подоконник — лучшая грядка: луковица в банке с водой даст зелёное перо за неделю. || In winter the windowsill is the best bed: an onion in a jar of water gives green shoots within a week.
Самые лютые морозы приходятся на середину января — укутайте горшки на холодном окне. || The fiercest frosts come in mid-January: wrap up the pots on a cold windowsill.
Ясный и холодный день в середине января — к сухому лету. || A clear, cold day in mid-January points to a dry summer.
Снегири прилетели к самому жилью — к сильным морозам. || Bullfinches coming right up to the houses mean hard frosts.
Ворона купается в снегу — к метели, сорока лезет под стреху — к бурану. || A crow bathing in the snow means a blizzard; a magpie creeping under the eaves means a snowstorm.
С конца января на тёплом окне можно сеять перец и баклажаны: им расти долго. || From late January, peppers and aubergines can be sown on a warm windowsill: they take a long time to grow.
Лес шумит без ветра — к оттепели. || A forest murmuring without wind means a thaw.
Федосья-весновка: весной сегодня уже пахнет, хоть мороз и не думает уходить. || Fedosya the Spring-teller: spring is in the air today, though the frost has no intention of leaving.
Татьянин день: солнышко — к раннему прилёту птиц, снег — к дождливому лету. || Tatyana's Day: sunshine means the birds will return early, snow means a rainy summer.
Длинные сосульки — к долгой весне. || Long icicles mean a long spring.
Звёзды сильно мерцают и переливаются цветом — к ветру и метели. || Stars twinkling hard and changing colour mean wind and snowstorms.
Снег прилипает к деревьям — к теплу. || Snow sticking to the tree trunks means warmer days.
Пётр-полукорм: прошла половина зимы — проверьте запасы, половина должна остаться. || Pyotr Half-fodder: half the winter is gone. Check your stores — half should still be left.
Антон-перезимник: мороз то отпускает, то снова берёт — зима перевалила за середину. || Anton the Over-winterer: the frost lets go, then grips again — winter is past its middle.
Афанасий-ломонос: морозы бьют по носу — берегите нос в большой мороз. || Afanasy Nose-breaker: the frosts nip at noses, so mind yours in the cold.
""",
# February
"""
Макар — весне указ: какова погода сегодня, такова и весна. || Makar points the way to spring: as the weather goes today, so goes the spring.
Ефимий: солнце в полдень — к ранней весне, метель — к поздней. || Yefim's Day: sun at noon means an early spring, a blizzard a late one.
Февраль-бокогрей: на солнечной стороне уже припекает. || February the side-warmer: on the sunny side you can already feel the heat.
Февраль-лютень: зима с весной в нём меряются силой, и каждая берёт свои дни. || February the fierce: winter and spring test their strength, each taking its own days.
Синица зазвенела по-весеннему — значит, тепло близко. || When the tit starts ringing its spring song, warm days are near.
Аксинья-полузимница: прошла половина зимы. Какова Аксинья, такова и весна. || Aksinya Half-winter: half the winter is gone. As Aksinya goes, so goes the spring.
Звёзды на небе тусклые — к оттепели. || Dim stars mean a thaw.
Пора готовить ящики и землю для рассады: пусть отогреются в доме. || Time to get trays and soil ready for seedlings: let them warm up indoors.
Февраль с инеем — к урожайному году. || A February full of hoarfrost promises a fruitful year.
Именины домового: в старину оставляли ему горшочек каши за печкой, чтобы в доме был лад. || The house spirit's name day: folk once left a pot of porridge behind the stove for him, to keep peace in the home.
Ветер подул с юга, снег посерел — весна торопится. || The wind has turned southerly and the snow has gone grey: spring is in a hurry.
Галки стаями кружат в небе — к снегопаду. || Jackdaws circling in flocks mean snowfall.
Самое время перебрать семена и проверить всхожесть на мокрой тряпочке. || Good time to sort the seeds and test them for germination on a damp cloth.
В феврале снег прибывает — весной вода прибывает. || The more snow in February, the more water in spring.
В середине февраля, говорили, зима с весной встречаются: тепло сегодня — весна будет ранней. || In mid-February, it was said, winter meets spring: if it's warm today, spring will come early.
Капель в февральский день — к хорошему урожаю пшеницы. || A dripping roof on a February day promises a good wheat harvest.
Никола-студёный: последние крепкие морозы, держите укрытыми горшки у окна. || Nikola the Chill: the last hard frosts — keep the pots by the window covered.
Агафья-коровница: в старину берегли скотину от холода и окуривали хлев травами. || Agafya of the Cows: folk kept their cattle warm today and smoked the byre with herbs.
Петрушку и сельдерей можно сеять на рассаду: всходят они долго. || Parsley and celery can be sown for seedlings now: they're slow to come up.
Кошка скребёт стену — к ненастью. || A cat scratching at the wall means foul weather is coming.
Белка вышла из гнезда в феврале — к ранней весне. || A squirrel out of its nest in February means an early spring.
Февральская вьюга: что зима намела, то весна водой унесёт. || February blizzards: what winter sweeps up, spring will wash away.
Узор на окнах густой — к сильному морозу, тает к обеду — к теплу. || Thick frost patterns on the windows mean a hard frost; if they melt by lunch, warmth is coming.
Власий — сшиби рог зиме: с этого дня морозы начинают ломаться. || Vlasy knocks the horn off winter: from today the frosts begin to break.
Ночи ещё морозные, а днём на солнце пахнет весной. Пора сеять перец, если ещё не успели. || Nights are still frosty, but in the daytime sun there's a smell of spring. Time to sow peppers if you haven't yet.
Кот к весне линяет — значит, тепло не за горами. || When the cat starts to moult, the warm days aren't far off.
С Кирилла, говорили, зима с весной борются: днём капель, ночью мороз. || From Kirill's day, they said, winter and spring wrestle: drips by day, frost by night.
Тёплый конец февраля — к холодному марту. || A warm end to February means a cold March.
Касьянов день бывает раз в четыре года. Говорили: на что Касьян ни взглянет, всё вянет, — пусть рассада посидит дома. || Kasyan's Day comes once in four years. They said whatever Kasyan looks at wilts, so keep the seedlings indoors.
""",
# March
"""
Март: днём капель, ночью мороз — так весна и строится. || March: drips by day, frost by night — that's how spring gets built.
Мартовский снег рыхлый и серый — весна будет дружной. || Loose grey March snow means a quick, all-at-once spring.
Грачи прилетели — через месяц снег сойдёт. || The rooks are back: the snow will be gone within a month.
Сейте томаты на рассаду: к маю как раз окрепнут. || Sow tomatoes for seedlings: by May they'll be sturdy.
Туман в марте — к дождливому лету. || Fog in March means a rainy summer.
Много звёзд на мартовском небе — к грибному лету. || A March sky full of stars means a good mushroom summer.
Ветер с юга в марте — тепло придёт быстро. || A south wind in March means warmth will come fast.
Тюльпаны в вазах, а на подоконнике пора сеять бархатцы и астры. || Tulips in vases, and on the windowsill it's time to sow marigolds and asters.
Вороны сидят на нижних ветках — к ветру. || Crows sitting low in the trees mean wind.
Снег тает буграми — к урожаю гречихи. || Snow melting in hummocks means a good buckwheat crop.
Капель в ясный день, а к вечеру сосульки замирают — весна идёт по расписанию. || Drips on a clear day, icicles freezing still by evening: spring is running on time.
Ранняя весна — ненадёжная: не спешите выносить рассаду на балкон. || An early spring is a fickle one: don't rush the seedlings out to the balcony.
Василий-капельник: с крыш капает — жди тёплой весны. || Vasily the Dripper: roofs dripping today mean a warm spring.
Евдокия-весновка, Авдотья-плющиха: снег оседает. На Евдокию вода — на Егория трава. || Yevdokia the Spring-bringer, Avdotya the Flattener: the snow sinks. Water on Yevdokia's day, grass by Yegory's.
Тёплый ветер после Евдокии — к тёплому и сырому лету. || A warm wind after Yevdokia's day means a warm, wet summer.
Пора подкормить рассаду и повернуть её к свету другим боком. || Time to feed the seedlings and turn them so the other side faces the light.
Герасим-грачевник: грачи ходят по проталинам важно, как хозяева. || Gerasim the Rook-man: the rooks stroll the thawed patches like landlords.
Конон-огородник: в старину в этот день впервые брались за лопату — проверить, оттаяла ли земля. || Konon the Gardener: folk once picked up a spade today for the first time, to see if the soil had thawed.
Первые проталины на газонах: запомните, где снег сходит раньше, — там будут первые грядки. || First bare patches on the lawns: note where the snow goes first — that's where your first beds belong.
Весеннее равноденствие: день сравнялся с ночью, дальше — только к свету. || The spring equinox: day equals night, and from here on it's all toward the light.
Гуси летят высоко — вода весной будет большая. || Geese flying high mean big spring floods.
Сороки: прилетают сорок птиц, пекут жаворонков. Сколько утренников на Сороки, столько и после. || Soroki, the Forty Birds: forty kinds of bird return and lark buns are baked. As many frosty mornings as today, so many will follow.
Зажурчали ручьи — пора доставать семенную картошку на проращивание. || Streams are trickling: time to bring out the seed potatoes to sprout.
Скворец прилетел — весна на пороге. || The starling's back: spring is on the doorstep.
Вербочки распушились — скоро по берёзам пойдёт сок. || The pussy willows have fluffed out: the birch sap will soon be rising.
Сухой март да мокрый май — будет каша и каравай. || A dry March and a wet May bring porridge and bread.
«Марток — надевай двое порток»: утром мороз, а днём на солнце жарко. || “March — wear two pairs of trousers”: frost in the morning, heat in the midday sun.
Утренники держатся: в грунт сеять рано, а на подоконнике самое время для капусты. || The morning frosts hold: too early to sow outside, but just right for cabbage on the windowsill.
Облака плывут высоко — к хорошей погоде. || Clouds sailing high mean fine weather.
Алексей — с гор вода: овраги наполняются, ручьи бегут. || Alexey brings water down the hills: gullies fill and streams run.
Последний день марта: проверьте чеснок под снегом — зелёные носики уже бывают. || The last day of March: check the winter garlic under the snow — the green tips sometimes show already.
""",
# April
"""
Дарья — грязные проруби: снег темнеет, лёд на реке синеет и рыхлеет. || Darya of the Dirty Ice-holes: the snow darkens and the river ice turns blue and soft.
Апрель с водою — май с травою. || April brings the water, May the grass.
Ночь ясная и тихая — к утреннему заморозку, укройте рассаду на балконе. || A clear, still night means morning frost: cover the seedlings on the balcony.
Апрельский снег — что удобрение: тает и поит землю. || April snow is as good as fertiliser: it melts and waters the soil.
Широко разлилась вода в апреле — будет много сена. || If April floods spread wide, there'll be plenty of hay.
Жаворонок поёт высоко — к ясной погоде. || A lark singing high up means fine weather.
Птицы начинают вить гнёзда — не тревожьте кусты в парке. || Birds are starting their nests: leave the park bushes in peace.
Мать-и-мачеха зацвела — можно сеять редис под укрытие. || Coltsfoot in bloom: radishes can be sown under a cover.
Ветер с востока в апреле — к затяжному холоду. || An east wind in April means the cold will linger.
Мухи и комары рано вылетели — к тёплой весне. || Early flies and midges mean a warm spring.
Тёплый апрельский дождь будит землю: сейте шпинат и салат. || A warm April rain wakes the soil: sow spinach and lettuce.
Ясный апрель — к хорошему урожаю картошки. || A clear, sunny April promises a good potato crop.
Первый гром в апреле — к урожайному лету. || The first thunder in April means a fruitful summer.
Марья — зажги снега, заиграй овражки: снег горит на солнце, по оврагам бегут ручьи. || Marya Set-the-Snow-Alight: the snow blazes in the sun and the gullies sing with streams.
Тюльпаны проклюнулись — уберите с клумбы старые листья. || Tulip shoots are poking through: clear last year's leaves off the bed.
Никита-водопол: реки выходят из берегов, лёд уходит. || Nikita of the Floods: rivers spill over their banks and the ice goes out.
Много сока течёт из берёзы — к дождливому лету. || Birches running heavy with sap mean a rainy summer.
Сейте горох: он холода не боится. || Sow peas: they don't mind the cold.
Евтихий: тихий день сегодня — к урожаю ранних хлебов. || Yevtikhy the Quiet: a still day today means a good early grain crop.
Апрельские вечера светлые и холодные — не убирайте укрывной материал. || April evenings are light and cold: don't put the frost cloth away yet.
Родион-ледолом: на реках ломается последний лёд, рыба выходит на мелководье. || Rodion the Ice-breaker: the last river ice breaks up and fish come into the shallows.
Почки лопаются, а ночью уже не морозит — сейте укроп и петрушку. || When buds burst and nights stay frost-free, sow dill and parsley.
Шмель прилетел — теплу быть. || A bumblebee about means the warmth is here to stay.
Сейте на рассаду огурцы и тыквы: к началу июня им пора на грядку. || Sow cucumbers and pumpkins for seedlings: they'll need to go out by early June.
Василий-парийский: земля парит на солнце — значит, проснулась. || Vasily of the Steam: the soil steams in the sun, which means it's awake.
Лёд ушёл, утки летят — выносите рассаду днём на балкон, пусть закаляется. || The ice is gone and the ducks are flying: take the seedlings out on the balcony by day to harden off.
Лягушка заквакала — пора сеять овёс. || When the frogs start croaking, it's time to sow oats.
Лезет молодая крапива — значит, скоро будут зелёные щи. || Young nettles are coming up: green shchi is on the way.
Арина — урви берега: вода подмывает берега, ручьи шумят. || Arina Tear-the-banks: the water gnaws the riverbanks and the streams roar.
Зосима-пчельник: пасечники выставляют ульи после зимовки. || Zosima the Beekeeper: the hives come out after the winter.
""",
# May
"""
Май холодный — год хлебородный. || A cold May makes a bread-rich year.
Берёза распустилась раньше ольхи — лето будет сухое, ольха раньше — мокрое. || If birch leafs out before alder, the summer will be dry; alder first means a wet one.
Черёмуха цветёт — к холодам. || Bird cherry in bloom means a cold snap.
Майский дождь хлеба поднимает. || May rain raises the bread.
Картошку сажают, когда распустится берёза. || Plant potatoes when the birch comes into leaf.
Егорий вешний: Егорий с водой, Никола с травой. Скотину впервые выгоняли на росу. || Yegory of Spring: Yegory brings the water, Nikola the grass. Cattle were first driven out to the dewy pasture today.
Кукушка закуковала — заморозков больше не будет. || Once the cuckoo calls, the frosts are over.
Обильная роса на траве — к хорошей погоде. || Heavy dew on the grass means fine weather.
Глафира-горошница: сеют горох, приговаривая: «Расти крупный да сахарный». || Glafira of the Peas: peas are sown with the words, “Grow big and sweet.”
Семён-ранопашец: в старину пахали самые ранние поля. || Semyon the Early Plougher: the earliest fields were ploughed today.
Соловей запел — тепло пришло насовсем, сейте смело. || The nightingale is singing: the warmth is here for good, sow without fear.
Дуб распустился раньше ясеня — к сухому лету. || Oak in leaf before ash means a dry summer.
Комары толкутся столбом — к ясной погоде. || Gnats dancing in a column mean fine weather.
Заморозок на цветущую черёмуху — обычное дело, укройте огурцы. || Frost on the flowering bird cherry is nothing unusual: cover the cucumbers.
«Борис и Глеб сеют хлеб»: время сеять яровые. || “Boris and Gleb sow the grain”: time to sow the spring crops.
Мавра-рассадница: высаживают капустную рассаду. || Mavra of the Seedlings: cabbage seedlings go out into the beds.
Пчёлы с утра дружно летают — к ясному дню. || Bees busy from early morning mean a clear day.
Ирина-рассадница: досаживают рассаду и сеют свёклу. || Irina the Planter: the last seedlings go out and beetroot is sown.
Иов-горошник: досевают горох, чтобы стручки поспели к середине лета. || Iov of the Peas: the last peas are sown so the pods will be ripe by midsummer.
Сирень зацвела — сейте фасоль, земля прогрелась. || When the lilac blooms, sow beans: the soil has warmed.
Ласточки летают высоко — к хорошей погоде. || Swallows flying high mean fine weather.
Никола вешний: до Николы не сей гречи, не стриги овечек. || Nikola of Spring: don't sow buckwheat before Nikola, nor shear the sheep.
Симонов день, земля-именинница: её не тревожат, а собирают первые травы. || Simon's Day, the earth's name day: the soil is left undisturbed and the first herbs are gathered.
Мокий-мокрый: каков Мокий, таково и лето. Дождь сегодня — к сырому лету. || Moky the Wet: as Moky goes, so goes the summer. Rain today means a damp one.
Дождь при солнце — грибной, к хорошему урожаю. || Rain with sunshine is mushroom rain, a sign of a good harvest.
Помидоры высаживают, когда отцвела черёмуха и прошли холода. || Plant out tomatoes once the bird cherry is over and the cold snap has passed.
Сидор-огуречник: время сеять огурцы в открытый грунт, если ночи уже тёплые. || Sidor of the Cucumbers: time to sow cucumbers outdoors, if the nights are warm.
Пахом-бокогрей: Пахом тёплом пахнет, с этого дня теплеет основательно. || Pakhom the Side-warmer: Pakhom smells of warmth, and from today it warms up in earnest.
Рябина цветёт обильно — к урожаю льна. || Rowan blooming heavily means a good flax year.
Сейте тыкву и кабачки в лунки, пролитые тёплой водой. || Sow pumpkins and courgettes into holes soaked with warm water.
Последний день весны: соловьи поют всю ночь — лето будет тёплым. || The last day of spring: nightingales singing all night mean a warm summer.
""",
# June
"""
Июнь-светозар: самые светлые ночи впереди. || June the Bright: the lightest nights are just ahead.
Утренний туман стелется по земле — к ясному дню. || Morning mist lying low on the ground means a clear day.
Олёна-льносейка: сеют лён, чтобы вырос длинный, как косы. || Olyona the Flax-sower: flax is sown so it grows long, like braids.
Пауки усердно плетут паутину — к сухой погоде. || Spiders spinning hard mean dry weather.
Леонтий-огуречник: досевают огурцы, а ранние уже завязывают плоды. || Leonty of the Cucumbers: the last cucumbers are sown, and the early ones are setting fruit.
Вечером туман над рекой — к хорошей погоде. || Evening mist over the river means fine weather.
Июнь начался жарким — поливайте с вечера: утренняя вода сгорает на солнце. || If June starts hot, water in the evening: morning water just burns off.
Цветёт шиповник — лето пришло окончательно. || Wild roses in bloom: summer is here for good.
Роса утром не выпала — к дождю. || No dew in the morning means rain.
Никита-гусятник: гусят выпускают на травку, а на грядках пора прореживать морковь. || Nikita the Gosling-keeper: goslings go out on the grass, and in the beds it's time to thin the carrots.
Ласточки летают низко — к дождю. || Swallows flying low mean rain.
Июньские ночи коротки, а огурцы растут даже ночью — говорят, слышно, как хрустят. || June nights are short, and cucumbers grow even at night — some say you can hear them crunch.
Муравьи прячутся в муравейник — к грозе. || Ants hurrying into the anthill mean a storm.
Клевер складывает листочки — к дождю. || Clover folding its leaves means rain is coming.
Тёплые дожди в середине июня — к грибному году. || Warm rains in mid-June mean a good mushroom year.
Лукьян-ветреник: по ветру судили о лете — северный к холодному, южный к тёплому. || Lukyan the Windy: today's wind was read for the summer ahead — northerly cold, southerly warm.
Земляника краснеет — через неделю будет вдоволь. || Wild strawberries are reddening: in a week there'll be plenty.
Дым от костра стелется по земле — к дождю. || Campfire smoke hugging the ground means rain.
Пчёлы сидят в улье — к дождю. || Bees staying in the hive mean rain.
Мяту собирают до цветения — тогда она самая душистая. || Gather mint before it flowers: that's when it smells sweetest.
Самые длинные дни: солнце будто стоит на месте. || The longest days: the sun seems to stand still in the sky.
После солнцестояния день убывает на воробьиный скок. || After the solstice the days shrink by a sparrow's hop.
Комары и мошки кусаются злее — к дождю. || Mosquitoes and midges biting harder mean rain.
Вечерняя заря золотая — к ясному дню. || A golden sunset means a clear day.
Пётр-поворот: солнце — на зиму, лето — на жару. || Pyotr the Turn: the sun turns toward winter, the summer toward heat.
Акулина-гречишница: сеют гречиху, чтобы к осени была каша. || Akulina of the Buckwheat: buckwheat is sown so there'll be porridge by autumn.
Картошка зацвела — под кустами завязались первые клубни. || The potatoes are flowering: the first tubers are forming underground.
Радуга вечером — к ясной погоде. || A rainbow in the evening means fine weather.
Пчёлы улетают далеко от улья — к вёдру. || Bees flying far from the hive mean a spell of fine weather.
Цветы срезают утром, пока роса, — дольше простоят в вазе. || Cut flowers in the morning while the dew's on them: they last longer in a vase.
""",
# July
"""
Июль — макушка лета: всё в цвету и в силе. || July is the crown of summer: everything in bloom and at full strength.
Тучи идут против ветра — к грозе. || Clouds moving against the wind mean a thunderstorm.
Цветы пахнут сильнее обычного — к дождю. || Flowers smelling stronger than usual mean rain.
Липа цветёт — собирайте цвет в сухой день и сушите в тени. || The limes are in flower: gather the blossom on a dry day and dry it in the shade.
После тёплого дождя в парке появляются подберёзовики. || After a warm rain, birch boletes appear in the park.
Аграфена-купальница: вяжут банные веники из берёзы и душистых трав. || Agrafena the Bather: bath bundles are tied from birch twigs and fragrant herbs.
Иван Купала: травы в самой силе, росу собирают на рассвете. Тёплая ночь — к урожаю. || Ivan Kupala: herbs are at their strongest and dew is gathered at dawn. A warm night means a good harvest.
После Купалы проверяйте огурцы каждый день: переросший огурец задерживает новые. || After Kupala, check the cucumbers daily: an overgrown one holds back the rest.
Утром трава сухая — к вечеру гроза. || Dry grass in the morning means a storm by evening.
Вьюнок закрыл цветки среди дня — к дождю. || Bindweed closing its flowers in the middle of the day means rain.
Жаркий июль — к морозному декабрю. || A hot July means a frosty December.
Петров день — красное лето, зелёный покос. Соловей замолкает. || Peter's Day: high summer and the green hay harvest. The nightingale falls silent.
Календулу рвите каждый день: чем чаще собираете, тем пышнее цветёт. || Pick calendula daily: the more often you pick, the more it flowers.
Стрижи с криком носятся у самой земли — к грозе. || Swifts screaming low over the ground mean a storm.
Земляника отошла — начинается малина. || The wild strawberries are over: the raspberries are starting.
Отломите стрелки чеснока — головки будут крупнее. || Snap off the garlic flower stalks: the bulbs will grow bigger.
Петух поёт не вовремя — к перемене погоды. || A cockerel crowing at odd hours means a change in the weather.
Лягушки вылезают на берег и громко квакают — к дождю. || Frogs crawling out on the bank and croaking loudly mean rain.
Время сушить травы на зиму: мяту, чабрец, душицу. || Time to dry herbs for winter: mint, thyme, oregano.
Гроза за грозой в июле — к урожайной осени. || Storm after storm in July means a rich autumn.
Кучевые облака к вечеру тают — к хорошей погоде. || Puffy clouds that melt away by evening mean fine weather.
Тля на кустах — значит, рядом муравьи: побрызгайте крапивным настоем. || Aphids on the bushes mean ants nearby: spray with nettle tea.
Молодой подсолнух поворачивается за солнцем, а взрослый смотрит на восход. || A young sunflower turns to follow the sun; a grown one settles facing the sunrise.
Варенье варят в ясный день, говорили бабушки, — тогда оно не засахарится. || Jam is made on a clear day, the grannies said, and then it won't crystallise.
Пауки попрятались — к ненастью. || Spiders hiding away mean foul weather.
Пчёлы злые — к грозе. || Cross bees mean a storm.
Сейте редис и салат второй раз — к осени успеют. || Sow radishes and lettuce a second time: they'll be ready by autumn.
Лук убирают, когда перо полегло. || Lift the onions once their tops have flopped over.
Жара без ветра — к вечерней грозе. || Windless heat means a storm by evening.
Много паутины на траве в конце июля — к ясному августу. || Plenty of cobwebs on the grass in late July promises a clear August.
Последний день июля: начинайте собирать семена ранних цветов — мака и васильков. || The last day of July: start collecting seed from early flowers — poppies and cornflowers.
""",
# August
"""
Август-серпень: время жатвы и заготовок. || August the sickle month: time for reaping and putting things by.
Ильин день: лето кончается, вода в реке холодеет — купаться всё прохладнее. || Ilya's Day: summer is ending and the river turns cool — swimming gets chillier.
После Ильина дня ночи длинные и тёмные, роса холодная. || After Ilya's Day the nights are long and dark, the dew cold.
Утренние туманы в августе — к грибам. || Morning fogs in August mean mushrooms.
Август — время звездопада: смотрите на небо в ясную ночь. || August is the month of falling stars: look up on a clear night.
Огурцы на засолку собирают утром, пока прохладные. || Pick pickling cucumbers in the morning while they're cool.
Пчёлы заделывают летки воском — к ранней осени. || Bees sealing their hive entrances with wax mean an early autumn.
Рябина покраснела — осень близко. || The rowan has reddened: autumn is near.
Ночи холодные — снимайте помидоры бурыми, в доме они дозреют. || Cold nights: pick the tomatoes while still brown, they'll ripen indoors.
Ласточки собираются стаями на проводах — к ранней осени. || Swallows gathering in flocks on the wires mean an early autumn.
Чеснок готов, когда пожелтели нижние листья. || Garlic is ready when its lower leaves turn yellow.
Обильная августовская роса — к погожим дням. || Heavy August dew means fair days ahead.
Подсолнухи склонили головы — семечки зреют. || Sunflowers bowing their heads mean the seeds are ripening.
Медовый Спас, Маковей: качают мёд, собирают мак, пекут маковые пироги. || The Honey Feast, Makovey: honey is extracted, poppies harvested and poppy pies baked.
После Маковея пчела взятка не носит. || After Makovey the bees stop gathering nectar.
Сухой август — к тёплой осени. || A dry August means a warm autumn.
Пора делить и пересаживать пионы и ирисы. || Time to divide and replant peonies and irises.
Утки сбиваются в стаи на пруду — к холодам. || Ducks flocking on the pond mean the cold is coming.
Яблочный Спас: пробуют первые яблоки. Пришёл Спас — держи рукавички про запас. || The Apple Feast: the first apples are tasted. “The Feast is here — keep your mittens near.”
После Яблочного Спаса бывают первые утренники — укрывайте огурцы на ночь. || After the Apple Feast come the first cold mornings: cover the cucumbers overnight.
Берёза желтеет с верхушки — к ранней весне, снизу — к поздней. || A birch yellowing from the top means an early spring next year; from the bottom, a late one.
Время копать раннюю картошку и просушить её на солнце. || Time to lift the early potatoes and dry them in the sun.
Сорока стрекочет у жилья — к ненастью. || A magpie chattering near the house means foul weather.
Собирайте семена укропа и кориандра, пока не осыпались. || Collect dill and coriander seed before it scatters.
Опята пошли на пнях — лету конец. || Honey fungus on the stumps: summer's over.
Сажайте клубничные усы: к весне укоренятся. || Plant strawberry runners: they'll root by spring.
Гром в конце августа — к долгой тёплой осени. || Thunder in late August means a long warm autumn.
Дожинки: последний сноп в старину наряжали и несли в дом — на счастье. || Dozhinki, the end of reaping: the last sheaf was dressed up and carried home for luck.
Ореховый Спас: поспевают лесные орехи, пекут хлеб из нового зерна. || The Nut Feast: hazelnuts ripen, and bread is baked from the new grain.
Ласточки улетели — через месяц жди заморозков. || The swallows have gone: frost will follow within a month.
Последний день лета: соберите семена с лучших растений — пусть станут вашими сортами. || The last day of summer: save seed from your best plants and let them become varieties of your own.
""",
# September
"""
Сентябрь-хмурень: холодно, да сытно. || September the frowner: chilly, but well fed.
Паутина летит по ветру — к ясной осени. || Gossamer floating on the wind means a fine autumn.
Гуси летят высоко — осень будет долгой. || Geese flying high mean a long autumn.
Тыквы снимают до заморозков, с хвостиком. || Pick pumpkins before the frost, with the stalk left on.
Рябины много — осень дождливая, мало — сухая. || Plenty of rowan berries mean a rainy autumn; few mean a dry one.
Осенние туманы — к теплу. || Autumn fogs mean warmer days.
Капусту рубят после первых заморозков: она становится слаще. || Cabbage is chopped after the first frosts: it's sweeter for them.
Шиповник собирают, пока ягоды твёрдые и красные. || Pick rosehips while they're firm and red.
Журавли летят низко — к тёплой зиме. || Cranes flying low mean a mild winter.
Дуб ещё зелёный, а берёза пожелтела — к суровой зиме. || Oak still green while the birch has yellowed means a harsh winter.
Грибная пора: после тёплого дождя ищите белые под дубами и берёзами. || Mushroom time: after a warm rain look for ceps under the oaks and birches.
Комары ещё летают в сентябре — к мягкой осени. || Mosquitoes still about in September mean a mild autumn.
Сушите яблоки на нитке: зимой из них самый душистый компот. || Dry apples on a string: in winter they make the most fragrant kompot.
Семён-летопроводец: провожают лето, начинается бабье лето. || Semyon Who Sees Summer Off: summer is waved goodbye and the Indian summer begins.
Бабье лето ясное — осень будет ненастной. || A clear Indian summer means a wet autumn after it.
Листья опадают медленно — к суровой зиме. || Leaves falling slowly mean a hard winter.
Морковь и свёклу копайте в сухой день — дольше пролежат. || Lift carrots and beets on a dry day: they keep longer.
Скворцы задерживаются — к сухой и тёплой осени. || Starlings lingering mean a dry, warm autumn.
Чеснок под зиму сажают за месяц до морозов. || Plant winter garlic a month before the hard frosts.
Белка делает большие запасы — к холодной зиме. || A squirrel laying in big stores means a cold winter.
Осенины, вторая встреча осени: благодарят урожай и пекут пироги из новой муки. || Oseniny, the second meeting of autumn: the harvest is thanked and pies baked from new flour.
Сажайте тюльпаны и нарциссы: к весне укоренятся. || Plant tulips and daffodils now: they'll root by spring.
Гром в сентябре — к долгой осени. || Thunder in September means a long autumn.
Федора — обдери хвосты: начинаются осенняя грязь и слякоть. || Fedora Mud-tails: the autumn mud and slush begin.
Вода в реке потемнела — рыба уходит на глубину. || The river water has darkened: the fish are moving to deeper water.
Перекопайте грядки на зиму, но комья не разбивайте — мороз сам разрыхлит. || Dig the beds over for winter but leave the clods whole: the frost will break them up.
Сдвиженье: птицы в отлёт, змеи в норы. С этого дня начинают квасить капусту. || Sdvizhenye: birds fly south and snakes go to ground. From today, cabbage fermenting begins.
Ветер с севера — ночью жди заморозка. || A north wind means frost tonight.
Сухая и тёплая осень — к поздней весне. || A dry, warm autumn means a late spring.
Конец сентября: укройте многолетники сухими листьями. || End of September: tuck the perennials in under dry leaves.
""",
# October
"""
Арина — журавлиный лёт: если журавли летят сегодня, на Покров будет мороз. || Arina of the Cranes: if the cranes fly south today, there'll be frost by Pokrov.
Октябрь-грязник: ни колеса, ни полоза. || October the muddy: no good for wheels, no good for sledge runners.
Мыши роют норы глубоко — к холодной зиме. || Mice digging deep burrows mean a cold winter.
Сушите семена в бумажных пакетах и подписывайте сорт и год. || Dry seeds in paper bags and label each with the variety and year.
Иней на траве по утрам — к ясному дню. || Hoarfrost on the grass in the morning means a clear day.
Лист падает лицом вверх — к урожаю на будущий год. || Leaves landing face up foretell a good harvest next year.
Снимайте последние кабачки и тыквы: ночные заморозки их не пощадят. || Pick the last courgettes and pumpkins: the night frosts won't spare them.
Гром в октябре — к малоснежной зиме. || Thunder in October means a winter with little snow.
Гуси улетают — скоро снег. || The geese are leaving: snow is near.
Пересадите петрушку и лук с грядки на подоконник — будет своя зелень всю зиму. || Bring parsley and onions in from the bed to the windowsill: you'll have your own greens all winter.
Яркие звёзды в октябре — к ясной погоде. || Bright October stars mean clear weather.
Кот не отходит от батареи — к холодам. || A cat glued to the radiator means cold weather is coming.
Если до Покрова лист с вишни не облетит — зима будет тёплой. || If the cherry hasn't dropped its leaves by Pokrov, the winter will be mild.
Покров — первое зазимье: до обеда осень, после обеда зима. || Pokrov, the first breath of winter: autumn till lunch, winter after.
Ветер на Покров с севера — к холодной зиме. || A north wind around Pokrov means a cold winter.
Рябину собирают после первых заморозков: она становится слаще. || Pick rowan after the first frosts: they make it sweeter.
Сгребите последние листья под кусты — тёплое одеяло на зиму. || Rake the last leaves under the bushes as a warm winter blanket.
Снег выпал на сырую землю — к хорошей весне. || Snow falling on wet ground means a good spring.
Вода в реке ещё тёплая — зима придёт поздно. || If the river water is still warm, winter will be late.
Синицы подлетают к домам — пора вешать кормушки. || Tits coming close to the houses: time to hang the feeders.
Овощи закладывают в погреб сухими, целыми, без ссадин. || Vegetables go into the cellar dry, whole and unbruised.
Месяц в кругу — к морозу. || A ring round the moon means frost.
Пора квасить капусту: соль, морковь и терпение — к Новому году будет готова. || Time to ferment cabbage: salt, carrots and patience, and it'll be ready by New Year.
Снегири прилетели — зима на носу. || The bullfinches have arrived: winter is just around the corner.
Ясная погода в конце октября — к холодному ноябрю. || Clear weather at the end of October means a cold November.
Морковь и укроп сеют под зиму: весной взойдут раньше всех. || Sow carrots and dill before winter: they'll be the first up in spring.
Параскева-льняница: в старину начинали мять и трепать лён. || Paraskeva of the Flax: folk began breaking and scutching the flax today.
Ворона прячет клюв под крыло — к морозу. || A crow tucking its beak under its wing means frost.
Зайцы побелели — скоро зима. || The hares have turned white: winter is near.
Ранний снег — к ранней весне. || Early snow means an early spring.
Последний день октября: укройте клубнику лапником. || The last day of October: tuck the strawberries in under spruce boughs.
""",
# November
"""
Ноябрь-грудень: земля смёрзлась комьями — ни колеса, ни саней. || November of the frozen clods: ruts too stiff for wheels, too bare for sledges.
Ноябрь — сумерки года: день короткий, вечера длинные, самое время для чая с вареньем. || November is the dusk of the year: short days, long evenings, just right for tea with jam.
Лёд на лужах держится весь день — к ранней зиме. || Ice on the puddles holding all day means an early winter.
Время сеять микрозелень: неделя — и салат на столе. || Time to sow microgreens: a week later, salad on the table.
Перистые облака на небе — к снегопаду. || Wispy clouds across the sky mean snow.
Огонь в печи гудит — к морозу. || A roaring fire in the stove means frost.
Воробьи прячутся под крышей — к морозу. || Sparrows hiding under the eaves mean frost.
Дмитриев день: холод и снег сегодня — к поздней и холодной весне. || Dmitry's Day: cold and snow today mean a late, chilly spring.
Окно плачет — к оттепели. || A window streaming with condensation means a thaw.
Повесьте кормушки: синицы отблагодарят весной, склевав гусениц. || Hang up the feeders: the tits will repay you in spring by eating the caterpillars.
Сороки прилетели к домам — к снегу. || Magpies coming close to the houses mean snow.
Синичкин день, Зиновий-синичник: встречают зимних птиц и вешают кормушки. || Tit Day, Zinovy of the Tits: winter birds are welcomed and feeders hung.
Ноябрьская тишина: ни ветра, ни птиц — жди снегопада. || A November hush, no wind, no birds: expect snow.
Кузьминки: Кузьма-Демьян куёт лёд на реках, осень провожают. || Kuzminki: Kuzma and Demyan forge the ice on the rivers, and autumn is seen off.
Лёд у берега прочный, а на середине тонкий — на рыбалку рано. || Ice firm by the bank but thin in the middle: too early for fishing.
Поставьте амариллис в тепло — к Новому году зацветёт. || Move the amaryllis into the warmth: it'll flower by New Year.
Солнце садится в тучу — к снегу. || The sun setting into cloud means snow.
Снег сухой и скрипит — мороз крепкий. || Dry snow creaking underfoot means a hard frost.
Переберите запасы в погребе: одно гнилое яблоко портит весь ящик. || Go through the cellar stores: one bad apple spoils the whole crate.
Снег выпал и пролежал с утра до вечера — зима установилась. || If snow falls and stays from morning to night, winter has settled in.
Михайлов день: Михайло-зимний мосты мостит. Иней сегодня — к большим снегам. || Mikhail's Day: Mikhail of Winter builds ice bridges. Hoarfrost today means heavy snows.
Ворона кричит на юг — к теплу, на север — к холоду. || A crow cawing southward means warmth; northward, cold.
Морозный день в конце ноября — к ясному декабрю. || A frosty day in late November means a clear December.
Гиацинт к празднику выгоняют так: луковицу сначала в холод, потом в тепло. || To force a hyacinth for the holidays, keep the bulb cold first, then bring it into the warmth.
Фёдор-студит: студит землю, а ветер сегодня — к студёной зиме. || Fyodor the Chiller: he cools the earth, and wind today means a bitter winter.
Снег валит крупными хлопьями — к оттепели. || Snow falling in big flakes means a thaw.
Метели в конце ноября — к метельному декабрю. || Blizzards at the end of November mean a stormy December.
Ясный закат зимой — к морозу. || A clear winter sunset means frost.
Дятел стучит зимой — к теплу. || A woodpecker drumming in winter means warmer days.
Последний день осени: соберите шишки для самовара и поделок, пока не засыпало. || The last day of autumn: gather cones for the samovar and crafts before the snow buries them.
""",
# December
"""
Декабрь-студень: год кончает, зиму начинает. || December the frosty: it ends the year and begins the winter.
Декабрь начался морозом — зима будет долгой. || If December begins with frost, the winter will be long.
Сырой ветер и туман — к оттепели. || A raw wind and fog mean a thaw.
Звёзды крупные — к морозу, мелкие — к теплу. || Big stars mean frost, small ones warmth.
Снег в начале декабря лёг ровно — весна будет дружной. || If early December snow lies evenly, spring will come all at once.
Зимой подкармливайте подоконник светом: лампа над зеленью — второе солнце. || In winter feed the windowsill with light: a lamp over the greens is a second sun.
Екатерина-санница: катаются на санях по первопутку. Ясно сегодня — к морозной зиме. || Yekaterina of the Sledges: sledge rides on the first snow road. A clear day means a frosty winter.
Трещат углы дома — мороз крепчает. || When the corners of the house crack, the frost is tightening.
Юрий-холодный, Егорий осенний: медведь ложится в берлогу. || Yury the Cold, Yegory of Autumn: the bear settles into its den.
Мелкий снег сыплет надолго. || Fine snow falls for a long time.
Пора распутывать гирлянды и вспоминать, кто куда спрятал ёлочные игрушки. || Time to untangle the fairy lights and remember who hid the tree decorations where.
Снегопад в безветрие — к оттепели. || Snowfall on a windless day means a thaw.
Андреев день: прислушайтесь к воде в проруби — тихая к ровной зиме, шумная к метелям. || Andrey's Day: listen to the water in an ice hole — quiet means a gentle winter, noisy means blizzards.
Синицы прячутся в дупла и скворечники — к морозу. || Tits sheltering in hollows and nest boxes mean frost.
Луна красноватая — к ветру. || A reddish moon means wind.
В декабре на подоконнике быстрее всех кресс-салат: десять дней — и готово. || In December cress beats everything on the windowsill: ten days and it's ready.
Варвара мостит мосты: реки сковывает льдом. || Varvara builds the bridges: the rivers lock under ice.
Савва: Варвара мостит, Савва гвозди острит, Никола прибивает — морозы крепнут. || Savva's Day: Varvara builds the ice bridges, Savva sharpens the nails, Nikola hammers them in — the frosts grow fierce.
Никола-зимний: иней на деревьях сегодня — к урожаю ржи. || Nikola of Winter: frost on the trees today means a good rye harvest.
Перед праздниками в доме пахнет мандаринами и хвоей, а на подоконнике зеленеет лук. || Before the holidays the house smells of tangerines and pine, and green onions sprout on the windowsill.
Самые короткие дни: солнце встаёт поздно и садится рано, будто спешит домой. || The shortest days: the sun rises late and sets early, as if hurrying home.
Анна-зимняя, самая длинная ночь: зима встаёт во весь рост. || Anna of Winter, the longest night: winter rises to its full height.
Морозная ночь без ветра — к ясному дню. || A frosty, windless night means a clear day.
Тёплый конец декабря — к холодной весне. || A warm end to December means a cold spring.
Спиридон-солнцеворот: солнце на лето, зима на мороз. День прибавился на воробьиный скок. || Spiridon of the Solstice: the sun turns to summer, the winter to frost. The day is a sparrow's hop longer.
Метель в последнюю неделю года — к снежному январю. || Blizzards in the last week of the year mean a snowy January.
Пора ставить ёлку: свежая и душистая простоит до старого Нового года, если поливать. || Time to put up the tree: a fresh, fragrant one will last till Old New Year if you water it.
Переберите семена и начертите план грядок — самое приятное зимнее занятие. || Sort your seeds and sketch next year's beds — the nicest winter job there is.
Мороз и солнце, день чудесный, как у поэта, — к ясной неделе. || Frost and sunshine, a glorious day, as the poet said: a clear week ahead.
Снег под Новый год — к урожаю. || Snow just before New Year means a good harvest.
Последний день года: зажгите огни на окне и поблагодарите старый год за урожай. || The last day of the year: light up the windows and thank the old year for its harvest.
""",
]

DAYS = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]


def main():
    rows = []
    for m, (block, n) in enumerate(zip(MONTHS, DAYS), 1):
        lines = [ln for ln in block.strip().split("\n") if ln.strip()]
        assert len(lines) == n, (m, len(lines), n)
        for d, ln in enumerate(lines, 1):
            ru, en = (p.strip() for p in ln.split("||"))
            rows.append([f"{m:02d}-{d:02d}", ru, en])
    ru = [r[1] for r in rows]
    assert len(set(ru)) == len(ru), "duplicate almanac lines"
    write("almanac.tsv", COLS, rows)


if __name__ == "__main__":
    main()
