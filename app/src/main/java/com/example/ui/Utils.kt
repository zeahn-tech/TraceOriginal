package com.example.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow

fun String.toTraceNetLiberiaAnnotatedString(liberiaColor: Color = Color.White): AnnotatedString {
    val text = this
    return buildAnnotatedString {
        var currentIndex = 0
        val target = "TraceNet Liberia"
        while (currentIndex < text.length) {
            val index = text.indexOf(target, currentIndex)
            if (index == -1) {
                append(text.substring(currentIndex))
                break
            }
            append(text.substring(currentIndex, index))
            withStyle(SpanStyle(color = Color.Red)) {
                append("TraceNet ")
            }
            withStyle(SpanStyle(color = liberiaColor, shadow = Shadow(color = Color.Black, blurRadius = 4f))) {
                append("Liberia")
            }
            currentIndex = index + target.length
        }
    }
}
