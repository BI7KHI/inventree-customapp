# InvenTree Mobile 1.2.0

面向 `https://erp.bi7khi.xyz` 的原生 Android 客户端，本次更新聚焦 BLE 实际打印校准和个性化主题。

## 重点更新

### BLE 水平/垂直偏移校准

- 打印页新增水平偏移、垂直偏移，范围 **-15mm ～ +15mm**，**0.5mm** 步进。
- 正值向右/向下，负值向左/向上；偏移作用于整张标签（含边框）。
- 打印预览即时显示偏移结果，实际打印使用同一份位图。
- 偏移与协议、DPI、份数、走纸、阈值、反色、BLE 块大小、块间延迟一起保存。
- “保存参数”后重启 App、下次打印继续使用；“偏移归零”一键恢复。

建议真机流程：连接打印机 → 测试走纸 → 打印 1 张 → 按 0.5mm 微调 → 保存参数。

### 自定义应用主题色

“我的 → 外观 → 主题颜色”：

- 12 个常用色块：森林绿、青碧、海洋蓝、靛青、葡萄紫、玫瑰红、珊瑚橙、琥珀、石墨、松柏、赭石、午夜。
- 自定义 HSV 取色：色相条、饱和度/明度面板、十六进制输入。
- 主题即时应用到导航、按钮、首页/登录渐变、强调元素和状态色。
- DataStore 持久化，重启保留，可一键恢复默认。

## 安装

1. 下载本 Release 的 `InvenTree-Mobile-1.2.0.apk`。
2. 允许“安装未知应用”后安装，或：

```bash
adb install -r InvenTree-Mobile-1.2.0.apk
```

3. 首次启动使用默认服务器 `https://erp.bi7khi.xyz`，用网页端同一账户登录。SSO/2FA 账户可切换 API Token 登录。

可直接覆盖 1.1.0 / 1.0.0（同一签名，versionCode 3）。

## 校验

```text
SHA-256: 9c716b97c2b0a460db6bfded6695037fc1a611335dcdab24aa3fa4bf77d6ec48
Package: xyz.bi7khi.inventree.mobile
Version: 1.2.0 (versionCode 3)
Min Android: 8.0 / API 26
```

## 测试

- JVM 单元测试 19 项通过。
- Android 仪器测试 4 项通过：标签位图、偏移渲染、ESC/POS、TSPL2。
- Android Lint 0 error。
- 模拟器验证：登录、缩略图、折叠库位、标签打印预览、设计器、偏移保存重启保持、主题切换重启保持。

## 已知限制

- 真实 BLE 热敏打印机的 GATT 写入/走纸仍需真机验证；本机没有 BLE 打印机。
- 支持 ESC/POS 与 TSPL2，不支持经典蓝牙 SPP 或厂商私有协议。
- 标签编辑器目前支持文本、二维码、分隔线，不支持图片/Logo、一维码和自由旋转。

完整变更见 [CHANGELOG](https://github.com/BI7KHI/inventree-customapp/blob/main/CHANGELOG.md)。
