package com.irnova.novaradar.data.repository;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000h\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\"\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J \u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\r2\u0006\u0010\u001e\u001a\u00020\u001a2\u0006\u0010\u001f\u001a\u00020\u001aH\u0002J \u0010 \u001a\u00020!2\u0006\u0010\u001d\u001a\u00020\r2\u0006\u0010\u001e\u001a\u00020\u001a2\u0006\u0010\u001f\u001a\u00020\u001aH\u0002J\u001e\u0010\"\u001a\b\u0012\u0004\u0012\u00020\r0\f2\u0006\u0010#\u001a\u00020\r2\u0006\u0010$\u001a\u00020\u001aH\u0002J\u0006\u0010%\u001a\u00020&J\u0006\u0010\'\u001a\u00020&R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00050\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0017\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\b0\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0014\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u001a0\u0019X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006("}, d2 = {"Lcom/irnova/novaradar/data/repository/ScannerRepository;", "", "()V", "_scanResults", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/irnova/novaradar/data/model/ScanResult;", "_scanStats", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/irnova/novaradar/data/model/ScanStats;", "client", "Lokhttp3/OkHttpClient;", "fallbackCIDRs", "", "", "scanJob", "Lkotlinx/coroutines/Job;", "scanResults", "Lkotlinx/coroutines/flow/SharedFlow;", "getScanResults", "()Lkotlinx/coroutines/flow/SharedFlow;", "scanStats", "Lkotlinx/coroutines/flow/StateFlow;", "getScanStats", "()Lkotlinx/coroutines/flow/StateFlow;", "tlsPorts", "", "", "checkTcpConnection", "", "ip", "port", "timeout", "checkTlsConnection", "", "generateRandomIPs", "cidr", "count", "startScan", "", "stopScan", "app_debug"})
public final class ScannerRepository {
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableSharedFlow<com.irnova.novaradar.data.model.ScanResult> _scanResults = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.SharedFlow<com.irnova.novaradar.data.model.ScanResult> scanResults = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.irnova.novaradar.data.model.ScanStats> _scanStats = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.irnova.novaradar.data.model.ScanStats> scanStats = null;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job scanJob;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient client = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.Integer> tlsPorts = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> fallbackCIDRs = null;
    
    @javax.inject.Inject()
    public ScannerRepository() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.SharedFlow<com.irnova.novaradar.data.model.ScanResult> getScanResults() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.irnova.novaradar.data.model.ScanStats> getScanStats() {
        return null;
    }
    
    public final void startScan() {
    }
    
    public final void stopScan() {
    }
    
    private final long checkTcpConnection(java.lang.String ip, int port, int timeout) {
        return 0L;
    }
    
    private final boolean checkTlsConnection(java.lang.String ip, int port, int timeout) {
        return false;
    }
    
    private final java.util.List<java.lang.String> generateRandomIPs(java.lang.String cidr, int count) {
        return null;
    }
}