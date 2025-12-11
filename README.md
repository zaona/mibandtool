# mibandtool

A new Flutter project.

## Getting Started

This project is a starting point for a Flutter application.

A few resources to get you started if this is your first Flutter project:

- [Lab: Write your first Flutter app](https://docs.flutter.dev/get-started/codelab)
- [Cookbook: Useful Flutter samples](https://docs.flutter.dev/cookbook)

For help getting started with Flutter development, view the
[online documentation](https://docs.flutter.dev/), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

## 运行与构建

```bash
# 调试运行
flutter run

# 发布 APK（单包）
flutter build apk --release --tree-shake-icons

# 发布 APK（按架构拆分）
flutter build apk --release --split-per-abi --obfuscate --split-debug-info=build/app/outputs/symbols --tree-shake-icons

# 发布 App Bundle
flutter build appbundle --release --obfuscate --split-debug-info=build/app/outputs/symbols --tree-shake-icons
```
