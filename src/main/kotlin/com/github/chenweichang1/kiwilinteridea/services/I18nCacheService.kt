package com.github.chenweichang1.kiwilinteridea.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.chenweichang1.kiwilinteridea.settings.KiwiSettings
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * I18N 文案缓存服务
 * 从远程仓库获取并缓存中英文 properties 文件内容
 */
@Service(Service.Level.PROJECT)
class I18nCacheService(private val project: Project) {
    
    private val logger = thisLogger()
    
    // 缓存：key -> 中文文案
    private val zhCache = ConcurrentHashMap<String, String>()
    
    // 缓存：key -> 英文文案
    private val enCache = ConcurrentHashMap<String, String>()
    
    // 上次刷新时间
    private var lastRefreshTime: Long = 0
    
    // 缓存有效期（5分钟）
    private val cacheValidityMs = 5 * 60 * 1000L
    
    // 是否正在加载
    @Volatile
    private var isLoading = false
    
    /**
     * 获取文案信息
     * @return Pair<中文, 英文>，如果不存在返回 null
     */
    fun getI18nEntry(key: String): Pair<String?, String?> {
        // 检查是否需要刷新缓存
        refreshCacheIfNeeded()
        
        val zh = zhCache[key]
        val en = enCache[key]
        
        return Pair(zh, en)
    }
    
    /**
     * 检查 key 是否已存在
     */
    fun containsKey(key: String): Boolean {
        refreshCacheIfNeeded()
        return zhCache.containsKey(key) || enCache.containsKey(key)
    }
    
    /**
     * 强制刷新缓存
     */
    fun forceRefresh() {
        lastRefreshTime = 0
        refreshCacheIfNeeded()
    }
    
    /**
     * 更新本地缓存（提交成功后调用）
     * 直接更新内存缓存，避免重新从远程加载
     */
    fun updateLocalCache(key: String, zhValue: String) {
        zhCache[key] = zhValue
        logger.info("本地缓存已更新: $key")
    }
    
    /**
     * 批量更新本地缓存
     */
    fun updateLocalCacheBatch(entries: Map<String, String>) {
        zhCache.putAll(entries)
        logger.info("本地缓存已批量更新: ${entries.size} 条")
    }
    
    /**
     * 如果缓存过期则刷新
     */
    private fun refreshCacheIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime > cacheValidityMs && !isLoading) {
            loadFromRemote()
        }
    }
    
    /**
     * 从远程仓库加载 properties 文件
     */
    private fun loadFromRemote() {
        if (isLoading) return
        
        isLoading = true
        
        // 在后台线程中加载
        Thread {
            try {
                val settings = KiwiSettings.getInstance(project)
                val token = settings.state.privateToken
                val projectId = settings.state.projectId
                val branch = settings.state.targetBranch
                
                if (token.isBlank() || projectId.isBlank()) {
                    logger.warn("未配置 Token 或 Project ID，跳过加载")
                    return@Thread
                }
                
                val baseApiUrl = "https://code.alibaba-inc.com/api/v3"
                
                // 加载中文文件
                val zhPath = settings.state.zhPropertiesPath
                val zhContent = fetchFileContent(baseApiUrl, projectId, branch, zhPath, token)
                if (zhContent != null) {
                    parsePropertiesContent(zhContent, zhCache)
                    logger.info("已加载中文文案 ${zhCache.size} 条")
                }
                
                // 加载英文文件（路径替换 _zh 为 _en）
                val enPath = zhPath.replace("_zh.properties", "_en.properties")
                val enContent = fetchFileContent(baseApiUrl, projectId, branch, enPath, token)
                if (enContent != null) {
                    parsePropertiesContent(enContent, enCache)
                    logger.info("已加载英文文案 ${enCache.size} 条")
                }
                
                lastRefreshTime = System.currentTimeMillis()
                logger.info("I18N 缓存刷新完成")
                
            } catch (e: Exception) {
                logger.error("加载 I18N 文案失败", e)
            } finally {
                isLoading = false
            }
        }.start()
    }
    
    /**
     * 从 API 获取文件内容
     */
    private fun fetchFileContent(
        baseUrl: String, 
        projectId: String, 
        branch: String, 
        filePath: String, 
        token: String
    ): String? {
        return try {
            val encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8")
            val apiUrl = "$baseUrl/projects/$projectId/repository/files?file_path=$encodedPath&ref=$branch"
            
            logger.info("获取文件: $filePath")
            
            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Private-Token", token)
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                // 解析 JSON 获取 content 字段（Base64 编码）
                val contentMatch = Regex(""""content"\s*:\s*"([^"]+)"""").find(response)
                if (contentMatch != null) {
                    val base64Content = contentMatch.groupValues[1]
                    String(Base64.getDecoder().decode(base64Content), StandardCharsets.UTF_8)
                } else {
                    null
                }
            } else {
                logger.warn("获取文件失败: $filePath, HTTP $responseCode")
                null
            }
        } catch (e: Exception) {
            logger.warn("获取文件内容失败: $filePath", e)
            null
        }
    }
    
    /**
     * 解析 properties 内容到缓存
     */
    private fun parsePropertiesContent(content: String, cache: ConcurrentHashMap<String, String>) {
        cache.clear()
        
        content.lines().forEach { line ->
            val trimmedLine = line.trim()
            // 跳过空行和注释
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#") || trimmedLine.startsWith("!")) {
                return@forEach
            }
            
            // 解析 key=value
            val separatorIndex = line.indexOfFirst { it == '=' || it == ':' }
            if (separatorIndex > 0) {
                val key = line.substring(0, separatorIndex).trim()
                val value = line.substring(separatorIndex + 1).trim()
                cache[key] = value
            }
        }
    }
    
    companion object {
        fun getInstance(project: Project): I18nCacheService {
            return project.service<I18nCacheService>()
        }
    }
}
