import 'package:flutter/material.dart';

class DevicePreset {
  const DevicePreset(this.label, this.value);

  final String label;
  final String value;
}

const devicePresets = [
  DevicePreset('小米手环10', 'o66'),
  DevicePreset('小米手环9', 'n66'),
  DevicePreset('小米手环9Pro', 'n67'),
  DevicePreset('小米手环8', 'mi8'),
  DevicePreset('小米手环8Pro', 'mi8pro'),
  DevicePreset('小米手环7', 'mi7'),
  DevicePreset('小米手环7Pro', 'mi7pro'),
  DevicePreset('小米手表S3/S4Sport', 'ws3'),
  DevicePreset('小米手表S4', 'o62'),
  DevicePreset('红米手表4', 'rw4'),
  DevicePreset('红米手表5', 'o65'),
  DevicePreset('红米手表6', 'p65'),
];

class DeviceTabs extends StatefulWidget {
  const DeviceTabs({super.key, required this.deviceType});

  final ValueNotifier<String> deviceType;

  @override
  State<DeviceTabs> createState() => _DeviceTabsState();
}

class _DeviceTabsState extends State<DeviceTabs>
    with SingleTickerProviderStateMixin {
  late final TabController _controller;
  late final VoidCallback _deviceListener;

  @override
  void initState() {
    super.initState();
    final initialIndex = _resolveIndex(widget.deviceType.value);
    _controller = TabController(
      length: devicePresets.length,
      vsync: this,
      initialIndex: initialIndex,
    );
    _controller.addListener(_handleTabChanged);
    _deviceListener = () {
      final targetIndex = _resolveIndex(widget.deviceType.value);
      if (targetIndex != _controller.index) {
        _controller.animateTo(targetIndex);
      }
    };
    widget.deviceType.addListener(_deviceListener);
  }

  @override
  void didUpdateWidget(covariant DeviceTabs oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.deviceType != widget.deviceType) {
      oldWidget.deviceType.removeListener(_deviceListener);
      widget.deviceType.addListener(_deviceListener);
      _deviceListener();
    }
  }

  @override
  void dispose() {
    widget.deviceType.removeListener(_deviceListener);
    _controller
      ..removeListener(_handleTabChanged)
      ..dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerLeft,
      child: TabBar(
        controller: _controller,
        isScrollable: true,
        tabAlignment: TabAlignment.start,
        padding: EdgeInsets.zero,
        labelPadding: const EdgeInsets.symmetric(horizontal: 16),
        tabs: [
          for (final preset in devicePresets) Tab(text: preset.label),
        ],
      ),
    );
  }

  void _handleTabChanged() {
    if (_controller.indexIsChanging) return;
    final selected = devicePresets[_controller.index].value;
    if (widget.deviceType.value != selected) {
      widget.deviceType.value = selected;
    }
  }

  int _resolveIndex(String value) {
    final index =
        devicePresets.indexWhere((preset) => preset.value == value);
    return index == -1 ? 0 : index;
  }
}
