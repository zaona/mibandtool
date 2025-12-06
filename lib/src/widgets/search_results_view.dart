import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../models/watchface_resource.dart';
import '../state/resource_store.dart';
import 'watchface_card.dart';

class SearchResultsView extends StatefulWidget {
  const SearchResultsView({
    super.key,
    required this.store,
    required this.onResourceTap,
  });

  final SearchStore store;
  final ValueChanged<WatchfaceResource> onResourceTap;

  @override
  State<SearchResultsView> createState() => _SearchResultsViewState();
}

class _SearchResultsViewState extends State<SearchResultsView> {
  final _scrollController = ScrollController();

  SearchStore get store => widget.store;

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
        _scrollController.position.maxScrollExtent - 200;
    if (_scrollController.position.pixels >= threshold) {
      store.loadMore();
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: store,
      builder: (context, _) {
        if (store.keyword.isEmpty) {
          return const _Hint(
            message: '输入关键词并点击搜索来查找资源',
          );
        }
        if (store.isFetching && store.items.isEmpty) {
          return const Center(child: CircularProgressIndicator());
        }
        if (store.error != null && store.items.isEmpty) {
          return _Hint(
            message: store.error!,
            onRetry: () => store.search(store.keyword),
          );
        }
        if (store.items.isEmpty) {
          return const _Hint(message: '没有找到匹配的资源');
        }
        return RefreshIndicator(
          onRefresh: () => store.search(store.keyword),
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
                sliver: SliverToBoxAdapter(child: _buildLoader()),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildLoader() {
    if (store.error != null) {
      return _Hint(
        message: store.error!,
        onRetry: store.loadMore,
      );
    }
    if (!store.hasMore) {
      return const SizedBox(height: 16);
    }
    if (!store.isFetching) {
      return const SizedBox(height: 24);
    }
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 24),
      child: Center(child: CircularProgressIndicator()),
    );
  }
}

class _Hint extends StatelessWidget {
  const _Hint({required this.message, this.onRetry});

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
            Text(
              message,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyLarge,
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
