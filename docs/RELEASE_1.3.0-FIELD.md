# InvenTree Mobile 1.3.0-field

**Field Style 预发布版本。**

这是 `end` 分支视觉改造合并到 `main` 后的首个预览版。功能与 1.2.1 对齐，主要变化是完整的工业编辑风 / 近未来现场终端界面和交互。

> 预发布用于真机视觉验证，不建议替换生产环境中的稳定版 1.2.1。

## Field Style

- 暖纸白 / 碳黑中性底，信号黄与工业青作为唯一高饱和强调。
- 细网格、切角、四角括号、警示斜纹、HUD 微标签、扫描线与状态脉冲。
- 首页、登录 / 节点接入、库存与库位、零件、详情、表单、标签打印 / 设计器、我的页面统一重绘。
- 底部导航、页面转场、卡片按压、统计数字、Hero 扫描线、扫描准星动效。
- 自定义几何品牌图标与启动页；显示字体 Oxanium（OFL-1.1）。

## 主题

- 四组主信号色：谷地黄、武陵青、重燃赤、浸蓝。
- 12 组工业 / 材质色与 HSV 自定义取色、HEX 输入。
- 亮色 / 暗色共用中性色，只切换信号与选中态。
- 信号色、打印参数、账户会话继续使用本机持久化；旧自定义主题色保留。

## 版权说明

本版本没有使用人物立绘、官方 Logo、游戏截图或官方字体。页面中的图标、纹理、切角框线与 HUD 元素均为本项目原创绘制，仅借鉴公开的工业编辑风设计方法。

参考：

- [endfield.pryphline.com](https://endfield.pryphline.com)
- [endfield-ui 设计规范（非官方）](https://icaruszezen.github.io/endfield-ui/design/)

本项目与鹰角网络 / Hypergryph 无隶属或授权关系。

## 安装

- 文件：`InvenTree-Mobile-1.3.0-field.apk`
- 版本：`1.3.0-field`（versionCode 5）
- 包名：`xyz.bi7khi.inventree.mobile`
- 系统：Android 8.0+（API 26），targetSdk 35
- SHA-256：`164ff49efed6493b1af90d82ee81becf11e0ae692ca7a20e8fb90feb25a83f07`
- 与 1.2.1 同签名，可直接覆盖安装；保留应用数据。

## 验证

- 19 项 JVM 单元测试通过。
- 5 项仪器测试通过。
- Android Lint：0 errors，2 warnings。
- 模拟器 1080×2400 验证亮色、暗色、登录、库存、零件、我的、信号色弹层与启动页。
- 签名 APK 构建、安装、启动验证通过。
- BLE 协议代码未改；实体打印机本轮未重跑。

## 说明

- 这是 GitHub Pre-release，`latest` 仍指向稳定版 1.2.1。
- 完整设计说明：[`docs/FIELD_STYLE.md`](FIELD_STYLE.md)。
- 稳定版、历史版本和 main 分支保持可回退。
