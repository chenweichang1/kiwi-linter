# Kiwi-linter

![Build](https://github.com/chenweichang1/kiwi-linter-idea/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/29609.svg)](https://plugins.jetbrains.com/plugin/29609-kiwi-linter)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/29609.svg)](https://plugins.jetbrains.com/plugin/29609-kiwi-linter)

> 🥝 一款强大的 IntelliJ IDEA 国际化（I18N）文案管理插件，帮助开发者快速提取、录入并提交 I18N 文案到远程仓库。

<!-- Plugin description -->

**Kiwi-linter** is a powerful IntelliJ IDEA plugin for managing I18N internationalization entries. It helps developers quickly extract, input, and commit I18N entries to remote repositories.

## ✨ Features

- 🔍 **Auto Extract** - Automatically detect ErrorCode enum patterns and extract key-value pairs
- 📦 **Batch Entry** - Batch input and manage multiple I18N entries in the tool window
- 🚀 **One-click Push** - Directly commit to remote properties repository
- 🔄 **Smart Merge** - Automatically detect duplicates, update existing entries, skip unchanged ones
- 🎯 **Auto Dedup** - Automatically check for duplicate keys when adding entries
<!-- Plugin description end -->

---

## 📋 目录

- [安装](#-安装)
- [配置](#-配置)
- [使用方法](#-使用方法)
- [快捷键](#-快捷键)
- [功能详解](#-功能详解)
- [常见问题](#-常见问题)

---

## 📥 安装

### 方式一：通过 IDE 插件市场安装（推荐）

1. 打开 IntelliJ IDEA
2. 进入 <kbd>Settings/Preferences</kbd> → <kbd>Plugins</kbd> → <kbd>Marketplace</kbd>
3. 搜索 **"Kiwi-linter"**
4. 点击 <kbd>Install</kbd> 安装
5. 重启 IDE

### 方式二：通过 JetBrains Marketplace 网站安装

1. 访问 [Kiwi-linter 插件页面](https://plugins.jetbrains.com/plugin/29609-kiwi-linter)
2. 点击 <kbd>Install to IDE</kbd> 按钮
3. 按提示完成安装

### 方式三：手动安装

1. 下载 [最新版本](https://github.com/chenweichang1/kiwi-linter-idea/releases/latest) 的 `.zip` 文件
2. 进入 <kbd>Settings/Preferences</kbd> → <kbd>Plugins</kbd> → <kbd>⚙️</kbd> → <kbd>Install plugin from disk...</kbd>
3. 选择下载的 `.zip` 文件
4. 重启 IDE

---

## ⚙️ 配置

首次使用前，需要配置插件连接到你的代码仓库。

### 打开设置

<kbd>Settings/Preferences</kbd> → <kbd>Tools</kbd> → <kbd>Kiwi-linter</kbd>

### 配置项说明

| 配置项                  | 说明                            | 示例                                                |
| ----------------------- | ------------------------------- | --------------------------------------------------- |
| **项目 ID**             | Code 平台的项目 ID（必填）      | `12345`                                             |
| **目标分支**            | 提交文案的目标分支              | `master`                                            |
| **Properties 文件路径** | 中文 properties 文件的相对路径  | `src/main/resources/i18n/messages_zh_CN.properties` |
| **Private Token**       | Code 平台的个人访问令牌（必填） | `glpat-xxxx`                                        |
| **提交信息模板**        | Git commit message 模板         | `feat: 添加 I18N 文案 - {key}`                      |

### 获取 Private Token

1. 登录阿里 Code 平台
2. 进入个人设置 → Access Tokens
3. 创建新 Token，勾选 `api` 和 `write_repository` 权限
4. 复制 Token 填入插件设置

---

## 🚀 使用方法

### 方式一：工具窗口（推荐）

1. 点击 IDE 右侧边栏的 **Kiwi-linter** 图标打开工具窗口
2. 使用快速添加区域输入 Key 和中文文案，按回车添加
3. 也可以点击表格工具栏的 ➕ 按钮添加空行后编辑
4. 添加完所有条目后，点击 **📤 上传** 统一提交

> 💡 工具窗口会自动去重，相同 Key 的条目会更新而非重复添加

### 方式二：右键菜单提取

1. 在编辑器中选中包含 ErrorCode 模式的代码
2. 右键打开上下文菜单
3. 选择 **Kiwi-linter** → **提取 I18N 文案**
4. 文案会自动添加到工具窗口的表格中

### 方式三：批量提取

1. 打开包含多个 ErrorCode 定义的文件
2. 右键菜单 → **Kiwi-linter** → **批量提取文件中的 I18N 文案**
3. 选择要提取的条目
4. 确认后自动添加到工具窗口

### 方式四：灯泡菜单（Intention）

1. 将光标放在包含 ErrorCode 模式的代码行
2. 按 <kbd>Alt</kbd> + <kbd>Enter</kbd> 打开灯泡菜单
3. 选择 **提取 I18N 文案到仓库**

---

## ⌨️ 快捷键

| 功能               | Windows/Linux                                                      | macOS                                                     |
| ------------------ | ------------------------------------------------------------------ | --------------------------------------------------------- |
| 提取 I18N 文案     | <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>I</kbd>                    | <kbd>⌘</kbd> + <kbd>⌥</kbd> + <kbd>I</kbd>                |
| 批量提取当前文件   | <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>Shift</kbd> + <kbd>I</kbd> | <kbd>⌘</kbd> + <kbd>⌥</kbd> + <kbd>⇧</kbd> + <kbd>I</kbd> |

---

## 📖 功能详解

### 1. 工具窗口批量录入

工具窗口是管理 I18N 文案的核心界面，支持：

- **快速添加**：在顶部输入区输入 Key 和文案，按回车快速添加
- **表格编辑**：直接在表格中编辑 Key 和 Value
- **批量上传**：所有条目在一个 commit 中统一提交
- **自动去重**：添加时自动检测重复 Key，已存在则更新

### 2. 提取 I18N 文案

从代码中自动识别并提取 I18N 文案到工具窗口。支持的模式：

```java
// ErrorCode 枚举模式
ERROR_CODE(code: "DPN.Module.ErrorName", message: "错误信息描述")

// 标准 properties 格式
DPN.Module.Key = 中文文案内容
```

**使用步骤：**

1. 选中包含上述模式的代码
2. 使用快捷键 <kbd>Ctrl</kbd> + <kbd>Alt</kbd> + <kbd>I</kbd> 或右键菜单
3. 文案自动添加到工具窗口表格
4. 在工具窗口点击 **上传** 提交

### 3. 批量提取文件

扫描当前文件中的所有可提取模式，批量选择并添加到工具窗口。

**使用步骤：**

1. 打开包含 ErrorCode 定义的文件
2. 右键菜单 → **Kiwi-linter** → **批量提取文件中的 I18N 文案**
3. 在列表中勾选需要提取的条目
4. 点击 **确定**，条目添加到工具窗口
5. 在工具窗口统一上传

### 4. 智能合并

插件会自动处理重复和更新：

| 情况             | 行为    |
| ---------------- | ------- |
| Key 不存在       | ✅ 新增 |
| Key 存在但值不同 | 🔄 更新 |
| Key 存在且值相同 | ⏭️ 跳过 |

提交完成后会显示详细统计：

> ✅ 新增 2 条  
> 🔄 更新 1 条  
> ⏭️ 跳过 3 条（已存在且内容相同）

---

## ❓ 常见问题

### Q: 提交失败，提示 "请先配置项目 ID"

**A:** 进入 <kbd>Settings</kbd> → <kbd>Tools</kbd> → <kbd>Kiwi-linter</kbd>，填写项目 ID 和 Private Token。

### Q: 提交失败，提示 "401 Unauthorized"

**A:** Private Token 无效或已过期，请重新生成 Token。

### Q: 提交失败，提示 "403 Forbidden"

**A:** 检查 Token 权限是否包含 `api` 和 `write_repository`，以及你是否有该分支的写入权限。

### Q: 工具窗口在哪里？

**A:** 点击 IDE 右侧边栏的 🥝 **Kiwi-linter** 图标，或通过菜单 <kbd>View</kbd> → <kbd>Tool Windows</kbd> → <kbd>Kiwi-linter</kbd> 打开。

### Q: 支持哪些 IDE？

**A:** 支持所有基于 IntelliJ Platform 的 IDE（2024.3+），包括：

- IntelliJ IDEA (Community / Ultimate)
- WebStorm
- PyCharm
- GoLand
- 等等

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

- **Issues**: [GitHub Issues](https://github.com/chenweichang1/kiwi-linter-idea/issues)
- **源码**: [GitHub Repository](https://github.com/chenweichang1/kiwi-linter-idea)

---

## 📄 许可证

[Apache License 2.0](LICENSE)

---

## 🙏 致谢

- 基于 [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- 感谢 JetBrains 提供的优秀开发工具
