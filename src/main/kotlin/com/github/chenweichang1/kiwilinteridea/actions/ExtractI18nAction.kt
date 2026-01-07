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
 * 从选中代码提取 I18N 文案的 Action
 * 直接添加到工具窗口的表格中，无需确认对话框
 */
class ExtractI18nAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        // 获取选中的文本
        val selectedText = editor.selectionModel.selectedText
        
        val entry = if (!selectedText.isNullOrBlank()) {
            // 尝试从选中文本提取
            I18nExtractor.extractFromSelection(selectedText)
        } else {
            // 尝试从当前行提取
            val document = editor.document
            val offset = editor.caretModel.offset
            val lineNumber = document.getLineNumber(offset)
            val lineStart = document.getLineStartOffset(lineNumber)
            val lineEnd = document.getLineEndOffset(lineNumber)
            val lineText = document.getText(com.intellij.openapi.util.TextRange(lineStart, lineEnd))
            I18nExtractor.extractFromLine(lineText)
        }
        
        if (entry == null) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Kiwi-linter")
                .createNotification("未识别到 I18N 文案模式", NotificationType.WARNING)
                .notify(project)
            return
        }
        
        // 获取工具窗口面板，直接添加到表格（自动去重）
        val panel = KiwiToolWindowPanel.getInstance(project)
        if (panel != null) {
            val isNew = panel.addEntry(entry)
            
            // 打开工具窗口
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Kiwi-linter")
            toolWindow?.show()
            
            // 显示通知
            val msg = if (isNew) "已添加: ${entry.key}" else "已更新: ${entry.key}"
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
