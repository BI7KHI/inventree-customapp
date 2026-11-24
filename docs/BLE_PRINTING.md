# BLE 标签打印

Android 端把本地 `ble-label-printer` 插件的核心能力移植为原生 BLE 实现。网页端依赖 Web Bluetooth，移动端使用 Android BluetoothGatt。

## 流程

```text
详情页 → 打印标签页
  ├─ 加载对象数据 + 服务器条码
  ├─ LabelRenderer 渲染 40×30mm 位图（203/300 dpi）
  ├─ 扫描/连接 BLE 打印机
  └─ LabelProtocolEncoder → BLE GATT 分包写入
```

打印页的「编辑版式」进入 `LabelEditorScreen`，保存后通过 Lifecycle ON_RESUME 自动刷新预览。

## 支持的协议

### ESC/POS

- 整个任务只发送一次 `ESC @`；
- 每张标签使用 `GS v 0` 光栅位图；
- 走纸 0mm 时发送 `FF`，由打印机间隙传感器定位下一张；
- 走纸 > 0 时发送 `ESC J n`。

### TSPL2

- `SIZE w mm,h mm`
- `GAP 2 mm,0 mm`
- `DIRECTION 1` / `CLS`
- `BITMAP 0,0,bytesPerRow,rows,0,<raster>`
- `PRINT 1,1`

## GATT 传输

- 扫描所有 BLE 广播设备，显示名称/地址/RSSI；
- 连接后自动发现服务，选择第一个 `WRITE` 或 `WRITE_NO_RESPONSE` 特征；
- 优先常见热敏打印机写特征（`FF02`、`FFE1`、`18F0`）；
- 连接后请求 MTU 512；写入块大小取 `min(配置值, MTU-3)`，默认 23 MTU 时退化为 20 字节；
- 有 `WRITE` 属性时使用有应答写入并等待 `onCharacteristicWrite`，5 秒超时；
- 只有 `WRITE_NO_RESPONSE` 时使用无应答写入，并加 35ms 间隔；
- 每块之间可配置延迟，默认 15ms。

## 权限

- Android 12+：`BLUETOOTH_SCAN`（neverForLocation）、`BLUETOOTH_CONNECT`
- Android 8–11：`BLUETOOTH`、`BLUETOOTH_ADMIN`、`ACCESS_FINE_LOCATION`
- 蓝牙关闭、权限拒绝、无 writable 特征都会在打印页显示明确错误。

## 打印参数

| 参数 | 默认 | 说明 |
|---|---|---|
| 协议 | ESC/POS | ESC/POS 或 TSPL2 |
| DPI | 203 | 影响位图尺寸，300dpi 需打印机支持 |
| 份数 | 1 | 1–9 |
| 走纸 | 0 | 0=FF 间隙，>0=固定 ESC J 距离 |
| 阈值 | 128 | 灰度二值化 |
| 反色 | 关 | 白底黑字/黑底白字 |
| 块大小 | 180 | 实际会按 MTU 限制 |
| 块间延迟 | 15ms | 慢速打印机可调大 |

## 常见问题

### 只打印第一张或后续截断

- 确保没有把 `ESC @` 插入到每张标签之间；本实现的 ESC/POS 整个任务只复位一次。
- 增大块间延迟（例如 30–50ms）。
- 降低块大小（例如 100 字节）。
- 打印机接收缓冲极小时，优先使用 TSPL2，或改用有应答特征。

### 连接后提示“未找到可写入的 BLE 特征”

- 设备可能不是打印机，或使用厂商私有 GATT；
- 用 nRF Connect 查看设备的 service/characteristic；
- 当前版本不硬编码厂商 UUID，只自动选择 `WRITE`/`WRITE_NO_RESPONSE` 特征。

### 打印内容偏移/裁切

- 在打印页调整走纸；
- 标签设计器中调整纸张宽高和元素位置；
- 203dpi 下 40mm 宽约 319 像素，打印头实际宽度可能不同。

## 与网页插件的关系

网页插件 `ble_label_printer` 通过浏览器 Web Bluetooth 发送同样的 ESC/POS/TSPL2 指令，并保留校准参数、日志和指令流导出。Android 端复用了其图像二值化、`GS v 0`、TSPL2 `BITMAP`、块大小/延迟和复位策略，但不依赖浏览器或 Web Bluetooth。

## 测试状态

已测试：

- 标签位图渲染（非空、尺寸）；
- ESC/POS 复位只出现一次、位图指令头、FF；
- TSPL2 头尾；
- Android BLE 权限与扫描 UI。

未测试：

- 真实 BLE 打印机的 GATT 写入、MTU 协商、走纸和打印浓度。需要真机验证。
