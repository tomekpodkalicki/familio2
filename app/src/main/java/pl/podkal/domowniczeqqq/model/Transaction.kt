package pl.podkal.domowniczeqqq.model

import androidx.compose.ui.graphics.Color
import java.util.*

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val date: Date = Date(),
    val category: String = "",
    val isExpense: Boolean = true,
    val color: Color = if (isExpense) Color(0xFFF44336) else Color(0xFF4CAF50),
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val recurringPeriod: RecurringPeriod = RecurringPeriod.NONE,
    val userId: String = ""
) {
    companion object {
        fun createEmpty(): Transaction {
            return Transaction(
                title = "",
                amount = 0.0,
                category = "",
                isExpense = true
            )
        }
    }
}

enum class RecurringPeriod {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
}

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Color,
    val isExpense: Boolean = true,
    val icon: String? = null
)

data class FinanceState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val userId: String? = null,
    val showAddTransactionDialog: Boolean = false,
    val showFilterDialog: Boolean = false,
    val currentTransaction: Transaction = Transaction.createEmpty(),
    val startDate: Date? = null,
    val endDate: Date? = null,
    val selectedCategoryFilter: String? = null
)