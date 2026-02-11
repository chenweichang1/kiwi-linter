package com.github.chenweichang1.kiwilinteridea.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.github.chenweichang1.kiwilinteridea.i18n.I18nEntry
import com.github.chenweichang1.kiwilinteridea.services.I18nSubmitService
import com.github.chenweichang1.kiwilinteridea.settings.KiwiSettings
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * Kiwi-linter 工具窗口面板
 * 核心功能：批量录入表格 + 上传
 */
class KiwiToolWindowPanel(private val project: Project) {
    
    private val tableModel = object : DefaultTableModel(arrayOf("Key", "中文文案", "英文文案"), 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean = true
    }
    
    private val table = JBTable(tableModel).apply {
        setShowGrid(true)
        rowHeight = 28
        columnModel.getColumn(0).preferredWidth = 200
        columnModel.getColumn(1).preferredWidth = 200
        columnModel.getColumn(2).preferredWidth = 200
    }
    
    private val submitButton = JButton("📤 上传").apply {
        addActionListener { submitAllEntries() }
    }
    
    private val clearButton = JButton("🗑️ 清空").apply {
        addActionListener { clearTable() }
    }
    
    private val countLabel = JBLabel("共 0 条待提交")
    
    // 快速添加区域
    private val quickKeyField = JBTextField().apply {
        emptyText.text = "输入 Key，如: DPN.DataProcess.CalendarNotFound"
    }
    private val quickValueField = JBTextField().apply {
        emptyText.text = "输入中文文案"
    }
    private val quickEnValueField = JBTextField().apply {
        emptyText.text = "输入英文文案（可选）"
    }
    
    // 保存面板引用，用于 loading 时禁用
    private lateinit var mainPanel: JPanel
    
    // 状态标签，需要动态更新
    private val statusLabel = JBLabel()
    
    fun getContent(): JComponent {
        mainPanel = JPanel(BorderLayout())
        
        // 顶部提示
        val tipLabel = JBLabel("📝 所有文案将在一个 commit 中统一提交").apply {
            border = JBUI.Borders.empty(5, 0, 10, 0)
        }
        
        // 带工具栏的表格
        val decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction { 
                tableModel.addRow(arrayOf("", "", ""))
                table.editCellAt(tableModel.rowCount - 1, 0)
                updateCount()
            }
            .setRemoveAction {
                val selectedRows = table.selectedRows.sortedDescending()
                selectedRows.forEach { tableModel.removeRow(it) }
                updateCount()
            }
            .disableUpDownActions()
        
        val tablePanel = decorator.createPanel()
        
        // 快速添加区域
        val quickAddPanel = createQuickAddPanel()
        
        // 按钮区域
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(submitButton)
            add(clearButton)
            add(Box.createHorizontalStrut(20))
            add(countLabel)
        }
        
        // 状态区域（动态更新）
        statusLabel.border = JBUI.Borders.emptyTop(5)
        updateStatusLabel()
        
        // 顶部区域
        val topPanel = JPanel(BorderLayout()).apply {
            add(tipLabel, BorderLayout.NORTH)
            add(quickAddPanel, BorderLayout.CENTER)
        }
        
        // 底部区域
        val bottomPanel = JPanel(BorderLayout()).apply {
            add(buttonPanel, BorderLayout.CENTER)
            add(statusLabel, BorderLayout.SOUTH)
        }
        
        mainPanel.apply {
            border = JBUI.Borders.empty(10)
            add(topPanel, BorderLayout.NORTH)
            add(tablePanel, BorderLayout.CENTER)
            add(bottomPanel, BorderLayout.SOUTH)
        }
        
        return mainPanel
    }
    
    private fun createQuickAddPanel(): JPanel {
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(0, 0, 10, 0)
        }
        
        val addButton = JButton("➕ 添加").apply {
            addActionListener {
                if (quickKeyField.text.isNotBlank() && quickValueField.text.isNotBlank()) {
                    addEntry(I18nEntry(quickKeyField.text.trim(), quickValueField.text.trim(), quickEnValueField.text.trim()))
                    quickKeyField.text = ""
                    quickValueField.text = ""
                    quickEnValueField.text = ""
                    quickKeyField.requestFocus()
                }
            }
        }
        
        // 支持回车快速添加
        quickValueField.addActionListener {
            addButton.doClick()
        }
        quickEnValueField.addActionListener {
            addButton.doClick()
        }
        
        // Key 输入行
        val keyRow = JPanel(BorderLayout()).apply {
            add(JBLabel("Key:      "), BorderLayout.WEST)
            add(quickKeyField, BorderLayout.CENTER)
        }
        
        // 文案输入行
        val valueRow = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(5)
            add(JBLabel("文案:    "), BorderLayout.WEST)
            add(quickValueField, BorderLayout.CENTER)
        }
        
        // 英文输入行
        val enValueRow = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(5)
            add(JBLabel("英文:    "), BorderLayout.WEST)
            add(quickEnValueField, BorderLayout.CENTER)
        }
        
        // 按钮行
        val buttonRow = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            border = JBUI.Borders.emptyTop(5)
            add(addButton)
        }
        
        panel.add(keyRow)
        panel.add(valueRow)
        panel.add(enValueRow)
        panel.add(buttonRow)
        
        return panel
    }
    
    /**
     * 获取表格中已有的所有 Key
     */
    private fun getExistingKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        for (row in 0 until tableModel.rowCount) {
            val key = (tableModel.getValueAt(row, 0) as? String)?.trim() ?: ""
            if (key.isNotBlank()) {
                keys.add(key)
            }
        }
        return keys
    }
    
    /**
     * 添加条目到表格（供外部 Action 调用）
     * 自动去重：如果 Key 已存在，则更新值
     */
    fun addEntry(entry: I18nEntry): Boolean {
        // 每次操作时刷新状态标签
        updateStatusLabel()
        
        // 检查是否已存在
        for (row in 0 until tableModel.rowCount) {
            val existingKey = (tableModel.getValueAt(row, 0) as? String)?.trim() ?: ""
            if (existingKey == entry.key) {
                // 已存在，更新值
                tableModel.setValueAt(entry.value, row, 1)
                if (entry.enValue.isNotBlank()) {
                    tableModel.setValueAt(entry.enValue, row, 2)
                }
                updateCount()
                return false // 返回 false 表示是更新而非新增
            }
        }
        // 不存在，添加新行
        tableModel.addRow(arrayOf(entry.key, entry.value, entry.enValue))
        updateCount()
        return true // 返回 true 表示是新增
    }
    
    /**
     * 批量添加条目到表格（供外部 Action 调用）
     * 自动去重：跳过已存在的 Key
     * @return 实际新增的条目数
     */
    fun addEntries(entries: List<I18nEntry>): Int {
        val existingKeys = getExistingKeys()
        var addedCount = 0
        
        // 先对 entries 内部去重（保留最后一个）
        val uniqueEntries = entries.associateBy { it.key }.values
        
        uniqueEntries.forEach { entry ->
            if (!existingKeys.contains(entry.key)) {
                tableModel.addRow(arrayOf(entry.key, entry.value, entry.enValue))
                addedCount++
            }
        }
        updateCount()
        return addedCount
    }
    
    /**
     * 获取所有有效条目
     */
    private fun getEntries(): List<I18nEntry> {
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
    
    private fun updateCount() {
        val count = getEntries().size
        countLabel.text = "共 $count 条待提交"
    }
    
    /**
     * 更新状态标签（读取最新配置）
     */
    private fun updateStatusLabel() {
        val settings = KiwiSettings.getInstance(project)
        statusLabel.text = if (settings.state.projectId.isNotBlank()) {
            "📁 项目: ${settings.state.projectId} | 分支: ${settings.state.targetBranch}"
        } else {
            "⚠️ 请先配置 (Settings -> Tools -> Kiwi-linter)"
        }
    }
    
    private fun clearTable() {
        tableModel.setRowCount(0)
        updateCount()
    }
    
    private fun submitAllEntries() {
        val entries = getEntries()
            
            if (entries.isEmpty()) {
                Messages.showInfoMessage(project, "没有需要提交的文案", "提示")
                return
            }
            
        // 检查 Key 格式
        for (entry in entries) {
            if (!entry.key.matches(Regex("^[\\w.]+$"))) {
                Messages.showWarningDialog(
                    project,
                    "Key '${entry.key}' 格式不正确，只能包含字母、数字、下划线和点",
                    "格式错误"
                )
                return
            }
        }
        
        // 检查重复 Key
        val keys = entries.map { it.key }
        val duplicates = keys.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            Messages.showWarningDialog(
                project,
                "存在重复的 Key: ${duplicates.joinToString(", ")}",
                "重复 Key"
            )
            return
        }
        
        // 显示 loading 状态
        setLoading(true)
        
        // 在后台线程执行提交
        ApplicationManager.getApplication().executeOnPooledThread {
            val submitService = I18nSubmitService.getInstance(project)
            val result = submitService.submitEntries(entries)
            
            // 回到 EDT 更新 UI
            SwingUtilities.invokeLater {
                setLoading(false)
                
                when (result) {
                is I18nSubmitService.SubmitResult.Success -> {
                    // 构建详细的统计信息
                    val statsMsg = buildString {
                            if (result.added > 0) append("✅ 新增 ${result.added} 条\n")
                            if (result.updated > 0) append("🔄 更新 ${result.updated} 条\n")
                            if (result.skipped > 0) append("⏭️ 跳过 ${result.skipped} 条（已存在且内容相同）")
                        if (isEmpty()) append("没有需要变更的内容")
                    }
                    
                        val title = if (result.changedCount > 0) "🎉 提交成功" else "提交完成"
                        Messages.showInfoMessage(project, statsMsg.trim(), title)
                        
                        // 提交成功后清空表格
                        if (result.changedCount > 0) {
                            clearTable()
                        }
                }
                is I18nSubmitService.SubmitResult.Failure -> {
                        Messages.showErrorDialog(project, result.error, "❌ 提交失败")
                }
            }
        }
    }
}

    /**
     * 设置 loading 状态
     */
    private fun setLoading(loading: Boolean) {
        submitButton.isEnabled = !loading
        clearButton.isEnabled = !loading
        table.isEnabled = !loading
        
        if (loading) {
            submitButton.text = "⏳ 上传中..."
            mainPanel.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
        } else {
            submitButton.text = "📤 上传"
            mainPanel.cursor = Cursor.getDefaultCursor()
        }
    }
    
    companion object {
        private val instances = mutableMapOf<Project, KiwiToolWindowPanel>()
        
        /**
         * 获取项目对应的工具窗口面板实例
         */
        fun getInstance(project: Project): KiwiToolWindowPanel? {
            return instances[project]
        }
        
        /**
         * 注册实例（由 ToolWindowFactory 调用）
         */
        fun registerInstance(project: Project, panel: KiwiToolWindowPanel) {
            instances[project] = panel
        }
        
        /**
         * 移除实例
         */
        fun removeInstance(project: Project) {
            instances.remove(project)
        }
    }
}
