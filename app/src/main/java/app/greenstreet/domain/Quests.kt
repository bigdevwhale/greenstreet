package app.greenstreet.domain

import app.greenstreet.data.Reward
import app.greenstreet.data.Tx

/** A first-weeks task that teaches one thing, given by someone, with a small reward. */
class Quest(
    val key: String,
    /** Who asks: a person id, or "klava" for Aunt Klava's letter. */
    val from: String,
    val title: Tx,
    val how: Tx,
    val goal: Int,
    val reward: List<Reward>,
    val progress: (GameState) -> Int,
)

/**
 * The opening tasks, one at a time, in order: the yard, the first bed, tending on the way, the neighbours,
 * the kitchen, a request, the harvest, fishing, the fair, a long walk. Done ones are flagged "quest.<key>".
 */
object Quests {
    private fun c(s: GameState, k: String) = s.counts[k] ?: 0

    val all = listOf(
        Quest("meet_yard", "semyonych", Tx("Знакомство с двором", "Meet the yard"),
            Tx("Загляните во двор и поговорите с Семёнычем: он присматривает за домом и курами.", "Go into the yard and talk to Semyonych: he looks after the house and the hens."),
            1, listOf(Reward.Coins(10))) { s -> if (s.bonds["semyonych"]?.isOpen == true) 1 else 0 },
        Quest("eggs", "semyonych", Tx("Свежие яйца", "Fresh eggs"),
            Tx("Соберите яйца в курятнике — во дворе, кнопка «Собрать». Куры несутся каждый день.", "Collect the eggs in the hen house — in the yard, the 'Collect' button. The hens lay every day."),
            1, listOf(Reward.Coins(5))) { s -> c(s, "eggs") },
        Quest("sow", "klava", Tx("Первая грядка", "The first bed"),
            Tx("Встаньте у газона рядом с тротуаром или парковой дорожкой — внизу появится «Посеять». Семена тёти Клавы уже в сумке.", "Stand by a lawn next to a pavement or a park path — 'Sow' appears below. Aunt Klava's seeds are in your bag."),
            1, listOf(Reward.Coins(15), Reward.Seeds("calendula_orange", 2))) { s -> c(s, "sown") },
        Quest("tend", "klava", Tx("Полить на ходу", "Water on the way"),
            Tx("Пройдите мимо своей грядки — хватит десяти метров. В кармане тоже работает, если нажать «Выйти на прогулку».", "Walk past your bed — ten metres is enough. It works in your pocket too after 'Go for a walk'."),
            1, listOf(Reward.Coins(10))) { s -> c(s, "tend") },
        Quest("find", "klava", Tx("Что под ногами", "What's underfoot"),
            Tx("Пройдите полкилометра. На улице всегда что-нибудь попадается: грибы, ягоды, чужие семена, грош у бордюра. Карман подскажет.",
                "Walk half a kilometre. There's always something in the street: mushrooms, berries, someone's seeds, a groat by the kerb. Your pocket will tell you."),
            1, listOf(Reward.Coins(10))) { s -> c(s, "find") },
        Quest("neighbour", "klava", Tx("Первый сосед", "The first neighbour"),
            Tx("Зайдите к соседу — их места отмечены вывесками на карте. Возьмите семена: у каждого свои, раз в день.", "Drop in on a neighbour — their places have signs on the map. Take seeds: each has their own, once a day."),
            1, listOf(Reward.Coins(10))) { s -> c(s, "seeds") },
        Quest("gift", "klava", Tx("Маленький подарок", "A small present"),
            Tx("Подарите соседу что-нибудь из сумки. По ответу поймёте, что он любит, — сердечки растут быстрее.", "Give a neighbour something from your bag. Their answer shows what they like — hearts grow faster."),
            1, listOf(Reward.Coins(10))) { s -> c(s, "gift") },
        Quest("cook", "semyonych", Tx("Своя кухня", "Your own kitchen"),
            Tx("Приготовьте что-нибудь на кухне во дворе. Нажмите на блюдо — покажет, чего не хватает.", "Cook something in the yard kitchen. Tap a dish to see what's missing."),
            1, listOf(Reward.Coins(15))) { s -> c(s, "cook") },
        Quest("request", "klava", Tx("Помочь соседу", "Help a neighbour"),
            Tx("Выполните просьбу соседа — у его имени на карте «!». Просьбы приходят одна за другой и рассказывают их истории.", "Fulfil a neighbour's request — their name on the map has a '!'. Requests come one after another and tell their stories."),
            1, listOf(Reward.Coins(20))) { s -> c(s, "request") },
        Quest("harvest", "klava", Tx("Первый урожай", "The first harvest"),
            Tx("Дождитесь искорки над грядкой и соберите урожай. Дождь поливает сам, а без присмотра грядка дичает, но не пропадает.", "Wait for the sparkle over a bed and pick it. Rain waters for you; left alone a bed goes wild but isn't lost."),
            1, listOf(Reward.Coins(20), Reward.Seeds("radish_black", 2))) { s -> c(s, "harvest") },
        Quest("fish", "palych", Tx("На удочку", "Rod and line"),
            Tx("Подойдите к воде — к реке, каналу или пруду — и закиньте удочку. Подсекайте, когда поплавок нырнёт.", "Go to the water — a river, a canal or a pond — and cast. Strike when the float dips."),
            1, listOf(Reward.Coins(15))) { s -> c(s, "fish") },
        Quest("fair", "zina", Tx("Субботняя ярмарка", "The Saturday fair"),
            Tx("В субботу продайте что-нибудь на ярмарке у рынка — у тёти Зины.", "On a Saturday sell something at the fair by the market — at Aunt Zina's."),
            1, listOf(Reward.Coins(10))) { s -> if ((s.counts["fair_best"] ?: 0) > 0) 1 else 0 },
        Quest("upgrade", "semyonych", Tx("Своё хозяйство", "A household of your own"),
            Tx("Купите что-нибудь для хозяйства — во дворе, раздел «Хозяйство». Подумайте, что вам нужнее: больше грядок, больше яиц или грядки, которые подождут.",
                "Buy something for the household — in the yard, under 'Household'. Think what you need most: more beds, more eggs, or beds that will wait."),
            1, listOf(Reward.Coins(20))) { s -> c(s, "upgrade") },
        Quest("walk5", "gena", Tx("Пять километров", "Five kilometres"),
            Tx("Пройдите пять километров по своему району. Грядки любят, когда мимо ходят.", "Walk five kilometres around your district. Beds love being walked past."),
            5, listOf(Reward.Coins(30), Reward.Seeds("sunflower_redsun", 2))) { s -> s.km.toInt() },
    )

    /** The task on now, or null when the opening is done. */
    fun current(s: GameState): Quest? = all.firstOrNull { "quest.${it.key}" !in s.flags }

    /** Marks finished tasks done and pays their rewards; returns the ones finished just now. */
    fun settle(s: GameState, give: (Reward) -> Unit): List<Quest> {
        val done = ArrayList<Quest>()
        while (true) {
            val q = current(s) ?: break
            if (q.progress(s) < q.goal) break
            s.flags += "quest.${q.key}"
            q.reward.forEach(give)
            done += q
        }
        return done
    }

    fun from(id: String, people: (String) -> Tx?): Tx = if (id == "klava") Tx("тётя Клава, из письма", "Aunt Klava's letter") else people(id) ?: Tx(id, id)
}
