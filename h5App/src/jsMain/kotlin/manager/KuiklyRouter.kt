package com.tencent.kuikly.h5app.manager

import com.tencent.kuikly.h5app.KuiklyWebRenderViewDelegator
import com.tencent.kuikly.core.render.web.ktx.SizeI
import kotlinx.browser.sessionStorage
import kotlinx.browser.window
import com.tencent.kuikly.h5app.module.KRRouterModule
import org.w3c.dom.HTMLElement
import com.tencent.kuikly.h5app.utils.URL
import kotlin.js.Date
import kotlin.js.Json
import kotlin.js.json
import kotlin.math.floor
import kotlin.random.Random

object KuiklyRouter {
    private const val CONTAINER_ID = "root"
    private const val SCROLL_KEY_PREFIX = "kr_scroll_"
    
    // Feature Flag: Set to true to enable SPA mode by default, 
    // or control via URL param "use_spa=1"
    private const val ENABLE_BY_DEFAULT = false

    private const val DEFAULT_PAGE_NAME = "router"
    private const val URL_PARAM_USE_SPA = "use_spa"
    private const val URL_PARAM_PAGE_NAME = "page_name"
    private const val PARAM_IS_H5 = "is_H5"
    private const val VALUE_FLAG_ON = "1"
    private const val DEFAULT_STATUS_BAR_HEIGHT = 0f
    private const val SCROLL_RESTORATION_DELAY_MS = 10
    private const val RANDOM_MULTIPLIER = 10000
    private const val KEY_GENERATION_RADIX = 36

    private const val SCROLL_RESTORATION_MANUAL = "manual"
    private const val STATE_KEY = "key"
    
    private const val SCROLL_DATA_Y = "y"
    private const val SCROLL_DATA_TARGET = "target"
    
    private const val SCROLL_TARGET_ROOT = "root"
    private const val SCROLL_TARGET_CHILD = "child"
    private const val SCROLL_TARGET_WINDOW = "window"

    // Page Cache: Key -> PageInfo
    private val pageCache = mutableMapOf<String, PageInfo>()
    
    // Current Active Key
    private var currentKey: String = ""

    data class PageInfo(
        val key: String,
        val delegator: KuiklyWebRenderViewDelegator,
        val element: HTMLElement,
        var isDestroyed: Boolean = false
    )

    /**
     * Try to hijack the entry point.
     * Returns true if Router took over (SPA mode active), false otherwise.
     */
    fun handleEntry(): Boolean {
        val urlParams = URL.parseParams(window.location.href)
        val useSpa = urlParams[URL_PARAM_USE_SPA] == VALUE_FLAG_ON || ENABLE_BY_DEFAULT
        
        if (useSpa) {
            init()
            return true
        }
        return false
    }

    private fun init() {
        console.log("##### Kuikly H5 SPA Mode (Router Active) #####")

        // 1. Disable browser automatic scroll restoration
        if (window.history.asDynamic().scrollRestoration != null) {
            window.history.asDynamic().scrollRestoration = SCROLL_RESTORATION_MANUAL
        }

        // 2. Initialize Current State Key
        val state = window.history.state?.unsafeCast<Json>()
        if (state == null || state[STATE_KEY] == null) {
            val newKey = generateKey()
            val newState = json(STATE_KEY to newKey)
            window.history.replaceState(newState, "", window.location.href)
            currentKey = newKey
        } else {
            currentKey = state[STATE_KEY] as String
        }

        // 3. Render Initial Page
        renderPage(currentKey, window.location.href)

        // 4. Listen to History Changes (Back/Forward)
        window.onpopstate = { event ->
            val s = event.state?.unsafeCast<Json>()
            val newKey = s?.get(STATE_KEY) as? String
            
            if (newKey != null) {
                saveScrollPosition(currentKey)
                handlePageSwitch(newKey)
                currentKey = newKey
            } else {
                console.warn("Popstate missing key, reloading")
                window.location.reload()
            }
        }

        // 5. Register Global Router Hooks
        KRRouterModule.globalNavigationHandler = { url ->
            push(url)
            true
        }
        
        KRRouterModule.globalClosePageHandler = {
            back()
            true
        }

        // 6. Handle Visibility globally for the router
        // We attach the listener here so Main.kt doesn't need to worry about it in SPA mode
        val doc = window.document
        doc.addEventListener("visibilitychange", {
             val hidden = doc.asDynamic().hidden as Boolean
             handleVisibilityChange(hidden)
        })
    }

    fun push(url: String) {
        saveScrollPosition(currentKey)
        val newKey = generateKey()
        val newState = json(STATE_KEY to newKey)
        window.history.pushState(newState, "", url)
        handlePageSwitch(newKey)
        currentKey = newKey
    }
    
    fun replace(url: String) {
        val newKey = generateKey()
        val newState = json(STATE_KEY to newKey)
        window.history.replaceState(newState, "", url)
        pageCache.remove(currentKey)?.let { destroyPage(it) }
        handlePageSwitch(newKey)
        currentKey = newKey
    }

    fun back() {
        window.history.back()
    }

    private fun handleVisibilityChange(hidden: Boolean) {
        pageCache[currentKey]?.let { page ->
            if (hidden) page.delegator.pause() else page.delegator.resume()
        }
    }

    private fun handlePageSwitch(targetKey: String) {
        pageCache[currentKey]?.let { page ->
            if (!page.isDestroyed) {
                page.delegator.pause()
                page.element.style.display = "none"
            }
        }

        renderPage(targetKey, window.location.href)
        
        window.setTimeout({
            restoreScrollPosition(targetKey)
        }, SCROLL_RESTORATION_DELAY_MS)
    }

    private fun renderPage(key: String, url: String) {
        val cachedPage = pageCache[key]
        if (cachedPage != null) {
            if (cachedPage.isDestroyed) {
                 pageCache.remove(key)
                 renderPage(key, url)
                 return
            }
            cachedPage.element.style.display = "block"
            cachedPage.delegator.resume()
        } else {
            val pageInfo = createPage(key, url)
            if (pageInfo != null) {
                pageCache[key] = pageInfo
            }
        }
    }

    /**
     * Create and initialize a KuiklyWebRenderViewDelegator for a given URL.
     * This method encapsulates the common logic for initializing a page, used by both
     * the SPA router and the standalone Main entry point.
     */
    fun createDelegator(url: String): KuiklyWebRenderViewDelegator {
        val urlParams = URL.parseParams(url)
        val pageName = urlParams[URL_PARAM_PAGE_NAME] ?: DEFAULT_PAGE_NAME

        val containerWidth = window.innerWidth
        val containerHeight = window.innerHeight

        val params: MutableMap<String, String> = urlParams.toMutableMap()
        params[PARAM_IS_H5] = VALUE_FLAG_ON

        val paramMap: Map<String, Any> = mapOf(
            "statusBarHeight" to DEFAULT_STATUS_BAR_HEIGHT,
            "activityWidth" to containerWidth,
            "activityHeight" to containerHeight,
            "param" to params.toMap(),
        )

        val delegator = KuiklyWebRenderViewDelegator()
        delegator.init(
            CONTAINER_ID, 
            pageName, 
            paramMap, 
            SizeI(containerWidth, containerHeight)
        )
        delegator.resume()
        return delegator
    }

    private fun createPage(key: String, url: String): PageInfo? {
        val delegator = createDelegator(url)
        val element = delegator.getKuiklyRenderContext()?.kuiklyRenderRootView?.view as? HTMLElement
        
        return if (element != null) {
            element.style.display = "block"
            PageInfo(key, delegator, element)
        } else {
            console.error("Failed to create page element")
            null
        }
    }

    private fun destroyPage(page: PageInfo) {
        page.isDestroyed = true
        page.delegator.detach()
        page.element.remove()
        sessionStorage.removeItem(SCROLL_KEY_PREFIX + page.key)
    }

    private fun saveScrollPosition(key: String) {
        val page = pageCache[key] ?: return
        val view = page.element
        val scrollData = json()
        
        if (view.scrollHeight > view.clientHeight && view.scrollTop > 0.0) {
            scrollData[SCROLL_DATA_Y] = view.scrollTop
            scrollData[SCROLL_DATA_TARGET] = SCROLL_TARGET_ROOT
        } else {
            val firstChild = view.firstElementChild as? HTMLElement
            if (firstChild != null && firstChild.scrollHeight > firstChild.clientHeight && firstChild.scrollTop > 0.0) {
                scrollData[SCROLL_DATA_Y] = firstChild.scrollTop
                scrollData[SCROLL_DATA_TARGET] = SCROLL_TARGET_CHILD
            } else if (window.scrollY > 0.0) {
                scrollData[SCROLL_DATA_Y] = window.scrollY
                scrollData[SCROLL_DATA_TARGET] = SCROLL_TARGET_WINDOW
            }
        }
        
        if (scrollData[SCROLL_DATA_Y] != null) {
            sessionStorage.setItem(SCROLL_KEY_PREFIX + key, JSON.stringify(scrollData))
        }
    }

    private fun restoreScrollPosition(key: String) {
        val dataStr = sessionStorage.getItem(SCROLL_KEY_PREFIX + key) ?: return
        try {
            val data = JSON.parse<Json>(dataStr).unsafeCast<dynamic>()
            val y = data[SCROLL_DATA_Y] as Double
            val target = data[SCROLL_DATA_TARGET] as String
            
            val page = pageCache[key] ?: return
            val view = page.element
            
            when (target) {
                SCROLL_TARGET_ROOT -> view.scrollTop = y
                SCROLL_TARGET_CHILD -> (view.firstElementChild as? HTMLElement)?.scrollTop = y
                SCROLL_TARGET_WINDOW -> window.scrollTo(0.0, y)
            }
        } catch (e: dynamic) {
            console.error("Error restoring scroll", e)
        }
    }

    private fun generateKey(): String {
        return (Date.now().toLong()).toString(KEY_GENERATION_RADIX) + floor(Random.nextDouble() * RANDOM_MULTIPLIER).toString()
    }
}
