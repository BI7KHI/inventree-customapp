package xyz.bi7khi.inventree.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import xyz.bi7khi.inventree.mobile.ui.navigation.MainNavigation
import xyz.bi7khi.inventree.mobile.ui.screens.LoginScreen
import xyz.bi7khi.inventree.mobile.ui.screens.ServerSetupScreen
import xyz.bi7khi.inventree.mobile.ui.theme.InvenTreeTheme
import xyz.bi7khi.inventree.mobile.ui.theme.blendWith

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
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(primary.blendWith(Color.Black, 0.55f), primary),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            BrandMark(size = 76.dp, backgroundColor = Color.White.copy(alpha = 0.16f))
            Spacer(Modifier.height(22.dp))
            Text(
                "InvenTree",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
            )
            Text(
                "移动库存",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primaryContainer,
            )
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        }
    }
}
