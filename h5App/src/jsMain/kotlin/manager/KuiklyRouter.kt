package manager

import KuiklyWebRenderViewDelegator
import com.tencent.kuikly.core.render.web.ktx.SizeI
import kotlinx.browser.sessionStorage
import kotlinx.browser.window
import module.KRRouterModule
import org.w3c.dom.HTMLElement
import utils.URL
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
        val useSpa = urlParams["use_spa"] == "1" || ENABLE_BY_DEFAULT
        
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
            window.history.asDynamic().scrollRestoration = "manual"
        }

        // 2. Initialize Current State Key
        val state = window.history.state?.unsafeCast<Json>()
        if (state == null || state["key"] == null) {
            val newKey = generateKey()
            val newState = json("key" to newKey)
            window.history.replaceState(newState, "", window.location.href)
            currentKey = newKey
        } else {
            currentKey = state["key"] as String
        }

        // 3. Render Initial Page
        renderPage(currentKey, window.location.href)

        // 4. Listen to History Changes (Back/Forward)
        window.onpopstate = { event ->
            val s = event.state?.unsafeCast<Json>()
            val newKey = s?.get("key") as? String
            
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
        val newState = json("key" to newKey)
        window.history.pushState(newState, "", url)
        handlePageSwitch(newKey)
        currentKey = newKey
    }
    
    fun replace(url: String) {
        val newKey = generateKey()
        val newState = json("key" to newKey)
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
        }, 10)
    }

    private fun renderPage(key: String, url: String) {
        if (pageCache.containsKey(key)) {
            val page = pageCache[key]!!
            if (page.isDestroyed) {
                 pageCache.remove(key)
                 renderPage(key, url)
                 return
            }
            page.element.style.display = "block"
            page.delegator.resume()
        } else {
            val pageInfo = createPage(key, url)
            if (pageInfo != null) {
                pageCache[key] = pageInfo
            }
        }
    }

    private fun createPage(key: String, url: String): PageInfo? {
        val urlParams = URL.parseParams(url)
        val pageName = urlParams["page_name"] ?: "router"
        
        val containerWidth = window.innerWidth
        val containerHeight = window.innerHeight
        
        val params: MutableMap<String, String> = mutableMapOf()
        if (urlParams.isNotEmpty()) {
            urlParams.forEach { (k, v) -> params[k] = v }
        }
        params["is_H5"] = "1"
        
        val paramMap = mapOf(
            "statusBarHeight" to 0f,
            "activityWidth" to containerWidth,
            "activityHeight" to containerHeight,
            "param" to params,
        )

        val delegator = KuiklyWebRenderViewDelegator()
        delegator.init(
            CONTAINER_ID, 
            pageName, 
            paramMap, 
            SizeI(containerWidth, containerHeight)
        )
        delegator.resume()
        
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
            scrollData["y"] = view.scrollTop
            scrollData["target"] = "root"
        } else {
            val firstChild = view.firstElementChild as? HTMLElement
            if (firstChild != null && firstChild.scrollHeight > firstChild.clientHeight && firstChild.scrollTop > 0.0) {
                scrollData["y"] = firstChild.scrollTop
                scrollData["target"] = "child"
            } else if (window.scrollY > 0.0) {
                scrollData["y"] = window.scrollY
                scrollData["target"] = "window"
            }
        }
        
        if (scrollData["y"] != null) {
            sessionStorage.setItem(SCROLL_KEY_PREFIX + key, JSON.stringify(scrollData))
        }
    }

    private fun restoreScrollPosition(key: String) {
        val dataStr = sessionStorage.getItem(SCROLL_KEY_PREFIX + key) ?: return
        try {
            val data = JSON.parse<Json>(dataStr).unsafeCast<dynamic>()
            val y = data.y as Double
            val target = data.target as String
            
            val page = pageCache[key] ?: return
            val view = page.element
            
            when (target) {
                "root" -> view.scrollTop = y
                "child" -> (view.firstElementChild as? HTMLElement)?.scrollTop = y
                "window" -> window.scrollTo(0.0, y)
            }
        } catch (e: dynamic) {
            console.error("Error restoring scroll", e)
        }
    }

    private fun generateKey(): String {
        return (Date.now().toLong()).toString(36) + floor(Random.nextDouble() * 10000).toString()
    }
}
