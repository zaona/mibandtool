import 'package:flutter/material.dart';

import '../models/watchface_resource.dart';
import '../state/resource_store.dart';
import '../widgets/device_type_selector.dart';
import '../widgets/search_results_view.dart';

class SearchPage extends StatefulWidget {
  const SearchPage({
    super.key,
    required this.store,
    required this.deviceType,
    required this.onResourceTap,
  });

  final SearchStore store;
  final ValueNotifier<String> deviceType;
  final ValueChanged<WatchfaceResource> onResourceTap;

  @override
  State<SearchPage> createState() => _SearchPageState();
}

class _SearchPageState extends State<SearchPage> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.store.keyword);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        titleSpacing: 0,
        title: _buildSearchField(context),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(48),
          child: DeviceTabs(deviceType: widget.deviceType),
        ),
      ),
      body: SafeArea(
        child: SearchResultsView(
          store: widget.store,
          onResourceTap: widget.onResourceTap,
        ),
      ),
    );
  }

  Widget _buildSearchField(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: TextField(
        controller: _controller,
        textInputAction: TextInputAction.search,
        onSubmitted: (_) => _triggerSearch(),
        decoration: InputDecoration(
          hintText: '输入关键词（作者、资源名称等）',
          filled: true,
          contentPadding: const EdgeInsets.symmetric(horizontal: 16),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(32),
            borderSide: BorderSide.none,
          ),
          suffixIcon: IconButton(
            tooltip: '搜索',
            icon: const Icon(Icons.search),
            onPressed: _triggerSearch,
          ),
        ),
      ),
    );
  }

  void _triggerSearch() {
    FocusScope.of(context).unfocus();
    widget.store.search(_controller.text.trim());
  }

}
