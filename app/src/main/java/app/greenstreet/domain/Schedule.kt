package app.greenstreet.domain

import app.greenstreet.data.Person
import app.greenstreet.data.Tx
import java.time.LocalDateTime

/** Where a neighbour is at a given moment. */
enum class Whereabouts { WORK, OUT, ASLEEP }

/**
 * The neighbours' days. A town neighbour is at their place in working hours, out walking the district
 * in the rest of the waking day (and all day on a day off), and asleep at night. The yard's people
 * are about the courtyard while awake.
 */
object Schedule {
    /** The waking day: from an early start (or eight) to ten at night. */
    private fun wake(p: Person) = minOf(p.open, 8)
    private const val BED = 22

    fun dayOff(p: Person, t: LocalDateTime) = t.dayOfWeek.value in p.off

    fun status(p: Person, t: LocalDateTime): Whereabouts {
        val h = t.hour
        if (p.open == 0 && p.close >= 24) return Whereabouts.WORK
        val working = !dayOff(p, t) && h >= p.open && h < p.close
        if (working) return Whereabouts.WORK
        if (p.yard) return if (h >= wake(p) && h < maxOf(BED, p.close)) Whereabouts.WORK else Whereabouts.ASLEEP
        val late = maxOf(BED, if (dayOff(p, t)) BED else p.close)
        return if (h >= wake(p) && h < late) Whereabouts.OUT else Whereabouts.ASLEEP
    }

    /** "7:00–19:00, closed on Mondays" in words. */
    fun hours(p: Person): Tx {
        if (p.open == 0 && p.close >= 24) return Tx("всегда где-то рядом", "always somewhere about")
        val span = "${p.open}:00–${p.close}:00"
        if (p.off.isEmpty()) return Tx("$span, без выходных", "$span, every day")
        val ru = listOf("", "пн", "вт", "ср", "чт", "пт", "сб", "вс")
        val en = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return Tx("$span, выходной: " + p.off.sorted().joinToString { ru[it] }, "$span, off: " + p.off.sorted().joinToString { en[it] })
    }

    /** The next time the neighbour is at work, from [t]. */
    fun nextOpen(p: Person, t: LocalDateTime): LocalDateTime {
        var d = t.withMinute(0).withSecond(0).withNano(0)
        repeat(24 * 8) {
            d = d.plusHours(1)
            if (status(p, d) == Whereabouts.WORK) return d
        }
        return d
    }

    /** "now: at work till 19:00" / "day off, out walking" / "asleep till 7:00", for the lists. */
    fun now(p: Person, t: LocalDateTime): Tx {
        fun at(d: LocalDateTime): Tx {
            val days = java.time.Duration.between(t.toLocalDate().atStartOfDay(), d.toLocalDate().atStartOfDay()).toDays()
            val ru = when (days) { 0L -> "сегодня"; 1L -> "завтра"; else -> listOf("", "в понедельник", "во вторник", "в среду", "в четверг", "в пятницу", "в субботу", "в воскресенье")[d.dayOfWeek.value] }
            val en = when (days) { 0L -> "today"; 1L -> "tomorrow"; else -> "on " + d.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() } }
            return Tx("$ru в ${d.hour}:00", "$en at ${d.hour}:00")
        }
        return when (status(p, t)) {
            Whereabouts.WORK -> if (p.yard || (p.open == 0 && p.close >= 24)) Tx("сейчас дома, во дворе", "home now, about the yard")
                else Tx("сейчас на месте, до ${p.close}:00", "in now, till ${p.close}:00")
            Whereabouts.OUT -> {
                val o = at(nextOpen(p, t))
                Tx((if (dayOff(p, t)) "выходной, гуляет по району" else "гуляет по району") + "; на месте ${o.ru}",
                    (if (dayOff(p, t)) "a day off, out walking" else "out walking") + "; back in ${o.en}")
            }
            Whereabouts.ASLEEP -> {
                val o = at(nextOpen(p, t))
                Tx("спит; ${if (p.yard) "выйдет" else "откроется"} ${o.ru}", "asleep; ${if (p.yard) "up" else "open"} ${o.en}")
            }
        }
    }
}
