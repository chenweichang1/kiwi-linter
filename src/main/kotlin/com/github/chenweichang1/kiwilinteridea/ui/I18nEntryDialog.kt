package com.github.chenweichang1.kiwilinteridea.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.github.chenweichang1.kiwilinteridea.i18n.I18nEntry
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JScrollPane

/**
 * I18N 文案录入/编辑对话框
 */
class I18nEntryDialog(
    private val project: Project,
    private val initialEntry: I18nEntry? = null,
    dialogTitle: String = "录入 I18N 文案"
) : DialogWrapper(project, true) {
    
    private val keyField = JBTextField(40).apply {
        emptyText.text = "例如: DPN.DataProcess.CalendarNotFound"
    }
    
    private val valueArea = JTextArea(3, 40).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    
    private val enValueArea = JTextArea(3, 40).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    
    private val sourceLabel = JBLabel()
    
    init {
        title = dialogTitle
        init()
        
        // 填充初始值
        initialEntry?.let {
            keyField.text = it.key
            valueArea.text = it.value
            enValueArea.text = it.enValue
            it.sourceLocation?.let { loc ->
                sourceLabel.text = "来源: $loc"
            }
        }
    }
    
    override fun createCenterPanel(): JComponent {
        val valueScrollPane = JScrollPane(valueArea).apply {
            preferredSize = JBUI.size(400, 80)
        }
        
        val enValueScrollPane = JScrollPane(enValueArea).apply {
            preferredSize = JBUI.size(400, 80)
        }
        
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Key:"), keyField, 1, false)
            .addLabeledComponent(JBLabel("中文文案:"), valueScrollPane, 1, false)
            .addLabeledComponent(JBLabel("英文文案:"), enValueScrollPane, 1, false)
            .addComponentToRightColumn(sourceLabel, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        panel.border = JBUI.Borders.empty(10)
        return panel
    }
    
    override fun doValidate(): ValidationInfo? {
        if (keyField.text.isBlank()) {
            return ValidationInfo("Key 不能为空", keyField)
        }
        if (valueArea.text.isBlank()) {
            return ValidationInfo("中文文案不能为空", valueArea)
        }
        // Key 格式校验
        if (!keyField.text.matches(Regex("^[\\w.]+$"))) {
            return ValidationInfo("Key 只能包含字母、数字、下划线和点", keyField)
        }
        return null
    }
    
    override fun getPreferredFocusedComponent(): JComponent = keyField
    
    /**
     * 获取用户输入的 I18N 条目
     */
    fun getEntry(): I18nEntry {
        return I18nEntry(
            key = keyField.text.trim(),
            value = valueArea.text.trim(),
            enValue = enValueArea.text.trim(),
            sourceLocation = initialEntry?.sourceLocation
        )
    }
}

