package com.baxailab.cadebot.robot

import android.app.Activity
import android.graphics.PixelFormat
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Hosts the whole Cadebot UI in a window that outranks the Cruzr Assistant.
 *
 * The robot's launcher floats its wake-up microphone in a `TYPE_SYSTEM_ALERT`
 * window owned by `com.ubtechinc.cruzr.behavior`. An ordinary activity window
 * sits far below that, which is why the button paints over the app and steals
 * the taps underneath it.
 *
 * The first attempt at fixing this painted a small opaque patch over the button.
 * It worked, but a patch hides whatever the app had drawn there — the corner of
 * the "Hỏi Cadebot" card on Home, the quantity control on the detail screen —
 * on every screen. Covering the button without losing app pixels is only
 * possible if the *app itself* is the thing on top.
 *
 * So the Compose tree is attached to a full-screen `TYPE_SYSTEM_ERROR` window
 * (layer 24) instead of the activity's own window (layer 2). Cadebot then paints
 * above the microphone and receives every touch in its bounds, and no part of
 * the UI is masked out.
 *
 * The activity is still the lifecycle, ViewModel, saved-state and back-press
 * owner — only the window the views live in changes — so Hilt, Navigation and
 * the payment flow behave exactly as before.
 *
 * [tryInstall] returns `null` when the overlay cannot be created (a phone
 * without the permission, a stricter OS). Callers fall back to a normal
 * `setContent`, which loses the layering but keeps the app working.
 *
 * Nothing here disables `com.ubtrobot.service.speech`,
 * `com.ubtrobot.skill.launcher`, `com.ubtechinc.cruzr.mini.launcher` or
 * `com.ubtechinc.cruzr.behavior`.
 */
class CruzrKioskWindow private constructor(
    private val activity: Activity,
    private val root: View
) {

    companion object {
        private const val TAG = "CadebotCruzr"

        fun tryInstall(activity: ComponentActivity, content: @Composable () -> Unit): CruzrKioskWindow? {
            val compose = ComposeView(activity)
            // The overlay window owns the focus, so BACK arrives here rather than
            // at the activity. Hand it to the same dispatcher Navigation listens on.
            val host = object : FrameLayout(activity) {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        activity.onBackPressedDispatcher.onBackPressed()
                        return true
                    }
                    return super.dispatchKeyEvent(event)
                }
            }
            host.addView(
                compose,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            // Compose resolves these by walking up to the window's root view, so
            // they have to sit on the host — setting them only on the ComposeView
            // throws "ViewTreeLifecycleOwner not found" when it attaches.
            listOf<View>(host, compose).forEach { view ->
                view.setViewTreeLifecycleOwner(activity)
                view.setViewTreeViewModelStoreOwner(activity)
                view.setViewTreeSavedStateRegistryOwner(activity)
                view.setViewTreeOnBackPressedDispatcherOwner(activity)
            }
            compose.setContent(content)

            @Suppress("DEPRECATION")
            val params = WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                PixelFormat.OPAQUE
            ).apply {
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }

            val windowManager = activity.getSystemService(Activity.WINDOW_SERVICE) as WindowManager
            return runCatching {
                windowManager.addView(host, params)
                Log.i(TAG, "Cadebot UI hosted in a TYPE_SYSTEM_ERROR window — it now paints above the Cruzr wake-up button")
                CruzrKioskWindow(activity, host)
            }.getOrElse {
                Log.w(
                    TAG,
                    "could not host the UI above the Cruzr Assistant " +
                        "(${it.javaClass.simpleName}: ${it.message}) — falling back to a normal activity window"
                )
                null
            }
        }
    }

    fun remove() {
        val windowManager = activity.getSystemService(Activity.WINDOW_SERVICE) as WindowManager
        runCatching { windowManager.removeViewImmediate(root) }
            .onFailure { Log.w(TAG, "could not remove the kiosk window: ${it.message}") }
    }
}
