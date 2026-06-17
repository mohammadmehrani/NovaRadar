package com.irnova.novaradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irnova.novaradar.data.model.ScanResult
import com.irnova.novaradar.data.model.ScanStats
import com.irnova.novaradar.data.repository.ScannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ScannerRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(ScanStats())
    val stats = _stats.asStateFlow()

    private val _results = MutableStateFlow<List<ScanResult>>(emptyList())
    val results = _results.asStateFlow()

    init {
        viewModelScope.launch {
            repository.scanResults.collect { result ->
                _results.update { (it + result).sortedBy { r -> r.latencyMs } }
                _stats.update { it.copy(
                    aliveCount = it.aliveCount + 1,
                    totalScanned = it.totalScanned + 1,
                    currentIP = result.ip
                ) }
            }
        }
    }

    fun startScan() {
        _results.value = emptyList()
        _stats.update { ScanStats(scanning = true) }
        repository.startScan("1.1.1", 443) // رنج پیش‌فرض برای تست
    }

    fun stopScan() {
        repository.stopScan()
        _stats.update { it.copy(scanning = false) }
    }

    fun getResultsForCopy(top10Only: Boolean): String {
        val list = if (top10Only) results.value.take(10) else results.value
        return list.joinToString("\n") { it.link }
    }
}
