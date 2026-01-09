package com.github.chenweichang1.kiwilinteridea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.github.chenweichang1.kiwilinteridea.i18n.I18nExtractor
import com.github.chenweichang1.kiwilinteridea.ui.KiwiToolWindowPanel

/**
 * 从选中代码提取 I18N 文案的 Action
 * 直接添加到工具窗口的表格中，无需确认对话框
 * 支持多行代码提取
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
            // 尝试从当前行及后续几行提取（支持多行代码）
            val document = editor.document
            val offset = editor.caretModel.offset
            val lineNumber = document.getLineNumber(offset)
            val lineStart = document.getLineStartOffset(lineNumber)
            
            // 先尝试单行
            val lineEnd = document.getLineEndOffset(lineNumber)
            val lineText = document.getText(TextRange(lineStart, lineEnd))
            var result = I18nExtractor.extractFromLine(lineText)
            
            // 如果单行无法匹配，尝试读取多行（当前行 + 后续2行）
            if (result == null) {
                val maxLine = minOf(lineNumber + 2, document.lineCount - 1)
                val multiLineEnd = document.getLineEndOffset(maxLine)
                val multiLineText = document.getText(TextRange(lineStart, multiLineEnd))
                result = I18nExtractor.extractFromSelection(multiLineText)
            }
            
            result
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
