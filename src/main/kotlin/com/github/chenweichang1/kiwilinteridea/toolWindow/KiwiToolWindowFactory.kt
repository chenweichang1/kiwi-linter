package com.github.chenweichang1.kiwilinteridea.toolWindow

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.github.chenweichang1.kiwilinteridea.ui.KiwiToolWindowPanel
import com.intellij.util.ui.JBUI
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel

/**
 * Kiwi I18N 工具窗口工厂
 */
class KiwiToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        
        // 文案管理面板
        val panel = KiwiToolWindowPanel(project)
        KiwiToolWindowPanel.registerInstance(project, panel)
        val mainContent = contentFactory.createContent(panel.getContent(), "", false)
        toolWindow.contentManager.addContent(mainContent)
        
        // 添加标题栏链接按钮
        val actions = listOf(
            createLinkAction("📋 提交历史", "https://code.alibaba-inc.com/dataphin/dataphin-i18n-data/commits/release"),
            createLinkAction("🚀 持续集成", "https://code.alibaba-inc.com/dataphin/dataphin-i18n-data/ci?createType=yaml&tab=task")
        )
        
        toolWindow.setTitleActions(actions)
    }
    
    /**
     * 创建可点击的链接 Action（显示文字）
     */
    private fun createLinkAction(text: String, url: String): AnAction {
        return object : AnAction(text), CustomComponentAction {
            override fun actionPerformed(e: AnActionEvent) {
                BrowserUtil.browse(url)
            }
            
            override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
                return JLabel("<html><nobr><a href='#'>$text</a></nobr></html>").apply {
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    border = JBUI.Borders.empty(0, 8) // 左右各 8px 间距
                    addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent) {
                            BrowserUtil.browse(url)
                        }
                    })
                }
            }
        }
    }
    
    override fun shouldBeAvailable(project: Project) = true
}
