package pl.podkal.domowniczeqqq.pantry

import com.google.firebase.firestore.FirebaseFirestore

object PantryMergeHelper {
    fun mergeDuplicates(pantryItems: List<PantryItem>, onSuccess: () -> Unit) {
        val groupedItems = pantryItems.groupBy { it.name.lowercase() }
        val itemsToMerge = groupedItems.filter { it.value.size > 1 }
        val db = FirebaseFirestore.getInstance()

        itemsToMerge.forEach { (_, items) ->
            val firstItem = items.firstOrNull() ?: return@forEach
            val otherItems = items.drop(1)

            val updatedQuantity = items.sumOf { it.quantity }

            // Ensure no null values in the data
            val safeUpdates = mapOf(
                "quantity" to updatedQuantity,
                "userId" to (firstItem.userId.ifEmpty { "" }),
                "groupId" to (firstItem.groupId.ifEmpty { "" }),
                "name" to (firstItem.name.ifEmpty { "" }),
                "category" to (firstItem.category.ifEmpty { "" }),
                "unit" to (firstItem.unit.ifEmpty { "szt." })
            )

            // Update the first item with the total quantity
            db.collection("pantry_items")
                .document(firstItem.id)
                .update(safeUpdates)

            // Delete the duplicate items
            otherItems.forEach { item ->
                if (item.id.isNotEmpty()) {
                    db.collection("pantry_items")
                        .document(item.id)
                        .delete()
                }
            }
        }
        onSuccess()
    }
}