class Comment {
  Comment({
    required this.id,
    required this.nickname,
    required this.avatar,
    required this.content,
    required this.time,
    required this.isDeleted,
  });

  final int id;
  final String nickname;
  final String avatar;
  final String content;
  final DateTime time;
  final bool isDeleted;

  factory Comment.fromJson(Map<String, dynamic> json) {
    return Comment(
      id: (json['id'] as num?)?.toInt() ?? 0,
      nickname: json['nickname'] as String? ?? '匿名',
      avatar: json['avator'] as String? ?? '',
      content: json['content'] as String? ?? '',
      time: DateTime.fromMillisecondsSinceEpoch(
        (json['time'] as num? ?? 0).toInt(),
      ),
      isDeleted: json['delflag'] as bool? ?? false,
    );
  }
}
