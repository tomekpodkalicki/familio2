package pl.podkal.domowniczeqqq.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CalendarShapes = Shapes(
    // Calendar cell shape
    small = RoundedCornerShape(8.dp),

    // Button shape, dialog corners
    medium = RoundedCornerShape(12.dp),

    // Card shape for larger elements
    large = RoundedCornerShape(16.dp)
)

// Calendar specific shapes
val CalendarDayShape = RoundedCornerShape(12.dp)
val EventShape = RoundedCornerShape(6.dp)
val MonthNavigationShape = RoundedCornerShape(12.dp)
