package pl.podkal.domowniczeqqq.pantry

data class PantryItem(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val groupId: String = "", // This will be either userId or the shared groupId
    val description: String = "",
    val category: String? = null,
    val location: String = "Spiżarnia", // "Spiżarnia", "Lodówka", "Apteczka"
    val quantity: Double = 0.0,
    val unit: String = "szt.",
    val expiryDate: Long? = null,
    val purchaseDate: Long? = null,
    val price: Double? = null,
) {
    // No-arg constructor required for Firestore
    constructor() : this("", "", "","", "", null, "Spiżarnia", 0.0, "szt.", null, null, null)

    companion object {
        val LOCATIONS = listOf("Spiżarnia", "Lodówka", "Apteczka")

        val CATEGORIES = mapOf(
            "Nabiał" to listOf("Mleko", "Ser", "Masło", "Jogurt", "Śmietana", "Jajka"),
            "Mięso" to listOf("Kurczak", "Wołowina", "Wieprzowina", "Ryba", "Parówki", "Wędlina"),
            "Warzywa" to listOf("Pomidor", "Ogórek", "Marchew", "Ziemniak", "Cebula", "Czosnek", "Papryka"),
            "Owoce" to listOf("Jabłko", "Banan", "Pomarańcza", "Gruszka", "Cytryna", "Truskawka"),
            "Pieczywo" to listOf("Chleb", "Bułka", "Bagietka", "Chleb tostowy"),
            "Produkty suche" to listOf("Ryż", "Makaron", "Mąka", "Cukier", "Sól", "Kasza", "Płatki"),
            "Napoje" to listOf("Woda", "Sok", "Herbata", "Kawa", "Napój gazowany"),
            "Przekąski" to listOf("Chipsy", "Ciastka", "Czekolada", "Orzechy", "Batony"),
            "Konserwy" to listOf("Tuńczyk", "Groszek", "Kukurydza", "Fasola", "Pomidory"),
            "Przyprawy" to listOf("Pieprz", "Papryka", "Oregano", "Bazylia", "Curry", "Zioła prowansalskie"),
            "Sosy" to listOf("Ketchup", "Majonez", "Musztarda", "Sos sojowy", "Ocet"),
            "Leki" to listOf("Przeciwbólowe", "Przeciwgorączkowe", "Witaminy", "Plastry", "Bandaż", "Syrop", "Maść"),
            "Środki czystości" to listOf("Mydło", "Szampon", "Płyn do naczyń", "Proszek do prania", "Płyn do WC"),
            "Inne" to emptyList<String>()
        )

        val DEFAULT_UNITS = mapOf(
            // Nabiał
            "Mleko" to "l",
            "Ser" to "kg",
            "Masło" to "g",
            "Jogurt" to "ml",
            "Śmietana" to "ml",
            "Jajka" to "szt.",

            // Mięso
            "Kurczak" to "kg",
            "Wołowina" to "kg",
            "Wieprzowina" to "kg",
            "Ryba" to "kg",
            "Parówki" to "szt.",
            "Wędlina" to "kg",

            // Warzywa
            "Pomidor" to "kg",
            "Ogórek" to "kg",
            "Marchew" to "kg",
            "Ziemniak" to "kg",
            "Cebula" to "kg",
            "Czosnek" to "szt.",
            "Papryka" to "kg",

            // Owoce
            "Jabłko" to "kg",
            "Banan" to "kg",
            "Pomarańcza" to "kg",
            "Gruszka" to "kg",
            "Cytryna" to "kg",
            "Truskawka" to "kg",

            // Pieczywo
            "Chleb" to "szt.",
            "Bułka" to "szt.",
            "Bagietka" to "szt.",
            "Chleb tostowy" to "szt.",

            // Produkty suche
            "Ryż" to "kg",
            "Makaron" to "kg",
            "Mąka" to "kg",
            "Cukier" to "kg",
            "Sól" to "kg",
            "Kasza" to "kg",
            "Płatki" to "kg",

            // Napoje
            "Woda" to "l",
            "Sok" to "l",
            "Herbata" to "opak.",
            "Kawa" to "opak.",
            "Napój gazowany" to "l",

            // Przekąski
            "Chipsy" to "opak.",
            "Ciastka" to "opak.",
            "Czekolada" to "szt.",
            "Orzechy" to "g",
            "Batony" to "szt.",

            // Konserwy
            "Tuńczyk" to "szt.",
            "Groszek" to "szt.",
            "Kukurydza" to "szt.",
            "Fasola" to "szt.",
            "Pomidory" to "szt.",

            // Przyprawy
            "Pieprz" to "opak.",
            "Papryka" to "opak.",
            "Oregano" to "opak.",
            "Bazylia" to "opak.",
            "Curry" to "opak.",
            "Zioła prowansalskie" to "opak.",

            // Sosy
            "Ketchup" to "ml",
            "Majonez" to "ml",
            "Musztarda" to "ml",
            "Sos sojowy" to "ml",
            "Ocet" to "ml",

            // Leki
            "Przeciwbólowe" to "opak.",
            "Przeciwgorączkowe" to "opak.",
            "Witaminy" to "opak.",
            "Plastry" to "opak.",
            "Bandaż" to "szt.",
            "Syrop" to "ml",
            "Maść" to "opak.",

            // Środki czystości
            "Mydło" to "szt.",
            "Szampon" to "ml",
            "Płyn do naczyń" to "ml",
            "Proszek do prania" to "kg",
            "Płyn do WC" to "ml"
        )
    }
}