<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# kiwi-linter-idea Changelog

## [Unreleased]

## [1.0.0] - 2026-01-27

### Added

- **Hover 预览功能**：鼠标悬停在 DPN key 上显示中英文文案信息
- **文案缓存服务**：自动从远程仓库加载并缓存中英文 properties 文件
- **实时缓存更新**：提交文案后自动更新本地缓存

### Changed

- 优化 I18N 模式识别：支持第三项为字符串或枚举的 ErrorCode 定义
- 精确过滤：只识别 `DPN.` 开头的 key，避免误识别
- 快捷键提示同时显示 Mac (`⌘⌥I`) 和 Windows (`Ctrl+Alt+I`)

### Fixed

- 修复第三参数为字符串时无法识别的问题

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

[Unreleased]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v0.0.9...v1.0.0
[0.0.9]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v0.0.8...v0.0.9
[0.0.8]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v0.0.7...v0.0.8
[0.0.7]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v0.0.6...v0.0.7
[0.0.6]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v0.0.5...v0.0.6
[0.0.5]: https://github.com/chenweichang1/kiwi-linter-idea/compare/v0.0.4...v0.0.5
[0.0.4]: https://github.com/chenweichang1/kiwi-linter-idea/commits/v0.0.4
