package it.curzel.tama.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import tama.composeapp.generated.resources.*

@Composable
fun firaCodeFontFamily() = FontFamily(
    Font(Res.font.FiraCode_Regular, FontWeight.Normal),
    Font(Res.font.FiraCode_Light, FontWeight.Light),
    Font(Res.font.FiraCode_Medium, FontWeight.Medium),
    Font(Res.font.FiraCode_SemiBold, FontWeight.SemiBold),
    Font(Res.font.FiraCode_Bold, FontWeight.Bold)
)
