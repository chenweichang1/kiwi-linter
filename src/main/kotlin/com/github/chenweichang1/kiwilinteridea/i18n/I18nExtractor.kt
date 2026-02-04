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
     * 匹配三参数模式（支持多行）
     * 只提取前两项（key 和 value），第三项可以是任意内容
     * key 必须包含 D、P、N 三个字母组合开头才会被识别
     * 支持 DPN、DNP、PND、PDN、NPD、NDP 等所有组合
     * 
     * 示例1: CALENDAR_NOT_FOUND("DPN.DataProcess.CalendarNotFound","根据id或者编码:{0} 找不到公共日历",ErrorLevel.LOGIC)
     * 示例2: NO_AGENT_USE_PERMISSION("DPN.Mdc.AiAgent.NoAgentUsePermission", "你没有该智能体权限", USER_ERROR)
     * 示例3: SOME_ERROR("DPN.Some.Error", "错误描述", "字符串参数")
     * 示例4: OLD_ERROR("DNP.Some.Error", "老代码错误", ErrorLevel.LOGIC)
     */
    private val ERROR_CODE_PATTERN = Pattern.compile(
        """(\w+)\s*\(\s*"([DPN]{3}\.[^"]+)"\s*,\s*"([^"]+)"\s*,[^)]+\)""",
        Pattern.DOTALL
    )
    
    /**
     * 匹配简单的双参数模式
     * key 必须包含 D、P、N 三个字母组合开头才会被识别
     * 支持 DPN、DNP、PND、PDN、NPD、NDP 等所有组合
     * 示例: SOME_KEY("DPN.Key.Name", "中文描述")
     */
    private val SIMPLE_PATTERN = Pattern.compile(
        """(\w+)\s*\(\s*"([DPN]{3}\.[^"]+)"\s*,\s*"([^"]+)"\s*\)""",
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
    
    /**
     * 从 JSON 格式提取 I18N 条目
     * 支持格式: {"key": "value", ...}
     * key 必须包含 D、P、N 三个字母组合开头才会被识别
     */
    fun extractFromJson(jsonContent: String): List<I18nEntry> {
        val entries = mutableListOf<I18nEntry>()
        
        // 匹配 JSON 中的 key-value 对
        // 支持 "key":"value" 和 "key": "value" 两种格式
        val jsonPattern = Pattern.compile(""""([DPN]{3}\.[^"]+)"\s*:\s*"([^"]+)"""")
        val matcher = jsonPattern.matcher(jsonContent)
        
        while (matcher.find()) {
            val key = matcher.group(1)
            val value = matcher.group(2)
            entries.add(
                I18nEntry(
                    key = key,
                    value = value,
                    sourceLocation = "json"
                )
            )
        }
        
        return entries
    }
    
    /**
     * 检测文本是否为 JSON 格式
     */
    fun isJsonContent(content: String): Boolean {
        val trimmed = content.trim()
        return trimmed.startsWith("{") && trimmed.endsWith("}")
    }
}

