package top.zaona.mibandtool

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Tune
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import top.zaona.mibandtool.data.ApiException
import top.zaona.mibandtool.data.ApiProvider
import top.zaona.mibandtool.data.MiBandApi
import top.zaona.mibandtool.data.WatchfaceResource
import top.zaona.mibandtool.data.devicePresets
import top.zaona.mibandtool.ui.theme.MibandtoolTheme
import coil.request.ImageRequest
import kotlin.math.max
import kotlin.math.roundToInt

const val EXTRA_DEVICE_TYPE = "extra_device_type"
const val EXTRA_RESOURCE_JSON = "extra_resource_json"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MibandtoolTheme {
                val viewModel: HomeViewModel = viewModel()
                val context = LocalContext.current
                val launcher = rememberLauncherForActivityResult(viewModel)
                HomeScreen(
                    viewModel = viewModel,
                    onSearchTap = {
                        launcher.launch(
                            Intent(context, SearchActivity::class.java)
                                .putExtra(EXTRA_DEVICE_TYPE, viewModel.deviceType)
                        )
                    },
                    onResourceTap = { resource ->
                        context.startActivity(
                            Intent(context, DetailActivity::class.java)
                                .putExtra(
                                    EXTRA_RESOURCE_JSON,
                                    ApiProvider.gson.toJson(resource)
                                )
                                .putExtra(EXTRA_DEVICE_TYPE, viewModel.deviceType)
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun rememberLauncherForActivityResult(
    viewModel: HomeViewModel,
) = androidx.activity.compose.rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
    result.data?.getStringExtra(EXTRA_DEVICE_TYPE)?.takeIf { it.isNotBlank() }?.let {
        viewModel.updateDeviceType(it)
    }
}

class HomeViewModel(
    private val api: MiBandApi = ApiProvider.api,
) : ViewModel() {
    var deviceType by mutableStateOf(devicePresets.first().value)
        private set
    var items by mutableStateOf<List<WatchfaceResource>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var hasMore by mutableStateOf(true)
        private set

    private var page = 1

    init {
        refresh(isUserInitiated = false)
    }

    fun updateDeviceType(type: String) {
        if (type == deviceType) return
        deviceType = type
        refresh(isUserInitiated = false)
    }

    fun refresh(isUserInitiated: Boolean) {
        page = 1
        hasMore = true
        items = emptyList()
        error = null
        isRefreshing = isUserInitiated
        fetchNext(isRefresh = true)
    }

    fun loadMore() {
        if (isLoading || !hasMore) return
        fetchNext(isRefresh = false)
    }

    private fun fetchNext(isRefresh: Boolean) {
        isLoading = true
        error = null
        viewModelScope.launch {
            try {
                val result = api.fetchHomeResources(
                    deviceType = deviceType,
                    page = page,
                )
                items = if (page == 1) result else items + result
                hasMore = result.isNotEmpty()
                if (hasMore) {
                    page += 1
                }
            } catch (exception: Exception) {
                error = if (exception is ApiException) exception.message else exception.message
            } finally {
                isLoading = false
                if (isRefresh) {
                    isRefreshing = false
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSearchTap: () -> Unit,
    onResourceTap: (WatchfaceResource) -> Unit,
) {
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = { Text("表盘自定义工具") },
                    actions = {
                        IconButton(onClick = onSearchTap) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "搜索资源",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                DeviceFilterSection(
                    selectedValue = viewModel.deviceType,
                    onSelected = { value -> viewModel.updateDeviceType(value) },
                )
            }
        }
    ) { padding ->
        HomeResourceGrid(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            viewModel = viewModel,
            onResourceTap = onResourceTap,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun HomeResourceGrid(
    modifier: Modifier,
    viewModel: HomeViewModel,
    onResourceTap: (WatchfaceResource) -> Unit,
) {
    val listState = rememberLazyStaggeredGridState()
    val shouldLoadMore by remember(viewModel.items, listState) {
        derivedStateOf {
            val lastIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastIndex >= viewModel.items.size - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = viewModel.isRefreshing,
        onRefresh = { viewModel.refresh(isUserInitiated = true) },
    )

    Box(modifier = modifier.pullRefresh(pullRefreshState)) {
        when {
            viewModel.isLoading && viewModel.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }

            viewModel.error != null && viewModel.items.isEmpty() -> {
                ErrorHint(
                    modifier = Modifier.fillMaxSize(),
                    message = viewModel.error ?: "加载失败",
                    onRetry = { viewModel.refresh(isUserInitiated = false) },
                )
            }

            viewModel.items.isEmpty() -> {
                HintMessage(modifier = Modifier.fillMaxSize(), message = "暂无相关资源")
            }

            else -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(viewModel.items.size) { index ->
                        val item = viewModel.items[index]
                        WatchfaceCard(resource = item, onClick = { onResourceTap(item) })
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        LoaderFooter(
                            isLoading = viewModel.isLoading,
                            hasMore = viewModel.hasMore,
                            error = viewModel.error,
                            onRetry = viewModel::loadMore,
                        )
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = viewModel.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun DeviceFilterSection(
    selectedValue: String,
    onSelected: (String) -> Unit,
) {
    val selectedIndex = devicePresets.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0)
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 12.dp,
    ) {
        devicePresets.forEachIndexed { index, preset ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelected(preset.value) },
                text = { Text(preset.label) },
            )
        }
    }
}

@Composable
fun WatchfaceCard(
    resource: WatchfaceResource,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val overlay = if (!isSystemInDarkTheme()) {
        Color.Black.copy(alpha = 0.04f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val cardColor = overlay.compositeOver(colorScheme.surface)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(resource.previewUrl)
                    .crossfade(true)
                    .size(600)
                    .build(),
                contentDescription = resource.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(colorScheme.surfaceVariant),
                error = ColorPainter(colorScheme.surfaceVariant),
            )
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = resource.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                MetaLine(
                    icon = Icons.Outlined.Download,
                    label = "下载 ${resource.downloads}",
                )
                MetaLine(
                    icon = Icons.Outlined.Visibility,
                    label = "浏览 ${resource.views}",
                )
                MetaLine(
                    icon = Icons.Outlined.Storage,
                    label = "体积 ${resource.fileSizeKb} KB",
                )
            }
        }
    }
}

@Composable
private fun MetaLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    val theme = MaterialTheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = theme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = theme.typography.bodySmall,
            color = theme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun LoaderFooter(
    isLoading: Boolean,
    hasMore: Boolean,
    error: String?,
    onRetry: () -> Unit,
) {
    when {
        error != null -> {
            ErrorHint(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                message = error,
                onRetry = onRetry,
            )
        }

        !hasMore -> {
            Spacer(modifier = Modifier.height(16.dp))
        }

        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }

        else -> Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ErrorHint(
    modifier: Modifier,
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = message, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun HintMessage(
    modifier: Modifier,
    message: String,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}
