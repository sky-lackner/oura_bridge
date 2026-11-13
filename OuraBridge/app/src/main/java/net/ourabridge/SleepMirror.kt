package net.ourabridge

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val REQUIRED_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getWritePermission(SleepSessionRecord::class),
    HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
)

/**
 * Reads sleep sessions that the Oura app wrote to Health Connect and writes copies
 * owned by this app, tagged with the chosen device preset.
 *
 * Re-running is safe: each copy's clientRecordId is derived from the Oura record ID,
 * so Health Connect updates the existing copy instead of making a duplicate.
 */
class SleepMirror(private val ctx: Context) {
    private val client = HealthConnectClient.getOrCreate(ctx)

    suspend fun hasPermissions(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(REQUIRED_PERMISSIONS)

    suspend fun sync(): String {
        if (!hasPermissions()) return "Missing Health Connect permissions. Open the app and tap Grant."

        val since = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS)
        val ouraSessions = readAll(since)
        if (ouraSessions.isEmpty()) {
            return stamp("No Oura sleep found in the last $LOOKBACK_DAYS days. " +
                "Check that the Oura app is allowed to write Sleep in Health Connect.")
        }

        val device = Prefs.preset(ctx).device
        val copies = ouraSessions.map { s ->
            SleepSessionRecord(
                startTime = s.startTime,
                startZoneOffset = s.startZoneOffset,
                endTime = s.endTime,
                endZoneOffset = s.endZoneOffset,
                title = s.title,
                notes = s.notes,
                stages = s.stages,
                metadata = Metadata.autoRecorded(
                    device = device,
                    clientRecordId = "oura-${s.metadata.id}",
                    // Bump when the source changes so the copy is refreshed.
                    clientRecordVersion = s.metadata.lastModifiedTime.toEpochMilli(),
                ),
            )
        }
        client.insertRecords(copies)

        val fmt = DateTimeFormatter.ofPattern("EEE MMM d HH:mm").withZone(ZoneId.systemDefault())
        val lines = ouraSessions.joinToString("\n") {
            "• ${fmt.format(it.startTime)} → ${fmt.format(it.endTime)}  (${it.stages.size} stages)"
        }
        return stamp("Mirrored ${copies.size} session(s) as ${Prefs.preset(ctx).label}:\n$lines")
    }

    private suspend fun readAll(since: Instant): List<SleepSessionRecord> {
        val out = mutableListOf<SleepSessionRecord>()
        var token: String? = null
        do {
            val resp = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(since),
                    dataOriginFilter = setOf(DataOrigin(OURA_PACKAGE)),
                    pageToken = token,
                )
            )
            out += resp.records
            token = resp.pageToken
        } while (token != null)
        return out
    }

    private fun stamp(msg: String): String {
        val now = DateTimeFormatter.ofPattern("MMM d HH:mm").withZone(ZoneId.systemDefault()).format(Instant.now())
        return "[$now] $msg".also { Prefs.setLastLog(ctx, it) }
    }
}
