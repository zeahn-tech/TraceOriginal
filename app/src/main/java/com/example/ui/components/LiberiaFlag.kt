package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LiberiaFlag(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 38.dp, height = 20.dp).clip(RoundedCornerShape(2.dp))) {
        val flagWidth = size.width
        val flagHeight = size.height
        val stripeHeight = flagHeight / 11f
        
        for (i in 0 until 11) {
            val color = if (i % 2 == 0) Color(0xFFD32F2F) else Color.White
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(0f, i * stripeHeight),
                size = androidx.compose.ui.geometry.Size(flagWidth, stripeHeight)
            )
        }
        
        val cantonSize = stripeHeight * 5
        drawRect(
            color = Color(0xFF0F47AF),
            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(cantonSize, cantonSize)
        )
        
        val centerX = cantonSize / 2f
        val centerY = cantonSize / 2f
        val outerRadius = cantonSize * 0.3f
        val innerRadius = outerRadius * 0.4f
        
        val path = androidx.compose.ui.graphics.Path().apply {
            var angle = -Math.PI / 2
            val nextAngle = Math.PI / 5
            moveTo(
                (centerX + outerRadius * Math.cos(angle)).toFloat(),
                (centerY + outerRadius * Math.sin(angle)).toFloat()
            )
            for (i in 1..10) {
                angle += nextAngle
                val r = if (i % 2 == 0) outerRadius else innerRadius
                lineTo(
                    (centerX + r * Math.cos(angle)).toFloat(),
                    (centerY + r * Math.sin(angle)).toFloat()
                )
            }
            close()
        }
        drawPath(path = path, color = Color.White)
    }
}
