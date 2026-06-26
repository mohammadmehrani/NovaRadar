package com.novaradar.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.novaradar.app.data.dao.IpSourceDao
import com.novaradar.app.data.dao.PortConfigDao
import com.novaradar.app.data.dao.ScanHistoryDao
import com.novaradar.app.data.model.IpSource
import com.novaradar.app.data.model.PortConfig
import com.novaradar.app.data.model.ScanHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Database(
    entities = [IpSource::class, PortConfig::class, ScanHistory::class],
    version = 3,
    exportSchema = false
)
abstract class NovaRadarDatabase : RoomDatabase() {
    abstract fun ipSourceDao(): IpSourceDao
    abstract fun portConfigDao(): PortConfigDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: NovaRadarDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): NovaRadarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NovaRadarDatabase::class.java,
                    "nova_radar_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance

                // Populate defaults on first creation (Room's onCreate fires during build(),
                // but at that point INSTANCE is null, so a callback can't access it).
                scope.launch(Dispatchers.IO) {
                    if (instance.ipSourceDao().getAllIpSources().first().isEmpty()) {
                        populateDatabase(instance)
                    }
                }

                instance
            }
        }

        private suspend fun populateDatabase(db: NovaRadarDatabase) {
            val defaultPorts = listOf(
                PortConfig(80, true),
                PortConfig(443, true),
                PortConfig(2053, false),
                PortConfig(2083, false),
                PortConfig(2087, false),
                PortConfig(2096, false),
                PortConfig(8443, false),
                PortConfig(2052, false),
                PortConfig(2082, false),
                PortConfig(2086, false),
                PortConfig(2095, false),
                PortConfig(8080, false)
            )
            val portDao = db.portConfigDao()
            defaultPorts.forEach { portDao.insertPortConfig(it) }

            val defaultSources = listOf(
                IpSource(1, "Cloudflare Official 1", "کلودفلر رسمی ۱", "103.21.244.0/22", "https://www.cloudflare.com/ips-v4/", "cidr", true),
                IpSource(2, "Cloudflare Official 2", "کلودفلر رسمی ۲", "103.22.200.0/22", "https://www.cloudflare.com/ips-v4/", "cidr", true),
                IpSource(3, "Cloudflare Official 3", "کلودفلر رسمی ۳", "103.31.4.0/22", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(4, "Cloudflare Official 4", "کلودفلر رسمی ۴", "104.16.0.0/13", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(5, "Cloudflare Official 5", "کلودفلر رسمی ۵", "104.24.0.0/14", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(6, "Cloudflare Official 6", "کلودفلر رسمی ۶", "108.162.192.0/18", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(7, "Cloudflare Official 7", "کلودفلر رسمی ۷", "131.0.72.0/22", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(8, "Cloudflare Official 8", "کلودفلر رسمی ۸", "141.101.64.0/18", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(9, "Cloudflare Official 9", "کلودفلر رسمی ۹", "162.158.0.0/15", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(10, "Cloudflare Official 10", "کلودفلر رسمی ۱۰", "172.64.0.0/13", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(11, "Cloudflare Official 11", "کلودفلر رسمی ۱۱", "173.245.48.0/20", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(12, "Cloudflare Official 12", "کلودفلر رسمی ۱۲", "188.114.96.0/20", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(13, "Cloudflare Official 13", "کلودفلر رسمی ۱۳", "190.93.240.0/20", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(14, "Cloudflare Official 14", "کلودفلر رسمی ۱۴", "197.234.240.0/22", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(15, "Cloudflare Official 15", "کلودفلر رسمی ۱۵", "198.41.128.0/17", "https://www.cloudflare.com/ips-v4/", "cidr", false),
                IpSource(16, "Foreign Domains", "دامنه‌های خارجی", "", "https://raw.githubusercontent.com/Blacknuno/Nova-Proxy/refs/heads/main/dominos.text", "domain", false),
                IpSource(17, "Iranian Domains", "دامنه‌های ایرانی", "", "https://raw.githubusercontent.com/Blacknuno/Nova-Proxy/refs/heads/main/IRdominos.text", "domain", false)
            )
            val ipSourceDao = db.ipSourceDao()
            defaultSources.forEach { ipSourceDao.insertIpSource(it) }
        }
    }
}
