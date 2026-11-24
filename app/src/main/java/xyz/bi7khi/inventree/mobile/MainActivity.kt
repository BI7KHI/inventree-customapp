package xyz.bi7khi.inventree.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.local.ThemeSettings
import xyz.bi7khi.inventree.mobile.ui.SessionUiState
import xyz.bi7khi.inventree.mobile.ui.SessionViewModel
import xyz.bi7khi.inventree.mobile.ui.components.BrandMark
import xyz.bi7khi.inventree.mobile.ui.components.FieldHazardStrip
import xyz.bi7khi.inventree.mobile.ui.components.FieldHero
import xyz.bi7khi.inventree.mobile.ui.components.FieldMicroLabel
import xyz.bi7khi.inventree.mobile.ui.components.FieldPulseDot
import xyz.bi7khi.inventree.mobile.ui.navigation.MainNavigation
import xyz.bi7khi.inventree.mobile.ui.screens.LoginScreen
import xyz.bi7khi.inventree.mobile.ui.screens.ServerSetupScreen
import xyz.bi7khi.inventree.mobile.ui.theme.InvenTreeTheme
import xyz.bi7khi.inventree.mobile.ui.theme.LocalFieldPalette

class MainActivity : ComponentActivity() {

    private val viewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = (application as InvenTreeApplication).container
            val themeSettings by container.themeStore.settings
                .collectAsStateWithLifecycle(initialValue = ThemeSettings())
            val scope = rememberCoroutineScope()
            InvenTreeTheme(seedColor = Color(themeSettings.seedArgb)) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                when (val current = state) {
                    SessionUiState.Loading -> LaunchScreen()
                    is SessionUiState.SignedOut -> {
                        if (current.showServerSetup) {
                            ServerSetupScreen(current, viewModel)
                        } else {
                            LoginScreen(current, viewModel)
                        }
                    }
                    is SessionUiState.SignedIn -> MainNavigation(
                        session = current.session,
                        repository = current.repository,
                        themeColor = Color(themeSettings.seedArgb),
                        onThemeColorChange = { color ->
                            scope.launch { container.themeStore.save(color.toArgb()) }
                        },
                        onLogout = { viewModel.logout() },
                        onChangeServer = { viewModel.switchServer() },
                        onRefreshUser = { viewModel.refreshUser() },
                    )
                }
            }
        }
    }
}

@Composable
private fun LaunchScreen() {
    xyz.bi7khi.inventree.mobile.ui.StatusBarIcons(lightIcons = true)
    val palette = LocalFieldPalette.current
    FieldHero(
        modifier = Modifier.fillMaxSize(),
        showGhost = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandMark(size = 88.dp)
            Spacer(Modifier.height(24.dp))
            FieldMicroLabel("INVENTREE // MOBILE TERMINAL", color = palette.signal)
            Spacer(Modifier.height(6.dp))
            Text(
                "物资管控终端",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(5.dp))
            FieldMicroLabel("END FIELD STYLE // INITIALIZING", color = Color.White.copy(alpha = 0.44f))
            Spacer(Modifier.height(28.dp))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(54.dp),
                    strokeWidth = 1.5.dp,
                    color = palette.signal,
                )
                FieldPulseDot(color = palette.signal, size = 5.dp)
            }
            Spacer(Modifier.height(20.dp))
            BootLine("01", "KEYSTORE", "OK", palette)
            Spacer(Modifier.height(5.dp))
            BootLine("02", "SESSION", "VERIFY", palette)
            Spacer(Modifier.height(5.dp))
            BootLine("03", "API LINK", "STANDBY", palette)
        }
        FieldHazardStrip(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = palette.signal.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun BootLine(code: String, label: String, value: String, palette: xyz.bi7khi.inventree.mobile.ui.theme.FieldPalette) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FieldMicroLabel(code, color = palette.signal.copy(alpha = 0.65f))
        Spacer(Modifier.width(10.dp))
        FieldMicroLabel(label, color = Color.White.copy(alpha = 0.50f))
        Spacer(Modifier.weight(1f))
        FieldMicroLabel(value, color = Color.White.copy(alpha = 0.72f))
    }
}
