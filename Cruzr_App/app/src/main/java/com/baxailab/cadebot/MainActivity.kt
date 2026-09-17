package com.baxailab.cadebot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.baxailab.cadebot.robot.CruzrAssistant
import com.baxailab.cadebot.robot.CruzrKioskWindow
import com.baxailab.cadebot.ui.navigation.CadebotNavGraph
import com.baxailab.cadebot.ui.theme.CadebotTheme
import com.baxailab.cadebot.ui.theme.ScaledToScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var kioskWindow: CruzrKioskWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ask the Cruzr SDK to hide its wake-up button outright. No-op until
        // cruzr-sdk-2.8.0.jar is dropped into app/libs — see CruzrAssistant.
        CruzrAssistant.hideSystemWakeupButton(this)
        enableEdgeToEdge()

        // On Cruzr the UI goes into a window that outranks the launcher's
        // floating microphone; everywhere else this returns null and the app
        // uses an ordinary activity window.
        kioskWindow = CruzrKioskWindow.tryInstall(this, appContent)
        if (kioskWindow == null) {
            setContent(content = appContent)
        }
    }

    private val appContent: @Composable () -> Unit = {
        CadebotTheme {
            ScaledToScreen {
                val navController = rememberNavController()
                CadebotNavGraph(navController = navController)
            }
        }
    }

    override fun onDestroy() {
        kioskWindow?.remove()
        kioskWindow = null
        super.onDestroy()
    }
}
