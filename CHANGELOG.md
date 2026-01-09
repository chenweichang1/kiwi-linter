<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# kiwi-linter-idea Changelog

## [Unreleased]

## [0.0.9] - 2026-01-09

### Added

- 工具窗口标题栏添加「提交历史」和「持续集成」快捷链接

### Fixed

- 修复多行代码单个提取不生效的问题（支持换行的 ErrorCode 定义）

## [0.0.8] - 2026-01-08

### Changed

- 移除批量手动录入菜单项（与侧边栏功能重复）
- 批量提取添加快捷键 `Ctrl+Alt+Shift+B`

## [0.0.7] - 2026-01-08

### Changed

- 精简菜单项：移除单条手动录入菜单项
- 重构工具窗口交互：统一使用批量录入表格
- 提取操作直接添加到表格，无需确认对话框
- 优化输入框布局，适配窄侧边栏

### Added

- 自动去重功能：添加条目时检查是否已存在相同 Key
- 状态栏动态更新：修改配置后自动刷新显示

### Fixed

- 修复状态栏不更新项目 ID 的问题

## [0.0.6] - 2026-01-07

### Added

- 新增批量录入工具窗口
- 支持表格批量编辑和提交

## [0.0.5] - 2026-01-07

### Added

- 新增 Key 按字母顺序排列
- 优化提交统计信息显示

## [0.0.4] - 2026-01-06

### Added

- Initial release to JetBrains Marketplace
- 从代码自动提取 I18N 文案
- 批量提取文件中的 I18N 文案
- 手动录入 I18N 文案
- 一键提交到远程仓库

[Unreleased]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v0.0.9...HEAD
[0.0.9]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v0.0.8...v0.0.9
[0.0.8]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v0.0.7...v0.0.8
[0.0.7]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v0.0.6...v0.0.7
[0.0.6]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v0.0.5...v0.0.6
[0.0.5]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v0.0.4...v0.0.5
[0.0.4]: https://github.com/chenweichang1/kiwi-linter-idea/commits/v0.0.4
