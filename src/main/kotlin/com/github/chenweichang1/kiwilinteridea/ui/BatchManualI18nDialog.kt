package com.github.chenweichang1.kiwilinteridea.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.github.chenweichang1.kiwilinteridea.i18n.I18nEntry
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * 批量手动录入 I18N 文案对话框
 * 支持一次性输入多条文案，统一提交
 */
class BatchManualI18nDialog(
    private val project: Project
) : DialogWrapper(project, true) {
    
    private val tableModel = object : DefaultTableModel(arrayOf("Key", "中文文案", "英文文案"), 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean = true
    }
    
    private val table = JBTable(tableModel).apply {
        setShowGrid(true)
        rowHeight = 28
        columnModel.getColumn(0).preferredWidth = 200
        columnModel.getColumn(1).preferredWidth = 250
        columnModel.getColumn(2).preferredWidth = 250
    }
    
    init {
        title = "批量录入 I18N 文案"
        init()
        // 不预留空行，用户通过"+"按钮或"快速添加"来添加
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // 说明文字
        val tipLabel = JBLabel("请输入多条 I18N 文案，空行会被自动忽略。所有文案将在一个 commit 中提交。")
        tipLabel.border = JBUI.Borders.empty(0, 0, 10, 0)
        
        // 带工具栏的表格（添加/删除行）
        val decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction { 
                tableModel.addRow(arrayOf("", "", ""))
                table.editCellAt(tableModel.rowCount - 1, 0)
            }
            .setRemoveAction {
                val selectedRows = table.selectedRows.sortedDescending()
                selectedRows.forEach { tableModel.removeRow(it) }
            }
            .disableUpDownActions()
        
        val tablePanel = decorator.createPanel()
        tablePanel.preferredSize = Dimension(800, 350)
        
        // 快速添加区域
        val quickAddPanel = createQuickAddPanel()
        
        panel.add(tipLabel, BorderLayout.NORTH)
        panel.add(tablePanel, BorderLayout.CENTER)
        panel.add(quickAddPanel, BorderLayout.SOUTH)
        panel.border = JBUI.Borders.empty(10)
        
        return panel
    }
    
    private fun createQuickAddPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.emptyTop(10)
        
        val keyField = JBTextField(15).apply {
            emptyText.text = "Key"
        }
        val valueField = JBTextField(15).apply {
            emptyText.text = "中文文案"
        }
        val enValueField = JBTextField(15).apply {
            emptyText.text = "英文文案（可选）"
        }
        val addButton = JButton("快速添加").apply {
            addActionListener {
                if (keyField.text.isNotBlank() && valueField.text.isNotBlank()) {
                    tableModel.addRow(arrayOf(keyField.text.trim(), valueField.text.trim(), enValueField.text.trim()))
                    keyField.text = ""
                    valueField.text = ""
                    enValueField.text = ""
                    keyField.requestFocus()
                }
            }
        }
        
        val inputPanel = JPanel().apply {
            add(JBLabel("Key:"))
            add(keyField)
            add(Box.createHorizontalStrut(5))
            add(JBLabel("文案:"))
            add(valueField)
            add(Box.createHorizontalStrut(5))
            add(JBLabel("英文:"))
            add(enValueField)
            add(Box.createHorizontalStrut(5))
            add(addButton)
        }
        
        panel.add(inputPanel, BorderLayout.CENTER)
        return panel
    }
    
    override fun doValidate(): ValidationInfo? {
        val entries = getEntries()
        if (entries.isEmpty()) {
            return ValidationInfo("请至少输入一条有效的文案（Key 和中文文案都不能为空）")
        }
        
        // 检查 Key 格式
        for (entry in entries) {
            if (!entry.key.matches(Regex("^[\\w.]+$"))) {
                return ValidationInfo("Key '${entry.key}' 格式不正确，只能包含字母、数字、下划线和点")
            }
        }
        
        // 检查重复 Key
        val keys = entries.map { it.key }
        val duplicates = keys.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            return ValidationInfo("存在重复的 Key: ${duplicates.joinToString(", ")}")
        }
        
        return null
    }
    
    /**
     * 获取用户输入的所有有效 I18N 条目
     */
    fun getEntries(): List<I18nEntry> {
        // 停止编辑以确保获取最新数据
        if (table.isEditing) {
            table.cellEditor?.stopCellEditing()
        }
        
        val entries = mutableListOf<I18nEntry>()
        for (row in 0 until tableModel.rowCount) {
            val key = (tableModel.getValueAt(row, 0) as? String)?.trim() ?: ""
            val value = (tableModel.getValueAt(row, 1) as? String)?.trim() ?: ""
            val enValue = (tableModel.getValueAt(row, 2) as? String)?.trim() ?: ""
            
            if (key.isNotBlank() && value.isNotBlank()) {
                entries.add(I18nEntry(key, value, enValue))
            }
        }
        return entries
    }
}

