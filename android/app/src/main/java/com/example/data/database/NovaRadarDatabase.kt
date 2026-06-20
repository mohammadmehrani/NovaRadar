package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.IpSourceDao
import com.example.data.dao.PortConfigDao
import com.example.data.dao.ScanHistoryDao
import com.example.data.model.IpSource
import com.example.data.model.PortConfig
import com.example.data.model.ScanHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
                .addCallback(NovaRadarDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class NovaRadarDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: NovaRadarDatabase) {
            // Default Ports
            val defaultPorts = listOf(
                PortConfig(443, true),
                PortConfig(2053, true),
                PortConfig(2083, true),
                PortConfig(2087, true),
                PortConfig(2096, true),
                PortConfig(8443, true),
                PortConfig(80, false),
                PortConfig(2052, false),
                PortConfig(2082, false),
                PortConfig(2086, false),
                PortConfig(2095, false),
                PortConfig(8080, false)
            )
            val portDao = db.portConfigDao()
            defaultPorts.forEach { portDao.insertPortConfig(it) }

            // Default IP Sources / Subnets (Cloudflare ranges used in desktop app)
            val defaultSources = listOf(
                IpSource(1, "Cloudflare Official", "کلودفلر رسمی", "173.245.48.0/20", "https://www.cloudflare.com/ips-v4/", "cidr", true),
                IpSource(2, "CM List", "لیست سی‌ام", "103.21.244.0/22", "https://raw.githubusercontent.com/cmliu/cmliu/main/CF-CIDR.txt", "cidr", true),
                IpSource(3, "AS13335 (Cloudflare)", "کلودفلر AS13335", "104.16.0.0/13", "https://raw.githubusercontent.com/ipverse/asn-ip/master/as/13335/ipv4-aggregated.txt", "cidr", false),
                IpSource(4, "AS209242 (Cloudflare)", "کلودفلر AS209242", "172.64.0.0/13", "https://raw.githubusercontent.com/ipverse/asn-ip/master/as/209242/ipv4-aggregated.txt", "cidr", false),
                IpSource(5, "AS24429 (Alibaba)", "علی بابا AS24429", "", "https://raw.githubusercontent.com/ipverse/asn-ip/master/as/24429/ipv4-aggregated.txt", "cidr", false),
                IpSource(6, "AS199524 (G-Core)", "جی-کور AS199524", "", "https://raw.githubusercontent.com/ipverse/asn-ip/master/as/199524/ipv4-aggregated.txt", "cidr", false),
                IpSource(7, "Reverse Proxy IPs", "آی‌پی‌های پروکسی معکوس", "", "https://raw.githubusercontent.com/cmliu/ACL4SSR/main/baipiao.txt", "proxyip", false),
                IpSource(8, "Foreign Domains", "دامنه‌های خارجی", "", "https://raw.githubusercontent.com/Blacknuno/Nova-Proxy/refs/heads/main/dominos.text", "domain", false),
                IpSource(9, "Iranian Domains", "دامنه‌های ایرانی", "", "https://raw.githubusercontent.com/Blacknuno/Nova-Proxy/refs/heads/main/IRdominos.text", "domain", false)
            )
            val ipSourceDao = db.ipSourceDao()
            defaultSources.forEach { ipSourceDao.insertIpSource(it) }
        }
    }
}
