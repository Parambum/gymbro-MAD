package com.gymbro.mad.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

enum class WeightUnit(val label: String, val suffix: String) {
    KG("Kilograms", "kg"),
    LB("Pounds", "lb");

    fun fromKilograms(kg: Double): Double = if (this == KG) kg else kg * 2.2046226218
    fun toKilograms(value: Double): Double = if (this == KG) value else value / 2.2046226218
    fun format(kg: Double): String = String.format(Locale.US, "%.1f", fromKilograms(kg)).removeSuffix(".0")
}

val LocalWeightUnit = staticCompositionLocalOf { WeightUnit.KG }
