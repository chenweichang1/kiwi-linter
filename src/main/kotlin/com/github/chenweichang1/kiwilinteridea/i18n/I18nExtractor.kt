package com.github.chenweichang1.kiwilinteridea.i18n

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.regex.Pattern

/**
 * I18N 文案提取器
 * 用于从代码中提取国际化 key-value 对
 */
object I18nExtractor {
    
    /**
     * 匹配 ErrorCode 枚举定义模式（支持多行）
     * 示例1: CALENDAR_NOT_FOUND("DPN.DataProcess.CalendarNotFound","根据id或者编码:{0} 找不到公共日历",ErrorLevel.LOGIC)
     * 示例2: NO_AGENT_USE_PERMISSION("DPN.Mdc.AiAgent.NoAgentUsePermission", "你没有该智能体权限", USER_ERROR)
     * 示例3: CONVERSATION_LIST_LIMIT_PARAM_VALID("DPN.Mdc.AiAgent.ConversationListLimitParamValid",
     *            "会话列表查询参数错误，limit 要大于0", USER_ERROR)
     */
    private val ERROR_CODE_PATTERN = Pattern.compile(
        """(\w+)\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*(?:\w+\.)?(\w+)\s*\)""",
        Pattern.DOTALL
    )
    
    /**
     * 匹配简单的双参数模式
     * 示例: SOME_KEY("key.name", "中文描述")
     */
    private val SIMPLE_PATTERN = Pattern.compile(
        """(\w+)\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)""",
        Pattern.DOTALL
    )
    
    /**
     * 从选中的文本中提取 I18N 条目
     */
    fun extractFromSelection(selectedText: String): I18nEntry? {
        // 先尝试 ErrorCode 模式
        var matcher = ERROR_CODE_PATTERN.matcher(selectedText)
        if (matcher.find()) {
            return I18nEntry(
                key = matcher.group(2),
                value = matcher.group(3),
                sourceLocation = "selection"
            )
        }
        
        // 尝试简单模式
        matcher = SIMPLE_PATTERN.matcher(selectedText)
        if (matcher.find()) {
            return I18nEntry(
                key = matcher.group(2),
                value = matcher.group(3),
                sourceLocation = "selection"
            )
        }
        
        return null
    }
    
    /**
     * 从当前行提取 I18N 条目
     */
    fun extractFromLine(lineText: String): I18nEntry? {
        return extractFromSelection(lineText)
    }
    
    /**
     * 从整个文件中提取所有 I18N 条目
     */
    fun extractFromFile(fileContent: String): List<I18nEntry> {
        val entries = mutableListOf<I18nEntry>()
        
        val matcher = ERROR_CODE_PATTERN.matcher(fileContent)
        while (matcher.find()) {
            entries.add(
                I18nEntry(
                    key = matcher.group(2),
                    value = matcher.group(3),
                    sourceLocation = "file"
                )
            )
        }
        
        return entries
    }
    
    /**
     * 检测文本是否包含可提取的 I18N 模式
     */
    fun containsI18nPattern(text: String): Boolean {
        return ERROR_CODE_PATTERN.matcher(text).find() || SIMPLE_PATTERN.matcher(text).find()
    }
    
    /**
     * 从光标位置的 PSI 元素尝试提取
     */
    fun extractFromPsiElement(element: PsiElement?): I18nEntry? {
        if (element == null) return null
        
        // 获取当前行的完整文本
        val document = element.containingFile?.viewProvider?.document ?: return null
        val offset = element.textOffset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(com.intellij.openapi.util.TextRange(lineStart, lineEnd))
        
        return extractFromLine(lineText)
    }
}

