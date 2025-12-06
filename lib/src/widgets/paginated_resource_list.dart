import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../models/watchface_resource.dart';
import '../state/resource_store.dart';
import 'watchface_card.dart';

class PaginatedResourceList extends StatefulWidget {
  const PaginatedResourceList({
    super.key,
    required this.store,
    required this.onResourceTap,
  });

  final PaginatedResourceStore store;
  final ValueChanged<WatchfaceResource> onResourceTap;

  @override
  State<PaginatedResourceList> createState() => _PaginatedResourceListState();
}

class _PaginatedResourceListState extends State<PaginatedResourceList> {
  final _scrollController = ScrollController();

  PaginatedResourceStore get store => widget.store;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController
      ..removeListener(_onScroll)
      ..dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_scrollController.hasClients) return;
    final threshold =
        _scrollController.position.maxScrollExtent - 200; // load slightly early
    if (_scrollController.position.pixels >= threshold) {
      store.loadMore();
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: store,
      builder: (context, _) {
        if (store.isFetching && store.items.isEmpty) {
          return const Center(child: CircularProgressIndicator());
        }
        if (store.error != null && store.items.isEmpty) {
          return _ErrorHint(
            message: store.error!,
            onRetry: store.refresh,
          );
        }
        if (store.items.isEmpty) {
          return const _ErrorHint(message: '暂无相关资源');
        }
        return RefreshIndicator(
          onRefresh: store.refresh,
          child: CustomScrollView(
            controller: _scrollController,
            slivers: [
              SliverPadding(
                padding: const EdgeInsets.all(12),
                sliver: SliverMasonryGrid.count(
                  crossAxisCount: 2,
                  mainAxisSpacing: 8,
                  crossAxisSpacing: 8,
                  childCount: store.items.length,
                  itemBuilder: (context, index) {
                    final item = store.items[index];
                    return WatchfaceCard(
                      resource: item,
                      onTap: () => widget.onResourceTap(item),
                    );
                  },
                ),
              ),
              SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                sliver: SliverToBoxAdapter(
                  child: _buildLoader(context),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildLoader(BuildContext context) {
    if (store.error != null) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 16),
        child: _ErrorHint(
          message: store.error!,
          onRetry: store.loadMore,
        ),
      );
    }
    if (!store.hasMore) {
      return const SizedBox(height: 16);
    }
    if (store.isFetching) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 24),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    return const SizedBox(height: 24);
  }
}

class _ErrorHint extends StatelessWidget {
  const _ErrorHint({required this.message, this.onRetry});

  final String message;
  final Future<void> Function()? onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.error_outline,
              size: 40,
              color: Theme.of(context).colorScheme.error,
            ),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
            ),
            if (onRetry != null) ...[
              const SizedBox(height: 12),
              FilledButton(
                onPressed: onRetry,
                child: const Text('重试'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
