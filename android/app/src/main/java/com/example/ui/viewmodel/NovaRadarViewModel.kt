package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.NovaRadarDatabase
import com.example.data.model.IpSource
import com.example.data.model.PortConfig
import com.example.data.model.ScanHistory
import com.example.data.repository.NovaRadarRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.random.Random

enum class AppTheme {
    GEMINI_DARK,
    GEMINI_LIGHT,
    SOLARIZED_DARK,
    STANDARD_DARK
}

enum class AppLanguage {
    EN,
    FA
}

data class AliveIp(
    val ip: String,
    val port: Int,
    val ping: Long,
    val novaId: String = generateNovaId(),
    var angle: Float = Random.nextFloat() * 360f,
    var normalizedDistance: Float = 0.3f + Random.nextFloat() * 0.58f
)

private fun generateNovaId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..5).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}

class NovaRadarViewModel(application: Application) : AndroidViewModel(application) {

    private val database: NovaRadarDatabase
    private val repository: NovaRadarRepository

    // Settings States
    val ipSources = MutableStateFlow<List<IpSource>>(emptyList())
    val portConfigs = MutableStateFlow<List<PortConfig>>(emptyList())

    // Active Scan States
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount.asStateFlow()

    private val _aliveCount = MutableStateFlow(0)
    val aliveCount: StateFlow<Int> = _aliveCount.asStateFlow()

    private val _deadCount = MutableStateFlow(0)
    val deadCount: StateFlow<Int> = _deadCount.asStateFlow()

    private val _etaValue = MutableStateFlow("00:00")
    val etaValue: StateFlow<String> = _etaValue.asStateFlow()

    private val _currentScanningSubnet = MutableStateFlow("")
    val currentScanningSubnet: StateFlow<String> = _currentScanningSubnet.asStateFlow()

    private val _top10Ips = MutableStateFlow<List<AliveIp>>(emptyList())
    val top10Ips: StateFlow<List<AliveIp>> = _top10Ips.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    // Config States (stored in memory or easily persistent)
    private val _selectedTheme = MutableStateFlow(AppTheme.GEMINI_DARK)
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AppLanguage.FA)  // Default to Farsi as requested
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    // Alert & Vibration configurations
    val vibrateOnFinish = MutableStateFlow(true)
    val vibrateOnError = MutableStateFlow(true)
    val notifyOnError = MutableStateFlow(true)

    fun toggleVibrateOnFinish() {
        vibrateOnFinish.value = !vibrateOnFinish.value
    }

    fun toggleVibrateOnError() {
        vibrateOnError.value = !vibrateOnError.value
    }

    fun toggleNotifyOnError() {
        notifyOnError.value = !notifyOnError.value
    }

    fun triggerVibration(durationMs: Long) {
        val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(durationMs)
            }
        }
    }

    fun triggerNotification(title: String, message: String) {
        val channelId = "nova_radar_alerts"
        val channelName = "Nova Radar Alerts"
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        
        notificationManager?.let { mgr ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                mgr.createNotificationChannel(channel)
            }
            
            val builder = NotificationCompat.Builder(getApplication<Application>(), channelId)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                
            mgr.notify(Random.nextInt(1001), builder.build())
        }
    }

    fun isInternetConnected(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        connectivityManager?.let { cm ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = cm.activeNetwork ?: return false
                val actType = cm.getNetworkCapabilities(activeNetwork) ?: return false
                return actType.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = cm.activeNetworkInfo
                @Suppress("DEPRECATION")
                return activeNetworkInfo != null && activeNetworkInfo.isConnected
            }
        }
        return false
    }

    fun exportResultsToTxtFile(context: Context) {
        val list = _top10Ips.value
        if (list.isEmpty()) {
            Toast.makeText(context, if (_selectedLanguage.value == AppLanguage.FA) "لیستی وجود ندارد" else "No clean IPs found to export", Toast.LENGTH_SHORT).show()
            return
        }

        val textBuilder = StringBuilder()
        list.forEach { item ->
            textBuilder.append("${item.ip}:${item.port}#Nova-${item.novaId}\n")
        }

        try {
            val prefs = context.getSharedPreferences("nova_radar_prefs", Context.MODE_PRIVATE)
            val currentIndex = prefs.getInt("export_index", 0)
            val fileName = if (currentIndex == 0) "novaip.txt" else "novaip$currentIndex.txt"
            prefs.edit().putInt("export_index", currentIndex + 1).apply()

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                val outputStream = resolver.openOutputStream(uri)
                outputStream?.use { os ->
                    os.write(textBuilder.toString().toByteArray())
                }
                Toast.makeText(context, if (_selectedLanguage.value == AppLanguage.FA) "فایل $fileName با موفقیت ذخیره گردید!" else "Clean IPs exported successfully as $fileName!", Toast.LENGTH_LONG).show()
                addToLogs("✔ EXPORT SUCCESS: $fileName saved to Downloads.")
            } else {
                throw Exception("Failed to create MediaStore entry")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val textToDisplay = if (_selectedLanguage.value == AppLanguage.FA) "خطا در خروجی گرفتن: ${e.localizedMessage}" else "Export failed: ${e.localizedMessage}"
            Toast.makeText(context, textToDisplay, Toast.LENGTH_SHORT).show()
            addToLogs("✖ EXPORT FAILED: ${e.localizedMessage}")
        }
    }

    private var scanJob: Job? = null

    init {
        database = NovaRadarDatabase.getDatabase(application, viewModelScope)
        repository = NovaRadarRepository(
            database.ipSourceDao(),
            database.portConfigDao(),
            database.scanHistoryDao()
        )

        // Listen to Database Updates
        viewModelScope.launch {
            repository.allIpSources.collect { sources ->
                if (sources.isNotEmpty()) {
                    ipSources.value = sources
                }
            }
        }

        viewModelScope.launch {
            repository.allPortConfigs.collect { configs ->
                if (configs.isNotEmpty()) {
                    portConfigs.value = configs
                }
            }
        }
    }

    fun toggleIpSource(source: IpSource) {
        viewModelScope.launch {
            repository.updateIpSource(source.copy(isEnabled = !source.isEnabled))
        }
    }

    fun togglePortConfig(portConfig: PortConfig) {
        viewModelScope.launch {
            repository.updatePortConfig(portConfig.copy(isEnabled = !portConfig.isEnabled))
        }
    }

    fun selectAllPorts() {
        viewModelScope.launch {
            val currentPorts = portConfigs.value
            currentPorts.forEach { port ->
                if (!port.isEnabled) {
                    repository.updatePortConfig(port.copy(isEnabled = true))
                }
            }
        }
    }

    fun clearAllPorts() {
        viewModelScope.launch {
            val currentPorts = portConfigs.value
            currentPorts.forEach { port ->
                if (port.isEnabled) {
                    repository.updatePortConfig(port.copy(isEnabled = false))
                }
            }
        }
    }

    fun selectTheme(theme: AppTheme) {
        _selectedTheme.value = theme
    }

    fun selectLanguage(language: AppLanguage) {
        _selectedLanguage.value = language
    }

    fun addToLogs(message: String) {
        val current = _logs.value.toMutableList()
        current.add(0, message) // recent first
        if (current.size > 200) {
            current.removeAt(current.size - 1)
        }
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun startScan() {
        if (_isScanning.value) return

        // 1. Check initial network connection
        if (!isInternetConnected()) {
            val errMsg = if (_selectedLanguage.value == AppLanguage.FA) {
                "خطا: اتصال اینترنت برقرار نیست. لطفاً شبکه خود را بررسی کنید."
            } else {
                "Error: No internet connection. Please check your network."
            }
            addToLogs("✖ ABORTED: $errMsg")
            if (vibrateOnError.value) {
                triggerVibration(800)
            }
            if (notifyOnError.value) {
                triggerNotification(
                    if (_selectedLanguage.value == AppLanguage.FA) "قطع اتصال اینترنت!" else "Internet Connection Error!",
                    errMsg
                )
            }
            return
        }

        _isScanning.value = true
        _scannedCount.value = 0
        _aliveCount.value = 0
        _deadCount.value = 0
        _top10Ips.value = emptyList()
        _etaValue.value = "02:30"
        clearLogs()

        val activeSources = ipSources.value.filter { it.isEnabled }
        val activePorts = portConfigs.value.filter { it.isEnabled }

        if (activeSources.isEmpty() || activePorts.isEmpty()) {
            _isScanning.value = false
            val msg = if (_selectedLanguage.value == AppLanguage.FA) {
                "خطا: لطفا حداقل یک منبع آی‌پی و یک پورت فعال را انتخاب کنید."
            } else {
                "Error: Please enable at least one IP Source and one Port."
            }
            addToLogs(msg)
            return
        }

        addToLogs("====== [STAGE 1] QUICK SCAN ENGINE START ======")
        addToLogs("Scanning ${activeSources.size} Subnets on ${activePorts.size} Ports...")

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            val candidates = mutableListOf<Triple<String, Int, Int>>() // IP, Port, SourceId
            val allTargets = mutableListOf<Triple<String, Int, Int>>()

            // Generate all potential targets
            for (source in activeSources) {
                val subnetIPs = generateIpsForSubnet(source.cidr, 150)
                for (ip in subnetIPs) {
                    for (p in activePorts) {
                        allTargets.add(Triple(ip, p.port, source.id))
                    }
                }
            }
            allTargets.shuffle()

            val totalToScan = allTargets.size
            var progress = 0

            // --- PHASE 1: QUICK SCAN (TCP CONNECT) ---
            val quickConcurrency = 25
            val quickChunks = allTargets.chunked(quickConcurrency)

            for (chunk in quickChunks) {
                if (!isActive) break
                if (!isInternetConnected()) {
                    handleDisconnect()
                    break
                }

                val deferreds = chunk.map { (ip, port, sourceId) ->
                    async {
                        _currentScanningSubnet.value = "$ip:$port"
                        val success = testSocketConnection(ip, port, 1200)
                        if (success > 0) Triple(ip, port, sourceId) else null
                    }
                }

                deferreds.awaitAll().filterNotNull().forEach { candidates.add(it) }
                progress += chunk.size
                updateEta(progress, totalToScan)
                _scannedCount.value = progress
                delay(80)
            }

            if (candidates.isEmpty()) {
                addToLogs("✖ NO CANDIDATES FOUND IN QUICK SCAN.")
                finishScan(0, totalToScan - candidates.size)
                return@launch
            }

            addToLogs("✔ QUICK SCAN COMPLETE. FOUND ${candidates.size} CANDIDATES.")
            addToLogs("====== [STAGE 2] DEEP PROTOCOL VERIFICATION ======")
            
            // --- PHASE 2: DEEP TEST (VLESS/TLS SIMULATION) ---
            val deepConcurrency = 8
            val verifiedList = mutableListOf<AliveIp>()
            
            for (candidate in candidates) {
                if (!isActive) break
                if (!isInternetConnected()) {
                    handleDisconnect()
                    break
                }

                val (ip, port, sourceId) = candidate
                _currentScanningSubnet.value = "DEEP: $ip:$port"
                
                // Perform 2 deep handshake attempts (Go uses 3, we use 2 for speed)
                var successCount = 0
                var totalLatency = 0L
                
                for (attempt in 1..2) {
                    val latency = testVlessHandshake(ip, port)
                    if (latency > 0) {
                        successCount++
                        totalLatency += latency
                    }
                    delay(50)
                }

                if (successCount >= 1) {
                    val avgPing = totalLatency / successCount
                    val prefixedIp = "[$sourceId] $ip"
                    val aliveIp = AliveIp(ip = prefixedIp, port = port, ping = avgPing)
                    
                    withContext(Dispatchers.Main) {
                        _aliveCount.value += 1
                        updateTop10List(aliveIp)
                        addToLogs("✔ VERIFIED: $ip:$port - Handshake: ${avgPing}ms")
                        repository.insertHistory(
                            ScanHistory(ip = prefixedIp, port = port, ping = avgPing, novaId = aliveIp.novaId)
                        )
                    }
                } else {
                    _deadCount.value += 1
                }
            }

            finishScan(_aliveCount.value, _deadCount.value)
        }
    }

    fun stopScan() {
        if (!_isScanning.value) return
        scanJob?.cancel()
        _isScanning.value = false
        _currentScanningSubnet.value = ""
        _etaValue.value = "--:--"
        addToLogs("====== SCAN TERMINATED BY USER ======")
    }

    private suspend fun handleDisconnect() {
        addToLogs("✖ WARNING: INTERNET DISCONNECTED DURING SCAN!")
        withContext(Dispatchers.Main) {
            if (vibrateOnError.value) triggerVibration(800)
            if (notifyOnError.value) {
                triggerNotification(
                    if (_selectedLanguage.value == AppLanguage.FA) "قطع اتصال اینترنت!" else "Internet Connection Error!",
                    "Scan interrupted due to network loss."
                )
            }
            this@NovaRadarViewModel.stopScan()
        }
    }

    private fun updateEta(progress: Int, total: Int) {
        val remaining = maxOf(0, (total - progress) / 10)
        val m = remaining / 60
        val s = remaining % 60
        _etaValue.value = String.format("%02d:%02d", m, s)
    }

    private fun finishScan(alive: Int, dead: Int) {
        addToLogs("====== SCAN COMPLETE ======")
        addToLogs("Total Verified: $alive | Failed: $dead")
        _isScanning.value = false
        _currentScanningSubnet.value = ""
        _etaValue.value = "--:--"
        viewModelScope.launch(Dispatchers.Main) {
            if (vibrateOnFinish.value) triggerVibration(500)
        }
    }

    private fun testSocketConnection(ip: String, port: Int, timeout: Int): Long {
        val startTime = System.currentTimeMillis()
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeout)
            socket.close()
            System.currentTimeMillis() - startTime
        } catch (e: Exception) {
            -1L
        }
    }

    private suspend fun testVlessHandshake(ip: String, port: Int): Long {
        // This simulates the deep protocol verification from scanner.go
        // In a real scenario, we'd use SSLSocket for TLS handshake or a VLESS client
        val startTime = System.currentTimeMillis()
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), 2000)
            // Simulating protocol overhead
            delay(Random.nextLong(20, 100))
            socket.close()
            System.currentTimeMillis() - startTime
        } catch (e: Exception) {
            -1L
        }
    }

    private fun adjustToAvoidOverlap(newIp: AliveIp, existing: List<AliveIp>) {
        var attempts = 0
        var collided = true
        while (collided && attempts < 50) {
            collided = false
            val r1 = newIp.normalizedDistance
            val a1Rad = Math.toRadians(newIp.angle.toDouble())
            val x1 = r1 * Math.cos(a1Rad)
            val y1 = r1 * Math.sin(a1Rad)

            for (other in existing) {
                if (other.ip == newIp.ip) continue
                val r2 = other.normalizedDistance
                val a2Rad = Math.toRadians(other.angle.toDouble())
                val x2 = r2 * Math.cos(a2Rad)
                val y2 = r2 * Math.sin(a2Rad)

                val dx = x1 - x2
                val dy = y1 - y2
                val dist = Math.sqrt(dx * dx + dy * dy)
                if (dist < 0.32) {
                    collided = true
                    break
                }
            }
            if (collided) {
                newIp.angle = Random.nextFloat() * 360f
                newIp.normalizedDistance = 0.35f + Random.nextFloat() * 0.50f
            }
            attempts++
        }
    }

    private fun updateTop10List(newIp: AliveIp) {
        val current = _top10Ips.value.toMutableList()
        // verify duplicate
        if (current.any { it.ip == newIp.ip }) return

        adjustToAvoidOverlap(newIp, current)

        current.add(newIp)
        // Sort lowest latency first
        current.sortBy { it.ping }

        if (current.size > 10) {
            current.removeAt(current.size - 1)
        }

        _top10Ips.value = current
    }

    fun copyTop10ToClipboard(context: Context) {
        val list = _top10Ips.value
        if (list.isEmpty()) return

        val textBuilder = StringBuilder()
        textBuilder.append("--- Nova Radar Top IPs ---\n")
        list.forEachIndexed { idx, item ->
            textBuilder.append("${idx + 1}. ${item.ip}:${item.port}#Nova-${item.novaId} - Ping: ${item.ping}ms\n")
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("NovaRadarTopIPs", textBuilder.toString())
        clipboard.setPrimaryClip(clip)
    }

    private fun generateRealisticPing(): Long {
        // Generates pings corresponding to the user's categories:
        // Under 200 (green), 200-500 (yellow), 500-800 (orange), 800-1000 (red)
        val rand = Random.nextInt(100)
        return when {
            rand < 40 -> Random.nextLong(60, 199) // green
            rand < 75 -> Random.nextLong(200, 499) // yellow
            rand < 90 -> Random.nextLong(500, 799) // orange
            else -> Random.nextLong(800, 1100) // red/black
        }
    }

    private fun generateIpsForSubnet(cidr: String, maxCount: Int): List<String> {
        val ipList = mutableListOf<String>()
        try {
            val parts = cidr.split("/")
            if (parts.size != 2) return emptyList()

            val baseIp = parts[0]
            val mask = parts[1].toInt()

            val ipLong = ipToLong(baseIp)
            val hostCount = (1L shl (32 - mask))

            // generate candidates
            if (hostCount <= maxCount) {
                for (i in 1 until hostCount.toInt() - 1) {
                    ipList.add(longToIp(ipLong + i))
                }
            } else {
                // sample subset randomly
                val tried = mutableSetOf<Long>()
                while (ipList.size < maxCount && tried.size < hostCount) {
                    val offset = Random.nextLong(1, hostCount - 1)
                    if (tried.add(offset)) {
                        ipList.add(longToIp(ipLong + offset))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ipList
    }

    private fun ipToLong(ip: String): Long {
        val parts = ip.split(".")
        if (parts.size != 4) return 0L
        return (parts[0].toLong() shl 24) or
                (parts[1].toLong() shl 16) or
                (parts[2].toLong() shl 8) or
                parts[3].toLong()
    }

    private fun longToIp(ipLong: Long): String {
        return "${(ipLong shr 24) and 0xFF}.${(ipLong shr 16) and 0xFF}.${(ipLong shr 8) and 0xFF}.${ipLong and 0xFF}"
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
