class WatchfaceResource {
  WatchfaceResource({
    required this.id,
    required this.name,
    required this.creator,
    required this.description,
    required this.previewUrl,
    required this.downloads,
    required this.views,
    required this.deviceType,
    required this.isShare,
    required this.createdAt,
    required this.updatedAt,
    required this.isRecommend,
    required this.fileSize,
    this.mitanId,
    this.mitanType,
  });

  final int id;
  final String name;
  final String creator;
  final String description;
  final String previewUrl;
  final int downloads;
  final int views;
  final String deviceType;
  final bool isShare;
  final DateTime? createdAt;
  final DateTime? updatedAt;
  final bool isRecommend;
  final int fileSize;
  final String? mitanId;
  final String? mitanType;

  DateTime? get lastUpdatedAt => updatedAt ?? createdAt;

  String get shortDescription {
    if (description.trim().isEmpty) {
      return '暂无简介';
    }
    final sanitized = description.replaceAll('\\n', '\n').trim();
    return sanitized;
  }

  factory WatchfaceResource.fromJson(Map<String, dynamic> json) {
    final createdAt = _parseTimestamp(json['createdAt'] ?? json['createtime']);
    final updatedAt = _parseTimestamp(json['updatedAt'] ?? json['updatetime']);
    return WatchfaceResource(
      id: _parseInt(json['id']),
      name: (json['name'] as String?)?.trim() ?? '未命名资源',
      creator: (json['nickname'] as String?)?.trim().isNotEmpty == true
          ? json['nickname'].toString().trim()
          : (json['username'] as String?)?.trim() ?? '未知创作者',
      description: json['desc'] as String? ?? '',
      previewUrl: json['preview'] as String? ?? '',
      downloads: _parseInt(json['downloadTimes']),
      views: _parseInt(json['views']),
      deviceType: json['type'] as String? ?? '',
      isShare: (json['isShare'] as int? ?? 0) == 1,
      createdAt: createdAt,
      updatedAt: updatedAt,
      isRecommend: (json['isRecommend'] as int? ?? 0) == 1,
      fileSize: _parseInt(json['filesize']),
      mitanId: (json['mitantid'] as String?)?.trim(),
      mitanType: (json['mitantype'] as String?)?.trim(),
    );
  }

  static int _parseInt(dynamic value) {
    if (value == null) {
      return 0;
    }
    if (value is int) {
      return value;
    }
    if (value is double) {
      return value.toInt();
    }
    return int.tryParse(value.toString()) ?? 0;
  }

  static DateTime? _parseTimestamp(dynamic value) {
    if (value == null) return null;
    if (value is num) {
      return DateTime.fromMillisecondsSinceEpoch(value.toInt());
    }
    final parsed = int.tryParse(value.toString());
    if (parsed != null) {
      return DateTime.fromMillisecondsSinceEpoch(parsed);
    }
    return null;
  }
}
