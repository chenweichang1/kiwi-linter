package com.github.chenweichang1.kiwilinteridea.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.chenweichang1.kiwilinteridea.settings.KiwiSettings
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Code 平台服务
 * 使用阿里 Code 平台 API 提交文件更改
 */
@Service(Service.Level.PROJECT)
class CodePlatformService(private val project: Project) {
    
    private val logger = thisLogger()
    
    /**
     * 提交结果
     */
    data class CommitResult(
        val added: Int,      // 新增数量
        val updated: Int,    // 更新数量
        val skipped: Int,    // 跳过数量（已存在且内容相同）
        val message: String
    ) {
        /** 实际发生变更的数量 */
        val changedCount: Int get() = added + updated
    }
    
    /**
     * 提交文件更改到仓库
     * @return CommitResult 包含新增、更新、跳过的数量统计
     */
    fun commitFile(
        repoPath: String,
        branch: String,
        filePath: String,
        content: String,
        commitMessage: String,
        append: Boolean = true
    ): Result<CommitResult> {
        val settings = KiwiSettings.getInstance(project)
        
        logger.info("开始提交文件到 Code 平台")
        logger.info("仓库: $repoPath, 分支: $branch, 文件: $filePath")
        
        return commitViaApi(branch, filePath, content, commitMessage)
    }
    
    /**
     * 使用阿里 Code 平台 API 提交
     * 支持去重和更新：相同 key 且值相同则跳过，值不同则替换
     */
    private fun commitViaApi(
        branch: String,
        filePath: String,
        content: String,
        commitMessage: String
    ): Result<CommitResult> {
        return try {
            val settings = KiwiSettings.getInstance(project)
            val token = settings.state.privateToken
            
            if (token.isBlank()) {
                return Result.failure(Exception("请在设置中配置 Private Token"))
            }
            
            // 阿里 Code 平台 API（使用 HTTPS）
            val baseApiUrl = "https://code.alibaba-inc.com/api/v3"
            
            // 使用项目 ID（必须配置）
            val projectId = settings.state.projectId
            if (projectId.isBlank()) {
                return Result.failure(Exception("请在设置中配置项目 ID"))
            }
            
            val encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8")
            
            logger.info("使用阿里 Code API 提交")
            logger.info("项目 ID: $projectId, 分支: $branch, 文件: $filePath")
            
            // 1. 获取现有文件内容
            val existingContent = getFileContentViaApi(baseApiUrl, projectId, branch, encodedPath, token) ?: ""
            val isCreate = existingContent.isEmpty()
            
            // 2. 解析现有内容为 Map（key -> value）
            val existingEntries = parsePropertiesContent(existingContent)
            
            // 3. 解析新提交的内容
            val newEntries = parsePropertiesContent(content)
            
            // 4. 合并：检查重复、更新或新增
            var added = 0
            var updated = 0
            var skipped = 0
            
            val resultEntries = existingEntries.toMutableMap()
            
            for ((key, value) in newEntries) {
                when {
                    // key 不存在 → 新增
                    !resultEntries.containsKey(key) -> {
                        resultEntries[key] = value
                        added++
                        logger.info("新增: $key")
                    }
                    // key 存在但值不同 → 更新
                    resultEntries[key] != value -> {
                        resultEntries[key] = value
                        updated++
                        logger.info("更新: $key (旧值: ${resultEntries[key]}, 新值: $value)")
                    }
                    // key 存在且值相同 → 跳过
                    else -> {
                        skipped++
                        logger.info("跳过（已存在且内容相同）: $key")
                    }
                }
            }
            
            // 如果没有任何变更，直接返回成功（不执行提交）
            if (added == 0 && updated == 0) {
                return Result.success(CommitResult(
                    added = 0,
                    updated = 0,
                    skipped = skipped,
                    message = "没有需要提交的变更（${skipped}条文案已存在且内容相同）"
                ))
            }
            
            // 5. 重建文件内容（保持原有顺序，只更新本次提交的 key，新增的追加到末尾）
            val submittedKeys = newEntries.keys  // 本次提交的 key 集合
            val finalContent = rebuildPropertiesContent(existingContent, submittedKeys, newEntries)
            
            // 6. 调用阿里 Code API 提交文件
            val apiUrl = "$baseApiUrl/projects/$projectId/repository/files"
            
            logger.info("API URL: $apiUrl")
            logger.info("操作类型: ${if (isCreate) "创建" else "更新"}")
            logger.info("变更统计: 新增=$added, 更新=$updated, 跳过=$skipped")
            
            val connection = URI(apiUrl).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = if (isCreate) "POST" else "PUT"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Private-Token", token)
            
            // 构建请求体 - 阿里 Code API 格式
            val base64Content = Base64.getEncoder().encodeToString(finalContent.toByteArray(StandardCharsets.UTF_8))
            val escapedPath = filePath.replace("\"", "\\\"")
            val escapedMessage = commitMessage.replace("\"", "\\\"")
            val requestBody = """
                {
                    "file_path": "$escapedPath",
                    "branch_name": "$branch",
                    "content": "$base64Content",
                    "encoding": "base64",
                    "commit_message": "$escapedMessage"
                }
            """.trimIndent()
            
            logger.info("请求体: $requestBody")
            
            connection.outputStream.use { os ->
                OutputStreamWriter(os, StandardCharsets.UTF_8).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }
            }
            
            val responseCode = connection.responseCode
            val responseMessage = try {
                if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                }
            } catch (e: Exception) {
                e.message ?: "Unknown error"
            }
            
            logger.info("响应码: $responseCode")
            logger.info("响应: $responseMessage")
            
            if (responseCode in 200..299) {
                // 收集所有变更的 key（新增 + 更新）
                val changedKeys = newEntries.keys.filter { key ->
                    !existingEntries.containsKey(key) || existingEntries[key] != newEntries[key]
                }.toSet()
                
                // 从 en 和 zh_TW 文件中删除变更的 key，以便后续自动翻译阶段重新生成
                if (changedKeys.isNotEmpty()) {
                    removeKeysFromOtherLocales(baseApiUrl, projectId, branch, filePath, changedKeys, token, commitMessage)
                }
                
                val resultMsg = buildString {
                    append("提交成功")
                    if (added > 0) append("，新增 $added 条")
                    if (updated > 0) append("，更新 $updated 条")
                    if (skipped > 0) append("，跳过 $skipped 条（已存在）")
                }
                Result.success(CommitResult(
                    added = added,
                    updated = updated,
                    skipped = skipped,
                    message = resultMsg
                ))
            } else {
                Result.failure(Exception("提交失败 ($responseCode): $responseMessage"))
            }
        } catch (e: Exception) {
            logger.error("API 提交文件失败", e)
            Result.failure(Exception("提交失败: ${e.message}"))
        }
    }
    
    /**
     * 解析 properties 文件内容为 Map
     * 格式: key = value 或 key=value
     * key 和 value 都进行 trim，用于比较
     */
    private fun parsePropertiesContent(content: String): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        
        content.lines().forEach { line ->
            val trimmedLine = line.trim()
            // 跳过空行和注释
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#") || trimmedLine.startsWith("!")) {
                return@forEach
            }
            
            // 解析 key=value 或 key = value
            val separatorIndex = line.indexOfFirst { it == '=' || it == ':' }
            if (separatorIndex > 0) {
                val key = line.substring(0, separatorIndex).trim()
                val value = line.substring(separatorIndex + 1).trim()
                entries[key] = value
            }
        }
        
        return entries
    }
    
    /**
     * 重建 properties 文件内容
     * 只更新本次提交的 key，其他行完全保持不变
     * 不保留空行
     * 新增的 key 按首字母顺序插入到正确位置
     * 
     * @param originalContent 原文件内容
     * @param submittedKeys 本次提交的 key 集合（只有这些 key 会被更新）
     * @param submittedEntries 本次提交的 key-value
     */
    private fun rebuildPropertiesContent(
        originalContent: String,
        submittedKeys: Set<String>,
        submittedEntries: Map<String, String>
    ): String {
        val resultLines = mutableListOf<String>()
        val processedKeys = mutableSetOf<String>()
        // 记录每个 key 在结果列表中的位置，用于按顺序插入新 key
        val keyPositions = mutableListOf<Pair<String, Int>>()
        
        // 处理原有的行，保持顺序
        originalContent.lines().forEach { line ->
            val trimmedLine = line.trim()
            
            // 跳过空行（不保留空行）
            if (trimmedLine.isEmpty()) {
                return@forEach
            }
            
            // 保留注释（保持原格式）
            if (trimmedLine.startsWith("#") || trimmedLine.startsWith("!")) {
                resultLines.add(line)
                return@forEach
            }
            
            // 解析 key（在原始行中查找分隔符）
            val separatorIndex = line.indexOfFirst { it == '=' || it == ':' }
            if (separatorIndex > 0) {
                val key = line.substring(0, separatorIndex).trim()
                processedKeys.add(key)
                
                // 记录 key 及其位置
                keyPositions.add(key to resultLines.size)
                
                // 只有本次提交的 key 才会被更新，其他保持原样
                if (submittedKeys.contains(key)) {
                    // 这个 key 在本次提交中，用新值替换
                    val newValue = submittedEntries[key]
                    if (newValue != null) {
                        val prefix = line.substring(0, separatorIndex + 1)
                        val hasSpaceAfterSeparator = separatorIndex + 1 < line.length && line[separatorIndex + 1] == ' '
                        val space = if (hasSpaceAfterSeparator) " " else ""
                        resultLines.add("$prefix$space$newValue")
                    } else {
                        resultLines.add(line)
                    }
                } else {
                    // 不在本次提交中，保持原有行完全不变
                    resultLines.add(line)
                }
            } else {
                // 非 key=value 格式的行，保持原样
                resultLines.add(line)
            }
        }
        
        // 收集需要新增的 key（原文件中不存在的），按字母顺序排序
        val newKeys = submittedEntries.keys
            .filter { !processedKeys.contains(it) }
            .sorted()
        
        // 将新 key 按字母顺序插入到正确位置
        // 策略：找到第一个比新 key 大的现有 key，在其前面插入
        var insertOffset = 0  // 因为每次插入会影响后续位置，需要记录偏移量
        
        for (newKey in newKeys) {
            val newLine = "$newKey = ${submittedEntries[newKey]}"
            
            // 找到应该插入的位置
            var insertIndex = resultLines.size  // 默认插入到末尾
            
            for ((existingKey, position) in keyPositions) {
                if (existingKey.compareTo(newKey, ignoreCase = true) > 0) {
                    // 找到第一个比新 key 大的 key，在其前面插入
                    insertIndex = position + insertOffset
                    break
                }
            }
            
            resultLines.add(insertIndex, newLine)
            insertOffset++  // 增加偏移量
        }
        
        // 用换行符连接所有行，末尾添加换行符（确保下次添加新行时不会修改原有最后一行）
        return resultLines.joinToString("\n") + "\n"
    }
    
    /**
     * 从 en 和 zh_TW 文件中删除指定的 key
     * 用于确保后续自动翻译阶段可以重新生成这些 key 的翻译
     */
    private fun removeKeysFromOtherLocales(
        baseApiUrl: String,
        projectId: String,
        branch: String,
        zhFilePath: String,
        keysToRemove: Set<String>,
        token: String,
        originalCommitMessage: String
    ) {
        // 根据 zh 文件路径推导 en 和 zh_TW 文件路径
        // 例如: dataphin_i18n_data_zh.properties -> dataphin_i18n_data_en.properties, dataphin_i18n_data_zh_TW.properties
        val otherLocalePaths = listOf(
            zhFilePath.replace("_zh.properties", "_en.properties"),
            zhFilePath.replace("_zh.properties", "_zh_TW.properties")
        )
        
        for (localePath in otherLocalePaths) {
            // 跳过无效路径（如果替换后路径没变，说明不匹配预期格式）
            if (localePath == zhFilePath) {
                logger.warn("无法推导出其他语言文件路径，跳过: $zhFilePath")
                continue
            }
            
            try {
                val encodedPath = java.net.URLEncoder.encode(localePath, "UTF-8")
                
                // 获取现有文件内容
                val existingContent = getFileContentViaApi(baseApiUrl, projectId, branch, encodedPath, token)
                if (existingContent.isNullOrEmpty()) {
                    logger.info("文件不存在或为空，跳过: $localePath")
                    continue
                }
                
                // 解析现有内容
                val existingEntries = parsePropertiesContent(existingContent)
                
                // 检查是否有需要删除的 key
                val keysToRemoveInFile = keysToRemove.filter { existingEntries.containsKey(it) }
                if (keysToRemoveInFile.isEmpty()) {
                    logger.info("没有需要删除的 key，跳过: $localePath")
                    continue
                }
                
                logger.info("准备从 $localePath 删除 ${keysToRemoveInFile.size} 个 key: $keysToRemoveInFile")
                
                // 重建文件内容（删除指定的 key）
                val newContent = removeKeysFromContent(existingContent, keysToRemoveInFile.toSet())
                
                // 提交文件
                val apiUrl = "$baseApiUrl/projects/$projectId/repository/files"
                val connection = URI(apiUrl).toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setRequestProperty("Private-Token", token)
                
                val base64Content = Base64.getEncoder().encodeToString(newContent.toByteArray(StandardCharsets.UTF_8))
                val escapedPath = localePath.replace("\"", "\\\"")
                val commitMessage = "chore: 删除待重新翻译的 key（关联变更: $originalCommitMessage）"
                val escapedMessage = commitMessage.replace("\"", "\\\"")
                val requestBody = """
                    {
                        "file_path": "$escapedPath",
                        "branch_name": "$branch",
                        "content": "$base64Content",
                        "encoding": "base64",
                        "commit_message": "$escapedMessage"
                    }
                """.trimIndent()
                
                connection.outputStream.use { os ->
                    OutputStreamWriter(os, StandardCharsets.UTF_8).use { writer ->
                        writer.write(requestBody)
                        writer.flush()
                    }
                }
                
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    logger.info("成功从 $localePath 删除 ${keysToRemoveInFile.size} 个 key")
                } else {
                    val errorMessage = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    logger.warn("从 $localePath 删除 key 失败 ($responseCode): $errorMessage")
                }
            } catch (e: Exception) {
                logger.warn("处理 $localePath 时发生错误", e)
            }
        }
    }
    
    /**
     * 从 properties 内容中删除指定的 key
     */
    private fun removeKeysFromContent(content: String, keysToRemove: Set<String>): String {
        val resultLines = mutableListOf<String>()
        
        content.lines().forEach { line ->
            val trimmedLine = line.trim()
            
            // 跳过空行（不保留空行）
            if (trimmedLine.isEmpty()) {
                return@forEach
            }
            
            // 保留注释
            if (trimmedLine.startsWith("#") || trimmedLine.startsWith("!")) {
                resultLines.add(line)
                return@forEach
            }
            
            // 解析 key
            val separatorIndex = line.indexOfFirst { it == '=' || it == ':' }
            if (separatorIndex > 0) {
                val key = line.substring(0, separatorIndex).trim()
                // 如果 key 在删除列表中，则跳过这一行
                if (keysToRemove.contains(key)) {
                    return@forEach
                }
            }
            
            resultLines.add(line)
        }
        
        return resultLines.joinToString("\n") + "\n"
    }
    
    /**
     * 通过 API 获取文件内容
     */
    private fun getFileContentViaApi(baseUrl: String, projectId: String, branch: String, encodedPath: String, token: String): String? {
        return try {
            val apiUrl = "$baseUrl/projects/$projectId/repository/files?file_path=$encodedPath&ref=$branch"
            logger.info("获取文件: $apiUrl")
            
            val connection = URI(apiUrl).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Private-Token", token)
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                // 解析 JSON 获取 content 字段
                val contentMatch = Regex(""""content"\s*:\s*"([^"]+)"""").find(response)
                if (contentMatch != null) {
                    val base64Content = contentMatch.groupValues[1]
                    String(Base64.getDecoder().decode(base64Content), StandardCharsets.UTF_8)
                } else {
                    null
                }
            } else {
                logger.info("文件不存在或获取失败: $responseCode")
                null
            }
        } catch (e: Exception) {
            logger.warn("获取文件内容失败", e)
            null
        }
    }
    
    companion object {
        fun getInstance(project: Project): CodePlatformService {
            return project.service<CodePlatformService>()
        }
    }
}
