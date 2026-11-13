package net.ourabridge

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private val mirror by lazy { SleepMirror(this) }

    private val permLauncher =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) {
            refresh()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }

        col.addView(TextView(this).apply {
            text = "Copies Oura sleep in Health Connect into sessions Pokémon Sleep can read.\n\nPretend device:"
        })
        col.addView(Spinner(this).apply {
            val presets = DevicePreset.entries
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, presets.map { it.label })
            setSelection(presets.indexOf(Prefs.preset(this@MainActivity)))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) =
                    Prefs.setPreset(this@MainActivity, presets[pos])
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        })
        col.addView(Button(this).apply {
            text = "Grant Health Connect access"
            setOnClickListener { permLauncher.launch(REQUIRED_PERMISSIONS) }
        })
        col.addView(Button(this).apply {
            text = "Sync now"
            setOnClickListener {
                status.text = "Syncing…"
                lifecycleScope.launch {
                    status.text = runCatching { mirror.sync() }.getOrElse { "Sync failed: ${it.message}" }
                }
            }
        })
        status = TextView(this).apply { setPadding(0, pad, 0, 0) }
        col.addView(status)

        setContentView(ScrollView(this).apply { addView(col) })
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        if (HealthConnectClient.getSdkStatus(this) != HealthConnectClient.SDK_AVAILABLE) {
            status.text = "Health Connect isn't available on this phone."
            return
        }
        lifecycleScope.launch {
            if (mirror.hasPermissions()) {
                SyncWorker.schedule(this@MainActivity)
                status.text = "Access granted. Background sync runs hourly.\n\nLast result:\n${Prefs.lastLog(this@MainActivity)}"
            } else {
                status.text = "Tap \"Grant Health Connect access\" and allow everything (including background access)."
            }
        }
    }
}

/** Shown by Health Connect when the user asks how this app uses their data. */
class PrivacyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        setContentView(TextView(this).apply {
            setPadding(pad, pad, pad, pad)
            text = "Oura Bridge reads sleep sessions written by the Oura app and writes copies back to " +
                "Health Connect on this phone. Nothing leaves your device."
        })
    }
}
