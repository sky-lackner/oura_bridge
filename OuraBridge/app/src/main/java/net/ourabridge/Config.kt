package net.ourabridge

import android.content.Context
import androidx.health.connect.client.records.metadata.Device

/** Package name the Oura app writes to Health Connect under. */
const val OURA_PACKAGE = "com.ouraring.oura"

/** How far back to look for Oura sleep on each sync. */
const val LOOKBACK_DAYS = 3L

/**
 * The device the mirrored sleep sessions claim to come from. Pokémon Sleep may check
 * this device info in addition to (or instead of) the writing app, so pick one it supports.
 */
enum class DevicePreset(val label: String, val device: Device) {
    GALAXY_RING("Samsung Galaxy Ring", Device(type = Device.TYPE_RING, manufacturer = "Samsung", model = "Galaxy Ring")),
    GALAXY_WATCH("Samsung Galaxy Watch", Device(type = Device.TYPE_WATCH, manufacturer = "Samsung", model = "Galaxy Watch7")),
    PIXEL_WATCH("Google Pixel Watch", Device(type = Device.TYPE_WATCH, manufacturer = "Google", model = "Pixel Watch 3")),
    FITBIT("Fitbit", Device(type = Device.TYPE_FITNESS_BAND, manufacturer = "Fitbit", model = "Charge 6")),
    OURA("Oura Ring (unchanged)", Device(type = Device.TYPE_RING, manufacturer = "Oura", model = "Oura Ring"));
}

object Prefs {
    private const val FILE = "bridge"
    private fun p(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun preset(ctx: Context): DevicePreset =
        runCatching { DevicePreset.valueOf(p(ctx).getString("preset", null)!!) }
            .getOrDefault(DevicePreset.GALAXY_RING)

    fun setPreset(ctx: Context, v: DevicePreset) = p(ctx).edit().putString("preset", v.name).apply()

    fun lastLog(ctx: Context): String = p(ctx).getString("log", "Not synced yet.")!!
    fun setLastLog(ctx: Context, v: String) = p(ctx).edit().putString("log", v).apply()
}
