package xyz.bi7khi.inventree.mobile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.bi7khi.inventree.mobile.BuildConfig
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.ServerInfo
import xyz.bi7khi.inventree.mobile.data.api.Session
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.ConfirmDialog
import xyz.bi7khi.inventree.mobile.ui.components.GradientHero
import xyz.bi7khi.inventree.mobile.ui.components.InitialAvatar
import xyz.bi7khi.inventree.mobile.ui.components.InfoRow
import xyz.bi7khi.inventree.mobile.ui.components.SectionHeader
import xyz.bi7khi.inventree.mobile.ui.components.ThemeColorPickerDialog
import xyz.bi7khi.inventree.mobile.util.UrlUtils

@Composable
fun MoreScreen(
    repository: InvenTreeRepository,
    session: Session,
    onLogout: () -> Unit,
    onChangeServer: () -> Unit,
    onRefreshUser: () -> Unit,
    onOpenLabelDesigner: () -> Unit,
    themeColor: Color,
    onThemeColorChange: (Color) -> Unit,
) {
    StatusBarIcons(lightIcons = true)
    val context = LocalContext.current
    val user = session.user
    var serverInfo by remember { mutableStateOf<ServerInfo?>(null) }
    var showLogout by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { repository.serverInfo() }.onSuccess { serverInfo = it }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 100.dp),
    ) {
        GradientHero(
            modifier = Modifier
                .fillMaxWidth()
                .height(252.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(22.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialAvatar(user.displayName, size = 66.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            user.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "@${user.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                        if (user.email.isNotBlank()) {
                            Text(
                                user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.62f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (user.is_superuser) RoleChip("超级管理员")
                    else if (user.is_staff) RoleChip("管理员")
                    else RoleChip("普通账户")
                    RoleChip("在线同步")
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            AppCard {
                SectionHeader("服务器")
                Spacer(Modifier.height(2.dp))
                InfoRow("地址", UrlUtils.displayServer(session.serverUrl))
                InfoRow(
                    "版本",
                    serverInfo?.let { "${it.instance} ${it.version} · API ${it.apiVersion}" } ?: "正在获取…",
                )
                InfoRow("连接", if (serverInfo?.worker_running == true) "在线，后台任务正常" else "已连接")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onChangeServer) {
                        Icon(Icons.Outlined.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("更换服务器")
                    }
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(session.serverUrl)))
                    }) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("打开网页端")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            AppCard {
                SectionHeader("账户与同步")
                Spacer(Modifier.height(8.dp))
                SettingRow(
                    icon = Icons.Outlined.Sync,
                    title = "从服务器刷新账户",
                    subtitle = "重新获取用户名、邮箱与角色权限",
                    onClick = onRefreshUser,
                )
                SettingRow(
                    icon = Icons.Outlined.Person,
                    title = "账户信息",
                    subtitle = "用户 ID #${user.pk} · 数据直接来自 InvenTree",
                    onClick = {},
                )
                SettingRow(
                    icon = Icons.Outlined.ExitToApp,
                    title = "退出登录",
                    subtitle = "清除本机加密 Token，保留服务器地址",
                    danger = true,
                    onClick = { showLogout = true },
                )
            }

            Spacer(Modifier.height(12.dp))
            AppCard {
                SectionHeader("外观")
                Spacer(Modifier.height(6.dp))
                Surface(
                    onClick = { showThemePicker = true },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("主题颜色", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                "应用到导航、按钮、首页渐变和强调元素",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(themeColor, CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("更改", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            AppCard {
                SectionHeader("标签打印")
                Spacer(Modifier.height(6.dp))
                Text(
                    "连接 BLE 热敏标签打印机，打印 40×30mm 库存、零件与库位标签。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("标签设计器", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenLabelDesigner) { Text("打开") }
                }
                Text(
                    "可在打印界面临时调整版式；设计器支持拖动文本、二维码与分隔线，并保存到本机。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("安全说明", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "密码仅用于换取 API Token，不会写入磁盘。Token 使用 Android Keystore AES-GCM 加密保存。所有库存与账户操作都直接请求你的服务器。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            AppCard {
                SectionHeader("关于")
                InfoRow("应用", "InvenTree Mobile ${BuildConfig.VERSION_NAME}")
                InfoRow("兼容", "InvenTree 1.5.x / API 530+")
                InfoRow("构建", BuildConfig.VERSION_CODE.toString())
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF177A4C), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "库存、库位、零件、条码操作与网页端共用同一套 API",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "InvenTree Mobile · 为 erp.bi7khi.xyz 构建",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
    }

    if (showThemePicker) {
        ThemeColorPickerDialog(
            initialColor = themeColor,
            onApply = {
                showThemePicker = false
                onThemeColorChange(it)
            },
            onDismiss = { showThemePicker = false },
        )
    }

    if (showLogout) {
        ConfirmDialog(
            title = "退出登录？",
            message = "本机保存的 API Token 将被清除。服务器上的数据不会受影响。",
            confirmLabel = "退出",
            destructive = true,
            onConfirm = {
                showLogout = false
                onLogout()
            },
            onDismiss = { showLogout = false },
        )
    }
}

@Composable
private fun RoleChip(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
