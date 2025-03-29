package pl.podkal.domowniczeqqq.finance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * A model class that represents data for a pie chart segment
 */
data class PieChartData(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

// Category information - consistent across entire app
object CategoryInfo {
    // Category class with color and icon
    data class Category(val color: Color, val icon: ImageVector)

    // Map of category names to their standard color and icon
    private val categoryMap = mapOf(
        // Expense categories
        "żywność" to Category(Color(0xFF8E24AA), Icons.Default.Restaurant), // Purple
        "transport" to Category(Color(0xFF1E88E5), Icons.Default.DirectionsCar), // Blue
        "mieszkanie" to Category(Color(0xFF43A047), Icons.Default.House), // Green
        "rozrywka" to Category(Color(0xFFE53935), Icons.Default.Theaters), // Red
        "zdrowie" to Category(Color(0xFF5E35B1), Icons.Default.HealthAndSafety), // Deep Purple
        "edukacja" to Category(Color(0xFFEF6C00), Icons.Default.School), // Orange

        // Income categories
        "wynagrodzenie" to Category(Color(0xFFE53935), Icons.Default.AttachMoney), // Red
        "prezent" to Category(Color(0xFF43A047), Icons.Default.CardGiftcard), // Green
        "inwestycje" to Category(Color(0xFF1E88E5), Icons.Default.TrendingUp), // Blue
        "sprzedaż" to Category(Color(0xFFEF6C00), Icons.Default.ShoppingCart), // Orange
        "zwrot" to Category(Color(0xFF5E35B1), Icons.Default.SwapHoriz) // Deep Purple
    )

    // Default categories
    private val defaultExpenseCategory = Category(Color(0xFF757575), Icons.Default.MoreHoriz) // Gray
    private val defaultIncomeCategory = Category(Color(0xFF26C6DA), Icons.Default.MoreHoriz) // Cyan

    // Get color for a category
    fun getColor(category: String, isExpense: Boolean): Color {
        return categoryMap[category.lowercase()]?.color
            ?: if (isExpense) defaultExpenseCategory.color else defaultIncomeCategory.color
    }

    // Get icon for a category
    fun getIcon(category: String, isExpense: Boolean): ImageVector {
        return categoryMap[category.lowercase()]?.icon
            ?: if (isExpense) defaultExpenseCategory.icon else defaultIncomeCategory.icon
    }

    // Get all available categories for a type (expense or income)
    fun getCategories(isExpense: Boolean): List<String> {
        return if (isExpense) {
            listOf("żywność", "transport", "mieszkanie", "rozrywka", "zdrowie", "edukacja")
        } else {
            listOf("wynagrodzenie", "prezent", "inwestycje", "sprzedaż", "zwrot")
        }
    }
}

@Composable
fun CategoryPieChart(
    transactions: List<Transaction>,
    isExpense: Boolean,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    val filteredTransactions = transactions.filter { it.isExpense == isExpense }

    // Group transactions by category and sum amounts
    val categoryMap = filteredTransactions.groupBy { it.category }
        .mapValues { (_, trans) -> trans.sumOf { it.amount } }
        .filter { it.value > 0 }

    // Sort by amount (descending)
    val sortedCategories = categoryMap.entries
        .sortedByDescending { it.value }

    // Convert to PieChartData with percentages and consistent colors from CategoryInfo
    val pieChartData = sortedCategories.map { (category, amount) ->
        val categoryColor = CategoryInfo.getColor(category, isExpense)

        PieChartData(
            category = category,
            amount = amount,
            percentage = if (totalAmount == 0.0) 0f else (amount / totalAmount * 100).toFloat(),
            color = categoryColor
        )
    }

    // Prepare currency formatter
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pl", "PL"))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp) // Increased height for legend
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (pieChartData.isEmpty()) {
                // Brak danych - centered
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brak danych",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Chart and Legend in positioned layout with legends at top-right and top-left
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Legend (top-right corner) - no margins
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopEnd)
                            .padding(0.dp), // No padding/margins
                        contentAlignment = Alignment.TopEnd
                    ) {
                        // Take up to first 3 categories for right side
                        val rightLegendData = if (pieChartData.size > 3) {
                            pieChartData.take(3)
                        } else {
                            pieChartData
                        }

                        // Vertical column of legend items
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Top
                        ) {
                            // Display categories stacked vertically
                            rightLegendData.forEach { data ->
                                // Horizontal layout for each legend item
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier
                                        .padding(vertical = 0.dp)
                                        .fillMaxWidth(0.35f) // Legend uses only 35% of the width for smaller footprint
                                ) {
                                    // Category name first (smaller text)
                                    Text(
                                        text = data.category,
                                        fontSize = 6.sp, // Even smaller text
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.padding(end = 1.dp)
                                    )

                                    // Icon with colored background (smaller size)
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp) // Smaller circle
                                            .clip(CircleShape)
                                            .background(data.color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Show category icon (even smaller)
                                        Icon(
                                            imageVector = CategoryInfo.getIcon(data.category, isExpense),
                                            contentDescription = data.category,
                                            tint = Color.White,
                                            modifier = Modifier.size(8.dp) // Smaller icon
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Additional legend in top-left corner for 4th category and beyond
                    if (pieChartData.size > 3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart)
                                .padding(0.dp), // No padding/margins
                            contentAlignment = Alignment.TopStart
                        ) {
                            // Take categories starting from the 4th
                            val leftLegendData = pieChartData.drop(3)

                            // Vertical column of legend items
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Top
                            ) {
                                // Display categories stacked vertically
                                leftLegendData.forEach { data ->
                                    // Horizontal layout for each legend item
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start,
                                        modifier = Modifier
                                            .padding(vertical = 0.dp)
                                            .fillMaxWidth(0.35f) // Legend uses only 35% of the width
                                    ) {
                                        // Icon with colored background (smaller size)
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp) // Smaller circle
                                                .clip(CircleShape)
                                                .background(data.color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Show category icon (even smaller)
                                            Icon(
                                                imageVector = CategoryInfo.getIcon(data.category, isExpense),
                                                contentDescription = data.category,
                                                tint = Color.White,
                                                modifier = Modifier.size(8.dp) // Smaller icon
                                            )
                                        }

                                        // Category name (smaller text) - now on the right for left legend
                                        Text(
                                            text = data.category,
                                            fontSize = 6.sp, // Even smaller text
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.padding(start = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // The Chart (positioned at bottom of the card)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f) // Takes 80% of height
                            .align(Alignment.BottomCenter), // Positioned at bottom
                        contentAlignment = Alignment.Center
                    ) {
                        CompactDonutChart(
                            pieChartData = pieChartData,
                            totalAmount = totalAmount,
                            isExpense = isExpense,
                            modifier = Modifier
                                .fillMaxSize(0.85f) // Slightly smaller (85% of container)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactDonutChart(
    pieChartData: List<PieChartData>,
    totalAmount: Double,
    isExpense: Boolean,
    modifier: Modifier = Modifier
) {
    // Make sure we have data
    if (pieChartData.isEmpty()) return

    // Prepare currency formatter
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pl", "PL"))
    val formattedAmount = currencyFormat.format(totalAmount)
    val categoryCount = pieChartData.size

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val radius = (canvasWidth.coerceAtMost(canvasHeight) / 2f) * 0.9f
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val strokeWidth = radius * 0.25f // Donut thickness is 25% of radius

            if (pieChartData.size == 1) {
                // Special case: only one category - draw full circle
                drawCircle(
                    color = pieChartData[0].color,
                    radius = radius - strokeWidth/2,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                // Multiple categories - draw segments
                var startAngle = 0f
                pieChartData.forEach { data ->
                    val sweepAngle = (data.percentage / 100f) * 360f

                    // Draw donut segment
                    drawArc(
                        color = data.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false, // false for donut (ring) style
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth)
                    )

                    // Calculate position for icon
                    val iconAngle = startAngle + sweepAngle / 2
                    val iconRadius = radius - strokeWidth / 2

                    val iconX = center.x + iconRadius * cos(Math.toRadians(iconAngle.toDouble())).toFloat()
                    val iconY = center.y + iconRadius * sin(Math.toRadians(iconAngle.toDouble())).toFloat()

                    // Only draw icon if segment is large enough (more than 10%)
                    if (sweepAngle > 36f) {
                        // Draw a white circle on the segment
                        drawCircle(
                            color = Color.White,
                            radius = 5f,
                            center = Offset(iconX, iconY)
                        )
                    }

                    // Update start angle for next slice
                    startAngle += sweepAngle
                }
            }
        }

        // Display total amount and category count in the center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            // Total amount in bold
            Text(
                text = formattedAmount,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            // Category count in smaller text
            Text(
                text = "w $categoryCount ${getCategoryCountText(categoryCount)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

// Helper function to get the proper Polish declination for "category"
fun getCategoryCountText(count: Int): String {
    return when {
        count == 1 -> "kategorii"
        count in 2..4 -> "kategorie"
        else -> "kategorii"
    }
}
