package com.baxailab.cadebot.robot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.baxailab.cadebot.MainActivity

/**
 * Brings Cadebot up on its own once the robot has finished booting.
 *
 * Declaring `android.intent.category.HOME` on [MainActivity] already lets the
 * kiosk be *chosen* as the robot's launcher, but that only takes effect if a
 * staff member picks Cadebot in the home-app chooser. This receiver covers the
 * common case where the Cruzr launcher stays the default and Cadebot should
 * still be on screen when the shift starts.
 *
 * Both broadcasts are handled: the standard `BOOT_COMPLETED`, and
 * `com.ubtech.cruzr.BOOTED`, which the Cruzr firmware sends once its own
 * services are up — on this robot that is the later, more reliable of the two.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CadebotCruzr"
        private const val CRUZR_BOOTED = "com.ubtech.cruzr.BOOTED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != CRUZR_BOOTED) return

        Log.i(TAG, "boot broadcast '$action' received — starting Cadebot")
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        runCatching { context.startActivity(launch) }
            .onFailure { Log.w(TAG, "could not start Cadebot after boot: ${it.message}") }
    }
}
