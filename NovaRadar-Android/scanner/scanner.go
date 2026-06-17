package scanner

import (
	"crypto/rand"
	"crypto/tls"
	"encoding/binary"
	"fmt"
	"io"
	"math/big"
	"net"
	"strconv"
	"sync"
	"sync/atomic"
	"time"
)

var tlsPorts = map[int]bool{
	443: true, 2053: true, 2083: true, 2087: true, 2096: true, 8443: true,
}

type ScanResult struct {
	IP        string `json:"ip"`
	Port      int    `json:"port"`
	Link      string `json:"link"`
	LatencyMs int64  `json:"latencyMs"`
}

type ProgressCallback interface {
	OnProgress(scanned int64, total int64, alive int32, dead int32, currentIP string, currentPort int, secondPass bool)
	OnResult(ip string, port int, link string, latencyMs int64)
	OnComplete()
	OnError(message string)
}

type scannerState struct {
	scanning     atomic.Bool
	stopScan     chan struct{}
	stopOnce     sync.Once
	totalScanned int64
	totalToScan  int64
	aliveCount   int32
	deadCount    int32
	currentIP    string
	currentPort  int
	secondPass   bool
	scanIPs      []string
	callback     ProgressCallback
}

func StartScan(sourcesJSON string, portsJSON string, callback ProgressCallback) {
	s := &scannerState{
		stopScan: make(chan struct{}),
		callback: callback,
	}
	s.scanning.Store(true)

	currentMu.Lock()
	currentScan = s
	currentMu.Unlock()

	go func() {
		cidrs, directIPs := fetchIPsFromJSON(sourcesJSON)
		ips := generateRandomIPs(cidrs, 512)
		ips = append(ips, directIPs...)

		if len(ips) == 0 {
			if callback != nil {
				callback.OnError("No IPs loaded from sources")
			}
			s.scanning.Store(false)
			return
		}

		atomic.StoreInt64(&s.totalToScan, int64(len(ips)*len(parsePorts(portsJSON)))*2)
		s.scanIPs = ips

		s.runScan(parsePorts(portsJSON))
	}()
}

var (
	currentMu   sync.Mutex
	currentScan *scannerState
)

// StopScan stops the scan that is currently running, if any.
// Takes no arguments so gomobile can export it cleanly and the
// caller never has to (and never can) pass an unexported *scannerState.
func StopScan() {
	currentMu.Lock()
	s := currentScan
	currentMu.Unlock()
	if s == nil || !s.scanning.Load() {
		return
	}
	s.scanning.Store(false)
	s.stopOnce.Do(func() { close(s.stopScan) })
}

func (s *scannerState) runScan(ports []int) {
	atomic.StoreInt64(&s.totalScanned, 0)
	defer func() {
		s.scanning.Store(false)
		if s.callback != nil {
			s.callback.OnComplete()
		}
	}()

	candidates := s.quickScan(ports)
	if !s.scanning.Load() || len(candidates) == 0 {
		return
	}

	s.secondPass = true
	s.deepTest(candidates)
	s.secondPass = false
}

func (s *scannerState) quickScan(ports []int) []ScanResult {
	timeout := 2 * time.Second
	maxConcurrent := 100

	workers := make(chan struct{}, maxConcurrent)
	var mu sync.Mutex
	var wg sync.WaitGroup
	candidates := make([]ScanResult, 0)

	ips := s.scanIPs
	offset := uint32(0)
	if len(ips) > 1 {
		n, err := rand.Int(rand.Reader, big.NewInt(int64(len(ips))))
		if err == nil {
			offset = uint32(n.Int64())
		}
	}

	for i := 0; i < len(ips); i++ {
		select {
		case <-s.stopScan:
			return candidates
		default:
		}

		ip := ips[(int(offset)+i)%len(ips)]

		for _, port := range ports {
			select {
			case <-s.stopScan:
				return candidates
			default:
			}

			s.currentIP = ip
			s.currentPort = port
			atomic.AddInt64(&s.totalScanned, 1)

			select {
			case <-s.stopScan:
				return candidates
			case workers <- struct{}{}:
			}

			wg.Add(1)
			go func(ip string, port int) {
				defer func() {
					<-workers
					wg.Done()
				}()

				start := time.Now()
				if tryConnect(ip, port, timeout) {
					latency := time.Since(start).Milliseconds()
					result := ScanResult{
						IP:        ip,
						Port:      port,
						Link:      fmt.Sprintf("%s:%d#Nova-%s", ip, port, generateNovaID()),
						LatencyMs: latency,
					}
					mu.Lock()
					candidates = append(candidates, result)
					mu.Unlock()
					atomic.AddInt32(&s.aliveCount, 1)
				} else {
					atomic.AddInt32(&s.deadCount, 1)
				}
				s.emitProgress()
			}(ip, port)
		}
	}

	done := make(chan struct{})
	go func() {
		wg.Wait()
		close(done)
	}()
	select {
	case <-done:
	case <-s.stopScan:
	}
	return candidates
}

func (s *scannerState) deepTest(candidates []ScanResult) {
	maxConcurrent := 50
	workers := make(chan struct{}, maxConcurrent)
	var mu sync.Mutex
	var wg sync.WaitGroup

	type verified struct {
		result  ScanResult
		success int
		latency int64
	}

	vlist := make([]verified, len(candidates))
	for i, c := range candidates {
		vlist[i] = verified{result: c}
	}

	for idx := range vlist {
		select {
		case <-s.stopScan:
			goto finish
		default:
		}

		s.currentIP = vlist[idx].result.IP
		s.currentPort = vlist[idx].result.Port

		for attempt := 0; attempt < 3; attempt++ {
			select {
			case <-s.stopScan:
				goto finish
			default:
			}

			atomic.AddInt64(&s.totalScanned, 1)
			workers <- struct{}{}
			wg.Add(1)

			go func(idx, attempt int) {
				defer func() {
					<-workers
					wg.Done()
				}()

				start := time.Now()
				ok := deepTestConnect(vlist[idx].result.IP, vlist[idx].result.Port)
				lat := time.Since(start).Milliseconds()

				mu.Lock()
				if ok {
					vlist[idx].success++
					if vlist[idx].latency == 0 || lat < vlist[idx].latency {
						vlist[idx].latency = lat
					}
				} else {
					atomic.AddInt32(&s.deadCount, 1)
				}
				mu.Unlock()
				s.emitProgress()
			}(idx, attempt)
		}
	}

finish:
	done := make(chan struct{})
	go func() {
		wg.Wait()
		close(done)
	}()
	select {
	case <-done:
	case <-s.stopScan:
	}

	newAlive := int32(0)
	for _, v := range vlist {
		if v.success >= 2 {
			v.result.LatencyMs = v.latency
			v.result.Link = fmt.Sprintf("%s:%d#Nova-%s", v.result.IP, v.result.Port, generateNovaID())
			newAlive++
			if s.callback != nil {
				s.callback.OnResult(v.result.IP, v.result.Port, v.result.Link, v.result.LatencyMs)
			}
		}
	}
	atomic.StoreInt32(&s.aliveCount, newAlive)
}

func (s *scannerState) emitProgress() {
	if s.callback != nil {
		total := atomic.LoadInt64(&s.totalToScan)
		scanned := atomic.LoadInt64(&s.totalScanned)
		s.callback.OnProgress(
			scanned, total,
			atomic.LoadInt32(&s.aliveCount),
			atomic.LoadInt32(&s.deadCount),
			s.currentIP, s.currentPort, s.secondPass,
		)
	}
}

func deepTestConnect(ip string, port int) bool {
	addr := net.JoinHostPort(ip, strconv.Itoa(port))
	timeout := 3 * time.Second

	if tlsPorts[port] {
		conn, err := net.DialTimeout("tcp", addr, timeout)
		if err != nil {
			return false
		}
		tlsConn := tls.Client(conn, &tls.Config{
			ServerName:         "nova2.altramax083.workers.dev",
			InsecureSkipVerify: true,
			MinVersion:         tls.VersionTLS12,
		})
		tlsConn.SetDeadline(time.Now().Add(timeout))
		if err := tlsConn.Handshake(); err != nil {
			conn.Close()
			return false
		}
		tlsConn.Close()
		return true
	}

	conn, err := net.DialTimeout("tcp", addr, timeout)
	if err != nil {
		return false
	}
	conn.SetDeadline(time.Now().Add(timeout))
	buf := make([]byte, 1)
	_, err = io.ReadFull(conn, buf)
	conn.Close()
	return err == nil
}

func tryConnect(ip string, port int, timeout time.Duration) bool {
	conn, err := net.DialTimeout("tcp", net.JoinHostPort(ip, strconv.Itoa(port)), timeout)
	if err != nil {
		return false
	}
	conn.Close()
	return true
}

func parseCIDR(cidr string) (uint32, uint32, error) {
	_, ipnet, err := net.ParseCIDR(cidr)
	if err != nil {
		return 0, 0, err
	}
	ones, bits := ipnet.Mask.Size()
	start := ipToUint32(ipnet.IP)
	count := uint32(1) << (bits - ones)
	end := start + count - 1
	return start, end, nil
}

func ipToUint32(ip net.IP) uint32 {
	ip = ip.To4()
	if ip == nil {
		return 0
	}
	return binary.BigEndian.Uint32(ip)
}

func uint32ToIP(n uint32) net.IP {
	ip := make(net.IP, 4)
	binary.BigEndian.PutUint32(ip, n)
	return ip
}

func generateNovaID() string {
	const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
	b := make([]byte, 5)
	for i := range b {
		n, err := rand.Int(rand.Reader, big.NewInt(int64(len(chars))))
		if err != nil {
			b[i] = 'A'
			continue
		}
		b[i] = chars[n.Int64()]
	}
	return string(b)
}

func parsePorts(json string) []int {
	var ports []int
	s := json
	for i := 0; i < len(s); i++ {
		if s[i] >= '0' && s[i] <= '9' {
			n := 0
			for i < len(s) && s[i] >= '0' && s[i] <= '9' {
				n = n*10 + int(s[i]-'0')
				i++
			}
			ports = append(ports, n)
		}
	}
	if len(ports) == 0 {
		return []int{443, 2053, 2083, 2087, 2096, 8443}
	}
	return ports
}
