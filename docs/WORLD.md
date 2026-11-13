# Green Street — the world bible

«Зелёная улица» / "Green Street" is a cozy farming life game played on the real map of the player's own
neighbourhood. The district is drawn as warm pixel art from OpenStreetMap (the same art language as
Lipovka: ramps, ordered dithering, light from the upper left, dark outlines). The player does not own a
farm. The farm is their everyday life laid over the map: little garden beds on the lawns along the
streets they already walk, a courtyard at home, and the neighbourhood's real places, each kept by a
neighbour.

Walking past a bed tends it. Rain waters everything. Beds left alone for three days go wild but never
die. The harvest goes to the courtyard kitchen, to gifts, to neighbours' requests and to the Saturday
fair. Over three years the player becomes the person the whole district knows: the one who plants
calendula by the tram stop.

## Tone

Warm, quiet, lived-in, like Lipovka. Small concrete details: the smell of the bakery at six in the
morning, a chipped enamel mug, the tram bell on the turn, the first ice on the courtyard puddle. Gentle
humour. Bittersweet sometimes, never tragic: nobody dies on the page, no illness drama, no war, no
politics, no religion, no money troubles beyond "the bakery's oven is old". People grumble, are shy,
make mistakes and make up. Time is slow and seasonal.

The city is any city. Text never names a real city, street, brand or chain. Places are types: "the
bakery", "the pharmacy by the station", "the river", "the park". The climate assumed is temperate with
a real winter (snow, frozen ponds). On the southern hemisphere the calendar shifts by six months.

Everyone speaks to the player with polite «вы» (plural verbs: «вы принесли», «вы промокли») so the
text works for any gender and age. In English, "you". The player's name is never needed. Neighbours
use «вы» even when close; warmth comes from what they say, not from «ты».

## The loop

1. **Seeds** come from places: each neighbour gives the seeds of their place once a day, the market sells
   common ones, harvests sometimes return a handful, festivals and requests give rare varieties.
2. **Sowing** happens only on lawns within a few metres of a pavement or a park path ("the verge"). A bed
   is pinned to a real spot. Beds must be at least 25 m apart. Any month will do: each variety has its best
   months, and outdoors in winter (December to February) a bed grows at half pace, in March and November
   a little slower. Only the calendar decides: the weather doesn't.
3. **Tending**: passing within reach (10 m, upgraded to 20 and 30) counts as watering and weeding. Each
   crop needs a number of days and a number of tended days. Rain counts as tending. Frost does
   nothing to the beds.
4. **Harvest** when the sparkle shows. Wild beds give half.
5. **Use**: cook and preserve in the courtyard kitchen, give to neighbours, fulfil requests, sell at the
   Saturday fair, fill the season album.

## Places and neighbours

The app reads OSM around home (about 1.5 km) and binds each neighbour to the nearest place of their kind.
If a kind has no place nearby, the neighbour takes the nearest unclaimed shop or amenity instead, so the
whole cast appears everywhere. A neighbour opens when the player first visits their place.

| id | neighbour | place kind (OSM) | seeds | in short |
|----|-----------|------------------|-------|----------|
| prokhor | Прохор, пекарь / Prokhor, the baker | shop=bakery | rye, wheat, poppy | 60, big beard, father baked too; gruff, generous, gets up at four |
| agafya | Агафья Тихоновна, аптекарь-травница / Agafya, the pharmacist | amenity=pharmacy | chamomile, calendula, mint, valerian | 70, grandmother was a village herbalist; asks how you sleep before anything else |
| mila | Мила, кофейня / Mila, the café | amenity=cafe | pumpkin, basil, lavender | 34, grew up by a southern sea, homesick in a funny way; names drinks after neighbours |
| vera | Вера Павловна, учительница / Vera Pavlovna, the teacher | amenity=school, kindergarten | sunflower, flax, cornflower | 52, teaches year six; her class follows the player's beds as a nature project |
| kuzmich | Кузьмич, смотритель станции / Kuzmich, the station keeper | railway=station, subway_entrance, bus_station | onion, garlic, potato | 63, forty years at the station, close to retirement, wants a garden of his own |
| sych | Сыч, библиотекарь / Sych ("the Owl"), the librarian | amenity=library, shop=books | flax, nasturtium, sweet pea | 58, night person, keeps index cards on everything, including the player |
| zina | тётя Зина, рынок / Aunt Zina, the market | amenity=marketplace, shop=greengrocer | cucumber, tomato, dill, cabbage | 66, loud, warm, haggles for sport, knows every seed by its grandmother |
| lilya | Лиля, цветочница / Lilya, the florist | shop=florist, garden_centre | tulip, daffodil, sweet pea, aster | 27, shy, speaks to plants more easily than to people |
| mikhalych | Михалыч, хозтовары / Mikhalych, the hardware shop | shop=hardware, doityourself | — (gives tools, nails, twine) | 70, can fix anything, upgrades the watering can, basket and rod |
| tanya | Таня, почта / Tanya, the post office | amenity=post_office | — (letters from afar, parcels of seeds) | 41, brisk, kind; brings post from the player's far relatives and pen friends |
| zhanna | Жанна, парикмахерская / Zhanna, the hairdresser | shop=hairdresser, beauty | — (gossip, flowers for the window) | 45, knows all the district's news first; romantic |
| gena | тренер Гена, стадион / Coach Gena, the sports ground | leisure=sports_centre, stadium, pitch | beans, peas | 38, enthusiastic, counts everyone's steps, runs a kids' team |
| efim | Ефим, смотритель парка / Efim, the park keeper | leisure=park | rowan, lime saplings, wildflowers | 67, plants trees, knows every bird, slow speech |
| palych | Палыч, рыбак / Palych, the angler | water edge: river, canal, pond (natural=water, waterway) | — (bait, tackle) | 72, always on the same spot of the embankment; teaches fishing and patience |
| slava | Слава, автомастерская / Slava, the garage | shop=car_repair, amenity=fuel | — (old tyres as planters, crates) | 30, cheerful, oily, secretly paints |
| ada | Ада Львовна, театр или музей / Ada Lvovna, the theatre or museum | amenity=theatre, tourism=museum, arts_centre | — (bulbs, old posters) | 79, retired actress, grand gestures, sends notes on stationery |
| olga | доктор Ольга, ветклиника / Dr Olga, the vet | amenity=veterinary, shop=pet | catnip, oats | 39, tired and funny, adopted three-legged dog Tripod |
| zoya | Зоя, ателье / Zoya, the tailor | shop=tailor, laundry, dry_cleaning | woad, madder, flax | 55, sews, dyes cloth with plants, needs flax and dye crops |

### The courtyard (always there)

The player lives in an old five-storey house with a courtyard-well (двор-колодец): yellow walls, an arch
to the street, washing lines, a bench, a hen house that the residents grumbled about and now love.

| id | resident | in short |
|----|----------|----------|
| semyonych | Семёныч, дворник / Semyonych, the caretaker | 64, broom, transistor radio, knows the house's history, guards the hens |
| nina | Нина Петровна, первый этаж / Nina Petrovna, ground floor | 81, lived here since 1968, window full of geraniums, feeds the cat, tells stories |
| asya | Ася, пятый этаж / Asya, fifth floor | 9, curious, collects seeds in matchboxes, wants to be a botanist |
| gosha | Гоша, музыкант / Gosha, the musician | 31, plays the cello at night, shy, in love with Lilya the florist |
| pryanik | кот Пряник / Pryanik the cat | ginger, belongs to the whole house, sleeps on the warmest thing |

## Three years

The game is built to last at least three years of real time. Content is tied to months and to the year
of play (`y1`, `y2`, `y3`), so the second spring is not the first.

- **Year 1 — «Первая грядка» / "The first bed".** Lawns along the routes, the courtyard hen house, the
  kitchen with simple recipes, neighbours opening one by one. Up to 6 beds at once, 9 after the first
  autumn fair. Winter: windowsill crops at home, ice fishing, winter foraging (rowan, pine cones,
  birch bark), the courtyard New Year.
- **Year 2 — «Балкон и крыша» / "Balcony and roof".** A glazed balcony greenhouse (peppers, aubergines,
  cherry tomatoes, physalis, melons for fun, early seedlings for spring), beehives on the roof with
  Semyonych (honey, wax, candles), the cellar for ferments and fruit wines (non-alcoholic: kvass,
  sbiten, mors, kompot). Up to 12 beds. Neighbours' stories turn: Kuzmich retires, Gosha and Lilya,
  Asya's school project wins.
- **Year 3 — «Общий сад» / "The shared garden".** The courtyard and an empty lot become a shared garden
  with neighbours (raised beds, a greenhouse of old windows, perennials, berry bushes, a seed library
  where the player's varieties live on). Up to 16 beds. The district festival in the player's honour at
  the end of the third summer. After that the game keeps going with yearly cycles and rare varieties.

Year changes on the anniversary of the first bed.

## Calendar and festivals

Real dates, local weather (MET Norway forecast, Open-Meteo for what already fell). Festivals move through the year; text differs by year.
New Year, Old New Year, Maslenitsa (the moveable pancake week), the first thaw, the start of sowing,
the spring courtyard clean-up (субботник), City Day (a Saturday in late May), white nights or
summer solstice, Ivan Kupala night (7 July), Honey Spas (14 Aug), Apple Spas (19 Aug), Knowledge Day
(1 Sep), the Harvest fair (last Saturday of September), Pokrov (14 Oct, first snow folklore — as folk
weather, not religion), Day of the Tit (12 Nov, birds' feeders), the first ice, the longest night.

A folk weather almanac gives one line for every day of the year (народные приметы, careful, not
superstitious advice, just folklore about weather and gardening).

## The economy

- Money: «гроши» / "groats". Prices are small numbers (1–80).
- Crops have varieties (сорта). Common varieties come from neighbours and the market, rare ones from
  requests, festivals, letters and the fair. The season album records first harvests of each variety.
- Goods: fish (by water type, season, hour, weather; ice fishing in winter), park finds (mushrooms,
  berries, nuts, cones, leaves, feathers), courtyard produce (eggs; honey and wax from year 2),
  kitchen products (preserves, pastries, teas, drinks, soaps and salves, cloth and dyes).
- Requests come one at a time per neighbour, roughly every 2–5 weeks, in a chain that tells a story.
- The district board (доска объявлений) posts small anonymous requests every few days.
- Ribbons (ленты) are achievements: firsts, collections, seasons, kindness, walking.

## Who speaks how

- Neighbours: «вы», each with a voice (Prokhor short and dry, Agafya old-fashioned and caring, Mila
  quick and playful, Vera teacherly, Kuzmich slow with railway words, Sych bookish and precise, Zina
  loud with sayings, Lilya timid with ellipses, Mikhalych practical, Tanya brisk, Zhanna gossipy,
  Gena exclamation marks, Efim slow and about birds, Palych few words, Slava cheerful slang-free, Ada
  theatrical, Olga dry humour, Zoya precise about colour).
- Asya says «вы» too, as children do to grown-ups.
- Pryanik never speaks; his beats are written as small scenes in third person.
