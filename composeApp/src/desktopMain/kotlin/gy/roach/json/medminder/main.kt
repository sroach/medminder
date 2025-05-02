package gy.roach.json.medminder

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import java.io.File
import java.io.InputStream

@OptIn(ExperimentalResourceApi::class)
fun main() = application {

    // Load the icon from the resources directory
    val iconFile = File("composeApp/src/desktopMain/resources/medminder.png")
    val icon = if (iconFile.exists()) {
        BitmapPainter(iconFile.inputStream().readAllBytes().decodeToImageBitmap())
    } else {
        null
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "MedMinder",
        icon = icon
    ) {
        App()
    }
}
