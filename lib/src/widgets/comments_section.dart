import 'package:flutter/material.dart';

import '../models/comment.dart';
import '../state/resource_store.dart';

class CommentsSection extends StatelessWidget {
  const CommentsSection({super.key, required this.store});

  final CommentStore store;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return AnimatedBuilder(
      animation: store,
      builder: (context, _) {
        final comments = store.items;

        Widget content;
        if (store.isFetching && comments.isEmpty) {
          content = const Padding(
            padding: EdgeInsets.symmetric(vertical: 16),
            child: Center(child: CircularProgressIndicator()),
          );
        } else if (store.error != null && comments.isEmpty) {
          content = _ErrorHint(
            message: store.error!,
            onRetry: store.refresh,
          );
        } else if (comments.isEmpty) {
          content = Text(
            '暂无评论',
            style: theme.textTheme.bodyMedium,
          );
        } else {
          content = Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ...comments.map((comment) => _CommentTile(comment: comment)),
              _buildFooter(context, store),
            ],
          );
        }

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.reviews_outlined,
                    size: 18, color: theme.colorScheme.primary),
                const SizedBox(width: 8),
                Text(
                  '资源评论',
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            content,
          ],
        );
      },
    );
  }

  Widget _buildFooter(BuildContext context, CommentStore store) {
    if (store.items.isEmpty) {
      return const SizedBox.shrink();
    }
    if (store.error != null) {
      return _ErrorHint(
        message: store.error!,
        onRetry: store.loadMore,
      );
    }
    if (store.isFetching) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 8),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (!store.hasMore) {
      return const SizedBox.shrink();
    }
    return Align(
      alignment: Alignment.centerLeft,
      child: TextButton.icon(
        onPressed: store.loadMore,
        icon: const Icon(Icons.expand_more),
        label: const Text('加载更多评论'),
      ),
    );
  }
}

class _ErrorHint extends StatelessWidget {
  const _ErrorHint({required this.message, required this.onRetry});

  final String message;
  final Future<void> Function() onRetry;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '评论加载失败：$message',
          style: Theme.of(context)
              .textTheme
              .bodyMedium
              ?.copyWith(color: Theme.of(context).colorScheme.error),
        ),
        const SizedBox(height: 8),
        FilledButton.tonal(
          onPressed: onRetry,
          child: const Text('重试'),
        ),
      ],
    );
  }
}

class _CommentTile extends StatelessWidget {
  const _CommentTile({required this.comment});

  final Comment comment;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final base = theme.colorScheme.surface;
    final overlay = theme.brightness == Brightness.dark
        ? Colors.white.withValues(alpha: 0.06)
        : Colors.black.withValues(alpha: 0.03);
    final bgColor = Color.alphaBlend(overlay, base);
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 6),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CircleAvatar(
            backgroundImage: comment.avatar.isNotEmpty
                ? NetworkImage(comment.avatar)
                : null,
            child: comment.avatar.isEmpty
                ? Text(
                    comment.nickname.characters.isNotEmpty
                        ? comment.nickname.characters.first
                        : '?',
                  )
                : null,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  comment.nickname,
                  style: theme.textTheme.titleSmall,
                ),
                const SizedBox(height: 4),
                Text(
                  comment.content,
                  style: theme.textTheme.bodyMedium,
                ),
                const SizedBox(height: 6),
                Text(
                  _formatTime(comment.time),
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  String _formatTime(DateTime time) {
    final t = time.toLocal();
    String two(int value) => value.toString().padLeft(2, '0');
    return '${t.year}-${two(t.month)}-${two(t.day)} ${two(t.hour)}:${two(t.minute)}';
  }
}
