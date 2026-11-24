# Field Style

`end` 分支的界面实验：用**工业编辑风 + 近未来现场终端**的视觉语言重做 InvenTree Mobile。

设计参考了《明日方舟：终末地》官网及公开设计资料中可观察到的工业排版语言，例如纸白/碳黑的中性底、单一高饱和信号色、细网格、切角、警示斜纹与 HUD 微标签。**没有使用任何人物立绘、官方 Logo、游戏截图、官方字体或其它受版权保护的素材**；页面中的图形、图标、纹理、代码标签均由本项目原创绘制。

- 官方参考：[endfield.pryphline.com](https://endfield.pryphline.com)
- 本项目与鹰角网络 / Hypergryph 无隶属或授权关系。
- 设计目标是借用“工业编辑风”的方法，而不是复刻游戏界面。

## 设计原则

1. **纸与墨是主体，信号色是信号**
   大面积保持暖纸白或碳黑；黄色只用于当前选中项、关键按钮、告警或需要被看见的一处。

2. **直角与切角是机械，圆形只属于人和仪表**
   卡片、按钮、输入框、弹层使用切角；头像、状态点、进度环保留圆形。

3. **信息像技术文档一样分层**
   中文负责阅读，英文/编号负责结构：`MATERIAL CONTROL`、`S-02`、`API 530+`、`SECTOR 04`。

4. **纹理有预算**
   一屏最多叠加网格、扫描线、警示斜纹中的一到两种；不使用高噪点背景。

5. **动得短、动得硬**
   页面转场 170–280ms，卡片按压 0.985 缩放，选中态用颜色与短横线切换；不使用弹跳。

## 色板

| 角色 | 亮色 | 暗色 |
| --- | --- | --- |
| 纸底 / 背景 | `#E9E7DD` | `#0A0C0A` |
| 面板 | `#F4F2E9` | `#141813` |
| 沉底 / 次级面板 | `#DCD9CE` | `#1D221C` |
| 正文 | `#11130F` | `#F1EFE4` |
| 次要文字 | `#4E5149` | `#A8ADA2` |
| 细线 | `#BFC0B5` | `#343A32` |
| 信号黄 | `#F2E900` | `#F2E900` |
| 武陵青 | `#19D3D3` | `#19D3D3` |
| 重燃赤 / 错误 | `#B32414` | `#FF6B61` |

亮色模式下，黄色不作为正文色；正文使用同色相的深金 `#595200`。暗色模式下信号色直接使用高亮值。

## 信号色预设

“我的 → 外观 → 信号色”提供四组主预设：

| 名称 | 色值 |
| --- | --- |
| 谷地黄 | `#F2E900` |
| 武陵青 | `#19D3D3` |
| 重燃赤 | `#FF4B32` |
| 浸蓝 | `#6E63FF` |

另有 12 组工业/材质色与 HSV 自定义取色。切换只改变信号与选中态，纸白、碳黑、网格、警示斜纹保持稳定。

## 页面元素

- **FieldHero**：网格、等高线、移动扫描线、四角括号、底部警示斜纹、幽灵编号。
- **FieldCard**：切角、1px 细线、左上信号短线、四角定位刻度、按压缩放。
- **FieldSectionHeader**：方框编号 + 中文标题 + 横向细线 + 右对齐指令。
- **FieldTopBar**：状态栏安全区、返回切角按钮、中英双层标题、底部信号短线。
- **FieldBottomBar**：碳黑底、顶部信号线、选中项短横线与颜色过渡、中央扫描动作按钮、触感反馈。
- **ScanFrame**：青色外框、黄色内刻度、八宫格、移动扫描光束、中心瞄准点。
- **FieldTag / FieldPulseDot / FieldMicroLabel**：HUD 微标签、状态脉冲、等宽英文结构标签。

## 动效

| 场景 | 动效 |
| --- | --- |
| 页面切换 | 170–280ms 淡入 + 轻微横向滑入；返回反向 |
| 首页 Hero | 网格缓慢漂移、扫描线 7.2s 循环 |
| 卡片按压 | 0.985 缩放，900 stiffness spring |
| 底部导航 | 颜色 180ms、选中短线 spring、触感反馈 |
| 扫描页 | 1.8s 来回扫描光束、四角准星 |
| 数字变化 | 统计数字纵向滑动 + 淡入 |
| 信号状态 | 脉冲点 1.1s 明暗呼吸 |
| 主题弹层 | 切角容器，无系统默认圆角 |

## 实现位置

```text
ui/theme/Color.kt              色板常量
ui/theme/Theme.kt              FieldPalette + Material 3 语义映射
ui/theme/Type.kt               Oxanium 显示字体、切角 Shapes
ui/components/FieldComponents.kt
ui/components/AppComponents.kt 旧的 AppCard / 顶栏等统一适配
ui/navigation/AppNavigation.kt 页面转场、FieldBottomBar
ui/screens/HomeScreen.kt       首页 HUD
ui/screens/LoginScreen.kt      接入终端
ui/screens/ServerSetupScreen.kt 节点配置
ui/screens/StockHubScreen.kt   库存/库位
ui/screens/ScanScreen.kt       扫描 HUD
```

## 字体

显示字体使用 [Oxanium](https://github.com/sevmeyer/oxanium)，SIL Open Font License 1.1。许可证随 APK 打包于 `assets/licenses/OXANIUM-OFL.txt`。中文回退到 Android 系统无衬线字体。

## 构建

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

`1.3.0-field` 为 Field Style 预发布版本，功能与 1.2.1 保持一致，仅替换界面语言与交互表现。预发布已合并到 `main` 并发布 GitHub Pre-release；稳定版仍以 `latest` Release 1.2.1 为准。
