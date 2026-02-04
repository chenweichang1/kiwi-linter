package com.github.chenweichang1.kiwilinteridea.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.github.chenweichang1.kiwilinteridea.services.I18nCacheService

/**
 * I18N 模式标注器
 * 在编辑器中高亮显示 DPN 开头的 I18N key，并显示中英文信息
 */
class I18nPatternAnnotator : Annotator {
    
    // 匹配包含 D、P、N 三个字母组合开头的 key（只匹配 key 字符串部分）
    // 支持 DPN、DNP、PND、PDN、NPD、NDP 等所有组合
    private val keyPattern = Regex(""""([DPN]{3}\.[^"]+)"""")
    
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // 只处理文件级别，避免重复处理
        if (element !is PsiFile) return
        
        val text = element.text
        val project = element.project
        val cacheService = I18nCacheService.getInstance(project)
        
        keyPattern.findAll(text).forEach { matchResult ->
            val fullMatch = matchResult.value  // 包含引号的完整匹配
            val key = matchResult.groupValues[1]  // 不包含引号的 key
            
            // 计算范围（包含引号）
            val range = TextRange(matchResult.range.first, matchResult.range.last + 1)
            
            // 从缓存获取中英文
            val (zh, en) = cacheService.getI18nEntry(key)
            
            // 构建 tooltip HTML
            val tooltip = buildTooltipHtml(key, zh, en)
            
            // 根据是否已存在显示不同的高亮
            val severity = if (zh != null || en != null) {
                HighlightSeverity.INFORMATION
            } else {
                HighlightSeverity.WEAK_WARNING
            }
            
            val message = if (zh != null || en != null) {
                "I18N: $key (已录入)"
            } else {
                "I18N: $key (未录入)"
            }
            
            holder.newAnnotation(severity, message)
                .range(range)
                .textAttributes(DefaultLanguageHighlighterColors.METADATA)
                .tooltip(tooltip)
                .create()
        }
    }
    
    /**
     * 构建 tooltip HTML
     */
    private fun buildTooltipHtml(key: String, zh: String?, en: String?): String {
        return buildString {
            append("<html><body style='padding: 4px;'>")
            
            // Key
            append("<div style='margin-bottom: 8px;'>")
            append("<b>I18N Key:</b> <code>$key</code>")
            append("</div>")
            
            if (zh == null && en == null) {
                append("<div style='color: #FFA500;'>")
                append("⚠️ 该 Key 尚未录入到仓库")
                append("</div>")
                append("<div style='color: #888; margin-top: 4px; font-size: 0.9em;'>")
                append("使用 <b>⌘⌥I</b> (Mac) / <b>Ctrl+Alt+I</b> (Win) 或右键菜单提取")
                append("</div>")
            } else {
                // 中文
                append("<div style='margin: 4px 0;'>")
                append("<span style='color: #666;'>🇨🇳 中文:</span> ")
                if (zh != null) {
                    append("<span style='color: #2196F3;'>${escapeHtml(zh)}</span>")
                } else {
                    append("<span style='color: #999;'>未找到</span>")
                }
                append("</div>")
                
                // 英文
                append("<div style='margin: 4px 0;'>")
                append("<span style='color: #666;'>🇺🇸 English:</span> ")
                if (en != null) {
                    append("<span style='color: #4CAF50;'>${escapeHtml(en)}</span>")
                } else {
                    append("<span style='color: #999;'>未找到</span>")
                }
                append("</div>")
                
                append("<div style='color: #888; margin-top: 8px; font-size: 0.9em;'>")
                append("💡 使用 <b>⌘⌥I</b> (Mac) / <b>Ctrl+Alt+I</b> (Win) 更新文案")
                append("</div>")
            }
            
            append("</body></html>")
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
    }
}

