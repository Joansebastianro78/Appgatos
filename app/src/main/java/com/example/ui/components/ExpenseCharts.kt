package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.ExpenseEntry
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricPink
import com.example.ui.theme.RadiantPurple
import com.example.ui.theme.CardBackground
import com.example.ui.theme.SoftText
import kotlin.math.sin

@Composable
fun ExpenseTrendsChart(
    expenses: List<ExpenseEntry>,
    modifier: Modifier = Modifier
) {
    if (expenses.isEmpty()) {
        val infiniteTransition = rememberInfiniteTransition(label = "bouncing")
        val bounceY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bounce"
        )

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¡Aún no hay transacciones! 💸",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CyanGlow,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f + bounceY

                // Render beautiful background guidelines
                for (i in 1..3) {
                    val y = height * (i / 4f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Render placeholder glowing sine wave
                val path = Path()
                path.moveTo(0f, midY)
                val points = 60
                for (i in 0..points) {
                    val x = (i / points.toFloat()) * width
                    val angle = (i / points.toFloat()) * 2 * Math.PI.toFloat()
                    val y = midY + sin(angle) * 20f
                    path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(listOf(ElectricPink, RadiantPurple, CyanGlow)),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Registra un gasto o un ingreso para visualizar tu gráfico de tendencias.",
                fontSize = 12.sp,
                color = SoftText,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "📈 Historial y Tendencia de Flujos",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CyanGlow,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val sortedExpenses = expenses.sortedBy { it.timestamp }
            val last7 = sortedExpenses.takeLast(7)
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val width = size.width
                val height = size.height
                
                // Draw grid lines
                for (i in 0..4) {
                    val y = height * (i / 4f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.07f),
                        start = Offset(0f, y),
                        end = Offset(width, y)
                    )
                }

                if (last7.size == 1) {
                    val single = last7[0]
                    val drawY = if (single.isIncome) height * 0.3f else height * 0.7f
                    drawCircle(
                        brush = Brush.horizontalGradient(listOf(ElectricPink, CyanGlow)),
                        radius = 12.dp.toPx(),
                        center = Offset(width / 2f, drawY)
                    )
                } else {
                    val stepX = width / (last7.size - 1)
                    val maxAmount = last7.maxOf { it.amount }.coerceAtLeast(10.0)
                    val minAmount = last7.minOf { it.amount }

                    val mapY = { value: Double, isIncome: Boolean ->
                        val ratio = if (maxAmount == minAmount) 0.5f else ((value - minAmount) / (maxAmount - minAmount)).toFloat()
                        // Incomes plotted higher, Expenses plotted lower, let's keep it clean
                        val basePlot = height - (ratio * height * 0.6f) - (height * 0.2f)
                        if (isIncome) basePlot * 0.8f else basePlot
                    }

                    val path = Path()
                    val fillPath = Path()

                    val firstX = 0f
                    val firstY = mapY(last7[0].amount, last7[0].isIncome)

                    path.moveTo(firstX, firstY)
                    fillPath.moveTo(0f, height)
                    fillPath.lineTo(firstX, firstY)

                    val points = mutableListOf<Offset>()
                    points.add(Offset(firstX, firstY))

                    for (i in 1 until last7.size) {
                        val currentX = i * stepX
                        val currentY = mapY(last7[i].amount, last7[i].isIncome)
                        points.add(Offset(currentX, currentY))
                    }

                    // Draw curves
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val controlX = (p0.x + p1.x) / 2f
                        path.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                        fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                    }

                    fillPath.lineTo(width, height)
                    fillPath.close()

                    // Draw fill gradient and stroke
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(CyanGlow.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(listOf(RadiantPurple, ElectricPink, CyanGlow)),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw circles on point nodes with dynamic colors
                    points.forEachIndexed { index, offset ->
                        val item = last7[index]
                        val nodeColor = if (item.isIncome) CyanGlow else ElectricPink
                        drawCircle(
                            color = CardBackground,
                            radius = 6.dp.toPx(),
                            center = offset
                        )
                        drawCircle(
                            color = nodeColor,
                            radius = 4.dp.toPx(),
                            center = offset
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).background(CyanGlow, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ingresos", fontSize = 11.sp, color = SoftText)
                Spacer(modifier = Modifier.width(20.dp))
                Box(modifier = Modifier.size(8.dp).background(ElectricPink, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gastos", fontSize = 11.sp, color = SoftText)
            }
        }
    }
}

val chartColors = listOf(
    Color(0xFFFF5252), // Alimentos - Coral Red
    Color(0xFF40C4FF), // Transporte - Light Blue
    Color(0xFFFFD740), // Servicios - Soft Gold
    Color(0xFFE040FB), // Compras - Radiant Violet / Magenta
    Color(0xFF69F0AE), // Entretenimiento - Sea Green
    Color(0xFFFF8A80), // Salud - Soft Salmon
    Color(0xFFB0BEC5), // Otros - Cool Grey
    Color(0xFF9575CD), // Purple alternative
    Color(0xFF4DB6AC)  // Teal alternative
)

@Composable
fun CategoryDistributionChart(
    expenses: List<ExpenseEntry>,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    val expenseList = expenses.filter { !it.isIncome }
    if (expenseList.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay gastos registrados en este periodo.",
                fontSize = 12.sp,
                color = SoftText
            )
        }
        return
    }

    val totalsByCategory = expenseList.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
    val maxSpent = totalsByCategory.values.maxOrNull() ?: 1.0
    val totalExpenseSum = totalsByCategory.values.sum()

    var showPieChart by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 Distribución de Gastos",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricPink
            )

            // Dynamic view selector (Bar vs Pie Toggle)
            Row(
                modifier = Modifier
                    .width(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!showPieChart) RadiantPurple else Color.Transparent)
                        .clickable { showPieChart = false }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Barras", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (showPieChart) RadiantPurple else Color.Transparent)
                        .clickable { showPieChart = true }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Torta %", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        if (!showPieChart) {
            // Bars list representation
            totalsByCategory.forEach { (category, amount) ->
                val percentage = if (totalExpenseSum > 0) (amount / totalExpenseSum * 100).toInt() else 0
                val barRatio = (amount / maxSpent).toFloat()

                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "${currencySymbol.trim()} ${String.format(java.util.Locale.US, "%,.2f", amount)} ($percentage%)",
                            fontSize = 11.sp,
                            color = SoftText
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barRatio)
                                .fillMaxHeight()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(RadiantPurple, ElectricPink)
                                    )
                                )
                        )
                    }
                }
            }
        } else {
            // Pie/Donut Chart representation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Donut Canvas
                Canvas(
                    modifier = Modifier
                        .size(120.dp)
                        .weight(1f)
                ) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    var startAngle = -90f

                    totalsByCategory.toList().forEachIndexed { index, (_, amount) ->
                        val sweepAngle = ((amount / totalExpenseSum) * 360f).toFloat()
                        val sliceColor = chartColors[index % chartColors.size]

                        drawArc(
                            color = sliceColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = Size(radius * 2f, radius * 2f),
                            topLeft = Offset(center.x - radius, center.y - radius)
                        )

                        // Slice divider
                        drawArc(
                            color = CardBackground,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = Size(radius * 2f, radius * 2f),
                            topLeft = Offset(center.x - radius, center.y - radius),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        startAngle += sweepAngle
                    }

                    // Central hole
                    drawCircle(
                        color = CardBackground,
                        radius = radius * 0.5f,
                        center = center
                    )
                }

                // Legend List
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    totalsByCategory.toList().forEachIndexed { index, (category, amount) ->
                        val percentage = if (totalExpenseSum > 0) (amount / totalExpenseSum * 100).toInt() else 0
                        val sliceColor = chartColors[index % chartColors.size]

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(sliceColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = category,
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$percentage%",
                                fontSize = 11.sp,
                                color = SoftText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
