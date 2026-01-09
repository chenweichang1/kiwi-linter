package com.github.chenweichang1.kiwilinteridea.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.psi.PsiElement
import com.github.chenweichang1.kiwilinteridea.i18n.I18nExtractor
import com.github.chenweichang1.kiwilinteridea.ui.KiwiToolWindowPanel

/**
 * I18N 文案提取的 Intention Action
 * 当光标在可识别的 ErrorCode 模式上时，会在灯泡菜单中显示
 * 直接添加到工具窗口的表格中，无需确认对话框
 * 支持多行代码提取
 */
class ExtractI18nIntention : PsiElementBaseIntentionAction(), IntentionAction {
    
    override fun getFamilyName(): String = "Kiwi-linter"
    
    override fun getText(): String = "提取 I18N 文案"
    
    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (editor == null) return false
        
        // 获取当前行及后续几行文本
        val document = editor.document
        val offset = editor.caretModel.offset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        
        // 先检查单行
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStart, lineEnd))
        if (I18nExtractor.containsI18nPattern(lineText)) {
            return true
        }
        
        // 检查多行（当前行 + 后续2行）
        val maxLine = minOf(lineNumber + 2, document.lineCount - 1)
        val multiLineEnd = document.getLineEndOffset(maxLine)
        val multiLineText = document.getText(TextRange(lineStart, multiLineEnd))
        return I18nExtractor.containsI18nPattern(multiLineText)
    }
    
    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if (editor == null) return
        
        // 获取当前行及后续几行文本
        val document = editor.document
        val offset = editor.caretModel.offset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStart, lineEnd))
        
        // 先尝试单行提取
        var entry = I18nExtractor.extractFromLine(lineText)
        
        // 如果单行无法匹配，尝试读取多行（当前行 + 后续2行）
        if (entry == null) {
            val maxLine = minOf(lineNumber + 2, document.lineCount - 1)
            val multiLineEnd = document.getLineEndOffset(maxLine)
            val multiLineText = document.getText(TextRange(lineStart, multiLineEnd))
            entry = I18nExtractor.extractFromSelection(multiLineText)
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
    
    override fun startInWriteAction(): Boolean = false
}
