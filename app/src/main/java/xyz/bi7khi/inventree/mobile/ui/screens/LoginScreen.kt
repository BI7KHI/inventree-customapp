package xyz.bi7khi.inventree.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.bi7khi.inventree.mobile.BuildConfig
import xyz.bi7khi.inventree.mobile.ui.SessionUiState
import xyz.bi7khi.inventree.mobile.ui.SessionViewModel
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.BrandMark
import xyz.bi7khi.inventree.mobile.ui.components.FieldHero
import xyz.bi7khi.inventree.mobile.ui.components.FieldMicroLabel
import xyz.bi7khi.inventree.mobile.ui.components.FieldPulseDot
import xyz.bi7khi.inventree.mobile.ui.components.FieldTag
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCut
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCutLarge
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCutSmall
import xyz.bi7khi.inventree.mobile.ui.theme.FieldMono
import xyz.bi7khi.inventree.mobile.ui.theme.LocalFieldPalette
import xyz.bi7khi.inventree.mobile.util.UrlUtils

@Composable
fun LoginScreen(
    state: SessionUiState.SignedOut,
    viewModel: SessionViewModel,
) {
    StatusBarIcons(lightIcons = true)
    val palette = LocalFieldPalette.current
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var tokenVisible by rememberSaveable { mutableStateOf(false) }
    val submit = {
        if (!state.busy) {
            if (mode == 0) viewModel.login(username, password)
            else viewModel.loginWithToken(token)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper)
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        FieldHero(
            modifier = Modifier
                .fillMaxWidth()
                .height(314.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(size = 48.dp)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        FieldMicroLabel("INVENTREE // MOBILE", color = palette.signal)
                        Text(
                            "InvenTree",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            state.serverInfo?.let { "${it.instance} ${it.version}" } ?: "库存管理终端",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.55f),
                        )
                    }
                    Surface(
                        onClick = { viewModel.switchServer() },
                        color = Color.White.copy(alpha = 0.04f),
                        shape = FieldCutSmall,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Dns,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "更换",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(26.dp))
                FieldMicroLabel("SECURE CHANNEL // AUTH REQUIRED", color = palette.cyan)
                Spacer(Modifier.height(5.dp))
                Text(
                    "终端接入",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    UrlUtils.displayServer(state.serverUrl),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.60f),
                    maxLines = 1,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldPulseDot(color = palette.signal, size = 6.dp)
                    Spacer(Modifier.width(7.dp))
                    FieldMicroLabel("SYS/ONLINE", color = Color.White.copy(alpha = 0.65f))
                    Spacer(Modifier.width(12.dp))
                    FieldTag("TOKEN ENCRYPTED", color = palette.cyan)
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-22).dp),
            shape = FieldCutLarge,
            color = palette.panel,
            border = BorderStroke(1.dp, palette.line),
            shadowElevation = 10.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldMicroLabel("IDENTITY // VERIFY", color = palette.signalText)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "01 / 01",
                        fontFamily = FieldMono,
                        fontSize = 10.sp,
                        color = palette.muted,
                    )
                }
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeTab(
                        label = "账户密码",
                        code = "A",
                        selected = mode == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = 0 },
                    )
                    ModeTab(
                        label = "API Token",
                        code = "B",
                        selected = mode == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { mode = 1 },
                    )
                }

                if (!state.notice.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = palette.cyan.copy(alpha = 0.08f),
                        shape = FieldCutSmall,
                        border = BorderStroke(1.dp, palette.cyan.copy(alpha = 0.28f)),
                    ) {
                        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.Top) {
                            FieldPulseDot(color = palette.cyan, size = 6.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                state.notice,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                if (mode == 0) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("用户名 / OPERATOR ID") },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                        singleLine = true,
                        shape = FieldCutSmall,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                        enabled = !state.busy,
                    )
                    Spacer(Modifier.height(11.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("密码 / ACCESS KEY") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                )
                            }
                        },
                        singleLine = true,
                        shape = FieldCutSmall,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        enabled = !state.busy,
                    )
                } else {
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Token") },
                        placeholder = { Text("在网页端账户设置中创建") },
                        leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(
                                    if (tokenVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (tokenVisible) "隐藏 Token" else "显示 Token",
                                )
                            }
                        },
                        singleLine = true,
                        shape = FieldCutSmall,
                        visualTransformation = if (tokenVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        enabled = !state.busy,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "适合启用了 SSO / 双因素认证的账户。Token 由网页端「账户设置 → API Token」生成。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                }

                AnimatedVisibility(visible = state.error != null) {
                    Surface(
                        color = palette.danger.copy(alpha = 0.10f),
                        shape = FieldCutSmall,
                        border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 13.dp),
                    ) {
                        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            FieldPulseDot(color = palette.danger, size = 6.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                state.error.orEmpty(),
                                color = palette.danger,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = submit,
                    enabled = !state.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = FieldCut,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.signal,
                        contentColor = palette.onSignal,
                        disabledContainerColor = palette.panelAlt,
                        disabledContentColor = palette.muted,
                    ),
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(19.dp),
                            strokeWidth = 2.dp,
                            color = palette.onSignal,
                        )
                        Spacer(Modifier.width(9.dp))
                        Text("正在建立连接…")
                    } else {
                        Icon(Icons.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("接入并同步账户", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (mode == 0) {
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "登录仅用于换取 API Token。应用不会保存密码，Token 使用 Android Keystore 加密。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                }

                state.serverInfo?.let { info ->
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(palette.line),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Outlined.AlternateEmail,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = palette.muted,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${info.server} ${info.version} · API ${info.apiVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.muted,
                            fontFamily = FieldMono,
                        )
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FieldPulseDot(size = 5.dp)
            Spacer(Modifier.width(7.dp))
            FieldMicroLabel("END // BUILD ${BuildConfig.VERSION_NAME}", color = palette.muted)
            Spacer(Modifier.weight(1f))
            FieldMicroLabel("LOCAL KEYSTORE", color = palette.muted.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun ModeTab(
    label: String,
    code: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalFieldPalette.current
    Surface(
        modifier = modifier
            .clip(FieldCutSmall)
            .clickable(onClick = onClick),
        shape = FieldCutSmall,
        color = if (selected) palette.signal else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) palette.signal else palette.line,
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                code,
                fontFamily = FieldMono,
                fontSize = 9.sp,
                color = if (selected) palette.onSignal.copy(alpha = 0.7f) else palette.muted,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) palette.onSignal else palette.muted,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}
