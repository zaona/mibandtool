import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'src/api/miband_api.dart';
import 'src/models/watchface_resource.dart';
import 'src/pages/resource_detail_page.dart';
import 'src/pages/search_page.dart';
import 'src/state/resource_store.dart';
import 'src/widgets/device_type_selector.dart';
import 'src/widgets/paginated_resource_list.dart';

void main() {
  runApp(const MiBandToolApp());
}

class MiBandToolApp extends StatelessWidget {
  const MiBandToolApp({super.key});

  @override
  Widget build(BuildContext context) {
    const seed = Colors.deepPurple;
    return DynamicColorBuilder(
      builder: (lightDynamic, darkDynamic) {
        final colorScheme = lightDynamic ?? ColorScheme.fromSeed(seedColor: seed);
        final darkColorScheme = darkDynamic ??
            ColorScheme.fromSeed(seedColor: seed, brightness: Brightness.dark);
        return MaterialApp(
          debugShowCheckedModeBanner: false,
          title: '表盘自定义工具',
          theme: ThemeData(
            colorScheme: colorScheme,
            useMaterial3: true,
          ),
          darkTheme: ThemeData(
            colorScheme: darkColorScheme,
            useMaterial3: true,
          ),
          themeMode: ThemeMode.system,
          home: const AppShell(),
        );
      },
    );
  }
}

class AppShell extends StatefulWidget {
  const AppShell({super.key});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  late final MiBandApi _api;
  late final ValueNotifier<String> _deviceType;
  late final PaginatedResourceStore _homeStore;
  late final SearchStore _searchStore;

  @override
  void initState() {
    super.initState();
    _api = MiBandApi();
    _deviceType = ValueNotifier('o66');
    _homeStore = PaginatedResourceStore(
      (page) => _api.fetchHomeResources(
        deviceType: _deviceType.value,
        page: page,
      ),
    );
    _searchStore = SearchStore(
      (keyword, page) => _api.searchResources(
        keyword: keyword,
        deviceType: _deviceType.value,
        page: page,
      ),
    );
    _deviceType.addListener(_handleDeviceTypeChanged);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _homeStore.refresh();
    });
  }

  @override
  void dispose() {
    _deviceType.removeListener(_handleDeviceTypeChanged);
    _deviceType.dispose();
    _homeStore.dispose();
    _searchStore.dispose();
    _api.dispose();
    super.dispose();
  }

  void _handleDeviceTypeChanged() {
    final type = _deviceType.value;
    _homeStore.replaceLoader(
      (page) => _api.fetchHomeResources(deviceType: type, page: page),
    );
    if (_searchStore.keyword.isNotEmpty) {
      _searchStore.search(_searchStore.keyword);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: _buildAppBar(context),
      body: SafeArea(
        child: FeedTab(
          store: _homeStore,
          onResourceTap: _showResourceDetail,
        ),
      ),
    );
  }

  AppBar _buildAppBar(BuildContext context) {
    return AppBar(
      title: const Text('表盘自定义工具'),
      actions: [
        IconButton(
          tooltip: '搜索资源',
          onPressed: _openSearchPage,
          icon: const Icon(Icons.search),
        ),
      ],
      bottom: PreferredSize(
        preferredSize: const Size.fromHeight(48),
        child: DeviceTabs(deviceType: _deviceType),
      ),
    );
  }

  Future<void> _openSearchPage() async {
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => SearchPage(
          store: _searchStore,
          deviceType: _deviceType,
          onResourceTap: _showResourceDetail,
        ),
      ),
    );
  }

  Future<void> _showResourceDetail(WatchfaceResource resource) async {
    if (!mounted) return;
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ResourceDetailPage(
          resource: resource,
          api: _api,
          deviceType: _deviceType,
          onDownloadTap: _handleDownload,
        ),
      ),
    );
  }

  Future<void> _handleDownload(WatchfaceResource resource) async {
    final navigator = Navigator.of(context);
    final scaffoldMessenger = ScaffoldMessenger.of(context);
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(child: CircularProgressIndicator()),
    );
    try {
      final link = await _api.fetchDownloadUrl(
        resourceId: resource.id,
        deviceType: _deviceType.value,
      );
      if (!navigator.mounted) return;
      navigator.pop();
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: const Text('下载链接'),
            content: SelectableText(link),
            actions: [
              TextButton(
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: link));
                  Navigator.of(context).pop();
                  scaffoldMessenger.showSnackBar(
                    const SnackBar(content: Text('下载链接已复制')),
                  );
                },
                child: const Text('复制'),
              ),
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text('关闭'),
              ),
            ],
          );
        },
      );
    } catch (error) {
      if (navigator.canPop()) {
        navigator.pop();
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.toString())),
      );
    }
  }

}

class FeedTab extends StatelessWidget {
  const FeedTab({
    super.key,
    required this.store,
    required this.onResourceTap,
  });

  final PaginatedResourceStore store;
  final ValueChanged<WatchfaceResource> onResourceTap;

  @override
  Widget build(BuildContext context) {
    return PaginatedResourceList(
      store: store,
      onResourceTap: onResourceTap,
    );
  }
}
