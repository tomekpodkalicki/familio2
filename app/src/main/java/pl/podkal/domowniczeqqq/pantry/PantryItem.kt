package pl.podkal.domowniczeqqq.pantry

data class PantryItem(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val category: String? = null,
    val location: String = "Spiżarnia", // "Spiżarnia", "Lodówka", "Apteczka"
    val quantity: Double = 0.0,
    val unit: String = "szt.",
    val expiryDate: Long? = null,
    val purchaseDate: Long? = null,
    val price: Double? = null
) {
    // No-arg constructor required for Firestore
    constructor() : this("", "", "", "", null, "Spiżarnia", 0.0, "szt.", null, null, null)

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
            "Nabiał" to "szt.",
            "Mięso" to "kg",
            "Warzywa" to "kg",
            "Owoce" to "kg",
            "Pieczywo" to "szt.",
            "Produkty suche" to "kg",
            "Napoje" to "l",
            "Przekąski" to "opak.",
            "Konserwy" to "szt.",
            "Przyprawy" to "opak.",
            "Sosy" to "szt.",
            "Leki" to "opak.",
            "Środki czystości" to "szt.",
            "Inne" to "szt."
        )
    }
}