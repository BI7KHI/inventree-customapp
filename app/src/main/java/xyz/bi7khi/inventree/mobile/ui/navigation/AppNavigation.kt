package xyz.bi7khi.inventree.mobile.ui.navigation

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.Session
import xyz.bi7khi.inventree.mobile.ui.components.FieldBottomBar
import xyz.bi7khi.inventree.mobile.ui.components.FieldNavItem
import xyz.bi7khi.inventree.mobile.ui.screens.HomeScreen
import xyz.bi7khi.inventree.mobile.ui.screens.LabelEditorScreen
import xyz.bi7khi.inventree.mobile.ui.screens.LabelPrintScreen
import xyz.bi7khi.inventree.mobile.ui.screens.LocationDetailScreen
import xyz.bi7khi.inventree.mobile.ui.screens.LocationFormScreen
import xyz.bi7khi.inventree.mobile.ui.screens.MoreScreen
import xyz.bi7khi.inventree.mobile.ui.screens.PartDetailScreen
import xyz.bi7khi.inventree.mobile.ui.screens.PartFormScreen
import xyz.bi7khi.inventree.mobile.ui.screens.PartListScreen
import xyz.bi7khi.inventree.mobile.ui.screens.ScanScreen
import xyz.bi7khi.inventree.mobile.ui.screens.StockCreateScreen
import xyz.bi7khi.inventree.mobile.ui.screens.StockDetailScreen
import xyz.bi7khi.inventree.mobile.ui.screens.StockEditScreen
import xyz.bi7khi.inventree.mobile.ui.screens.StockHubScreen

private const val HOME = "home"
private const val STOCK = "stock?query={query}&tab={tab}"
private const val SCAN = "scan"
private const val PARTS = "parts?query={query}"
private const val MORE = "more"

@Composable
fun MainNavigation(
    session: Session,
    repository: InvenTreeRepository,
    themeColor: Color,
    onThemeColorChange: (Color) -> Unit,
    onLogout: () -> Unit,
    onChangeServer: () -> Unit,
    onRefreshUser: () -> Unit,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val topLevelRoutes = setOf(HOME, STOCK, SCAN, PARTS, MORE)
    val showBottom = route in topLevelRoutes
    val selectedIndex = when (route) {
        HOME -> 0
        STOCK -> 1
        SCAN -> 2
        PARTS -> 3
        MORE -> 4
        else -> -1
    }

    fun goTop(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun stockRoute(query: String = "", tab: Int = 0): String =
        "stock?query=${Uri.encode(query)}&tab=$tab"

    fun afterCreate(route: String) {
        nav.popBackStack()
        nav.navigate(route)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottom) {
                FieldBottomBar(
                    items = listOf(
                        FieldNavItem("首页", "HOME", Icons.Outlined.Home, Icons.Filled.Home),
                        FieldNavItem("库存", "STOCK", Icons.Outlined.Inventory2, Icons.Filled.Inventory2),
                        FieldNavItem(
                            label = "扫描",
                            code = "SCAN",
                            icon = Icons.Outlined.QrCodeScanner,
                            selectedIcon = Icons.Filled.QrCodeScanner,
                            isAction = true,
                        ),
                        FieldNavItem("零件", "PARTS", Icons.Outlined.Widgets, Icons.Filled.Widgets),
                        FieldNavItem("我的", "USER", Icons.Outlined.Person, Icons.Filled.Person),
                    ),
                    selectedIndex = selectedIndex,
                    onSelect = { index ->
                        when (index) {
                            0 -> goTop(HOME)
                            1 -> goTop(stockRoute())
                            2 -> goTop(SCAN)
                            3 -> goTop("parts?query=")
                            4 -> goTop(MORE)
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            enterTransition = {
                fadeIn(tween(170)) + slideInHorizontally(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    initialOffsetX = { it / 9 },
                )
            },
            exitTransition = {
                fadeOut(tween(150)) + slideOutHorizontally(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    targetOffsetX = { -it / 24 },
                )
            },
            popEnterTransition = {
                fadeIn(tween(170)) + slideInHorizontally(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetX = { -it / 12 },
                )
            },
            popExitTransition = {
                fadeOut(tween(150)) + slideOutHorizontally(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    targetOffsetX = { it / 16 },
                )
            },
        ) {
            composable(HOME) {
                HomeScreen(
                    repository = repository,
                    userName = session.user.displayName,
                    serverUrl = session.serverUrl,
                    onOpenStock = { nav.navigate("stock/$it") },
                    onOpenPart = { nav.navigate("parts/$it") },
                    onOpenScanner = { goTop(SCAN) },
                    onOpenMore = { goTop(MORE) },
                    onOpenStockList = { goTop(stockRoute()) },
                    onOpenPartList = { goTop("parts?query=") },
                    onOpenLocations = { nav.navigate(stockRoute(tab = 1)) },
                    onNewStock = { nav.navigate("stock/new?partId=-1") },
                    onNewPart = { nav.navigate("parts/new") },
                    onSearch = { nav.navigate(stockRoute(it)) },
                )
            }
            composable(
                route = STOCK,
                arguments = listOf(
                    navArgument("query") { type = NavType.StringType; defaultValue = "" },
                    navArgument("tab") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { entry ->
                StockHubScreen(
                    repository = repository,
                    initialQuery = entry.arguments?.getString("query").orEmpty(),
                    initialTab = entry.arguments?.getInt("tab") ?: 0,
                    onOpenStock = { nav.navigate("stock/$it") },
                    onOpenLocation = { nav.navigate("locations/$it") },
                    onNewStock = { nav.navigate("stock/new?partId=-1") },
                    onNewLocation = { nav.navigate("locations/new?parentId=-1") },
                )
            }
            composable(SCAN) {
                ScanScreen(
                    repository = repository,
                    onBack = { nav.popBackStack() },
                    onOpenStock = { nav.navigate("stock/$it") },
                    onOpenPart = { nav.navigate("parts/$it") },
                    onOpenLocation = { nav.navigate("locations/$it") },
                    onSearch = { nav.navigate(stockRoute(it)) },
                )
            }
            composable(
                route = PARTS,
                arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                PartListScreen(
                    repository = repository,
                    initialQuery = entry.arguments?.getString("query").orEmpty(),
                    onOpenPart = { nav.navigate("parts/$it") },
                    onNewPart = { nav.navigate("parts/new") },
                )
            }
            composable(MORE) {
                MoreScreen(
                    repository = repository,
                    session = session,
                    onLogout = onLogout,
                    onChangeServer = onChangeServer,
                    onRefreshUser = onRefreshUser,
                    onOpenLabelDesigner = { nav.navigate("label-editor/stock?id=-1") },
                    themeColor = themeColor,
                    onThemeColorChange = onThemeColorChange,
                )
            }

            composable(
                route = "stock/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: return@composable
                StockDetailScreen(
                    repository = repository,
                    stockId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("stock/$id/edit") },
                    onOpenPart = { nav.navigate("parts/$it") },
                    onOpenLocation = { nav.navigate("locations/$it") },
                    onPrint = { nav.navigate("print/stock/$id") },
                    onDeleted = { nav.popBackStack() },
                )
            }
            composable(
                route = "stock/{id}/edit",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: return@composable
                StockEditScreen(
                    repository = repository,
                    stockId = id,
                    onBack = { nav.popBackStack() },
                    onSaved = {
                        nav.popBackStack("stock/$id", inclusive = true)
                        nav.navigate("stock/$id")
                    },
                )
            }
            composable(
                route = "stock/new?partId={partId}",
                arguments = listOf(navArgument("partId") { type = NavType.IntType; defaultValue = -1 }),
            ) { entry ->
                val partId = entry.arguments?.getInt("partId")?.takeIf { it > 0 }
                StockCreateScreen(
                    repository = repository,
                    initialPartId = partId,
                    onBack = { nav.popBackStack() },
                    onCreated = { afterCreate("stock/$it") },
                )
            }
            composable(
                route = "parts/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: return@composable
                PartDetailScreen(
                    repository = repository,
                    partId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("parts/$id/edit") },
                    onAddStock = { nav.navigate("stock/new?partId=$id") },
                    onOpenStock = { nav.navigate("stock/$it") },
                    onPrint = { nav.navigate("print/part/$id") },
                    onDeleted = { nav.popBackStack() },
                )
            }
            composable("parts/new") {
                PartFormScreen(
                    repository = repository,
                    partId = null,
                    onBack = { nav.popBackStack() },
                    onSaved = { afterCreate("parts/$it") },
                )
            }
            composable(
                route = "parts/{id}/edit",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: return@composable
                PartFormScreen(
                    repository = repository,
                    partId = id,
                    onBack = { nav.popBackStack() },
                    onSaved = {
                        nav.popBackStack("parts/$id", inclusive = true)
                        nav.navigate("parts/$id")
                    },
                )
            }
            composable(
                route = "locations/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: return@composable
                LocationDetailScreen(
                    repository = repository,
                    locationId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("locations/$id/edit") },
                    onNewChild = { nav.navigate("locations/new?parentId=$id") },
                    onOpenLocation = { nav.navigate("locations/$it") },
                    onOpenStock = { nav.navigate("stock/$it") },
                    onPrint = { nav.navigate("print/location/$id") },
                    onDeleted = { nav.popBackStack() },
                )
            }
            composable(
                route = "locations/new?parentId={parentId}",
                arguments = listOf(navArgument("parentId") { type = NavType.IntType; defaultValue = -1 }),
            ) { entry ->
                val parentId = entry.arguments?.getInt("parentId")?.takeIf { it > 0 }
                LocationFormScreen(
                    repository = repository,
                    locationId = null,
                    initialParentId = parentId,
                    onBack = { nav.popBackStack() },
                    onSaved = { afterCreate("locations/$it") },
                )
            }
            composable(
                route = "locations/{id}/edit",
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: return@composable
                LocationFormScreen(
                    repository = repository,
                    locationId = id,
                    initialParentId = null,
                    onBack = { nav.popBackStack() },
                    onSaved = {
                        nav.popBackStack("locations/$id", inclusive = true)
                        nav.navigate("locations/$id")
                    },
                )
            }
            composable(
                route = "print/{type}/{id}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.IntType },
                ),
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: "stock"
                val id = entry.arguments?.getInt("id")?.takeIf { it > 0 }
                LabelPrintScreen(
                    repository = repository,
                    typeKey = type,
                    itemId = id,
                    onBack = { nav.popBackStack() },
                    onEditDesign = {
                        nav.navigate("label-editor/$type?id=${id ?: -1}")
                    },
                )
            }
            composable(
                route = "label-editor/{type}?id={id}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.IntType; defaultValue = -1 },
                ),
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: "stock"
                val id = entry.arguments?.getInt("id")?.takeIf { it > 0 }
                LabelEditorScreen(
                    repository = repository,
                    typeKey = type,
                    itemId = id,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
