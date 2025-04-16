
package pl.podkal.domowniczeqqq.utils

/**
 * Extension functions to safely handle strings in parcels
 */

/**
 * Returns this string or empty string if null
 */
fun String?.orEmpty(): String {
    return this ?: ""
}

/**
 * Returns this string or default value if null
 */
fun String?.orDefault(default: String): String {
    return this ?: default
}

/**
 * Safely reads a string from a data source, ensuring it's never null
 */
fun safeString(value: String?): String {
    return value ?: ""
}

/**
 * Ensures a Map has no null values by replacing null with empty string
 */
fun <K> Map<K, String?>.withNoNullValues(): Map<K, String> {
    return mapValues { it.value ?: "" }
}
