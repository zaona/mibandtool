package top.zaona.mibandtool

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Reviews
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import top.zaona.mibandtool.data.ApiException
import top.zaona.mibandtool.data.ApiProvider
import top.zaona.mibandtool.data.Comment
import top.zaona.mibandtool.data.MiBandApi
import top.zaona.mibandtool.data.WatchfaceResource
import top.zaona.mibandtool.ui.theme.MibandtoolTheme
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.min

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val resourceJson = intent.getStringExtra(EXTRA_RESOURCE_JSON)
        val deviceType = intent.getStringExtra(EXTRA_DEVICE_TYPE)
        if (resourceJson.isNullOrBlank() || deviceType.isNullOrBlank()) {
            finish()
            return
        }
        val resource = ApiProvider.gson.fromJson(resourceJson, WatchfaceResource::class.java)
        setContent {
            MibandtoolTheme {
                val viewModel: DetailViewModel = viewModel(
                    factory = DetailViewModelFactory(resource, deviceType),
                )
                DetailScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                )
            }
        }
    }
}

class DetailViewModelFactory(
    private val resource: WatchfaceResource,
    private val deviceType: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(resource, deviceType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class DetailViewModel(
    val resource: WatchfaceResource,
    val deviceType: String,
    private val api: MiBandApi = ApiProvider.api,
) : ViewModel() {
    private val pageSize = 10
    var comments by mutableStateOf<List<Comment>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var hasMore by mutableStateOf(true)
        private set
    var downloadState by mutableStateOf<DownloadState>(DownloadState.Idle)
        private set

    private var page = 1
    private var prefetched: List<Comment> = emptyList()
    private var isPrefetching = false

    init {
        refresh()
    }

    fun refresh() {
        page = 1
        hasMore = true
        comments = emptyList()
        prefetched = emptyList()
        loadMore()
    }

    fun loadMore() {
        if (isLoading || !hasMore) return
        isLoading = true
        error = null
        viewModelScope.launch {
            try {
                if (prefetched.isNotEmpty()) {
                    comments = comments + prefetched
                    prefetched = emptyList()
                    hasMore = true
                    page += 1
                } else {
                    val result = api.fetchComments(
                        resourceId = resource.id,
                        deviceType = deviceType,
                        page = page,
                    )
                    comments = if (page == 1) result else comments + result
                    if (page == 1 && result.isEmpty()) {
                        hasMore = false
                    } else {
                        page += 1
                        hasMore = result.size >= pageSize
                    }
                }

                if (hasMore && !isPrefetching) {
                    prefetchNext()
                }
            } catch (exception: Exception) {
                error = if (exception is ApiException) exception.message else exception.message
            } finally {
                isLoading = false
            }
        }
    }

    private fun prefetchNext() {
        if (isPrefetching) return
        isPrefetching = true
        viewModelScope.launch {
            try {
                val next = api.fetchComments(
                    resourceId = resource.id,
                    deviceType = deviceType,
                    page = page,
                )
                prefetched = next
                hasMore = next.size >= pageSize
                if (next.isEmpty()) {
                    hasMore = false
                }
            } catch (_: Exception) {
                // ignore prefetch failure
            } finally {
                isPrefetching = false
            }
        }
    }

    fun requestDownload() {
        if (downloadState is DownloadState.Loading) return
        downloadState = DownloadState.Loading
        viewModelScope.launch {
            try {
                val url = api.fetchDownloadUrl(
                    resourceId = resource.id,
                    deviceType = deviceType,
                )
                downloadState = DownloadState.Success(url)
            } catch (exception: Exception) {
                val message = if (exception is ApiException) exception.message else exception.message
                downloadState = DownloadState.Error(message ?: "下载失败")
            }
        }
    }

    fun clearDownloadState() {
        downloadState = DownloadState.Idle
    }
}

sealed class DownloadState {
    object Idle : DownloadState()
    object Loading : DownloadState()
    data class Success(val url: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

private sealed interface DeviceCornerStyle {
    fun toShape(scale: Float): Shape
}

private data class PercentCornerStyle(val percent: Int) : DeviceCornerStyle {
    override fun toShape(scale: Float): Shape = RoundedCornerShape(percent = percent)
}

private data class AbsoluteCornerStyle(val radiusDp: Float) : DeviceCornerStyle {
    override fun toShape(scale: Float): Shape = RoundedCornerShape((radiusDp * scale).dp)
}

private data class DevicePreviewSpec(
    val widthPx: Int,
    val heightPx: Int,
    val cornerStyle: DeviceCornerStyle,
)

private val devicePreviewSpecs = mapOf(
    "o66" to DevicePreviewSpec(
        widthPx = 212,
        heightPx = 520,
        cornerStyle = PercentCornerStyle(50),
    ),
    "n66" to DevicePreviewSpec(
        widthPx = 192,
        heightPx = 490,
        cornerStyle = PercentCornerStyle(50),
    ),
    "n67" to DevicePreviewSpec(
        widthPx = 336,
        heightPx = 480,
        cornerStyle = AbsoluteCornerStyle(48f),
    ),
    "mi8" to DevicePreviewSpec(
        widthPx = 192,
        heightPx = 490,
        cornerStyle = PercentCornerStyle(50),
    ),
    "mi8pro" to DevicePreviewSpec(
        widthPx = 192,
        heightPx = 490,
        cornerStyle = AbsoluteCornerStyle(48f),
    ),
    "mi7" to DevicePreviewSpec(
        widthPx = 192,
        heightPx = 490,
        cornerStyle = PercentCornerStyle(50),
    ),
    "mi7pro" to DevicePreviewSpec(
        widthPx = 280,
        heightPx = 456,
        cornerStyle = AbsoluteCornerStyle(48f),
    ),
    "ws3" to DevicePreviewSpec(
        widthPx = 466,
        heightPx = 466,
        cornerStyle = PercentCornerStyle(50),
    ),
    "o62" to DevicePreviewSpec(
        widthPx = 466,
        heightPx = 466,
        cornerStyle = PercentCornerStyle(50),
    ),
    "rw4" to DevicePreviewSpec(
        widthPx = 390,
        heightPx = 450,
        cornerStyle = AbsoluteCornerStyle(103f),
    ),
    "o65" to DevicePreviewSpec(
        widthPx = 432,
        heightPx = 514,
        cornerStyle = AbsoluteCornerStyle(103f),
    ),
    "p65" to DevicePreviewSpec(
        widthPx = 432,
        heightPx = 514,
        cornerStyle = AbsoluteCornerStyle(108f),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val isCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 240
        }
    }
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val view = LocalView.current
    val statusBarColor = if (isCollapsed) {
        MaterialTheme.colorScheme.surface
    } else {
        Color.Transparent
    }
    val useDarkStatusBarIcons = isCollapsed && statusBarColor.luminance() > 0.5f

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkStatusBarIcons
        }
    }

    if (viewModel.downloadState !is DownloadState.Idle) {
        DownloadDialog(
            state = viewModel.downloadState,
            onCopy = { url ->
                clipboard.setText(AnnotatedString(url))
                viewModel.clearDownloadState()
            },
            onDismiss = viewModel::clearDownloadState,
            onRetry = viewModel::requestDownload,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isCollapsed) {
                        Text(
                            text = viewModel.resource.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "返回",
                            )
                        }
                    },
                actions = {
                    IconButton(onClick = viewModel::requestDownload) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "获取下载链接",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isCollapsed) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = if (isCollapsed) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        Color.White
                    },
                    navigationIconContentColor = if (isCollapsed) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        Color.White
                    },
                    actionIconContentColor = if (isCollapsed) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        Color.White
                    },
                ),
                windowInsets = WindowInsets.statusBars,
            )
        }
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = padding.calculateStartPadding(layoutDirection),
            top = 0.dp,
            end = padding.calculateEndPadding(layoutDirection),
            bottom = padding.calculateBottomPadding(),
        )
        val previewDeviceType = viewModel.resource.deviceType
            .takeIf { it.isNotBlank() }
            ?: viewModel.deviceType
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            item {
                DetailHero(
                    resource = viewModel.resource,
                    deviceType = previewDeviceType,
                )
            }
            item {
                DetailMetaCard(resource = viewModel.resource)
            }
            item {
                DetailDescription(resource = viewModel.resource)
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            item {
                CommentsSection(
                    comments = viewModel.comments,
                    isLoading = viewModel.isLoading,
                    hasMore = viewModel.hasMore,
                    error = viewModel.error,
                    onLoadMore = viewModel::loadMore,
                    onRetry = viewModel::refresh,
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DetailHero(
    resource: WatchfaceResource,
    deviceType: String,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val maxPreviewWidth = (screenWidth - 32.dp).coerceAtLeast(120.dp)
    val maxPreviewHeight = (screenHeight * 0.6f).coerceAtLeast(200.dp)
    val effectiveType = resource.deviceType.takeIf { it.isNotBlank() } ?: deviceType
    val previewSpec = effectiveType
        .takeIf { it.isNotBlank() }
        ?.lowercase(Locale.ROOT)
        ?.let(devicePreviewSpecs::get)
    val previewScale = previewSpec?.let {
        val widthScale = maxPreviewWidth.value / it.widthPx.toFloat()
        val heightScale = maxPreviewHeight.value / it.heightPx.toFloat()
        min(1f, min(widthScale, heightScale))
    } ?: 1f
    val previewHeight = previewSpec?.let { it.heightPx.dp * previewScale } ?: 0.dp
    val topPadding = if (previewSpec != null) 120.dp else 0.dp
    val bottomPadding = if (previewSpec != null) 140.dp else 0.dp
    val desiredHeroHeight = if (previewSpec != null) {
        previewHeight + topPadding + bottomPadding
    } else {
        320.dp
    }
    val maxHeroHeight = (screenHeight * 0.85f).coerceAtLeast(320.dp)
    val heroHeight = desiredHeroHeight
        .coerceAtLeast(320.dp)
        .coerceAtMost(maxHeroHeight)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(resource.previewUrl)
                .crossfade(true)
                .size(1200)
                .build(),
            contentDescription = resource.name,
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp),
            contentScale = ContentScale.Crop,
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (previewSpec != null) bottomPadding else 200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                        ),
                    )
                )
        )
        if (previewSpec != null) {
            val previewShape = previewSpec.cornerStyle.toShape(previewScale)
            val previewWidth = previewSpec.widthPx.dp * previewScale
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topPadding)
                    .width(previewWidth)
                    .height(previewHeight)
                    .border(
                        BorderStroke(3.dp, Color.White.copy(alpha = 0.5f)),
                        previewShape,
                    )
                    .clip(previewShape)
                    .background(MaterialTheme.colorScheme.surface, previewShape),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                    .data(resource.previewUrl)
                    .crossfade(true)
                        .size(previewSpec.widthPx, previewSpec.heightPx)
                        .build(),
                    contentDescription = resource.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                    error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }
        Text(
            text = resource.name,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailMetaCard(resource: WatchfaceResource) {
    val colorScheme = MaterialTheme.colorScheme
    val overlay = if (colorScheme.background.luminance() > 0.5f) {
        Color.Black.copy(alpha = 0.04f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val cardColor = overlay.compositeOver(colorScheme.surface)
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = resource.creator,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            DateInfo(resource = resource)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    icon = Icons.Outlined.Download,
                    label = "下载",
                    value = resource.downloads.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    icon = Icons.Outlined.Visibility,
                    label = "浏览",
                    value = resource.views.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    icon = Icons.Outlined.Storage,
                    label = "体积",
                    value = "${resource.fileSizeKb} KB",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DateInfo(resource: WatchfaceResource) {
    val formatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column {
        val created = resource.createdAt
        val updated = resource.updatedAt
        if (created != null) {
            Text("创建：${formatter.format(created)}", style = textStyle)
        }
        if (updated != null) {
            if (created != null) Spacer(modifier = Modifier.height(4.dp))
            Text("更新：${formatter.format(updated)}", style = textStyle)
        }
        if (created == null && updated == null) {
            Text("更新时间未知", style = textStyle)
        }
    }
}

@Composable
private fun DetailDescription(resource: WatchfaceResource) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        SectionHeader(
            icon = Icons.Outlined.Description,
            title = "资源简介",
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = resource.shortDescription,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun StatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CommentsSection(
    comments: List<Comment>,
    isLoading: Boolean,
    hasMore: Boolean,
    error: String?,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        SectionHeader(
            icon = Icons.Outlined.Reviews,
            title = "资源评论",
        )
        Spacer(modifier = Modifier.height(8.dp))
        when {
            isLoading && comments.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }

            error != null && comments.isEmpty() -> {
                CommentError(message = error, onRetry = onRetry)
            }

            comments.isEmpty() -> {
                Text("暂无评论", style = MaterialTheme.typography.bodyMedium)
            }

            else -> {
                comments.forEach { comment ->
                    CommentTile(comment = comment)
                }
                when {
                    error != null -> {
                        CommentError(message = error, onRetry = onLoadMore)
                    }

                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.CircularProgressIndicator()
                        }
                    }

                    hasMore -> {
                        TextButton(onClick = onLoadMore) {
                            Icon(
                                imageVector = Icons.Outlined.ExpandMore,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("加载更多评论")
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun CommentError(
    message: String,
    onRetry: () -> Unit,
) {
    Column {
        Text(
            text = "评论加载失败：$message",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FilledTonalButton(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun CommentTile(comment: Comment) {
    val colorScheme = MaterialTheme.colorScheme
    val overlay = if (colorScheme.background.luminance() > 0.5f) {
        Color.Black.copy(alpha = 0.03f)
    } else {
        Color.White.copy(alpha = 0.06f)
    }
    val bgColor = overlay.compositeOver(colorScheme.surface)
    val context = LocalContext.current
    val timeFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(MaterialTheme.shapes.large)
            .background(bgColor)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (comment.avatar.isBlank()) {
                Text(
                    text = comment.nickname.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleSmall,
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(comment.avatar)
                        .crossfade(true)
                        .size(120)
                        .build(),
                    contentDescription = comment.nickname,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(colorScheme.surfaceVariant),
                    error = ColorPainter(colorScheme.surfaceVariant),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = comment.nickname, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = comment.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = timeFormatter.format(comment.time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DownloadDialog(
    state: DownloadState,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("下载链接") },
        text = {
            when (state) {
                is DownloadState.Loading -> {
                    Text("正在获取下载链接...")
                }

                is DownloadState.Success -> {
                    Text(state.url)
                }

                is DownloadState.Error -> {
                    Text(state.message)
                }

                DownloadState.Idle -> {
                    Text("")
                }
            }
        },
        confirmButton = {
            when (state) {
                is DownloadState.Success -> {
                    TextButton(onClick = { onCopy(state.url) }) {
                        Text("复制")
                    }
                }

                is DownloadState.Error -> {
                    TextButton(onClick = onRetry) {
                        Text("重试")
                    }
                }

                else -> {}
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}
