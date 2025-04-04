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

            // Update the first item with the total quantity
            db.collection("pantry_items")
                .document(firstItem.id)
                .update("quantity", updatedQuantity)

            // Delete the duplicate items
            otherItems.forEach { item ->
                db.collection("pantry_items")
                    .document(item.id)
                    .delete()
            }
        }
        onSuccess()
    }
}
