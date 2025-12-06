import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/watchface_resource.dart';
import '../models/comment.dart';

/// Exception thrown when the remote API returns a non-successful response.
class ApiException implements Exception {
  ApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => 'ApiException($statusCode): $message';
}

/// Lightweight client for the表盘自定义工具 API.
class MiBandApi {
  MiBandApi({
    http.Client? client,
    Uri? baseUri,
  })  : _client = client ?? http.Client(),
        _baseUri = baseUri ?? Uri.parse('https://www.mibandtool.club:9073');

  final http.Client _client;
  final Uri _baseUri;

  Map<String, String> _headers(String deviceType) => {
        'type': deviceType,
      };

  Future<List<WatchfaceResource>> fetchHomeResources({
    required String deviceType,
    String tag = '0',
    required int page,
    int pageSize = 10,
  }) {
    final path = '/watchface/listbytag/$tag/$page/$pageSize/9999';
    return _fetchResourceList(path, deviceType);
  }

  Future<List<WatchfaceResource>> fetchPaidResources({
    required String deviceType,
    required int page,
    int pageSize = 10,
  }) {
    final path = '/watchface/list/recommendsbytag/$page/$pageSize/9999';
    return _fetchResourceList(path, deviceType);
  }

  Future<List<WatchfaceResource>> searchResources({
    required String keyword,
    required String deviceType,
    required int page,
  }) async {
    final uri = _baseUri.replace(
      path: '/watchface/searchForPage',
      queryParameters: {
        'keyword': keyword,
        'page': '$page',
      },
    );
    final response = await _client.post(uri, headers: _headers(deviceType));
    final json = _decode(response);
    final list = (json['data'] as List? ?? [])
        .map((entry) => WatchfaceResource.fromJson(entry))
        .toList();
    return list;
  }

  Future<String> fetchDownloadUrl({
    required int resourceId,
    required String deviceType,
  }) async {
    final uri = _baseUri.replace(
      path: '/watchface/downloadUsr',
      queryParameters: {'id': '$resourceId'},
    );
    final response = await _client.post(uri, headers: _headers(deviceType));
    final json = _decode(response);
    final url = json['data'] as String?;
    if (url == null || url.isEmpty) {
      throw ApiException('未能获取下载链接，请稍后再试');
    }
    return url;
  }

  Future<List<Comment>> fetchComments({
    required int resourceId,
    required String deviceType,
    String? openId,
    int page = 1,
  }) async {
    final uri = _baseUri.replace(
      path: '/comment/get',
      queryParameters: {
        'relationid': '$resourceId',
        'type': 'wf',
        'page': '$page',
      },
    );
    final headers = {
      ..._headers(deviceType),
      if (openId != null && openId.isNotEmpty) 'openId': openId,
    };
    final response = await _client.post(
      uri,
      headers: headers,
    );
    final json = _decode(response);
    final data = json['data'] as List? ?? [];
    return data.map((e) => Comment.fromJson(e)).toList();
  }

  Future<List<WatchfaceResource>> _fetchResourceList(
    String path,
    String deviceType,
  ) async {
    final uri = _baseUri.replace(path: path);
    final response = await _client.get(uri, headers: _headers(deviceType));
    final json = _decode(response);
    final data = json['data'] as List? ?? [];
    return data.map((entry) => WatchfaceResource.fromJson(entry)).toList();
  }

  Map<String, dynamic> _decode(http.Response response) {
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException(
        '请求失败(${response.statusCode})',
        statusCode: response.statusCode,
      );
    }
    try {
      return json.decode(utf8.decode(response.bodyBytes))
          as Map<String, dynamic>;
    } catch (error) {
      throw ApiException('解析响应失败: $error',
          statusCode: response.statusCode);
    }
  }

  void dispose() => _client.close();
}
