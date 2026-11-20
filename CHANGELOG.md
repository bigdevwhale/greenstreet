# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [0.15.0] - 2026-11-20

### Added
- Weak GPS is said plainly on the map: with the phone's accuracy worse than 40 m, nothing is counted until the fix sharpens, so a jumping dot no longer walks you past neighbours or out of the yard.
- Settings: move home to where you stand (within 500 m it keeps the street and the neighbours), a courtyard to pick (as on the map, village house, panel-block yard, well courtyard) and a pause that stops days and nights from piling up while you're away.
- Your own pond: where there is no water near, or it is behind a fence, dig a pond where you stand (up to two, 300 m apart). It fishes like a river.
- Neighbours: "Another place" on a neighbour you can't reach moves them somewhere new; a "today" panel shows who was visited and who wasn't; a short note of your own in each neighbour's notebook.
- Seeds in the bag are grouped: vegetables, greens and herbs, berries and fruit, flowers, grain, dye plants.

### Changed
- Tastes list what left a neighbour cold too ("doesn't mind"), and the gift picker marks everything already given.
- Neighbour cards show the name without "aunt", "doctor" and the like.
- The walk button asks for the permissions it needs and says why a walk can't start.
- Transfer to another phone is easier to find.

<!-- tg
При слабом GPS игра так и пишет и ничего не засчитывает, пока сигнал не станет точнее
В настройках: перенести дом туда, где стоите, выбрать двор и поставить игру на паузу
Если рядом нет воды, можно выкопать свой пруд; соседа, к которому не подойти, можно переселить в другое место
Семена в сумке по группам, у соседей – своя заметка, «всё равно» во вкусах и список «сегодня»
-->

## [0.14.10] - 2026-11-18

### Fixed
- "Where is home?" could wait for the phone forever. The game listened only to the providers that were on when it started: location switched on later, or allowed only as approximate, left it deaf. It now listens to the fused provider too, picks up providers switched on later and checks the permission every two seconds.
- With approximate location only, the game says so and asks for precise location (or points to the app's settings once it was refused): home placed by an approximate fix would land a kilometre off.
- Location switched off on the phone: the page says so and opens the location settings. After 30 seconds without a fix it suggests going to a window or outdoors.
- A position the phone remembered from long ago is no longer taken for where you are.

<!-- tg
Исправили зависание на экране «Где ваш дом?»: игра теперь ловит позицию и по Wi-Fi, и после того как геолокацию включили в шторке
Если разрешено только приблизительное местоположение, игра попросит точное: иначе дом встал бы за километр
-->

## [0.14.9] - 2026-11-17

### Added
- Fishing: where the map has next to no water within 500 m of home, the game digs a lake and two ponds in the parks or on open ground, each with a path to its bank. The lake is fished like a river.
- Neighbours: a place you can't get to (a locked yard, behind a fence) can be moved up to 150 m from where the map put it: "Move it" on the neighbour's card, then a tap on the map inside the ring.

### Changed
- Fish bite all year round: no more months for fish. Time of day and weather still bring the rarer ones.
- A strolling neighbour stops and waits once you are within 80 m (was 60) and walks on only when you are 120 m away again.

### Fixed
- A neighbour you were walking up to with the screen off had counted all that time as walking on and was gone. The strolls are now worked out at every GPS fix too.

<!-- tg
Рыба теперь клюёт круглый год, а если рядом с домом нет воды, игра выкопает пруды в ближайших парках
Сосед на прогулке ждёт, когда вы в 80 м, и не уходит, пока вы не отойдёте на 120 м
Место соседа, к которому не подойти, можно передвинуть до 150 м: кнопка «Передвинуть» в его карточке
-->

## [0.14.8] - 2026-11-17

### Added
- Yard: all the verge beds in one list: what grows, when it ripens, whether it has had water today and how far it is. A tap opens the bed's card, "Show" finds it on the map.
- Street: the panel under the map folds away with "Hide", and the map takes the whole screen; "Details" brings it back.
- Seed packets are named under the picture: the crop and the ripening date on the street, the crop and the variety in the yard.

### Fixed
- Tapping a bed or a neighbour on the map after zooming often missed: the tap was read at the old scale. The tap area is now as wide as a finger at any zoom.

<!-- tg
Во дворе появился список всех грядок у дороги: что растёт, полита ли и куда идти. Кнопка «Показать» находит грядку на карте
Панель под картой можно скрыть кнопкой «Скрыть», и карта займёт весь экран
Под пакетиками семян подписано, что это за культура, больше не нужно угадывать рожь по картинке
Нажатие на грядку на карте теперь срабатывает при любом масштабе
-->

## [0.14.7] - 2026-11-16

### Fixed
- A sudden power-off (a flat battery, a frozen phone) could leave the save empty, and the game then started over. The save is now forced to the disk before it replaces the old one, the previous save is kept beside it as a spare, and a damaged save falls back to that spare instead of a new game. The district map is written the same way.

<!-- tg
Поправили сохранение: если телефон внезапно выключится (сел аккумулятор, завис), игра больше не может начаться заново.
Прогресс записывается на диск надёжно, а рядом хранится прошлое сохранение на всякий случай.
-->

## [0.14.6] - 2026-11-15

### Added
- Weather choice in Settings and on the welcome pages: the real weather at home (as before), the real weather of another town found by name, or weather made up by the game for the season, with no internet. Each option says what it means. The choice is kept in the save and moves to another phone with it.

<!-- tg
Погоду теперь можно выбрать: настоящая у дома, настоящая в другом городе или придуманная игрой
Город ищется по названию прямо в настройках
Придуманная погода работает без интернета
-->

## [0.14.5] - 2026-11-15

### Changed
- The player on the map is as tall as the neighbours and drawn by hand: a red knitted hat with a pompom, a face you can see, a yellow scarf, the green coat and a canvas bag. The ring at the feet and the arrow overhead stay.

### Fixed
- Bag: the number of seeds sits on a little plaque in the corner instead of running into the packet's frame.
- Neighbours: whether someone is in and their hearts no longer crowd one line, so the heart and its number stay in place; the cards of a row are one height, and a long trade ends in an ellipsis.
- The "Colouring the district…" card no longer cuts off its last line over a small map.

<!-- tg
Игрок на карте теперь одного роста с соседями и его легко узнать: красная шапка, шарф, сумка
В сумке числа семян больше не наезжают на пакетики
Карточки соседей ровные: статус и сердца не съезжают
-->

## [0.14.4] - 2026-11-15

### Added
- Moving to another phone: Settings can save the game to a file, and the new phone loads it in Settings or on the first screens. The district map goes in the file too, so home, the beds and every neighbour stay where they were.

### Changed
- The place each neighbour took is now kept in the save, so they stay put even when the map is downloaded again and OpenStreetMap has changed meanwhile. Only newcomers, and a move of home, place people afresh.

### Fixed
- In the painted old courtyard the cat no longer floats in the air over the arch, and on the bench he lies on the seat instead of hiding behind it.

<!-- tg
Прогресс можно перенести на другой телефон: Настройки → Сохранить прогресс в файл
Соседи остаются на своих местах, даже если карту скачать заново
Кот больше не висит над аркой и не прячется за скамейкой
-->

## [0.14.3] - 2026-11-15

### Added
- Ready for Google Play: each release now carries an app bundle (AAB) next to the APK, and the repository has the store listing, graphics and a privacy policy (PRIVACY.md).

<!-- tg
Готовимся к выходу в Google Play
-->

## [0.14.2] - 2026-11-15

### Changed
- The rest of the yard is painted: the hens (three breeds, walking and pecking), the hen house and its nest with today's eggs, the herb boxes (bare in the cold months), the bench, the hives, the shared garden and its cold frame, the washing on the line, Asya playing in the yard, Gosha with his cello on the bench in the evening (and Lilya beside him, later), the snowman, the New Year tree, the spring tools and everything bought at Mikhalych's.
- All 139 ribbons are painted rosettes, one for each kind; a ribbon for a particular crop, fish, find or dish shows that thing in the middle, and a friendship ribbon shows the neighbour's face.
- The Yard screen is split into Yard, Kitchen and Household. The pots show what grows in them with its picture; upgrades are tiles, the affordable ones first.
- The Neighbours screen is split into People, Requests and Letters, with counts. People are a grid of faces with whether they're in, out or asleep; requests show each thing's picture, the ready ones first; new letters come first, the read ones fold away.

<!-- tg
Двор дорисован: куры, курятник, ящики с зеленью, скамейка, бельё на верёвке и соседи
Ленты стали нарисованными розетками
Двор и Соседи разделены на вкладки: кухня и хозяйство отдельно, просьбы и письма отдельно
-->

## [0.14.1] - 2026-11-15

### Fixed
- After the map finished loading, the neighbours' names over it and the zoom buttons stayed hidden until something else redrew the screen. They now appear as soon as the map is ready.

<!-- tg
Имена соседей и кнопки масштаба появляются на карте сразу после её загрузки
-->

## [0.14.0] - 2026-11-14

### Added
- Painted pictures, at twice the old resolution: every neighbour and Pryanik, the fronts of their places and the park path, the three kinds of yard, all 261 items in the bag, the beds on the map at every stage (each ripe bed shows its own harvest), the prologue, the onboarding cover and the map's loading picture. Sky, weather, night, lit windows, the season and the life of the yard are still drawn live over them.
- A Support button in Settings.

### Changed
- The people on the map are the same figures as in their places. The player is redrawn and easier to find: bigger, with a ring at the feet and a marker overhead, never shaded by the night.
- A tidier interface with the same pixel font: cream cards on a soft shadow, a painted icon by each section's title, round faces, buttons that press in, painted tabs.
- The street panel is shorter: tap a seed packet to sow (with the day it would be ripe), small chips for what the spot offers, one wide button for the walk.
- The map's loading screen shows a map being drawn, with a bar of how far along it is; labels and zoom buttons wait until the map is ready.
- A new launcher icon: a bed with a calendula on the pavement, with a monochrome version for themed icons.

<!-- tg
Новая графика: соседи, их дома, двор, все предметы и грядки нарисованы заново
Обновлённый интерфейс: аккуратные карточки, иконки разделов, круглые портреты
Себя на карте теперь легко найти, а сажать проще: нажмите на пакетик семян
Новая иконка и кнопка «Поддержать» в настройках
-->

## [0.13.2] - 2026-11-14

### Fixed
- Walking past a bed often didn't water it: only a single GPS fix within reach counted, so a bed passed between two fixes, or a fix that drifted off the pavement, was missed. The whole way between two fixes counts now, and the reach grows by the GPS's own error (up to 15 m).
- Snow watered the beds, but the game only ever spoke of rain. The bed card and the morning now say when the snow did it.

### Changed
- Kitchen: a tap on a dish opens its card under the shelf and scrolls to it, with the dish, what it makes and its description first and the Cook button (naming the dish) last; a note says when it's cooked.

<!-- tg
Грядки поливаются на ходу надёжнее: считается весь путь мимо грядки и погрешность GPS
Снег поливает грядки, и игра теперь об этом говорит
На кухне сначала видно, что за блюдо, а уже потом кнопка «Приготовить»
-->

## [0.13.1] - 2026-11-13

### Fixed
- Rain still didn't water the beds if it fell while the app was closed: the game only knew the forecast, which covers the hours ahead. What has already fallen now comes from Open-Meteo (the last week and today so far), and the missed nights wait for it before judging the beds.
- On a rainy evening the bed card could still warn that the bed goes wild: the screen took today's weather from the evening forecast instead of the day's record. Rain at night counts too now.

### Changed
- Weather for past days comes from Open-Meteo.com (CC BY 4.0); only the rounded coordinates are sent, as before.

<!-- tg
Дождь засчитывается как полив, даже если он прошёл, пока приложение было закрыто
Грядка больше не грозит одичать в дождливый день, ночной дождь тоже считается
-->

## [0.13.0] - 2026-11-12

### Fixed
- Rain didn't count as watering when the app was opened again after it stopped: the forecast covers only the hours ahead, so an evening refresh wiped the morning's rain from the day. A day keeps its rain once seen, and a day the app isn't opened uses the previous day's forecast for it instead of a made-up sky.
- After a strike or a miss the float stayed in the water, so it wasn't clear you had to cast again. The line now comes out and the button says "Cast again".
- At the market it was easy to buy far more than meant (17 bottles of milk). The stall shows your money, says one tap buys one, shows how many of each you already have, and the row you just bought from says how many you bought.

### Changed
- Frost no longer hurts the beds, and the frost fleece is gone from the upgrades: whoever bought it gets the 220 groats back.
- Beds slow down by the calendar only: half pace outdoors from December to February, a little slower in March and November. A cold day at other times changes nothing.

<!-- tg
Дождь теперь засчитывается как полив за весь день, даже если к вечеру он кончился
Заморозки больше не вредят грядкам, спанбонд убран — кто купил, получит 220 грошей обратно
На рыбалке после подсечки поплавок вынимается из воды, кнопка «Закинуть снова»
На рынке видно, сколько у вас уже есть и сколько вы только что купили
-->

## [0.12.0] - 2026-11-12

### Added
- A neighbour's lost thing picked up on the walk (an umbrella, keys, a scarf) used to be kept out of sight: it showed only as a "Give back" button on that neighbour's page. The Goods tab of the bag now lists every such thing with whom it belongs to.

<!-- tg
Чужие вещи, найденные по дороге, теперь видны в сумке на вкладке «Добро» — с тем, кому их отдать
-->

## [0.11.2] - 2026-11-10

### Fixed
- Seeds wouldn't sow by the grass you were standing next to: the game looked for a verge only ~5 m around the GPS dot and turned down any lawn with a wall within 5 m. It now looks ~15 m out, takes the narrow strip between a pavement and a house, and where the map has no grass tagged, ground by a pavement or path will do.
- Rowan and sea buckthorn hardly ever turned up without frost, even when a neighbour was asking for them. A find that a neighbour or the board is waiting on now turns up whatever the weather, and three times as often.

<!-- tg
Семена теперь сажаются у газона, рядом с которым вы стоите, — даже если на карте он не отмечен
Рябина, облепиха и другие находки, которые просят соседи, попадаются в любую погоду и чаще
-->

## [0.11.1] - 2026-11-08

### Fixed
- Aunt Klava's "Water on the way" couldn't be done on the day the first bed was sown: sowing counts as watering, and a bed watered today didn't count when walked past. Walking by a bed sown today now counts (once a day, as for any bed), and the bed still grows on its first night.

<!-- tg
Задание тёти Клавы «Полить на ходу» теперь выполняется в день посадки: посадили грядку, прошли мимо — готово
-->

## [0.11.0] - 2026-11-08

### Added
- An errand for today, once the opening tasks are done: one small thing from a neighbour you know, doable on one walk (look in on someone, bring a present, pass your beds, pick a ripe bed, help with a request your bag can fill, walk a kilometre or two, find two things). It pays a little of their liking and a token groat.
- "What the neighbours need" at the top of the Neighbours tab: every open request, what is still missing (have/need) and where to get it — in the bag, growing and ripe in N days, seeds you can sow, who gives the seeds, the market, a find by the water, the hen house, or a dish and what goes into it. Requests ready to take come first.

### Changed
- The garden grows four times faster than a real one (a radish is ready in 7 days instead of 25, the median crop in about three weeks instead of three months), so the first weeks have harvests in them. Plant food now gives a day of growth instead of three, the same share as before. Beds of older games are rescaled once, keeping how far along they were.
- The groats last the first year: the fair pays half the listed price (there is four times as much to sell), and past the first 6 of a kind on a Saturday Aunt Zina pays half again; the later upgrades cost more (the compost box 400 and 1200, your own stall 900, all of them 4540 instead of 1990). A steady player fills the household in about eight months instead of three.
- Aunt Klava's basket has milk for cottage cheese, so the kitchen task can be done at home before the market is found (most players waited on it for weeks).
- Aunt Klava's starting packets are the two quickest verge crops; every neighbour now has a quick crop among their seeds and gives the quickest one first.
- The sow buttons put the quickest crops first and say the day each would be ripe ("by 18 May").
- Windowsill shoots always leave a seed for the next pot, so the sill doesn't empty.
- Seeds can be sown in any month: the months on a packet are the best time, not a rule. Outdoors in the cold (December to February, or below 2 °C) beds grow at half pace instead of not at all. In winter the market, found packets and surprise packets offer the first varieties of spring.
- A neighbour out for a walk stops and waits while you are within 60 m, then walks on from where they stood; you can talk from 40 m, as at their place.

### Fixed
- Feathers, honey and wax could not be had anywhere, so 52 requests, notices and recipes (candles, sbiten, medovik) waited forever, Tanya's very first request among them. The hens now leave a feather with every fourth egg, and a beekeeper at the market sells honey and wax until the yard keeps bees.
- Neighbours no longer settle or stroll on ground closed to the public (private driveways, military land, places deep inside a block): a fenced-off place loses to an open one nearby, and if it is still chosen, its keeper waits on the nearest pavement (#2).
- The bed card says plainly when the bed will be ripe, whether it has been watered today (by you, by the rain, or not yet, and how to water it), and warns before it goes wild or gets frost; growth and waterings show as bars. Lists say "ripe in ~5 days · watered today" (#3).

<!-- tg
Огород растёт вчетверо быстрее, а сажать можно в любой месяц: первый урожай — уже на первой неделе
Каждый день — маленькое поручение от соседа, а во вкладке «Соседи» видно, что кому нужно и где это взять
Соседи больше не стоят за заборами и не убегают на прогулке, а карточка грядки объясняет, когда она созреет и полита ли
Исправлены просьбы, которые нельзя было выполнить: перья дают куры, мёд и воск продаёт пасечник на рынке
-->

## [0.10.0] - 2026-11-03

### Added
- Neighbours keep hours and days off: at work in their place, out for a walk along the district's paths otherwise, asleep at night; closed places get a shutter.
- A first meeting with every neighbour: who they are, what they are good for and what presents give.
- A notebook per neighbour: five facts that open with friendship, each with where you learned it.
- The neighbour's card: portrait, where they are now, ten hearts with progress, tabs for the person, the notebook and their tastes.
- Mikhalych's hardware shop: decor for the yard (lights, birdhouse, feeder, tyre bed, gnome, barrel), feed, a hoe and surprise seed packets.
- Neighbours on the map look different: height, build, skirts, hats, beards, glasses, things of their trade.

### Changed
- Beds stand at least 25 m apart and are drawn with a frame and a flag, so they no longer merge.
- The yard: new hens of three breeds that wander and peck, a nest with the eggs, a ramp to the coop, labels on what is where (tap to hide).

<!-- tg
Соседи теперь живут по своему расписанию: работают, гуляют по району, ночью спят
При первой встрече сосед рассказывает о себе, а в блокноте постепенно открывается его история
У Михалыча открылись хозтовары: вещи для двора и для огорода
Грядки стоят не ближе 25 метров друг к другу, во дворе новые куры и подписи
-->

## [0.9.0] - 2026-11-02

First public beta.

### Added
- Your real district as the farm: OpenFreeMap vector tiles (OpenStreetMap data) painted into bright pixel art, with zoom, pan and labels.
- Beds on lawns by pavements and park paths, tended by walking past (GPS, also in the pocket during a walk); rain, going wild, frost.
- 128 varieties, 58 goods, 95 recipes; the yard with hens, windowsill pots, kitchen, cellar and the notice board.
- 23 neighbours bound to real places (bakery, pharmacy, library, market…) with 735 requests and 474 letters over three years; hearts, presents, seeds.
- Finds on the way every few hundred metres: forage by place, month and weather, seed packets, groats, neighbours' lost things, small moments.
- The household: eight upgrades bought with groats; the Saturday fair with a crop in demand each week.
- Opening tasks that teach every mechanic; a prologue; onboarding with the home picked on the map.
- Festivals, a folk calendar for every day, the second year (balcony, hives) and the third (the shared garden).
- The yard drawn after the home: a city courtyard, a block of flats or a house.
- Settings: language (system, Russian, English), the StillCraft community links, credits.

<!-- tg
Новая игра, пока в бета-версии: уютная ферма на настоящей карте вашего района
Грядки разбивают на газонах у привычных дорог, и они поливаются, когда вы проходите мимо
По пути что-нибудь находится, а у соседей из пекарни, аптеки и с рынка — истории на три года
-->
