import 'package:flutter/foundation.dart';

import '../api/miband_api.dart';
import '../models/comment.dart';
import '../models/watchface_resource.dart';

typedef ResourcePageLoader = Future<List<WatchfaceResource>> Function(int page);

class PaginatedResourceStore extends ChangeNotifier {
  PaginatedResourceStore(this._loader);

  ResourcePageLoader _loader;
  final List<WatchfaceResource> _items = [];
  int _page = 1;
  bool _isFetching = false;
  bool _hasMore = true;
  String? _error;

  List<WatchfaceResource> get items => List.unmodifiable(_items);
  bool get isFetching => _isFetching;
  bool get hasMore => _hasMore;
  String? get error => _error;

  Future<void> refresh() async {
    _page = 1;
    _items.clear();
    _hasMore = true;
    _error = null;
    notifyListeners();
    await _fetch();
  }

  Future<void> loadMore() async {
    if (_isFetching || !_hasMore) return;
    await _fetch();
  }

  void replaceLoader(ResourcePageLoader loader, {bool reload = true}) {
    _loader = loader;
    if (reload) {
      refresh();
    }
  }

  Future<void> _fetch() async {
    _isFetching = true;
    _error = null;
    notifyListeners();
    try {
      final result = await _loader(_page);
      if (_page == 1) {
        _items
          ..clear()
          ..addAll(result);
      } else {
        _items.addAll(result);
      }
      _hasMore = result.isNotEmpty;
      if (_hasMore) {
        _page += 1;
      }
    } catch (error) {
      _error = error is ApiException ? error.message : error.toString();
    } finally {
      _isFetching = false;
      notifyListeners();
    }
  }
}

class SearchStore extends ChangeNotifier {
  SearchStore(this._searcher);

  final Future<List<WatchfaceResource>> Function(
    String keyword,
    int page,
  ) _searcher;

  final List<WatchfaceResource> _items = [];
  bool _isFetching = false;
  bool _hasMore = true;
  int _page = 1;
  String _keyword = '';
  String? _error;

  List<WatchfaceResource> get items => List.unmodifiable(_items);
  bool get isFetching => _isFetching;
  bool get hasMore => _hasMore;
  String? get error => _error;
  String get keyword => _keyword;

  Future<void> search(String keyword) async {
    if (keyword.trim().isEmpty) {
      _keyword = '';
      _items.clear();
      _error = null;
      _hasMore = true;
      _page = 1;
      notifyListeners();
      return;
    }
    _keyword = keyword;
    _page = 1;
    _items.clear();
    _hasMore = true;
    await _fetch();
  }

  Future<void> loadMore() async {
    if (_isFetching || !_hasMore || _keyword.isEmpty) return;
    await _fetch();
  }

  Future<void> _fetch() async {
    _isFetching = true;
    _error = null;
    notifyListeners();
    try {
      final result = await _searcher(_keyword, _page);
      if (_page == 1) {
        _items
          ..clear()
          ..addAll(result);
      } else {
        _items.addAll(result);
      }
      _hasMore = result.isNotEmpty;
      if (_hasMore) {
        _page += 1;
      }
    } catch (error) {
      _error = error is ApiException ? error.message : error.toString();
    } finally {
      _isFetching = false;
      notifyListeners();
    }
  }
}

typedef CommentPageLoader = Future<List<Comment>> Function(int page);

class CommentStore extends ChangeNotifier {
  CommentStore(this._loader);

  final CommentPageLoader _loader;
  final List<Comment> _items = [];
  int _page = 1;
  bool _isFetching = false;
  bool _hasMore = true;
  String? _error;

  List<Comment> get items => List.unmodifiable(_items);
  bool get isFetching => _isFetching;
  bool get hasMore => _hasMore;
  String? get error => _error;

  Future<void> refresh() async {
    _page = 1;
    _items.clear();
    _hasMore = true;
    _error = null;
    notifyListeners();
    await _fetch();
  }

  Future<void> loadMore() async {
    if (_isFetching || !_hasMore) return;
    await _fetch();
  }

  Future<void> _fetch() async {
    _isFetching = true;
    _error = null;
    notifyListeners();
    try {
      final result = await _loader(_page);
      if (_page == 1) {
        _items
          ..clear()
          ..addAll(result);
      } else {
        _items.addAll(result);
      }
      _hasMore = result.isNotEmpty;
      if (_hasMore) {
        _page += 1;
      }
    } catch (error) {
      _error = error is ApiException ? error.message : error.toString();
    } finally {
      _isFetching = false;
      notifyListeners();
    }
  }
}
