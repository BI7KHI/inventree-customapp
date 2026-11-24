package xyz.bi7khi.inventree.mobile.data.label

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class BlePrinterDevice(
    val address: String,
    val name: String,
    val rssi: Int,
)

sealed interface BlePrinterState {
    data object Idle : BlePrinterState
    data object Scanning : BlePrinterState
    data class Connecting(val name: String) : BlePrinterState
    data class Discovering(val name: String) : BlePrinterState
    data class Connected(val name: String, val address: String) : BlePrinterState
    data class Printing(val progress: Float) : BlePrinterState
    data class Error(val message: String) : BlePrinterState
}

/**
 * Native BLE transport for ESC/POS and TSPL2 label printers.
 *
 * The web plugin discovers services/characteristics at runtime and writes
 * acknowledged chunks. This class does the same on Android: it selects the
 * first writable GATT characteristic, requests a larger MTU and throttles
 * writes so printers with small receive buffers do not silently drop data.
 */
@SuppressLint("MissingPermission")
class BlePrinterManager(context: Context) {

    private val appContext = context.applicationContext
    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val writeMutex = Mutex()

    private val _devices = MutableStateFlow<List<BlePrinterDevice>>(emptyList())
    val devices: StateFlow<List<BlePrinterDevice>> = _devices.asStateFlow()

    private val _state = MutableStateFlow<BlePrinterState>(BlePrinterState.Idle)
    val state: StateFlow<BlePrinterState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var pendingWrite: CompletableDeferred<Boolean>? = null
    private var connectedName: String = ""
    private var connectedAddress: String = ""
    @Volatile private var negotiatedMtu: Int = 23

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = runCatching { device.name }.getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: "未命名 BLE 设备"
            val item = BlePrinterDevice(device.address, name, result.rssi)
            _devices.value = (_devices.value.filterNot { it.address == item.address } + item)
                .sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            _state.value = BlePrinterState.Error(scanErrorMessage(errorCode))
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _state.value = BlePrinterState.Error("蓝牙连接失败（GATT $status）")
                closeGatt()
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedName = runCatching { gatt.device.name }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: "BLE 打印机"
                connectedAddress = gatt.device.address
                _state.value = BlePrinterState.Discovering(connectedName)
                runCatching { gatt.discoverServices() }
                    .onFailure { _state.value = BlePrinterState.Error("无法发现打印机服务") }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (_state.value !is BlePrinterState.Error) {
                    _state.value = BlePrinterState.Idle
                }
                writeCharacteristic = null
                pendingWrite?.complete(false)
                pendingWrite = null
                closeGatt()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _state.value = BlePrinterState.Error("发现打印机服务失败（$status）")
                return
            }
            val characteristic = chooseWritableCharacteristic(gatt)
            if (characteristic == null) {
                _state.value = BlePrinterState.Error("未找到可写入的 BLE 特征，请确认设备是打印机")
                return
            }
            writeCharacteristic = characteristic
            negotiatedMtu = 23
            runCatching { gatt.requestMtu(512) }
            _state.value = BlePrinterState.Connected(connectedName, connectedAddress)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            pendingWrite?.complete(status == BluetoothGatt.GATT_SUCCESS)
            pendingWrite = null
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                negotiatedMtu = mtu.coerceAtLeast(23)
            }
        }
    }

    fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        }
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun startScan() {
        if (adapter == null) {
            _state.value = BlePrinterState.Error("本机不支持蓝牙")
            return
        }
        if (!hasPermissions()) {
            _state.value = BlePrinterState.Error("缺少蓝牙扫描权限")
            return
        }
        if (!adapter.isEnabled) {
            _state.value = BlePrinterState.Error("蓝牙未开启，请先在系统设置中打开蓝牙")
            return
        }
        val bluetoothAdapter = adapter ?: run {
            _state.value = BlePrinterState.Error("本机不支持蓝牙")
            return
        }
        stopScan()
        _devices.value = emptyList()
        _state.value = BlePrinterState.Scanning
        runCatching { bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback) }
            .onFailure { _state.value = BlePrinterState.Error("无法启动 BLE 扫描：${it.message}") }
        scanJob = scope.launch {
            delay(15_000)
            if (_state.value is BlePrinterState.Scanning) stopScan()
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        if (_state.value is BlePrinterState.Scanning) {
            _state.value = BlePrinterState.Idle
        }
    }

    fun connect(address: String) {
        if (adapter == null) {
            _state.value = BlePrinterState.Error("蓝牙不可用")
            return
        }
        if (!hasPermissions()) {
            _state.value = BlePrinterState.Error("缺少蓝牙连接权限")
            return
        }
        if (!adapter.isEnabled) {
            _state.value = BlePrinterState.Error("蓝牙未开启，请先在系统设置中打开蓝牙")
            return
        }
        val bluetoothAdapter = adapter ?: return
        stopScan()
        closeGatt()
        val device = runCatching { bluetoothAdapter.getRemoteDevice(address) }.getOrNull()
        if (device == null) {
            _state.value = BlePrinterState.Error("无法打开该蓝牙设备")
            return
        }
        connectedAddress = address
        connectedName = runCatching { device.name }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: "BLE 打印机"
        _state.value = BlePrinterState.Connecting(connectedName)
        runCatching {
            gatt = device.connectGatt(
                appContext,
                false,
                gattCallback,
                BluetoothDeviceTransport.LE,
            )
        }.onFailure {
            _state.value = BlePrinterState.Error("连接失败：${it.message}")
        }
    }

    fun disconnect() {
        runCatching { gatt?.disconnect() }
        closeGatt()
        _state.value = BlePrinterState.Idle
    }

    suspend fun print(
        bitmap: Bitmap,
        settings: LabelPrintSettings,
        widthMm: Float,
        heightMm: Float,
    ) {
        val characteristic = writeCharacteristic
            ?: throw IllegalStateException("请先连接 BLE 打印机")
        val gatt = gatt ?: throw IllegalStateException("蓝牙连接已断开")
        val payload = LabelProtocolEncoder.encode(bitmap, settings, widthMm, heightMm)
        _state.value = BlePrinterState.Printing(0f)
        try {
            writeAll(gatt, characteristic, payload, settings)
            _state.value = BlePrinterState.Connected(connectedName, connectedAddress)
        } catch (error: Exception) {
            _state.value = BlePrinterState.Error(error.message ?: "打印失败")
            throw error
        }
    }

    suspend fun testFeed() {
        val characteristic = writeCharacteristic
            ?: throw IllegalStateException("请先连接 BLE 打印机")
        val gatt = gatt ?: throw IllegalStateException("蓝牙连接已断开")
        writeAll(gatt, characteristic, byteArrayOf(0x1B, 0x4A, 100), LabelPrintSettings())
    }

    private suspend fun writeAll(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        payload: ByteArray,
        settings: LabelPrintSettings,
    ) {
        writeMutex.withLock {
            val chunkSize = settings.writeChunkSize
                .coerceIn(16, 512)
                .coerceAtMost((negotiatedMtu - 3).coerceAtLeast(20))
            val canWrite = characteristic.properties and
                BluetoothGattCharacteristic.PROPERTY_WRITE != 0
            val writeType = if (canWrite) {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            var offset = 0
            while (offset < payload.size) {
                val end = minOf(offset + chunkSize, payload.size)
                val chunk = payload.copyOfRange(offset, end)
                if (canWrite) {
                    val deferred = CompletableDeferred<Boolean>()
                    pendingWrite = deferred
                    try {
                        writeCharacteristic(gatt, characteristic, chunk, writeType)
                        val success = withTimeout(5_000) { deferred.await() }
                        if (!success) throw IllegalStateException("打印机写入超时或被拒绝")
                    } finally {
                        if (pendingWrite === deferred) pendingWrite = null
                    }
                } else {
                    writeCharacteristic(gatt, characteristic, chunk, writeType)
                    delay(35)
                }
                offset = end
                _state.value = BlePrinterState.Printing(offset.toFloat() / payload.size)
                if (offset < payload.size) {
                    delay(settings.writeChunkDelayMs.coerceAtLeast(0).toLong())
                }
            }
        }
    }

    private suspend fun writeCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        writeType: Int,
    ) = withContext(Dispatchers.Main.immediate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = gatt.writeCharacteristic(characteristic, data, writeType)
            if (result != 0) {
                pendingWrite?.complete(false)
                throw IllegalStateException("BLE 写入请求失败（$result）")
            }
        } else {
            @Suppress("DEPRECATION")
            characteristic.writeType = writeType
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            if (!gatt.writeCharacteristic(characteristic)) {
                pendingWrite?.complete(false)
                throw IllegalStateException("BLE 写入请求失败")
            }
        }
    }

    private fun chooseWritableCharacteristic(gatt: BluetoothGatt): BluetoothGattCharacteristic? {
        val candidates = gatt.services
            .flatMap { it.characteristics }
            .filter {
                it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                    it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
            }
        if (candidates.isEmpty()) return null
        // Common thermal printer write characteristics first, then any writable one.
        val preferred = listOf(
            UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("000018f0-0000-1000-8000-00805f9b34fb"),
        )
        return candidates.minByOrNull { characteristic ->
            val index = preferred.indexOf(characteristic.uuid)
            if (index >= 0) index else 100
        }
    }

    private fun closeGatt() {
        runCatching { gatt?.close() }
        gatt = null
        writeCharacteristic = null
        pendingWrite?.complete(false)
        pendingWrite = null
    }

    fun close() {
        stopScan()
        disconnect()
    }

    private fun scanErrorMessage(code: Int): String = when (code) {
        ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "扫描已在进行中"
        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "蓝牙扫描注册失败"
        ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "蓝牙扫描内部错误"
        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "设备不支持 BLE 扫描"
        else -> "蓝牙扫描失败（$code）"
    }

    private object BluetoothDeviceTransport {
        const val LE = 2 // BluetoothDevice.TRANSPORT_LE
    }
}

object LabelProtocolEncoder {

    fun encode(
        bitmap: Bitmap,
        settings: LabelPrintSettings,
        widthMm: Float,
        heightMm: Float,
    ): ByteArray {
        val protocol = settings.protocol.uppercase()
        val copies = settings.copies.coerceIn(1, 9)
        if (protocol == "TSPL2") {
            val one = tspl(bitmap, settings, widthMm, heightMm)
            return (1..copies).fold(ByteArray(0)) { acc, _ -> acc + one }
        }
        val init = byteArrayOf(0x1B, 0x40)
        val one = escPosBody(bitmap, settings)
        return init + (1..copies).fold(ByteArray(0)) { acc, _ -> acc + one }
    }

    private fun escPosBody(bitmap: Bitmap, settings: LabelPrintSettings): ByteArray {
        val dotsX = bitmap.width
        val dotsY = bitmap.height
        val bytesPerRow = (dotsX + 7) / 8
        val raster = rasterize(bitmap, settings)
        val header = byteArrayOf(
            0x1D, 0x76, 0x30, 0x00, // GS v 0 m=0
            (bytesPerRow and 0xFF).toByte(),
            ((bytesPerRow shr 8) and 0xFF).toByte(),
            (dotsY and 0xFF).toByte(),
            ((dotsY shr 8) and 0xFF).toByte(),
        )
        return header + raster + feed(settings.feedMm, settings.dpi)
    }

    private fun tspl(
        bitmap: Bitmap,
        settings: LabelPrintSettings,
        widthMm: Float,
        heightMm: Float,
    ): ByteArray {
        val bytesPerRow = (bitmap.width + 7) / 8
        val raster = rasterize(bitmap, settings)
        val header = "SIZE $widthMm mm,$heightMm mm\r\n" +
            "GAP 2 mm,0 mm\r\n" +
            "DIRECTION 1\r\n" +
            "CLS\r\n" +
            "BITMAP 0,0,$bytesPerRow,${bitmap.height},0,"
        return header.toByteArray(Charsets.UTF_8) + raster +
            "\r\nPRINT 1,1\r\n".toByteArray(Charsets.UTF_8)
    }

    private fun rasterize(bitmap: Bitmap, settings: LabelPrintSettings): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = (width + 7) / 8
        val out = ByteArray(bytesPerRow * height)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val threshold = settings.threshold.coerceIn(0, 255)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = pixels[y * width + x]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                val luminance = (r * 299 + g * 587 + b * 114) / 1000
                var black = luminance < threshold
                if (settings.invert) black = !black
                if (black) {
                    out[y * bytesPerRow + (x shr 3)] =
                        (out[y * bytesPerRow + (x shr 3)].toInt() or (0x80 shr (x and 7))).toByte()
                }
            }
        }
        return out
    }

    private fun feed(feedMm: Float, dpi: Int): ByteArray {
        if (feedMm <= 0f) return byteArrayOf(0x0C) // FF: use printer gap sensor
        var dots = (feedMm / 25.4f * dpi).toInt().coerceAtLeast(1)
        val parts = mutableListOf<ByteArray>()
        while (dots > 0) {
            val n = minOf(dots, 255)
            parts += byteArrayOf(0x1B, 0x4A, n.toByte())
            dots -= n
        }
        return parts.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
    }
}
