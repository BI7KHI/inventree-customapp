package xyz.bi7khi.inventree.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.bi7khi.inventree.mobile.BuildConfig
import xyz.bi7khi.inventree.mobile.ui.SessionUiState
import xyz.bi7khi.inventree.mobile.ui.SessionViewModel
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.BrandMark
import xyz.bi7khi.inventree.mobile.ui.components.FieldHero
import xyz.bi7khi.inventree.mobile.ui.components.FieldMicroLabel
import xyz.bi7khi.inventree.mobile.ui.components.FieldPulseDot
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCutLarge
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCutSmall
import xyz.bi7khi.inventree.mobile.ui.theme.FieldMono
import xyz.bi7khi.inventree.mobile.ui.theme.LocalFieldPalette
import xyz.bi7khi.inventree.mobile.util.UrlUtils

@Composable
fun ServerSetupScreen(
    state: SessionUiState.SignedOut,
    viewModel: SessionViewModel,
) {
    StatusBarIcons(lightIcons = true)
    val palette = LocalFieldPalette.current
    var url by rememberSaveable { mutableStateOf(state.serverUrl) }
    val normalizedInput = remember(url) { runCatching { UrlUtils.normalizeServerUrl(url) }.getOrNull() }
    val defaultNormalized = remember { UrlUtils.normalizeServerUrl(BuildConfig.DEFAULT_SERVER_URL) }
    val isDefault = normalizedInput == defaultNormalized

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
                .height(300.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                BrandMark(size = 58.dp)
                Spacer(Modifier.height(18.dp))
                FieldMicroLabel("REMOTE NODE // CONFIGURE", color = palette.signal)
                Text(
                    "接入服务器",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "连接你的 InvenTree 库存节点",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.58f),
                )
                Spacer(Modifier.height(11.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldPulseDot(color = palette.signal, size = 6.dp)
                    Spacer(Modifier.size(7.dp))
                    FieldMicroLabel("HTTPS // API 530+", color = Color.White.copy(alpha = 0.60f))
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            shape = FieldCutLarge,
            color = palette.panel,
            border = BorderStroke(1.dp, palette.line),
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldMicroLabel("NODE ADDRESS // 01", color = palette.signalText)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "REQUIRED",
                        fontFamily = FieldMono,
                        color = palette.muted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    "服务器地址",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "填写自建 InvenTree 的访问地址，应用会自动补全 API 路径。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("例如 inventree.example.com") },
                    leadingIcon = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                    singleLine = true,
                    shape = FieldCutSmall,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { if (!state.busy) viewModel.confirmServer(url) },
                    ),
                    isError = state.error != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.panel,
                        unfocusedContainerColor = palette.panel,
                    ),
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isDefault) {
                        TextButton(onClick = { url = BuildConfig.DEFAULT_SERVER_URL }) {
                            Icon(Icons.Outlined.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("使用预置服务器")
                        }
                    } else {
                        Row(
                            Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FieldPulseDot(size = 5.dp)
                            Spacer(Modifier.size(6.dp))
                            Text(
                                "预置节点已选中",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.signalText,
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut()) {
                    Surface(
                        color = palette.danger.copy(alpha = 0.10f),
                        shape = FieldCutSmall,
                        border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text(
                            text = state.error.orEmpty(),
                            color = palette.danger,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { viewModel.confirmServer(url) },
                    enabled = !state.busy && url.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = FieldCutSmall,
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
                    } else {
                        Text("建立节点连接", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.size(8.dp))
                        Icon(Icons.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.line),
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = palette.muted,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(15.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "凭据仅保存在本机，API Token 使用 Android Keystore 加密。生产环境建议使用 HTTPS。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                }
            }
        }

        Text(
            "INVENTREE MOBILE ${BuildConfig.VERSION_NAME}  //  API v530+",
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
            fontFamily = FieldMono,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        )
    }
}
