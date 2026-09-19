package com.fitlog.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Escala tipografica de FitLog.
 *
 * Titulos con tracking negativo y peso alto (estilo editorial moderno) y, sobre todo, cifras
 * tabulares en los estilos numericos: los valores de peso, reps y volumen se alinean entre filas.
 */

/** Cifras tabulares: los numeros se alinean entre filas porque cada digito ocupa el mismo ancho. */
val TabularNumbers = TextStyle(
    fontFeatureSettings = "tnum",
)

private val SnugLines = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

val FitLogTypography = Typography().let { base ->
    base.copy(
        displayMedium = base.displayMedium.copy(
            fontFeatureSettings = "tnum",
            fontWeight = FontWeight.SemiBold,
        ),
        headlineLarge = base.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        titleLarge = base.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(letterSpacing = 1.2.sp, lineHeightStyle = SnugLines),
    )
}

/** Estilo de los valores numericos (peso, reps, volumen) en cifras tabulares. */
val NumericStyle: TextStyle = TabularNumbers.copy(
    fontWeight = FontWeight.SemiBold,
)
