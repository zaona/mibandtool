package top.zaona.mibandtool

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import top.zaona.mibandtool.data.ApiException
import top.zaona.mibandtool.data.ApiProvider
import top.zaona.mibandtool.data.MiBandApi
import top.zaona.mibandtool.data.WatchfaceResource
import top.zaona.mibandtool.data.devicePresets
import top.zaona.mibandtool.ui.theme.MibandtoolTheme

class SearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialDeviceType = intent.getStringExtra(EXTRA_DEVICE_TYPE)
            ?: devicePresets.first().value
        setContent {
            MibandtoolTheme {
                val viewModel: SearchViewModel = viewModel(
                    factory = SearchViewModelFactory(initialDeviceType),
                )
                SearchScreen(
                    viewModel = viewModel,
                    onBack = { finishWithResult(viewModel.deviceType) },
                    onResourceTap = { resource ->
                        startActivity(
                            Intent(this, DetailActivity::class.java)
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

    private fun finishWithResult(deviceType: String) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(EXTRA_DEVICE_TYPE, deviceType),
        )
        finish()
    }
}

class SearchViewModelFactory(
    private val initialDeviceType: String,
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(initialDeviceType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SearchViewModel(
    initialDeviceType: String,
    private val api: MiBandApi = ApiProvider.api,
) : ViewModel() {
    var deviceType by mutableStateOf(initialDeviceType)
        private set
    var keyword by mutableStateOf("")
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

    fun updateDeviceType(type: String) {
        if (type == deviceType) return
        deviceType = type
        if (keyword.isNotBlank()) {
            search(keyword)
        }
    }

    fun search(input: String) {
        val normalized = input.trim()
        keyword = normalized
        if (normalized.isEmpty()) {
            items = emptyList()
            error = null
            hasMore = true
            page = 1
            return
        }
        page = 1
        hasMore = true
        items = emptyList()
        fetchNext(isRefresh = false)
    }

    fun refresh(isUserInitiated: Boolean) {
        if (keyword.isBlank()) return
        page = 1
        hasMore = true
        items = emptyList()
        isRefreshing = isUserInitiated
        fetchNext(isRefresh = true)
    }

    fun loadMore() {
        if (isLoading || !hasMore || keyword.isBlank()) return
        fetchNext(isRefresh = false)
    }

    private fun fetchNext(isRefresh: Boolean) {
        isLoading = true
        error = null
        viewModelScope.launch {
            try {
                val result = api.searchResources(
                    keyword = keyword,
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
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onResourceTap: (WatchfaceResource) -> Unit,
) {
    var query by remember { mutableStateOf(viewModel.keyword) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = SearchBarDefaults.inputFieldShape,
                    color = SearchBarDefaults.colors().containerColor,
                    tonalElevation = 6.dp,
                ) {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {
                            keyboardController?.hide()
                            viewModel.search(query)
                        },
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text("输入关键词（作者、资源名称等）") },
                        leadingIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "返回",
                                )
                            }
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "清除",
                                    )
                                }
                            } else {
                                IconButton(onClick = {
                                    keyboardController?.hide()
                                    viewModel.search(query)
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "搜索",
                                    )
                                }
                            }
                        },
                    )
                }
                SearchDeviceTabs(
                    selectedValue = viewModel.deviceType,
                    onSelected = { value ->
                        viewModel.updateDeviceType(value)
                    },
                )
            }
        }
    ) { padding ->
        SearchResults(
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
private fun SearchResults(
    modifier: Modifier,
    viewModel: SearchViewModel,
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
            viewModel.keyword.isEmpty() -> {
                HintMessage(modifier = Modifier.fillMaxSize(), message = "输入关键词并点击搜索来查找资源")
            }

            viewModel.isLoading && viewModel.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }

            viewModel.error != null && viewModel.items.isEmpty() -> {
                ErrorHint(
                    modifier = Modifier.fillMaxSize(),
                    message = viewModel.error ?: "加载失败",
                    onRetry = { viewModel.search(viewModel.keyword) },
                )
            }

            viewModel.items.isEmpty() -> {
                HintMessage(modifier = Modifier.fillMaxSize(), message = "没有找到匹配的资源")
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

        if (viewModel.keyword.isNotEmpty()) {
            PullRefreshIndicator(
                refreshing = viewModel.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun SearchDeviceTabs(
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
