package com.github.chenweichang1.kiwilinteridea.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.github.chenweichang1.kiwilinteridea.i18n.I18nEntry
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 批量 I18N 文案选择对话框
 */
class BatchI18nDialog(
    private val project: Project,
    private val entries: List<I18nEntry>
) : DialogWrapper(project, true) {
    
    private val checkBoxList = CheckBoxList<I18nEntry>()
    
    init {
        title = "批量提取 I18N 文案"
        init()
        
        // 填充列表
        entries.forEach { entry ->
            val label = if (entry.hasEnValue()) {
                "${entry.key} = ${entry.value} | EN: ${entry.enValue}"
            } else {
                "${entry.key} = ${entry.value}"
            }
            checkBoxList.addItem(entry, label, true)
        }
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        val headerLabel = JBLabel("找到 ${entries.size} 条可提取的文案，请选择要录入的条目：")
        headerLabel.border = JBUI.Borders.empty(0, 0, 10, 0)
        
        val scrollPane = JBScrollPane(checkBoxList)
        scrollPane.preferredSize = Dimension(600, 300)
        
        panel.add(headerLabel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.border = JBUI.Borders.empty(10)
        
        return panel
    }
    
    /**
     * 获取用户选中的条目
     */
    fun getSelectedEntries(): List<I18nEntry> {
        val selected = mutableListOf<I18nEntry>()
        for (i in 0 until checkBoxList.itemsCount) {
            if (checkBoxList.isItemSelected(i)) {
                checkBoxList.getItemAt(i)?.let { selected.add(it) }
            }
        }
        return selected
    }
}

