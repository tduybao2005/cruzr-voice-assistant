package com.baxailab.cadebot.robot

import android.content.Context
import android.util.Log

/**
 * Hides only the green Assistant wake-up microphone that the Cruzr system
 * launcher floats above every app, so it cannot be mistaken for Cadebot's own
 * microphone or steal the mic while [com.baxailab.cadebot.ui.ai.SttService] is
 * recording.
 *
 * The call is made through reflection on purpose:
 *
 *  - `cruzr-sdk-2.8.0.jar` is UBTECH-licensed and is not redistributed with this
 *    handover package, so the project must still compile without it.
 *  - When the jar is dropped into `app/libs/` it is picked up by the `fileTree`
 *    dependency in `app/build.gradle.kts`, and this helper starts working with
 *    no code change.
 *  - On a phone, or on a Cruzr image without the SDK, every branch below simply
 *    logs and returns.
 *
 * Equivalent to the documented SDK call:
 *
 * ```
 * AssistantManager.get(context)
 *     .showOrHidePart(AssistantManager.TYPE_HIDE_PART_WAKEUP)
 * ```
 *
 * Nothing here touches `com.ubtrobot.service.speech`,
 * `com.ubtrobot.skill.launcher`, `com.ubtechinc.cruzr.mini.launcher` or
 * `com.ubtechinc.cruzr.behavior` — those system services stay running.
 */
object CruzrAssistant {

    private const val TAG = "CadebotCruzr"
    private const val ASSISTANT_MANAGER = "com.ubtrobot.assistant.AssistantManager"

    /** Extra class names tried in order, for SDK builds that moved the class. */
    private val CANDIDATES = listOf(
        ASSISTANT_MANAGER,
        "com.ubtechinc.cruzr.sdk.assistant.AssistantManager",
        "com.ubtrobot.master.assistant.AssistantManager"
    )

    fun hideSystemWakeupButton(context: Context) {
        val managerClass = CANDIDATES.firstNotNullOfOrNull { name ->
            runCatching { Class.forName(name) }.getOrNull()
        }
        if (managerClass == null) {
            Log.i(TAG, "Cruzr SDK absent (no AssistantManager on the classpath) — leaving the system wake-up button visible")
            return
        }

        val result = runCatching {
            val manager = managerClass
                .getMethod("get", Context::class.java)
                .invoke(null, context.applicationContext)
                ?: error("AssistantManager.get() returned null")

            val hideWakeup = managerClass.getField("TYPE_HIDE_PART_WAKEUP").getInt(null)
            managerClass
                .getMethod("showOrHidePart", Int::class.javaPrimitiveType)
                .invoke(manager, hideWakeup)
            hideWakeup
        }

        result
            .onSuccess { Log.i(TAG, "hid the Assistant wake-up button via ${managerClass.name} (TYPE_HIDE_PART_WAKEUP=$it)") }
            .onFailure { Log.w(TAG, "AssistantManager found but showOrHidePart failed: ${it.javaClass.simpleName}: ${it.message}") }
    }
}
