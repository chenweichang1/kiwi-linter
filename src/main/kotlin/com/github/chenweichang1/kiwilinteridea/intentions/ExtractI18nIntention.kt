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
 */
class ExtractI18nIntention : PsiElementBaseIntentionAction(), IntentionAction {
    
    override fun getFamilyName(): String = "Kiwi-linter"
    
    override fun getText(): String = "提取 I18N 文案"
    
    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (editor == null) return false
        
        // 获取当前行文本
        val document = editor.document
        val offset = editor.caretModel.offset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStart, lineEnd))
        
        // 检查是否包含可提取的模式
        return I18nExtractor.containsI18nPattern(lineText)
    }
    
    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if (editor == null) return
        
        // 获取当前行文本
        val document = editor.document
        val offset = editor.caretModel.offset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStart, lineEnd))
        
        // 提取 I18N 条目
        val entry = I18nExtractor.extractFromLine(lineText)
        
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
