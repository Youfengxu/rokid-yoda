package com.rokidyoda.havoice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rokid.sprite.aiapp.externalapp.auth.AuthResult
import com.rokid.sprite.aiapp.externalapp.auth.AuthorizationHelper
import com.rokid.sprite.aiapp.externalapp.auth.GlassPermission
import kotlinx.coroutines.launch

/**
 * HA-Voice (CustomApp mode). A two-finger tap on the glasses touchpad starts listening;
 * the glasses mic streams to the phone, which transcribes (Vosk), calls the orchestrator
 * (/run → ha_control), shows the reply on the glasses HUD, and reads it aloud.
 * The on-screen "Talk" button is a fallback for the same flow. See docs/HA-VOICE.md.
 */
class MainActivity : ComponentActivity() {

    private val session = GlassSession(
        onEvent = { ev -> runOnUiThread { status = ev } },
        onPtt = { runOnUiThread { startListening() } },   // two-finger tap on the glasses
    )
    private lateinit var speech: SpeechInput
    private lateinit var glassMic: GlassMic
    private var tts: Tts? = null
    private val history = mutableListOf<Turn>()

    private val usingGlassesMic get() = Config.MIC_SOURCE == Config.MicSource.GLASSES

    // ---- UI state ----
    private var status by mutableStateOf("Not authorized")
    private var transcript by mutableStateOf("")
    private var reply by mutableStateOf("")
    private var authed by mutableStateOf(false)
    private var listening by mutableStateOf(false)
    private var busy by mutableStateOf(false)

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) status = "Microphone permission denied"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val onPartial: (String) -> Unit = { runOnUiThread { transcript = it } }
        val onResult: (String) -> Unit = { text -> runOnUiThread { listening = false; onUtterance(text) } }
        val onErr: (String) -> Unit = { msg -> runOnUiThread { listening = false; status = msg } }

        speech = SpeechInput(this, onPartial, onResult, onErr)
        glassMic = GlassMic(this, session, onPartial, onResult, onErr)

        if (usingGlassesMic) {
            glassMic.loadModel()
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        if (Config.TTS_ENABLED) tts = Tts(this)

        setContent { HaVoiceScreen() }
    }

    // Tap-to-talk (from the glasses two-finger tap or the fallback button). Auto-stops on silence.
    private fun startListening() {
        if (listening || busy) return
        listening = true
        transcript = ""
        status = "Listening…"
        session.sendStatus("Listening…")
        if (usingGlassesMic) glassMic.start() else speech.start()
    }

    // ---- Auth: launches the Rokid AI companion app, token returns via onActivityResult ----
    private fun authorize() {
        val immediate = AuthorizationHelper.requestAuthorization(
            this, arrayOf(GlassPermission.MICROPHONE), Config.REQUEST_CODE_AUTH,
        )
        immediate?.let { handleAuthResult(it.first, it.second) }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Config.REQUEST_CODE_AUTH) handleAuthResult(resultCode, data)
    }

    private fun handleAuthResult(resultCode: Int, data: Intent?) {
        when (val r = AuthorizationHelper.parseAuthorizationResult(resultCode, data)) {
            is AuthResult.AuthSuccess -> {
                if (r.token.isNotBlank()) {
                    authed = true
                    status = "Authorized — connecting to glasses…"
                    session.connect(this, r.token)
                } else status = "Authorization returned empty token"
            }
            is AuthResult.AuthFail -> status = "Authorization failed"
            is AuthResult.AuthCancel -> status = "Authorization cancelled"
        }
    }

    // ---- Voice → orchestrator → glasses HUD ----
    private fun onUtterance(text: String) {
        transcript = text
        busy = true
        status = "Thinking…"
        session.sendStatus("Thinking…")
        lifecycleScope.launch {
            val answer = Orchestrator.run(text, history.toList())
            history += Turn("user", text)
            history += Turn("assistant", answer)
            trimHistory()
            reply = answer
            busy = false
            status = "Done"
            session.sendReply(answer)          // render on the glasses HUD
            tts?.speak(answer)                 // and read aloud (through the glasses over BT)
        }
    }

    private fun trimHistory() {
        val max = Config.MAX_HISTORY_TURNS * 2
        while (history.size > max) history.removeAt(0)
    }

    override fun onDestroy() {
        speech.destroy()
        glassMic.destroy()
        tts?.shutdown()
        session.release()
        super.onDestroy()
    }

    // ---- UI (phone is a control panel; the glasses show the HUD) ----
    @Composable
    private fun HaVoiceScreen() {
        val ready by session.ready.collectAsState()
        val appReady by session.appReady.collectAsState()
        val canTalk = ready && appReady && !busy && (!usingGlassesMic || glassMic.isModelReady)

        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("HA Voice — Rokid", style = MaterialTheme.typography.headlineSmall)
                    StatusRow("Authorized", authed)
                    StatusRow("Glasses linked", ready)
                    StatusRow("Glasses app running", appReady)
                    Text(status, style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(4.dp))

                    if (!authed) {
                        Button(onClick = { authorize() }, Modifier.fillMaxWidth()) {
                            Text("Authorize with Rokid AI app")
                        }
                    }

                    Text(
                        "Two-finger tap the glasses touchpad to talk. Auto-stops when you pause.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = { startListening() },
                        enabled = canTalk,
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                    ) {
                        Text(if (listening) "Listening…" else "Talk (fallback)")
                    }

                    if (transcript.isNotEmpty()) LabeledBox("You said", transcript)
                    if (reply.isNotEmpty()) LabeledBox("Reply (on HUD)", reply)

                    if (authed) {
                        OutlinedButton(onClick = {
                            session.release(); authed = false; reply = ""; transcript = ""
                            status = "Disconnected"
                        }, Modifier.fillMaxWidth()) { Text("Disconnect") }
                    }

                    Text(
                        "Endpoint: ${Config.ORCHESTRATOR_URL} · mic: ${if (usingGlassesMic) "glasses" else "phone"}" +
                            if (Config.TTS_ENABLED) " · reply read aloud" else "",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }

    @Composable
    private fun StatusRow(label: String, ok: Boolean) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (ok) "✅ " else "⬜ ")
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }

    @Composable
    private fun LabeledBox(label: String, body: String) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
