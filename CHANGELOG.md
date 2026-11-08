# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [Unreleased]

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
Каждый день — маленькое поручение от соседа, а во вкладке «Соседи» видно, что кому нужно и где это взять
Перо, мёд и воск теперь можно добыть: перья дают куры, мёд и воск продаёт пасечник на рынке — просьбы со свечами и сбитнем больше не застревают
Соседи больше не появляются на закрытых территориях: если место за забором, сосед ждёт на ближайшем тротуаре
В карточке грядки теперь понятно, когда она созреет и полита ли сегодня, а если нет — как её полить
Сажать можно в любой месяц: зимой на улице растёт вдвое медленнее, рынок продаёт весенние семена
Гуляющий сосед останавливается и ждёт, когда вы подходите, — больше не нужно за ним гоняться
Огород растёт вчетверо быстрее: редис за 7 дней, первый урожай с грядки — на первой неделе
На ярмарке тётя Зина берёт по 6 штук каждого по полной цене, улучшения хозяйства подорожали — копить интереснее
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
