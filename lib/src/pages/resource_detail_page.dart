import 'package:flutter/material.dart';
import 'package:palette_generator/palette_generator.dart';

import '../api/miband_api.dart';
import '../models/watchface_resource.dart';
import '../state/resource_store.dart';
import '../widgets/comments_section.dart';

class ResourceDetailPage extends StatefulWidget {
  const ResourceDetailPage({
    super.key,
    required this.resource,
    required this.api,
    required this.deviceType,
    required this.onDownloadTap,
  });

  final WatchfaceResource resource;
  final MiBandApi api;
  final ValueNotifier<String> deviceType;
  final ValueChanged<WatchfaceResource> onDownloadTap;

  @override
  State<ResourceDetailPage> createState() => _ResourceDetailPageState();
}

class _ResourceDetailPageState extends State<ResourceDetailPage> {
  late final CommentStore _commentStore;
  static const double _heroHeight = 320;
  Color _heroForegroundColor = Colors.white;
  bool _isHeroCollapsed = false;

  @override
  void initState() {
    super.initState();
    _commentStore = CommentStore(
      (page) => widget.api.fetchComments(
        resourceId: widget.resource.id,
        deviceType: widget.deviceType.value,
        page: page,
      ),
    );
    _commentStore.refresh();
    widget.deviceType.addListener(_handleDeviceChange);
    _generatePalette();
  }

  @override
  void dispose() {
    widget.deviceType.removeListener(_handleDeviceChange);
    _commentStore.dispose();
    super.dispose();
  }

  void _handleDeviceChange() {
    _commentStore.refresh();
  }

  Future<void> _generatePalette() async {
    try {
      final generator = await PaletteGenerator.fromImageProvider(
        NetworkImage(widget.resource.previewUrl),
        maximumColorCount: 12,
      );
      final candidate = generator.dominantColor?.color ??
          generator.vibrantColor?.color ??
          generator.darkVibrantColor?.color ??
          generator.lightVibrantColor?.color;
      if (!mounted || candidate == null) return;
      final isDark = candidate.computeLuminance() < 0.5;
      setState(() {
        _heroForegroundColor = isDark ? Colors.white : Colors.black87;
      });
    } catch (_) {
      // ignore palette failure
    }
  }

  @override
  Widget build(BuildContext context) {
    final resource = widget.resource;
    final colorScheme = Theme.of(context).colorScheme;
    final defaultForeground =
        Theme.of(context).appBarTheme.foregroundColor ??
            Theme.of(context).colorScheme.onSurface;
    final foregroundColor =
        _isHeroCollapsed ? defaultForeground : _heroForegroundColor;
    final heroTag = _previewHeroTag(resource);
    return Scaffold(
      body: NotificationListener<ScrollNotification>(
        onNotification: (notification) {
          if (notification.metrics.axis != Axis.vertical) return false;
          final collapsed =
              notification.metrics.pixels >= (_heroHeight - kToolbarHeight);
          if (collapsed != _isHeroCollapsed && mounted) {
            setState(() {
              _isHeroCollapsed = collapsed;
            });
          }
          return false;
        },
        child: CustomScrollView(
          slivers: [
            SliverAppBar(
              pinned: true,
              expandedHeight: _heroHeight,
              centerTitle: false,
              backgroundColor: _isHeroCollapsed
                  ? Theme.of(context).colorScheme.surface
                  : Colors.transparent,
              elevation: _isHeroCollapsed ? 2 : 0,
              forceElevated: _isHeroCollapsed,
              scrolledUnderElevation: 0,
              title: AnimatedOpacity(
                opacity: _isHeroCollapsed ? 1 : 0,
                duration: const Duration(milliseconds: 200),
                child: Text(
                  resource.name,
                  style: TextStyle(color: foregroundColor),
                ),
              ),
              leading: IconButton(
                icon: const Icon(Icons.arrow_back),
                color: _isHeroCollapsed
                    ? foregroundColor
                    : Colors.white,
                onPressed: () => Navigator.of(context).pop(),
              ),
              actions: [
                IconButton(
                  tooltip: '获取下载链接',
                  color: _isHeroCollapsed
                      ? foregroundColor
                      : Colors.white,
                  icon: const Icon(Icons.file_download_outlined),
                  onPressed: () => widget.onDownloadTap(resource),
                ),
              ],
              flexibleSpace: LayoutBuilder(
                builder: (context, constraints) {
                  return Stack(
                    fit: StackFit.expand,
                    children: [
                      AnimatedOpacity(
                        opacity: _isHeroCollapsed ? 0 : 1,
                        duration: const Duration(milliseconds: 200),
                        child: Stack(
                          fit: StackFit.expand,
                          children: [
                            Hero(
                              tag: heroTag,
                              child: Image.network(
                                resource.previewUrl,
                                fit: BoxFit.cover,
                                errorBuilder: (_, __, ___) => Container(
                                  color: colorScheme.surfaceContainerHighest,
                                  alignment: Alignment.center,
                                  child: const Icon(Icons.image_not_supported),
                                ),
                              ),
                            ),
                            DecoratedBox(
                              decoration: BoxDecoration(
                                gradient: LinearGradient(
                                  colors: [
                                    Colors.transparent,
                                    Colors.black.withValues(alpha: 0.7),
                                  ],
                                  begin: Alignment.topCenter,
                                  end: Alignment.bottomCenter,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      Positioned(
                        left: 16,
                        right: 16,
                        bottom: 24,
                        child: AnimatedOpacity(
                          opacity: _isHeroCollapsed ? 0 : 1,
                          duration: const Duration(milliseconds: 200),
                          child: Text(
                            resource.name,
                            style: Theme.of(context)
                                .textTheme
                                .headlineSmall
                                ?.copyWith(
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold,
                                ),
                          ),
                        ),
                      ),
                    ],
                  );
                },
              ),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _MetaInfoCard(resource: resource),
                    const SizedBox(height: 24),
                    _SectionHeader(icon: Icons.description_outlined, title: '资源简介'),
                    const SizedBox(height: 8),
                    Text(
                      resource.shortDescription,
                      style: Theme.of(context).textTheme.bodyLarge,
                    ),
                    const SizedBox(height: 24),
                  ],
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
            sliver: SliverToBoxAdapter(
              child: CommentsSection(store: _commentStore),
            ),
            ),
          ],
        ),
      ),
    );
  }

}

class _DateInfo extends StatelessWidget {
  const _DateInfo({required this.resource});

  final WatchfaceResource resource;

  @override
  Widget build(BuildContext context) {
    final textStyle = Theme.of(context).textTheme.bodyMedium?.copyWith(
          color: Theme.of(context).colorScheme.onSurfaceVariant,
        );
    final created = resource.createdAt;
    final updated = resource.updatedAt;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (created != null)
          Text('创建：${_formatDate(created)}', style: textStyle),
        if (updated != null) ...[
          if (created != null) const SizedBox(height: 4),
          Text('更新：${_formatDate(updated)}', style: textStyle),
        ],
        if (created == null && updated == null)
          Text('更新时间未知', style: textStyle),
      ],
    );
  }

  String _formatDate(DateTime date) {
    final local = date.toLocal();
    String two(int value) => value.toString().padLeft(2, '0');
    return '${local.year}-${two(local.month)}-${two(local.day)}';
  }
}

String _previewHeroTag(WatchfaceResource resource) =>
    'watchface-preview-${resource.id}';

class _MetaInfoCard extends StatelessWidget {
  const _MetaInfoCard({required this.resource});

  final WatchfaceResource resource;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final base = theme.colorScheme.surface;
    final overlay = theme.brightness == Brightness.dark
        ? Colors.white.withValues(alpha: 0.08)
        : Colors.black.withValues(alpha: 0.04);
    final cardColor = Color.alphaBlend(overlay, base);
    final stats = [
      _MetaStat(Icons.download_outlined, '下载', '${resource.downloads}'),
      _MetaStat(Icons.visibility_outlined, '浏览', '${resource.views}'),
      _MetaStat(
        Icons.storage_outlined,
        '体积',
        '${(resource.fileSize / 1024).toStringAsFixed(0)} KB',
      ),
    ];

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: cardColor,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              CircleAvatar(
                radius: 20,
                backgroundColor: theme.colorScheme.surface,
                child: const Icon(Icons.person_outline),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  resource.creator,
                  style: theme.textTheme.titleMedium,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          _DateInfo(resource: resource),
          const SizedBox(height: 16),
          Row(
            children: [
              for (var i = 0; i < stats.length; i++) ...[
                if (i != 0) const SizedBox(width: 12),
                Expanded(child: _StatTile(stat: stats[i])),
              ],
            ],
          )
        ],
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.icon, required this.title});

  final IconData icon;
  final String title;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        Icon(icon, size: 18, color: theme.colorScheme.primary),
        const SizedBox(width: 8),
        Text(
          title,
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    );
  }
}

class _MetaStat {
  const _MetaStat(this.icon, this.label, this.value);

  final IconData icon;
  final String label;
  final String value;
}

class _StatTile extends StatelessWidget {
  const _StatTile({required this.stat});

  final _MetaStat stat;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 10),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        color: theme.colorScheme.surface,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(stat.icon, size: 18, color: theme.colorScheme.primary),
          const SizedBox(height: 6),
          Text(
            stat.value,
            style: theme.textTheme.titleMedium,
          ),
          Text(
            stat.label,
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}
