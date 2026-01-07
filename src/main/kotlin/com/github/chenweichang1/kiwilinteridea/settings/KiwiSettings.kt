package com.github.chenweichang1.kiwilinteridea.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

/**
 * Kiwi I18N 插件的项目级设置
 */
@Service(Service.Level.PROJECT)
@State(
    name = "KiwiI18nSettings",
    storages = [Storage("kiwi-i18n.xml")]
)
class KiwiSettings : PersistentStateComponent<KiwiSettings.State> {
    
    data class State(
        /** 项目 ID（用于 API 调用） */
        var projectId: String = "1696014",
        /** 目标分支 */
        var targetBranch: String = "release",
        /** 中文 properties 文件路径 */
        var zhPropertiesPath: String = "data-offline/src/main/resources/dataphin_i18n_data_zh.properties",
        /** 提交信息模板 */
        var commitMessageTemplate: String = "feat: 添加 I18N 文案 - {key}",
        /** GitLab Private Token（用于 API 认证） */
        var privateToken: String = ""
    )
    
    private var myState = State()
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        myState = state
    }
    
    companion object {
        fun getInstance(project: Project): KiwiSettings {
            return project.service<KiwiSettings>()
        }
    }
}

