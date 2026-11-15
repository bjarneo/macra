package app.macra.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import app.macra.data.WeightEntry
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Two-way weight sync with Health Connect. */
object Health {
    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    fun status(ctx: Context): Int = HealthConnectClient.getSdkStatus(ctx)

    fun available(ctx: Context): Boolean = status(ctx) == HealthConnectClient.SDK_AVAILABLE

    private fun client(ctx: Context) = HealthConnectClient.getOrCreate(ctx)

    suspend fun granted(ctx: Context): Boolean =
        available(ctx) && client(ctx).permissionController.getGrantedPermissions().containsAll(PERMISSIONS)

    /** Returns the last weight of each day, for the given number of days. */
    suspend fun readWeights(ctx: Context, days: Long): List<WeightEntry> {
        val zone = ZoneId.systemDefault()
        val since = Instant.now().minus(days, ChronoUnit.DAYS)
        val out = mutableMapOf<Long, Pair<Instant, Double>>()
        var token: String? = null
        do {
            val res = client(ctx).readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(since),
                    pageToken = token,
                ),
            )
            res.records.forEach { r ->
                val day = r.time.atZone(zone).toLocalDate().toEpochDay()
                val cur = out[day]
                if (cur == null || r.time.isAfter(cur.first)) out[day] = r.time to r.weight.inKilograms
            }
            token = res.pageToken
        } while (token != null)
        return out.map { (day, v) -> WeightEntry(day, Math.round(v.second * 10) / 10.0) }.sortedBy { it.day }
    }

    suspend fun writeWeight(ctx: Context, day: Long, kg: Double) {
        val zone = ZoneId.systemDefault()
        val d = LocalDate.ofEpochDay(day)
        val time = if (d == LocalDate.now()) Instant.now() else d.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant()
        val record = WeightRecord(
            time = time,
            zoneOffset = zone.rules.getOffset(time),
            weight = Mass.kilograms(kg),
            metadata = Metadata.manualEntry(),
        )
        client(ctx).insertRecords(listOf(record))
    }
}
