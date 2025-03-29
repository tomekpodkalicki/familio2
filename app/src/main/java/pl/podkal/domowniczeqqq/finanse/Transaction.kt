package pl.podkal.domowniczeqqq.finance

import androidx.compose.ui.graphics.Color
import java.util.Date

data class Transaction(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val isExpense: Boolean = true,
    val category: String = "",
    val date: Date = Date(),
    val color: Color = Color.Gray,
    val userId: String = ""
)
