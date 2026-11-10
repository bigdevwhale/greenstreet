package app.greenstreet.domain

import app.greenstreet.data.Content
import app.greenstreet.data.Festival
import app.greenstreet.data.Line
import app.greenstreet.data.Need
import app.greenstreet.data.Note
import app.greenstreet.data.Place
import app.greenstreet.data.Request
import app.greenstreet.data.Reward
import app.greenstreet.data.Tx
import app.greenstreet.data.Variety
import app.greenstreet.data.When
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.max
import kotlin.math.min

/** The weather as the game needs it: what fell today and what tomorrow brings. */
data class Sky(
    val rain: Boolean = false,
    val snow: Boolean = false,
    val frost: Boolean = false,
    val fog: Boolean = false,
    val sun: Boolean = false,
    val heat: Boolean = false,
    /** Mean temperature, °C. */
    val temp: Double = 12.0,
) {
    val tags: Set<String>
        get() = buildSet {
            if (rain) add("rain"); if (snow) add("snow"); if (frost) add("frost"); if (fog) add("fog"); if (sun) add("sun"); if (heat) add("heat")
        }
}

/** The moment the game is asked about. */
data class Now(
    val date: LocalDate,
    /** 0–23 local hour. */
    val hour: Int = 12,
    val sky: Sky = Sky(),
    val tomorrow: Sky = Sky(),
    /** Southern hemisphere: the garden calendar shifts by six months. */
    val south: Boolean = false,
) {
    val day: Long get() = date.toEpochDay()
    /** The garden month: the real month, shifted on the southern hemisphere. */
    val month: Int get() = if (south) (date.monthValue + 5) % 12 + 1 else date.monthValue
    val season: String
        get() = when (month) {
            12, 1, 2 -> "winter"; 3, 4, 5 -> "spring"; 6, 7, 8 -> "summer"; else -> "autumn"
        }
    val part: String
        get() = when (hour) {
            in 5..10 -> "morning"; in 11..16 -> "day"; in 17..21 -> "evening"; else -> "night"
        }
    val weekend: Boolean get() = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    val saturday: Boolean get() = date.dayOfWeek == DayOfWeek.SATURDAY
}

/** What happened overnight, for the morning card. */
data class Morning(
    val grew: Int = 0,
    val ripe: Int = 0,
    val wild: Int = 0,
    val frost: Int = 0,
    val rained: Boolean = false,
    val eggs: Int = 0,
    val mail: List<String> = emptyList(),
    val requests: List<String> = emptyList(),
    val festival: Festival? = null,
    val year: Int = 1,
    val newYear: Boolean = false,
)

/** What a harvest gave. */
data class Harvest(val variety: String, val n: Int, val seedBack: Boolean, val first: Boolean)

/**
 * The rules. Pure: every function takes the [GameState] and changes it; nothing here touches Android, so the
 * whole game can be played in a unit test.
 */
class Game(val c: Content) {

    // ---------- time ----------

    fun year(s: GameState, day: Long): Int = (1 + ((day - s.start) / 365)).toInt().coerceAtLeast(1)

    fun yearOk(w: When, year: Int): Boolean {
        for (t in w.tokens) {
            when (t) {
                "y1" -> if (year != 1) return false
                "y2" -> if (year != 2) return false
                "y3" -> if (year != 3) return false
                "y2+" -> if (year < 2) return false
                "y3+" -> if (year < 3) return false
            }
        }
        return true
    }

    /** For stories: a year mark means "not before that year", so a chain never locks when its window passes. */
    fun notBefore(w: When, year: Int): Boolean {
        for (t in w.tokens) {
            val min = when (t) { "y2", "y2+" -> 2; "y3", "y3+" -> 3; else -> 1 }
            if (year < min) return false
        }
        return true
    }

    /** All the conditions of a line or a note. */
    fun holds(w: When, s: GameState, now: Now, hearts: Int, fest: String?): Boolean {
        val year = year(s, now.day)
        for (t in w.tokens) {
            val ok = when {
                t in SEASONS -> now.season == t
                t in WEATHER -> t in now.sky.tags
                t in PARTS -> now.part == t
                t == "weekend" -> now.weekend
                t.startsWith("y") -> yearOk(When(listOf(t)), year)
                t.startsWith("h") && t.endsWith("+") -> hearts >= (t.drop(1).dropLast(1).toIntOrNull() ?: 0)
                t.startsWith("fest:") -> fest == t.removePrefix("fest:")
                t.startsWith("after:") -> t.removePrefix("after:") in s.flags || builtin(s, now, t.removePrefix("after:"))
                else -> true
            }
            if (!ok) return false
        }
        return true
    }

    fun builtin(s: GameState, now: Now, flag: String): Boolean = when (flag) {
        "y2" -> year(s, now.day) >= 2
        "y3" -> year(s, now.day) >= 3
        "balcony", "hive" -> year(s, now.day) >= 2
        "garden" -> year(s, now.day) >= 3
        "km100" -> s.km >= 100; "km500" -> s.km >= 500; "km1000" -> s.km >= 1000; "km3000" -> s.km >= 3000
        else -> false
    }

    fun flag(s: GameState, now: Now, f: String) = f in s.flags || builtin(s, now, f)

    // ---------- festivals ----------

    /** The festival on this date for the player's year of play, if any. */
    fun festival(s: GameState, now: Now): Festival? {
        val y = year(s, now.day)
        val keys = c.festivals.filter { on(it, now) }.map { it.key }.distinct()
        val key = keys.firstOrNull() ?: return null
        val rows = c.festivals.filter { it.key == key }
        return rows.firstOrNull { it.year == y } ?: rows.firstOrNull { it.year == 0 } ?: rows.first()
    }

    private fun on(f: Festival, now: Now): Boolean {
        val d = now.date
        if (f.md.isNotBlank()) return "%02d-%02d".format(d.monthValue, d.dayOfMonth) == f.md
        return when (f.rule) {
            "maslenitsa" -> d == maslenitsaSunday(d.year)
            "sat_last_09" -> d == LocalDate.of(d.year, 9, 1).with(TemporalAdjusters.lastInMonth(DayOfWeek.SATURDAY))
            "sat_last_05" -> d == LocalDate.of(d.year, 5, 1).with(TemporalAdjusters.lastInMonth(DayOfWeek.SATURDAY))
            "solstice_s" -> d.monthValue == 6 && d.dayOfMonth == 21
            "solstice_w" -> d.monthValue == 12 && d.dayOfMonth == 21
            else -> false // first_snow and first_ice come with the weather, see weatherFestival

        }
    }

    /** First-snow and first-ice festivals depend on the weather, so the night decides them. */
    private fun weatherFestival(s: GameState, now: Now): Festival? {
        val y = year(s, now.day)
        fun pick(rule: String): Festival? {
            val rows = c.festivals.filter { it.rule == rule }
            if (rows.isEmpty() || "${rows[0].key}:$y" in s.fests) return null
            return rows.firstOrNull { it.year == y } ?: rows.firstOrNull { it.year == 0 }
        }
        if (now.sky.snow && now.month in listOf(10, 11, 12)) pick("first_snow")?.let { return it }
        if (now.sky.temp < -3 && now.month in listOf(11, 12, 1)) pick("first_ice")?.let { return it }
        return null
    }

    // ---------- beds ----------

    fun maxBeds(s: GameState, now: Now): Int {
        val y = year(s, now.day)
        val base = when (y) { 1 -> if ("fair.first_autumn" in s.flags) 9 else 6; 2 -> 12; else -> 16 }
        return base + (s.tools["beds"] ?: 0) + 2 * up(s, "cart")
    }

    /** Metres within which passing by tends a bed. */
    fun reach(s: GameState) = when (s.tool("can")) { 1 -> 10.0; 2 -> 20.0; else -> 30.0 }

    fun placeOpen(s: GameState, now: Now, p: Place) = when (p) {
        Place.VERGE, Place.SILL -> true
        Place.BALCONY -> flag(s, now, "balcony")
        Place.GARDEN -> flag(s, now, "garden")
    }

    fun canSow(s: GameState, now: Now, v: Variety): Boolean =
        (s.seeds[v.key] ?: 0) > 0 && placeOpen(s, now, v.place) &&
            s.beds.count { c.variety[it.variety]?.place == v.place } < capacity(s, now, v.place)

    fun capacity(s: GameState, now: Now, p: Place) = when (p) {
        Place.VERGE -> maxBeds(s, now); Place.SILL -> 4 + 2 * up(s, "sill"); Place.BALCONY -> 6; Place.GARDEN -> 8
    }

    fun sow(s: GameState, now: Now, variety: String, at: Geo): Bed? {
        val v = c.variety[variety] ?: return null
        if (!canSow(s, now, v)) return null
        if (v.place == Place.VERGE && tooClose(s, at)) return null
        s.add(s.seeds, variety, -1)
        val b = Bed(s.nextId++, variety, at.lat, at.lon, now.day, lastTend = now.day, tended = 0)
        s.beds += b
        s.count("sown")
        return b
    }

    /** Is there a bed by the road closer than [BED_GAP] to [at]? Beds packed tighter turn into a jumble on the map. */
    fun tooClose(s: GameState, at: Geo) = s.beds.any { c.variety[it.variety]?.place == Place.VERGE && Geo(it.lat, it.lon).metres(at) < BED_GAP }

    fun ripe(b: Bed): Boolean {
        val v = c.variety[b.variety] ?: return false
        return b.grown >= v.days && (b.wild || b.tended >= v.tends)
    }

    /**
     * What a bed is waiting for, in words the player can act on: nights left to ripen if it is tended every
     * day at today's pace, whether it has been watered today, whether
     * today's rain will water it tonight, how many untended nights before it goes wild (null if it can't
     * now), and whether tonight's frost will nip it unless it is visited today.
     */
    data class Outlook(val nights: Int, val watered: Boolean, val rain: Boolean, val indoors: Boolean, val wildIn: Int?, val frost: Boolean)

    fun outlook(s: GameState, b: Bed, now: Now): Outlook {
        val v = c.variety.getValue(b.variety)
        val indoors = v.place == Place.SILL || v.place == Place.BALCONY
        val watered = b.lastTend == now.day || b.self
        val rain = !indoors && (now.sky.rain || now.sky.snow)
        val ripe = ripe(b)
        val nights = when {
            ripe -> 0
            else -> {
                val grow = kotlin.math.ceil(((v.days - b.grown) / growth(now, indoors) - 1e-9).coerceAtLeast(0.0)).toInt()
                max(grow, if (b.wild) 0 else v.tends - b.tended).coerceAtLeast(1)
            }
        }
        val wildIn = if (indoors || ripe || b.wild || b.self) null else max(1, wildAfter(s) - b.idle)
        val frost = !indoors && !ripe && up(s, "cover") == 0 && now.tomorrow.frost && !watered && b.grown > 0 && !cold(now)
        return Outlook(nights, watered, rain, indoors, wildIn, frost)
    }

    /** The day a variety sown now would be ripe if tended every day, at today's pace. */
    fun ripeOn(v: Variety, now: Now): LocalDate {
        val indoors = v.place == Place.SILL || v.place == Place.BALCONY
        val n = max(kotlin.math.ceil(v.days / growth(now, indoors) - 1e-9).toInt(), v.tends).coerceAtLeast(1)
        return now.date.plusDays(n.toLong())
    }

    fun cold(now: Now) = now.month in listOf(12, 1, 2) || now.sky.temp < 2
    /** Days of growth a tended bed gains in one night: half outdoors in the cold, a little less in March and November. */
    private fun growth(now: Now, indoors: Boolean) = when {
        !indoors && cold(now) -> 0.5
        now.month in listOf(3, 11) -> 0.6
        else -> 1.0
    }
    /** Untended nights before a bed goes wild. */
    private fun wildAfter(s: GameState) = 3 + 2 * up(s, "mulch")

    /** The player is at [at]: every verge bed within reach is tended today. Returns the beds tended now. */
    fun pass(s: GameState, now: Now, at: Geo): List<Bed> {
        val r = reach(s)
        val out = ArrayList<Bed>()
        for (b in s.beds) {
            // sowing waters a bed but isn't a walk past, so walking by a bed sown today still counts
            if (b.passed == now.day) continue
            val p = c.variety[b.variety]?.place ?: continue
            if (p != Place.VERGE && p != Place.GARDEN) continue
            if (Geo(b.lat, b.lon).metres(at) <= r) {
                b.lastTend = now.day; b.passed = now.day; out += b
            }
        }
        if (out.isNotEmpty()) { s.count("tend", out.size); s.count("tend_today", out.size) }
        return out
    }

    /** Pots at home are tended by being at home; the app calls this when the player opens the courtyard. */
    fun tendHome(s: GameState, now: Now): Int {
        var n = 0
        for (b in s.beds) {
            val p = c.variety[b.variety]?.place ?: continue
            if ((p == Place.SILL || p == Place.BALCONY) && b.lastTend != now.day) { b.lastTend = now.day; n++ }
        }
        return n
    }

    fun harvest(s: GameState, now: Now, bedId: Long, rnd: Double = Math.random()): Harvest? {
        val b = s.beds.firstOrNull { it.id == bedId } ?: return null
        if (!ripe(b)) return null
        val v = c.variety.getValue(b.variety)
        val n = (if (b.wild) max(1, v.yield / 2) else v.yield) + (s.tool("basket") - 1) + up(s, "compost")
        s.add(s.bag, v.key, n)
        val first = s.album.add(v.key)
        // shoots on the sill always leave a pinch of seed for the next pot
        val seedBack = v.place == Place.SILL || rnd < 0.4
        if (seedBack) s.add(s.seeds, v.key, 1)
        s.count("harvest"); s.count("harvest:${v.crop}"); if (b.wild) s.count("wild")
        s.counts["harvest_day"] = now.day.toInt()
        if (v.regrow > 0 && !b.self) {
            b.grown = (v.days - v.regrow).toDouble(); b.tended = v.tends; b.harvests++; b.wild = false; b.idle = 0
        } else s.beds.remove(b)
        return Harvest(v.key, n, seedBack, first)
    }

    fun dig(s: GameState, bedId: Long) { s.beds.removeAll { it.id == bedId } }

    // ---------- the night ----------

    /**
     * Runs every night between [s].day and [now].day: beds grow where tended or rained on, idle beds go wild,
     * frost bites what wasn't covered, hens lay, notes and requests arrive. [past] gives the sky of a past day.
     */
    fun catchUp(s: GameState, now: Now, past: (Long) -> Pair<Sky, Sky>): Morning {
        repace(s)
        if (s.day == 0L) { s.day = now.day; return Morning(year = year(s, now.day)) }
        var m = Morning(year = year(s, now.day))
        var d = s.day
        while (d < now.day) {
            val (sky, next) = past(d)
            m = night(s, Now(LocalDate.ofEpochDay(d), 23, sky, next, now.south), m)
            d++
            s.day = d
        }
        if (m.year < year(s, now.day)) m = m.copy(newYear = true, year = year(s, now.day))
        val fest = festival(s, now) ?: weatherFestival(s, now)
        if (fest != null && "${fest.key}:${year(s, now.day)}" !in s.fests) {
            s.fests += "${fest.key}:${year(s, now.day)}"
            fest.gift.forEach { give(s, it) }
            if (fest.ribbon.isNotBlank()) s.ribbons += fest.ribbon
            m = m.copy(festival = fest)
        }
        return m
    }

    /** A game saved before the faster garden ([Content.PACE]) counts its beds' growth in real days: scale it once. */
    fun repace(s: GameState) {
        if ("pace" in s.flags) return
        for (b in s.beds) { b.grown /= Content.PACE; b.tended = (b.tended + Content.PACE - 1) / Content.PACE }
        s.flags += "pace"
    }

    private fun night(s: GameState, now: Now, acc: Morning): Morning {
        var grew = acc.grew; var ripe = acc.ripe; var wild = acc.wild; var frost = acc.frost
        val cold = cold(now)
        for (b in s.beds) {
            val v = c.variety[b.variety] ?: continue
            if (ripe(b)) continue
            val indoors = v.place == Place.SILL || v.place == Place.BALCONY
            val tended = b.lastTend == now.day || (!indoors && (now.sky.rain || now.sky.snow)) || b.self
            if (tended) {
                b.grown += growth(now, indoors); grew++
                b.tended++; b.idle = 0
            } else {
                b.idle++
                if (b.wild) b.grown += growth(now, indoors) / 2
                if (b.idle >= wildAfter(s) && !b.wild && !indoors) { b.wild = true; wild++ }
            }
            if (!indoors && up(s, "cover") == 0 && now.tomorrow.frost && b.lastTend != now.day && b.grown > 0 && !cold) { b.grown = max(0.0, b.grown - 1); frost++ }
            if (ripe(b)) ripe++
        }
        val laid = (if (now.month in listOf(12, 1)) 1 else 2) + up(s, "coop")
        val eggs = max(0, min(6 + 3 * up(s, "coop"), s.eggs + laid) - s.eggs)
        s.eggs += eggs
        s.kmToday = 0.0
        s.counts.remove("finds_today"); s.counts.remove("tend_today")
        // the board: a new notice every third day
        s.board.removeAll { now.day - it.day > 12 }
        if (now.day % 3 == 0L) {
            val open = c.board.filter { it.key !in s.boardDone && s.board.none { p -> p.key == it.key } && (it.months.isEmpty() || now.month in it.months) }
            if (open.isNotEmpty() && s.board.size < 4) s.board += Pinned(open[(now.day % open.size).toInt()].key, now.day)
        }
        val mail = ArrayList(acc.mail)
        val reqs = ArrayList(acc.requests)
        val delivered = deliver(s, now)
        mail += delivered
        reqs += showRequests(s, now)
        return Morning(grew, ripe, wild, frost, acc.rained || now.sky.rain, acc.eggs + eggs, mail, reqs, acc.festival, acc.year)
    }

    // ---------- neighbours ----------

    fun hearts(points: Int): Int {
        var h = 0
        while (h < 10 && points >= HEART_POINTS[h + 1]) h++
        return h
    }

    fun hearts(s: GameState, id: String) = hearts(s.bonds[id]?.points ?: 0)

    /** The player came to a neighbour's place today. Returns true when this opened the neighbour. */
    fun visit(s: GameState, now: Now, id: String): Boolean {
        val b = s.bond(id)
        val opened = !b.isOpen
        if (opened) { b.open = now.day; b.reqDone = now.day; b.noteDone = now.day }
        if (b.visited != now.day) { b.visited = now.day; b.points += 1 }
        showRequests(s, now)
        return opened
    }

    /** Today's words at the door: the most specific line whose conditions hold. */
    fun line(s: GameState, now: Now, id: String): Line? {
        val h = hearts(s, id)
        val fest = festival(s, now)?.key
        val ok = c.lines[id].orEmpty().filter {
            (it.months.isEmpty() || now.month in it.months) && h >= it.hearts && holds(it.`when`, s, now, h, fest)
        }
        if (ok.isEmpty()) return null
        val best = ok.maxOf { it.`when`.tokens.size }
        val top = ok.filter { it.`when`.tokens.size >= best - 1 }
        return top[((now.day * 31 + id.hashCode()) % top.size).toInt().let { if (it < 0) it + top.size else it }]
    }

    /** The seeds a neighbour hands out today, or null if already given or none. */
    fun seedsToday(s: GameState, now: Now, id: String): Pair<String, Int>? {
        val p = c.person[id] ?: return null
        if (p.seeds.isEmpty() || s.bond(id).seeded == now.day) return null
        // the first handful is the quickest of theirs, so a new acquaintance soon shows on the verge
        val k = if (s.bond(id).seeded < 0) p.seeds.minBy { c.variety[it]?.days ?: Int.MAX_VALUE } else p.seeds[(now.day % p.seeds.size).toInt()]
        return k to if (hearts(s, id) >= 4) 3 else 2
    }

    fun takeSeeds(s: GameState, now: Now, id: String): Pair<String, Int>? {
        val (k, n) = seedsToday(s, now, id) ?: return null
        s.add(s.seeds, k, n); s.bond(id).seeded = now.day; s.count("seeds")
        return k to n
    }

    /** How a neighbour takes a gift: "love", "like", "dislike" or "plain"; null if not allowed today. */
    fun gift(s: GameState, now: Now, id: String, key: String): String? {
        val b = s.bond(id)
        if (b.gifted == now.day || (s.bag[key] ?: 0) <= 0) return null
        val p = c.person[id] ?: return null
        s.add(s.bag, key, -1); b.gifted = now.day
        val r = when {
            p.loves.any { c.matches(it, key) } -> "love"
            p.likes.any { c.matches(it, key) } -> "like"
            p.dislikes.any { c.matches(it, key) } -> "dislike"
            else -> "plain"
        }
        b.points = max(0, b.points + when (r) { "love" -> 6; "like" -> 3; "dislike" -> -2; else -> 1 })
        b.known[key] = r
        s.count("gift")
        return r
    }

    /** The request a neighbour is waiting on now, if one is showing. */
    fun request(s: GameState, id: String): Request? {
        val b = s.bonds[id] ?: return null
        if (b.reqShown < 0) return null
        return c.requests[id]?.getOrNull(b.req)
    }

    /** Puts up the next request for every open neighbour whose time has come; returns their ids. */
    fun showRequests(s: GameState, now: Now): List<String> {
        val out = ArrayList<String>()
        val y = year(s, now.day)
        for ((id, b) in s.bonds) {
            if (!b.isOpen || b.reqShown >= 0) continue
            val r = c.requests[id]?.getOrNull(b.req) ?: continue
            if (now.day - b.reqDone < r.gap) continue
            if (r.months.isNotEmpty() && now.month !in r.months) continue
            if (hearts(b.points) < r.hearts) continue
            if (!notBefore(r.`when`, y)) continue
            if (!r.`when`.tokens.filter { it.startsWith("after:") }.all { flag(s, now, it.removePrefix("after:")) }) continue
            b.reqShown = now.day; out += id
            // asking for something cooked comes with how to cook it, so no request waits on a recipe nobody taught
            for (n in r.needs) if (n.key in c.products && n.key !in s.recipes && c.recipesFor[n.key]?.none { it.unlock == "start" } != false) s.recipes += n.key
        }
        return out
    }

    fun canFulfil(s: GameState, needs: List<Need>): Boolean = takeFrom(s, needs, dry = true)

    fun fulfil(s: GameState, now: Now, id: String): Request? {
        val r = request(s, id) ?: return null
        if (!takeFrom(s, r.needs, dry = false)) return null
        r.rewards.forEach { give(s, it) }
        val b = s.bond(id)
        b.req++; b.reqShown = -1; b.reqDone = now.day; b.points += 8
        if (r.sets.isNotBlank()) s.flags += r.sets
        s.count("request"); s.counts["request_day"] = now.day.toInt()
        return r
    }

    /** Takes what [needs] asks for from the bag (or just checks, when [dry]). */
    fun takeFrom(s: GameState, needs: List<Need>, dry: Boolean): Boolean {
        val bag = HashMap(s.bag)
        for (n in needs) {
            var left = n.qty
            for (k in bag.keys.toList().sortedBy { c.price(it) }) {
                if (left == 0) break
                if (!c.matches(n.key, k)) continue
                val t = min(left, bag[k] ?: 0)
                bag[k] = (bag[k] ?: 0) - t; left -= t
            }
            if (left > 0) return false
        }
        if (!dry) { s.bag.clear(); bag.filterValues { it > 0 }.forEach { (k, v) -> s.bag[k] = v } }
        return true
    }

    fun give(s: GameState, r: Reward) {
        when (r) {
            is Reward.Coins -> s.coins += r.n
            is Reward.Seeds -> s.add(s.seeds, r.variety, r.n)
            is Reward.Item -> s.add(s.bag, r.key, r.n)
            is Reward.Recipe -> s.recipes += r.key
            is Reward.Tool -> if (r.key == "beds") s.tools["beds"] = (s.tools["beds"] ?: 0) + 1 else s.tools[r.key] = min(3, s.tool(r.key) + 1)
        }
    }

    /** Notes whose time has come go to the letterbox, at most one per neighbour per night and three in all. */
    private fun deliver(s: GameState, now: Now): List<String> {
        val out = ArrayList<String>()
        val y = year(s, now.day)
        for ((id, b) in s.bonds.entries.sortedBy { it.value.noteDone }) {
            if (out.size >= 3) break
            if (!b.isOpen) continue
            val n: Note = c.notes[id]?.getOrNull(b.note) ?: continue
            if (now.day - b.noteDone < n.gap) continue
            if (n.months.isNotEmpty() && now.month !in n.months) continue
            if (hearts(b.points) < n.hearts) continue
            if (!notBefore(n.`when`, y)) continue
            if (!n.flags.all { flag(s, now, it) }) continue
            s.inbox += Mail(n.key, now.day)
            n.rewards.forEach { give(s, it) }
            if (n.sets.isNotBlank()) s.flags += n.sets
            b.note++; b.noteDone = now.day
            out += n.key
        }
        return out
    }

    fun note(key: String): Note? {
        val id = key.substringBefore(':'); val n = key.substringAfter(':').toIntOrNull() ?: return null
        return c.notes[id]?.firstOrNull { it.n == n }
    }

    // ---------- kitchen, fair, market, board ----------

    fun stationOpen(s: GameState, now: Now, st: String) = when (st) {
        "cellar", "hive" -> flag(s, now, "y2")
        else -> true
    }

    /** Recipes known from the start plus those learnt. */
    fun known(s: GameState) = c.recipes.filter { it.unlock == "start" || it.makes in s.recipes || it.key in s.recipes }

    fun cook(s: GameState, now: Now, recipeKey: String): Boolean {
        val r = c.recipes.firstOrNull { it.key == recipeKey } ?: return false
        if (r !in known(s) || !stationOpen(s, now, r.station)) return false
        if (!takeFrom(s, r.needs, dry = false)) return false
        s.add(s.bag, r.makes, r.qty)
        s.album += r.makes; s.count("cook"); s.count("cook:${r.makes}")
        return true
    }

    fun sell(s: GameState, now: Now, key: String, n: Int): Int {
        val have = s.bag[key] ?: 0
        val k = min(n, have)
        if (k <= 0 || !now.saturday) return 0
        if (s.fairDay != now.day) { s.fairDay = now.day; s.fair = 0; s.counts.keys.removeAll { it.startsWith("fair:") } }
        // one at a time: past the first few of a kind, Aunt Zina pays half
        var v = 0
        repeat(k) { v += salePrice(s, now, key); s.count("fair:" + c.speciesOf(key)) }
        s.add(s.bag, key, -k); s.coins += v
        s.fair += v
        s.counts["fair_best"] = max(s.counts["fair_best"] ?: 0, s.fair)
        if (now.month in 9..10) s.flags += "fair.first_autumn"
        return v
    }

    // ---------- the household ----------

    /** The level of a bought upgrade, 0 if none. */
    fun up(s: GameState, key: String) = s.tools["up.$key"] ?: 0

    fun nextPrice(s: GameState, key: String): Int? = Upgrades.of(key).prices.getOrNull(up(s, key))

    fun buyUpgrade(s: GameState, key: String): Boolean {
        val price = nextPrice(s, key) ?: return false
        if (s.coins < price) return false
        s.coins -= price; s.tools["up.$key"] = up(s, key) + 1; s.count("upgrade")
        return true
    }

    // ---------- Mikhalych's hardware shop ----------

    fun owns(s: GameState, key: String) = "own.$key" in s.flags
    fun stock(s: GameState, key: String) = s.tools["stock.$key"] ?: 0

    /** Buys a ware; returns the variety key for a mystery packet, "" for anything else, null if it can't be bought. */
    fun buyWare(s: GameState, now: Now, key: String, rnd: java.util.Random = java.util.Random()): String? {
        val w = Wares.of(key)
        if (s.coins < w.price || (w.once && owns(s, key))) return null
        s.coins -= w.price; s.count("bought")
        if (w.once) { s.flags += "own.$key"; return "" }
        if (key == "surprise") {
            val m = now.month
            val pool = seasonal(m)
                .ifEmpty { c.varieties.filter { it.place == Place.SILL } }
            val v = pool[rnd.nextInt(pool.size)]
            s.add(s.seeds, v.key, 2)
            return v.key
        }
        s.tools["stock.$key"] = stock(s, key) + 1
        return ""
    }

    /** Plant food on a bed: a day of growth at once (in the faster garden a day is what three real ones were). */
    fun feed(s: GameState, bedId: Long): Boolean {
        val b = s.beds.firstOrNull { it.id == bedId } ?: return false
        val v = c.variety[b.variety] ?: return false
        if (stock(s, "feed") <= 0 || ripe(b) || b.grown >= v.days) return false
        b.grown = minOf(v.days.toDouble(), b.grown + 1); b.tended += 1
        s.tools["stock.feed"] = stock(s, "feed") - 1
        return true
    }

    /** Weeds a bed gone wild, with the hoe. */
    fun weed(s: GameState, bedId: Long): Boolean {
        val b = s.beds.firstOrNull { it.id == bedId } ?: return false
        if (!owns(s, "hoe") || !b.wild) return false
        b.wild = false; b.idle = 0
        return true
    }

    /** The crop the fair wants this week: it sells for twice the price. Chosen from what can be ripe around now. */
    fun demand(s: GameState, now: Now): String? {
        val m = now.month
        val ok = c.varieties.filter { it.place == Place.VERGE && it.sow.any { sm -> ((m - sm + 12) % 12) in 1..4 } }.map { it.crop }.distinct().sorted()
        if (ok.isEmpty()) return null
        val week = (now.day + 3) / 7
        return ok[((week * 7919 + s.start) % ok.size).toInt().let { if (it < 0) it + ok.size else it }]
    }

    /** What one of [key] fetches at the fair today. */
    /**
     * What the fair pays for the next one. Half the listed price, as the garden runs [Content.PACE] times faster
     * than a real one and there is far more to sell than the prices were set for; and half again past the first
     * [FAIR_FULL] of a kind that Saturday, so a sea of radishes doesn't buy the whole household.
     */
    fun salePrice(s: GameState, now: Now, key: String): Int {
        var p = (c.price(key) + 1) / 2
        if (demand(s, now) == c.speciesOf(key)) p *= 2
        if (up(s, "stall") > 0) p = (p * 5 + 3) / 4
        if (s.fairDay == now.day && (s.counts["fair:" + c.speciesOf(key)] ?: 0) >= FAIR_FULL) p /= 2
        return max(1, p)
    }

    // ---------- finds on the way ----------

    /**
     * [m] metres walked at a spot of kind [where]. Every few hundred metres something turns up: something to forage,
     * a packet of seeds, a groat, a neighbour's lost thing, or just a moment worth seeing. At most six a day.
     */
    fun walked(s: GameState, now: Now, m: Double, where: Set<String>, rnd: java.util.Random = java.util.Random()): Find? {
        val acc = (s.counts["walk_m"] ?: 0) + m.toInt()
        var next = s.counts["find_next"] ?: 0
        if (next == 0) { next = nextFind(s, rnd); s.counts["find_next"] = next }
        if (acc < next || (s.counts["finds_today"] ?: 0) >= 6) { s.counts["walk_m"] = min(acc, next); return null }
        s.counts["walk_m"] = 0; s.counts["find_next"] = nextFind(s, rnd)
        val f = roll(s, now, where, rnd)
        s.count("finds_today"); s.count("find")
        when (f.kind) {
            "good" -> { s.add(s.bag, f.key, f.n); s.album += f.key }
            "seeds" -> s.add(s.seeds, f.key, f.n)
            "coins" -> s.coins += f.n
            "lost" -> s.flags += "lost.${f.key}.${f.n}"
        }
        return f
    }

    /** The keys neighbours and the board are waiting on now, minus what the bag already holds enough of. */
    fun wanted(s: GameState): Set<String> {
        val needs = s.bonds.keys.mapNotNull { request(s, it) }.flatMap { it.needs } +
            s.board.filter { it.key !in s.boardDone }.mapNotNull { p -> c.board.firstOrNull { it.key == p.key } }.flatMap { it.needs }
        return needs.filter { Sources.have(c, s, it.key) < it.qty }.map { it.key }.toSet()
    }

    private fun nextFind(s: GameState, rnd: java.util.Random) = if (up(s, "satchel") > 0) 180 + rnd.nextInt(220) else 280 + rnd.nextInt(320)

    private fun roll(s: GameState, now: Now, where: Set<String>, rnd: java.util.Random): Find {
        val r = rnd.nextDouble()
        // something to forage, fitting the month, the spot, the weather and the hour; what a neighbour is
        // waiting for turns up whatever the weather, and more often, so a request for rowan can be met in a thaw
        val wanted = wanted(s)
        fun asked(key: String) = wanted.any { c.matches(it, key) }
        val goods = c.goods.filter {
            it.kind == app.greenstreet.data.GoodKind.FIND && (it.months.isEmpty() || now.month in it.months) &&
                (it.where.isBlank() || it.where in where) && (it.weather.isBlank() || it.weather in now.sky.tags || asked(it.key) || rnd.nextDouble() < .25) &&
                (it.time.isBlank() || it.time == now.part || rnd.nextDouble() < .3)
        }
        val place = when {
            "water" in where -> Tx("У воды", "By the water"); "wood" in where -> Tx("В роще", "In the trees")
            "park" in where -> Tx("В парке", "In the park"); "lawn" in where -> Tx("На газоне", "On the grass"); else -> Tx("У дороги", "By the road")
        }
        if (goods.isNotEmpty() && r < (if (goods.any { asked(it.key) }) .6 else .5)) {
            val w = goods.map { (4 - it.rare).coerceAtLeast(1) * if (asked(it.key)) 3 else 1 }
            var x = rnd.nextDouble() * w.sum(); var g = goods.last()
            for (i in goods.indices) { x -= w[i]; if (x < 0) { g = goods[i]; break } }
            val n = if (g.rare >= 2) 1 else 1 + rnd.nextInt(2)
            val tail = if (n > 1) " ×$n" else ""
            return Find("good", g.key, n, Tx(place.ru + " попалось: " + g.name.ru.lowercase() + tail + ".", place.en + ": " + g.name.en.lowercase() + tail + "."))
        }
        // a neighbour's lost thing, if someone is known and nothing of theirs is already in the pocket
        val open = s.bonds.filter { b -> b.value.isOpen && c.person[b.key]?.yard == false && s.flags.none { f -> f.startsWith("lost." + b.key + ".") } }.keys.sorted()
        if (open.isNotEmpty() && r < .62) {
            val id = open[rnd.nextInt(open.size)]; val k = rnd.nextInt(Lost.things.size); val t = Lost.things[k]
            val who = c.person.getValue(id).name
            return Find("lost", id, k, Tx("На тротуаре кто-то обронил " + t.ru + ". Похоже, это вещь соседа: " + who.ru + ". Занесите при случае.",
                "Someone has dropped " + t.en + " on the pavement. It looks like " + who.en + "'s. Drop it off when you pass."))
        }
        // a forgotten packet of seeds
        val seeds = seasonal(now.month, setOf("market", "start"))
        if (seeds.isNotEmpty() && r < .74) {
            val v = seeds[rnd.nextInt(seeds.size)]
            return Find("seeds", v.key, 1 + rnd.nextInt(2), Tx("На скамейке забыли пакетик семян: " + v.name.ru + ". Не пропадать же.", "A packet of seeds left on a bench: " + v.name.en + ". Shame to waste it."))
        }
        if (r < .86) {
            val n = 2 + rnd.nextInt(5)
            return Find("coins", "coins", n, Tx("У бордюра что-то блеснуло: " + n + (if (n in 2..4) " гроша." else " грошей."), "Something glints by the kerb: $n groats."))
        }
        val ms = Moments.all.filter { it.months.isEmpty() || now.month in it.months }
        return Find("moment", "", 0, ms[rnd.nextInt(ms.size)].text)
    }

    /** The lost thing of [id] the player carries: its index in [Lost.things], or null. */
    fun lostOf(s: GameState, id: String): Int? = s.flags.firstOrNull { it.startsWith("lost.$id.") }?.substringAfterLast('.')?.toIntOrNull()

    fun returnLost(s: GameState, id: String): Boolean {
        val k = lostOf(s, id) ?: return false
        s.flags -= "lost.$id.$k"
        s.bond(id).points += 5; s.coins += 5; s.count("returned")
        return true
    }


    /**
     * Verge varieties worth sowing about now: those whose best months are this or next month, or, in the
     * winter gap when none are, those of the first sowing month ahead (they can go in any time, only slower).
     */
    fun seasonal(month: Int, unlock: Set<String>? = null): List<Variety> {
        val pool = c.varieties.filter { it.place == Place.VERGE && (unlock == null || it.unlock in unlock) }
        for (ahead in 0 until 12) {
            val m = (month - 1 + ahead) % 12 + 1; val n = m % 12 + 1
            val out = pool.filter { it.sow.isEmpty() || m in it.sow || (ahead == 0 && n in it.sow) }
            if (out.isNotEmpty()) return out
        }
        return pool
    }

    /**
     * Market stalls: staples, common seeds, and a beekeeper's honey and wax (dearer than the fair pays for them),
     * until the yard has hives of its own.
     */
    fun market(now: Now): List<Pair<String, Int>> {
        val verge = seasonal(now.month, setOf("market"))
        val seeds = (c.varieties.filter { it.unlock == "market" && it.place != Place.VERGE && (it.sow.isEmpty() || now.month in it.sow) } + verge)
            .map { it.key to max(2, it.price / 2 + 1) }
        val staples = c.goods.filter { it.kind == app.greenstreet.data.GoodKind.STAPLE }.map { it.key to it.price } +
            c.goods.filter { it.key in BEEKEEPER }.map { it.key to it.price * 2 }
        return seeds + staples
    }

    fun buy(s: GameState, now: Now, key: String): Boolean {
        val price = market(now).firstOrNull { it.first == key }?.second ?: return false
        if (s.coins < price) return false
        s.coins -= price
        if (c.variety[key] != null) s.add(s.seeds, key, 1) else s.add(s.bag, key, 1)
        return true
    }

    fun answerNotice(s: GameState, key: String): Boolean {
        val n = c.board.firstOrNull { it.key == key } ?: return false
        if (s.board.none { it.key == key } || !takeFrom(s, n.needs, dry = false)) return false
        n.rewards.forEach { give(s, it) }
        s.board.removeAll { it.key == key }; s.boardDone += key; s.count("board")
        return true
    }

    /** Takes the eggs from the nests; every fourth egg comes with a feather left in the straw. */
    fun collectEggs(s: GameState): Int {
        val n = s.eggs; s.add(s.bag, "egg", n); s.eggs = 0
        if (n > 0) {
            s.album += "egg"
            val before = s.counts["eggs"] ?: 0
            s.count("eggs", n)
            val feathers = (before + n) / 4 - before / 4
            if (feathers > 0) { s.add(s.bag, "feather", feathers); s.album += "feather" }
        }
        return n
    }

    /** A new game: some seeds in the pocket, a few eggs, the start recipes. */
    fun newGame(now: Now, home: Geo?): GameState = GameState().apply {
        start = now.day; day = now.day; this.home = home
        c.varieties.filter { it.unlock == "start" && (it.place == Place.VERGE || it.place == Place.SILL) }.take(4).forEach { seeds[it.key] = 2 }
        // Aunt Klava's packets: the two quickest verge crops, so the first bed is ripe within days whatever the month
        c.varieties.filter { it.place == Place.VERGE && it.unlock in setOf("market", "start") }.sortedBy { it.days }.take(2).forEach { seeds[it.key] = 2 }
        flags += "pace"
        bag["egg"] = 2
        // a bottle of milk in the basket: enough for cottage cheese, so the kitchen can be tried before the market is found
        bag["milk"] = 3
    }

    companion object {
        val SEASONS = setOf("spring", "summer", "autumn", "winter")
        val WEATHER = setOf("rain", "snow", "frost", "fog", "sun", "heat")
        val PARTS = setOf("morning", "day", "evening", "night")
        /** Points needed for each heart: about a season of regular visits for 4, a year for 7, three for 10. */
        /** Metres between beds by the road. */
        const val BED_GAP = 25.0
        /** How many of one kind the fair takes at full price on a Saturday; the rest go for half. */
        const val FAIR_FULL = 6
        /** What the beekeeper at the market sells. */
        val BEEKEEPER = setOf("honey", "wax")
        val HEART_POINTS = intArrayOf(0, 6, 20, 45, 80, 130, 200, 290, 400, 540, 720)

        /** Sunday of Maslenitsa (Forgiveness Sunday): Orthodox Easter minus 49 days. */
        fun maslenitsaSunday(year: Int): LocalDate {
            val a = year % 4; val b = year % 7; val c = year % 19
            val d = (19 * c + 15) % 30; val e = (2 * a + 4 * b - d + 34) % 7
            val month = (d + e + 114) / 31; val day = (d + e + 114) % 31 + 1
            val julian = LocalDate.of(year, month, day)
            return julian.plusDays(13).minusDays(49)
        }
    }
}

/** Distance in metres between two points (equirectangular, plenty for a district). */
fun Geo.metres(o: Geo): Double {
    val k = Math.cos(Math.toRadians((lat + o.lat) / 2))
    val dx = (lon - o.lon) * 111_320.0 * k
    val dy = (lat - o.lat) * 110_574.0
    return Math.sqrt(dx * dx + dy * dy)
}
