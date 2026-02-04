package com.github.chenweichang1.kiwilinteridea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.github.chenweichang1.kiwilinteridea.i18n.I18nExtractor
import com.github.chenweichang1.kiwilinteridea.ui.KiwiToolWindowPanel

/**
 * 从当前文件批量提取 I18N 文案的 Action
 * 直接添加到工具窗口的表格中，无需确认对话框
 * 
 * 支持的格式：
 * 1. ErrorCode 模式：ERROR_CODE("DPN.xxx", "中文", ...)
 * 2. JSON 格式：{"DPN.xxx": "中文", ...}
 */
class BatchExtractI18nAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        // 从文件内容提取所有 I18N 条目
        val fileContent = editor.document.text
        
        // 检测是否为 JSON 格式，如果是则使用 JSON 提取器
        val entries = if (I18nExtractor.isJsonContent(fileContent)) {
            I18nExtractor.extractFromJson(fileContent)
        } else {
            I18nExtractor.extractFromFile(fileContent)
        }
        
        if (entries.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Kiwi-linter")
                .createNotification("当前文件中未找到可提取的 I18N 文案", NotificationType.WARNING)
                .notify(project)
            return
        }
        
        // 获取工具窗口面板，直接添加到表格（自动去重）
        val panel = KiwiToolWindowPanel.getInstance(project)
        if (panel != null) {
            val addedCount = panel.addEntries(entries)
            
            // 打开工具窗口
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Kiwi-linter")
            toolWindow?.show()
            
            // 显示通知（包含去重信息）
            val skippedCount = entries.size - addedCount
            val msg = if (skippedCount > 0) {
                "已添加 $addedCount 条，跳过 $skippedCount 条重复"
            } else {
                "已添加 $addedCount 条文案"
            }
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Kiwi-linter")
                .createNotification(msg, NotificationType.INFORMATION)
                .notify(project)
        } else {
            // 打开工具窗口
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Kiwi-linter")
            toolWindow?.show()
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = project != null && editor != null
    }
}
