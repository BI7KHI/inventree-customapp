# InvenTree Mobile 1.2.1

这是一个针对 BLE 水平偏移裁切问题的修复版本。

## 修复内容

### +6mm 水平偏移导致右侧空白、二维码被截断

旧版在固定 40mm 宽画布内平移标签内容。水平正偏移会把二维码等右侧内容推到画布外并裁掉。

1.2.1 改为：

- 水平正偏移采用左侧补白，位图宽度随偏移增加（+6mm → 约 46mm 输出宽度）；
- 整张标签向右移动，右侧二维码完整保留；
- 预览宽高比跟随实际输出位图；
- 负偏移向左移动；垂直偏移仍在固定标签高度内。

> 如果打印机物理打印头只有 40mm，补白部分仍可能被硬件裁切。此时需要调整打印机原点，或把内容排布在 40mm 标签范围内。

## 安装

```bash
adb install -r InvenTree-Mobile-1.2.1.apk
```

可覆盖 1.2.0 / 1.1.0 / 1.0.0（同一签名，versionCode 4）。

## 校验

```text
Package: xyz.bi7khi.inventree.mobile
Version: 1.2.1 (versionCode 4)
Min Android: 8.0 / API 26
SHA-256: 55daf7e465269463e45ecf5d77b6b9e5b2c8638c767a31c9ce203b11b9352840
```

## 测试

- 19 项 JVM 单元测试通过；
- 5 项 Android 仪器测试通过（新增 +6mm 左侧补白回归测试）；
- Android Lint 0 error；
- 模拟器验证 +6mm 时二维码完整显示，输出宽度 46mm。

完整变更见 [CHANGELOG](https://github.com/BI7KHI/inventree-customapp/blob/main/CHANGELOG.md)。
