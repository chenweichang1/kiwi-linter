package com.github.chenweichang1.kiwilinteridea.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.chenweichang1.kiwilinteridea.i18n.I18nEntry
import com.github.chenweichang1.kiwilinteridea.settings.KiwiSettings

/**
 * I18N 文案提交服务
 * 负责将文案提交到远程仓库
 */
@Service(Service.Level.PROJECT)
class I18nSubmitService(private val project: Project) {
    
    private val logger = thisLogger()
    
    /**
     * 提交结果
     */
    sealed class SubmitResult {
        data class Success(
            val message: String,
            val added: Int = 0,      // 新增数量
            val updated: Int = 0,    // 更新数量
            val skipped: Int = 0     // 跳过数量（已存在且内容相同）
        ) : SubmitResult() {
            /** 实际发生变更的数量 */
            val changedCount: Int get() = added + updated
            /** 总处理数量 */
            val totalCount: Int get() = added + updated + skipped
        }
        data class Failure(val error: String) : SubmitResult()
    }
    
    /**
     * 提交单个 I18N 条目到仓库（中英文 + 删除其他语言 key 合并为一个 commit）
     */
    fun submitEntry(entry: I18nEntry): SubmitResult {
        val settings = KiwiSettings.getInstance(project)
        
        if (settings.state.projectId.isBlank()) {
            return SubmitResult.Failure("请先在设置中配置项目 ID (Settings -> Tools -> Kiwi-linter)")
        }
        
        return try {
            val codePlatformService = CodePlatformService.getInstance(project)
            val branch = settings.state.targetBranch
            val zhFilePath = settings.state.zhPropertiesPath
            val commitMessage = settings.state.commitMessageTemplate.replace("{key}", entry.key)
            
            logger.info("准备提交 I18N 文案: ${entry.key}")
            
            // 1. 准备中文文件变更
            val zhContent = entry.toPropertiesLine()
            val zhChange = codePlatformService.prepareFileChange(branch, zhFilePath, zhContent)
                .getOrElse { return SubmitResult.Failure("准备中文文案失败: ${it.message}") }
            
            val allChanges = mutableListOf<CodePlatformService.PreparedFileChange>()
            if (zhChange.changedCount > 0) allChanges.add(zhChange)
            
            // 2. 准备英文文件变更
            var enCommitResult: CodePlatformService.PreparedFileChange? = null
            if (entry.hasEnValue()) {
                val enFilePath = zhFilePath.replace("_zh.properties", "_en.properties")
                if (enFilePath != zhFilePath) {
                    val enContent = entry.toEnPropertiesLine()
                    val enChange = codePlatformService.prepareFileChange(branch, enFilePath, enContent).getOrNull()
                    if (enChange != null && enChange.changedCount > 0) {
                        allChanges.add(enChange)
                        enCommitResult = enChange
                    }
                }
            }
            
            // 3. 准备删除其他语言文件中的 key（跳过已手动提供英文的 _en.properties）
            if (zhChange.changedCount > 0) {
                val keysToRemove = setOf(entry.key)
                val removeChanges = codePlatformService.prepareRemoveKeysFromOtherLocales(branch, zhFilePath, keysToRemove)
                    .filterNot { change ->
                        entry.hasEnValue() && change.filePath.endsWith("_en.properties")
                    }
                allChanges.addAll(removeChanges)
            }
            
            // 4. 如果没有任何变更
            if (allChanges.isEmpty()) {
                val notifyMsg = "Key: ${entry.key} 已存在且内容相同，已跳过"
                showNotification("I18N 文案录入", notifyMsg, NotificationType.INFORMATION)
                return SubmitResult.Success(
                    message = notifyMsg,
                    added = 0, updated = 0, skipped = zhChange.skipped
                )
            }
            
            // 5. 批量提交所有文件变更为一个 commit
            val batchResult = codePlatformService.batchCommitFiles(branch, commitMessage, allChanges)
            
            batchResult.fold(
                onSuccess = {
                    if (zhChange.changedCount > 0) {
                        I18nCacheService.getInstance(project).updateLocalCache(entry.key, entry.value)
                    }
                    val notifyMsg = when {
                        zhChange.skipped > 0 && zhChange.changedCount == 0 -> "Key: ${entry.key} 已存在且内容相同，已跳过"
                        zhChange.updated > 0 -> "Key: ${entry.key} 已更新"
                        else -> "Key: ${entry.key} 已新增"
                    }
                    showNotification("I18N 文案录入", notifyMsg, NotificationType.INFORMATION)
                    SubmitResult.Success(
                        message = notifyMsg,
                        added = zhChange.added, updated = zhChange.updated, skipped = zhChange.skipped
                    )
                },
                onFailure = {
                    showNotification("I18N 文案录入失败", it.message ?: "未知错误", NotificationType.ERROR)
                    SubmitResult.Failure("提交失败: ${it.message}")
                }
            )
        } catch (e: Exception) {
            logger.error("提交失败", e)
            showNotification("I18N 文案录入失败", e.message ?: "未知错误", NotificationType.ERROR)
            SubmitResult.Failure("提交失败: ${e.message}")
        }
    }
    
    /**
     * 批量提交多个 I18N 条目（中英文 + 删除其他语言 key 合并为一个 commit）
     */
    fun submitEntries(entries: List<I18nEntry>): SubmitResult {
        if (entries.isEmpty()) {
            return SubmitResult.Failure("没有需要提交的文案")
        }
        
        val settings = KiwiSettings.getInstance(project)
        
        if (settings.state.projectId.isBlank()) {
            return SubmitResult.Failure("请先在设置中配置项目 ID (Settings -> Tools -> Kiwi-linter)")
        }
        
        return try {
            val codePlatformService = CodePlatformService.getInstance(project)
            val branch = settings.state.targetBranch
            val zhFilePath = settings.state.zhPropertiesPath
            
            val keys = entries.take(3).map { it.key }.joinToString(", ") + 
                if (entries.size > 3) " 等${entries.size}条" else ""
            val commitMessage = "feat: 批量添加 I18N 文案 - $keys"
            
            val allChanges = mutableListOf<CodePlatformService.PreparedFileChange>()
            
            // 1. 准备中文文件变更
            val zhContent = entries.joinToString("\n") { it.toPropertiesLine() }
            val zhChange = codePlatformService.prepareFileChange(branch, zhFilePath, zhContent)
                .getOrElse { return SubmitResult.Failure("准备中文文案失败: ${it.message}") }
            
            if (zhChange.changedCount > 0) allChanges.add(zhChange)
            
            // 2. 准备英文文件变更
            val enEntries = entries.filter { it.hasEnValue() }
            var enChange: CodePlatformService.PreparedFileChange? = null
            if (enEntries.isNotEmpty()) {
                val enFilePath = zhFilePath.replace("_zh.properties", "_en.properties")
                if (enFilePath != zhFilePath) {
                    val enContent = enEntries.joinToString("\n") { it.toEnPropertiesLine() }
                    enChange = codePlatformService.prepareFileChange(branch, enFilePath, enContent).getOrNull()
                    if (enChange != null && enChange.changedCount > 0) {
                        allChanges.add(enChange)
                    }
                }
            }
            
            // 3. 准备删除其他语言文件中的 key（跳过已手动提供英文的 _en.properties）
            if (zhChange.changedCount > 0) {
                val changedKeys = entries.map { it.key }.toSet()
                val removeChanges = codePlatformService.prepareRemoveKeysFromOtherLocales(branch, zhFilePath, changedKeys)
                    .filterNot { change ->
                        enEntries.isNotEmpty() && change.filePath.endsWith("_en.properties")
                    }
                allChanges.addAll(removeChanges)
            }
            
            // 4. 如果没有任何变更
            if (allChanges.isEmpty()) {
                val notifyMsg = "没有需要变更的内容（${zhChange.skipped}条文案已存在且内容相同）"
                showNotification("批量录入完成", notifyMsg, NotificationType.INFORMATION)
                return SubmitResult.Success(
                    message = notifyMsg,
                    added = 0, updated = 0, skipped = zhChange.skipped
                )
            }
            
            // 5. 批量提交所有文件变更为一个 commit
            val batchResult = codePlatformService.batchCommitFiles(branch, commitMessage, allChanges)
            
            batchResult.fold(
                onSuccess = {
                    // 提交成功后批量更新本地缓存
                    if (zhChange.changedCount > 0) {
                        val cacheEntries = entries.associate { it.key to it.value }
                        I18nCacheService.getInstance(project).updateLocalCacheBatch(cacheEntries)
                    }
                    
                    // 构建通知消息
                    val notifyMsg = buildString {
                        append("中文文案: ")
                        if (zhChange.added > 0) append("新增 ${zhChange.added} 条")
                        if (zhChange.updated > 0) {
                            if (!endsWith(": ")) append("，")
                            append("更新 ${zhChange.updated} 条")
                        }
                        if (zhChange.skipped > 0) {
                            if (!endsWith(": ")) append("，")
                            append("跳过 ${zhChange.skipped} 条（已存在）")
                        }
                        if (enChange != null && enChange.changedCount > 0) {
                            append("\n英文文案: ")
                            if (enChange.added > 0) append("新增 ${enChange.added} 条")
                            if (enChange.updated > 0) {
                                if (!endsWith(": ")) append("，")
                                append("更新 ${enChange.updated} 条")
                            }
                            if (enChange.skipped > 0) {
                                if (!endsWith(": ")) append("，")
                                append("跳过 ${enChange.skipped} 条（已存在）")
                            }
                        }
                        if (isEmpty()) append("没有需要变更的内容")
                    }
                    showNotification("批量录入完成", notifyMsg, NotificationType.INFORMATION)
                    SubmitResult.Success(
                        message = "提交成功",
                        added = zhChange.added,
                        updated = zhChange.updated,
                        skipped = zhChange.skipped
                    )
                },
                onFailure = { 
                    showNotification("批量录入失败", it.message ?: "未知错误", NotificationType.ERROR)
                    SubmitResult.Failure("批量提交失败: ${it.message}") 
                }
            )
        } catch (e: Exception) {
            logger.error("批量提交失败", e)
            showNotification("批量录入失败", e.message ?: "未知错误", NotificationType.ERROR)
            SubmitResult.Failure("批量提交失败: ${e.message}")
        }
    }
    
    /**
     * 显示通知
     */
    private fun showNotification(title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Kiwi-linter")
            .createNotification(title, content, type)
            .notify(project)
    }
    
    companion object {
        fun getInstance(project: Project): I18nSubmitService {
            return project.service<I18nSubmitService>()
        }
    }
}
