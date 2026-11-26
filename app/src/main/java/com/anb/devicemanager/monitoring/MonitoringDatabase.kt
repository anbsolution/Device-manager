package com.anb.devicemanager.monitoring

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "device_snapshots")
data class DeviceSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val ramUsedPercent: Int,
    val storageUsedPercent: Int,
    val batteryPercent: Int,
    val batteryTemperature: Float,
    val charging: Boolean,
    val thermalStatus: String
)

@Dao
interface DeviceSnapshotDao {
    @Insert
    suspend fun insert(snapshot: DeviceSnapshot)

    @Query("SELECT * FROM device_snapshots ORDER BY timestamp DESC LIMIT 100")
    suspend fun getRecent(): List<DeviceSnapshot>

    @Query("SELECT COUNT(*) FROM device_snapshots")
    suspend fun getCount(): Int

    @Query("SELECT * FROM device_snapshots WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getSince(since: Long): List<DeviceSnapshot>

    @Query("SELECT * FROM device_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): DeviceSnapshot?
}

@Database(
    entities = [DeviceSnapshot::class],
    version = 1,
    exportSchema = false
)
abstract class MonitoringDatabase : RoomDatabase() {
    abstract fun snapshotDao(): DeviceSnapshotDao

    companion object {
        @Volatile private var INSTANCE: MonitoringDatabase? = null

        fun get(context: Context): MonitoringDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MonitoringDatabase::class.java,
                    "anb_device_monitoring.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
