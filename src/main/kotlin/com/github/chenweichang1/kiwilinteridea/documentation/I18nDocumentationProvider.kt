package com.github.chenweichang1.kiwilinteridea.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.github.chenweichang1.kiwilinteridea.services.I18nCacheService
import java.util.regex.Pattern

/**
 * I18N 文案文档提供器
 * 在 hover 或 Ctrl+Q 时显示文案的中英文信息
 */
class I18nDocumentationProvider : AbstractDocumentationProvider() {
    
    // 匹配包含 D、P、N 三个字母组合开头的 key
    // 支持 DPN、DNP、PND、PDN、NPD、NDP 等所有组合
    private val keyPattern = Pattern.compile(""""([DPN]{3}\.[^"]+)"""")
    
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (originalElement == null) return null
        
        val key = extractKeyFromContext(originalElement) ?: return null
        
        return generateDocumentation(element?.project ?: originalElement.project, key)
    }
    
    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        if (originalElement == null) return null
        
        val key = extractKeyFromContext(originalElement) ?: return null
        val project = element?.project ?: originalElement.project
        val cacheService = I18nCacheService.getInstance(project)
        val (zh, en) = cacheService.getI18nEntry(key)
        
        if (zh == null && en == null) {
            return "I18N: $key (未找到)"
        }
        
        return buildString {
            append("I18N: $key")
            if (zh != null) append(" | 中: $zh")
            if (en != null) append(" | EN: $en")
        }
    }
    
    override fun generateHoverDoc(element: PsiElement, originalElement: PsiElement?): String? {
        return generateDoc(element, originalElement)
    }
    
    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if (contextElement == null) return null
        
        // 检查当前位置是否在 DPN key 字符串内
        val key = extractKeyFromContext(contextElement)
        if (key != null) {
            return contextElement
        }
        
        return null
    }
    
    /**
     * 从上下文中提取 DPN key
     */
    private fun extractKeyFromContext(element: PsiElement): String? {
        // 获取当前元素的文本
        var text = element.text
        
        // 如果当前元素是字符串的一部分，尝试获取父元素
        var current: PsiElement? = element
        for (i in 0..3) {
            if (current == null) break
            text = current.text
            
            // 检查是否包含 DPN key
            val matcher = keyPattern.matcher(text)
            if (matcher.find()) {
                val key = matcher.group(1)
                // 确保光标在这个 key 的范围内
                if (text.contains("\"$key\"")) {
                    return key
                }
            }
            
            current = current.parent
        }
        
        // 尝试从当前行提取
        val document = element.containingFile?.viewProvider?.document ?: return null
        val offset = element.textOffset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(com.intellij.openapi.util.TextRange(lineStart, lineEnd))
        
        val lineMatcher = keyPattern.matcher(lineText)
        if (lineMatcher.find()) {
            return lineMatcher.group(1)
        }
        
        return null
    }
    
    /**
     * 生成文档 HTML
     */
    private fun generateDocumentation(project: com.intellij.openapi.project.Project, key: String): String {
        val cacheService = I18nCacheService.getInstance(project)
        val (zh, en) = cacheService.getI18nEntry(key)
        
        return buildString {
            append(DocumentationMarkup.DEFINITION_START)
            append("<b>I18N Key:</b> <code>$key</code>")
            append(DocumentationMarkup.DEFINITION_END)
            
            append(DocumentationMarkup.CONTENT_START)
            
            if (zh == null && en == null) {
                append("<p style='color: #999;'>⚠️ 该 Key 尚未在远程仓库中找到</p>")
                append("<p style='color: #666; font-size: 0.9em;'>可能原因：</p>")
                append("<ul style='color: #666; font-size: 0.9em;'>")
                append("<li>文案尚未提交到仓库</li>")
                append("<li>缓存尚未刷新（5分钟自动刷新）</li>")
                append("</ul>")
            } else {
                append("<table style='border-collapse: collapse; width: 100%;'>")
                
                // 中文
                append("<tr>")
                append("<td style='padding: 4px 8px; color: #666; white-space: nowrap;'><b>🇨🇳 中文:</b></td>")
                append("<td style='padding: 4px 8px;'>")
                if (zh != null) {
                    append("<span style='color: #2196F3;'>${escapeHtml(zh)}</span>")
                } else {
                    append("<span style='color: #999;'>未找到</span>")
                }
                append("</td>")
                append("</tr>")
                
                // 英文
                append("<tr>")
                append("<td style='padding: 4px 8px; color: #666; white-space: nowrap;'><b>🇺🇸 English:</b></td>")
                append("<td style='padding: 4px 8px;'>")
                if (en != null) {
                    append("<span style='color: #4CAF50;'>${escapeHtml(en)}</span>")
                } else {
                    append("<span style='color: #999;'>未找到</span>")
                }
                append("</td>")
                append("</tr>")
                
                append("</table>")
            }
            
            append(DocumentationMarkup.CONTENT_END)
            
            // 底部提示
            append(DocumentationMarkup.SECTIONS_START)
            append("<p style='color: #888; font-size: 0.85em; margin-top: 8px;'>")
            append("💡 使用 <b>⌘⌥I</b> (Mac) / <b>Ctrl+Alt+I</b> (Win) 提取或更新文案")
            append("</p>")
            append(DocumentationMarkup.SECTIONS_END)
        }
    }
    
    /**
     * HTML 转义
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("{", "&#123;")
            .replace("}", "&#125;")
    }
}
