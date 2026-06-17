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

    val stats = repository.scanStats

    private val _results = MutableStateFlow<List<ScanResult>>(emptyList())
    val results = _results.asStateFlow()

    init {
        viewModelScope.launch {
            repository.scanResults.collect { result ->
                _results.update { (it + result).sortedBy { r -> r.latencyMs } }
            }
        }
    }

    fun startScan() {
        _results.value = emptyList()
        repository.startScan()
    }

    fun stopScan() {
        repository.stopScan()
    }

    fun getResultsForCopy(top10Only: Boolean): String {
        val list = if (top10Only) results.value.take(10) else results.value
        return list.joinToString("\n") { it.link }
    }
}
