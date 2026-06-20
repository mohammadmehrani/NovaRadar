package com.example.data.repository

import com.example.data.dao.IpSourceDao
import com.example.data.dao.PortConfigDao
import com.example.data.dao.ScanHistoryDao
import com.example.data.model.IpSource
import com.example.data.model.PortConfig
import com.example.data.model.ScanHistory
import kotlinx.coroutines.flow.Flow

class NovaRadarRepository(
    private val ipSourceDao: IpSourceDao,
    private val portConfigDao: PortConfigDao,
    private val scanHistoryDao: ScanHistoryDao
) {
    val allIpSources: Flow<List<IpSource>> = ipSourceDao.getAllIpSources()
    val allPortConfigs: Flow<List<PortConfig>> = portConfigDao.getAllPortConfigs()
    val allHistory: Flow<List<ScanHistory>> = scanHistoryDao.getAllHistory()

    suspend fun insertIpSource(source: IpSource) = ipSourceDao.insertIpSource(source)
    suspend fun updateIpSource(source: IpSource) = ipSourceDao.updateIpSource(source)
    suspend fun deleteIpSource(source: IpSource) = ipSourceDao.deleteIpSource(source)
    suspend fun getEnabledSources(): List<IpSource> = ipSourceDao.getEnabledSources()

    suspend fun insertPortConfig(port: PortConfig) = portConfigDao.insertPortConfig(port)
    suspend fun updatePortConfig(port: PortConfig) = portConfigDao.updatePortConfig(port)
    suspend fun getEnabledPorts(): List<PortConfig> = portConfigDao.getEnabledPorts()

    suspend fun insertHistory(history: ScanHistory) = scanHistoryDao.insertHistory(history)
    suspend fun clearHistory() = scanHistoryDao.clearHistory()
}
