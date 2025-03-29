package pl.podkal.domowniczeqqq.finance

import androidx.compose.ui.graphics.Color

/**
 * Data class representing information about a single slice in the pie chart
 */
data class SliceData(
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color,
    val category: String
)
