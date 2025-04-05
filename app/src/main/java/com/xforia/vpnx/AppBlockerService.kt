package com.xforia.vpnx

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class AppBlockerService : AccessibilityService() {
    private var lastEventTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        Log.d(
            "ForegroundApp",
            "Currently in the foreground: $packageName   || "//cl: $className text: $eventText  evTypw: ${event?.eventType}
        )

        callDomainBlocking(event)
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        serviceInfo = info
    }

    private fun callDomainBlocking(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) {
            restrictBrowser(event)
        }
    }

    private fun getAllAppDomains(): List<String> {
        val sharedPreferences =
            applicationContext.getSharedPreferences("BlockedWebsites", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("blocked_domains", emptySet())?.toList()
            ?: emptyList()
    }

    private fun restrictBrowser(event: AccessibilityEvent) {

        val browserPackages = setOf(
            "com.android.chrome",                     // Chrome
            "org.mozilla.firefox",                    // Firefox
            "com.microsoft.emmx",                     // Edge
            "com.sec.android.app.sbrowser",           // Samsung Internet
            "com.UCMobile.intl",                      // UC Browser
            "com.opera.mini.native",                  // Opera Mini
            "com.opera.browser",                       // Opera
            "com.uc.browser.en",                      // UC Browser (English)
            "com.google.android.googlequicksearchbox", // Google Search
            "com.brave.browser",                      // Brave

            "com.duckduckgo.mobile.android",          // DuckDuckGo Privacy Browser
            "com.vivaldi.browser",                     // Vivaldi
            "com.microsoft.bing",                     // Microsoft Bing
            "com.ghostery.android",                   // Ghostery Privacy Browser
            "org.mozilla.fennec_aurora",              // Fennec (Firefox for Android)
            "com.cloudmosa.puffin",                   // Puffin Browser
            "com.opera.touch",                         // Opera Touch
            "com.kiwibrowser.browser",                 // Kiwi Browser
            "com.xbrowser",                           // XBrowser
            "com.sec.android.app.sbrowser.beta",      // Samsung Internet Beta

            "com.cleanmaster.security",                // CM Browser
            "com.mx.browser",                         // Maxthon Browser
            "com.yandex.browser",                      // Yandex Browser
            "com.droidyoo.aloha",                     // Aloha Browser
            "com.flynx",                              // Flynx
            "com.opera.gx",                           // Opera GX
            "com.silo",                               // Silo
            "com.cloudmosa.puffinTV",                 // Puffin TV Browser
            "com.snda.browser",                       // Phoenix Browser
            "com.cma.browser",                        // CM Browser

            "com.dolphin.browser",                    // Dolphin Browser
            "com.netspector",                         // NetSpector
            "com.tenta",                              // Tenta Browser
            "net.privacybrowser.privacysbrowser",     // Privacy Browser
            "org.torproject.torbrowser",              // Tor Browser
            "com.startpage.android",                   // StartPage
            "com.dolphin.browser.zero",               // Dolphin Zero
            "com.ghostery.android.lite",              // Ghostery Lite
            "com.android.browser",                     // AOSP Browser
            "com.kmeleon.browser",                    // K-Meleon

            "com.mx.browser",                         // Maxthon Cloud
            "com.droidweb",                           // DroidWeb
            "me.via",                                 // Via Browser
            "com.opera.mini.native.beta",             // Opera Mini Beta
            "com.wizkhalifa.browser",                 // Wiz Khalifa Browser
            "com.simple.browser",                      // Simple Browser
            "com.naked.browser",                       // Naked Browser
            "com.droidyoo.aloha.lite",                // Aloha Browser Lite
            "com.zerodim.webexplorer",                // Web Explorer
            "com.opera.news",                         // Opera News

            "com.android.browser",                     // Mi Browser
            "com.coccoc.browser",                     // Coc Coc Browser
            "com.apusapps.browser",                   // Apus Browser
            "org.kde.falkon",                         // Falkon
            "com.browserstack",                        // BrowserStack
            "com.cloudmosa.puffin.pro",
            "com.sec.android.app.sbrowser.beta",
            "com.sft.inbrowser",
            "com.mx.browser",
            "com.cloudmosa.puffin",
            "com.xbrowser"
        )

        val domainNameList = getAllAppDomains()

        val uniqueCombinedList = domainNameList

        if (event.packageName.toString() in browserPackages) {

            val rootNode = rootInActiveWindow ?: run {
                Log.d("AccessibilityService", "⚠️ Root node is NULL after delay")
                return
            }
            if (System.currentTimeMillis() - lastEventTime < 500) return
            lastEventTime = System.currentTimeMillis()

            if (event.packageName.toString().contains("google")) {
                val searchBoxNodes =
                    rootNode.findAccessibilityNodeInfosByViewId("android.widget.EditText")
                        ?: rootNode.findAccessibilityNodeInfosByText("Search")
                        ?: rootNode.findAccessibilityNodeInfosByViewId("android.widget.SearchView")

                if (searchBoxNodes != null && searchBoxNodes.isNotEmpty()) {
                    val isSearchBoxFocused =
                        searchBoxNodes.any { it.isFocused } // True when clicked inside
                    val isTyping =
                        searchBoxNodes.any { !it.text.isNullOrEmpty() } // True if there's text
                    Log.e("AccessibilityContent", "Focus: $isSearchBoxFocused, Typing: $isTyping")

                    if (isSearchBoxFocused || isTyping) {
                        Log.e("AccessibilityContent", "User is in search box, skipping blocking")
                        return
                    }
                }
            } else {
                val searchBoxNodes = rootNode.findAccessibilityNodeInfosByText("Search")
                    ?: rootNode.findAccessibilityNodeInfosByViewId("android.widget.EditText")
                    ?: rootNode.findAccessibilityNodeInfosByViewId("android.widget.SearchView")

                if (searchBoxNodes != null && searchBoxNodes.isNotEmpty()) {
                    Log.e("AccessibilityContent", "Search box detected, skipping blocking")
                    return
                }
            }


            Handler(Looper.getMainLooper()).postDelayed({

                // Collect text content from the UI tree
                val content = buildString { rootNode.traverseAndCollectText(this) }
                logNodeStructure(rootNode) // Debug UI tree structure

                // Regular expression to extract URLs from the content
                val urlRegex = Regex(
                    "\\b((?:https?:\\/\\/|www\\.)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6})(?:\\/\\S*)?\\b"
                )
                val urlMatches = urlRegex.findAll(content)

                // Extract, normalize, and filter URLs
                val extractedUrls = urlMatches.map { it.value }
                    .map { normalizeDomain(it) }  // Ensure this function returns something like "linkedin.com"
                    .filterNot { it in listOf("gmail.com", "google.com", "remote.co") }
                    .distinct()
                    .toList()

                Log.d("AccessibilityService", "Extracted URLs: $extractedUrls")

                // Allowed domains list
                val allowedDomains = uniqueCombinedList

                // Helper function: returns true if either domain is the same or one ends with the other.
                fun domainMatches(extracted: String, allowed: String): Boolean {
                    return extracted.equals(allowed, ignoreCase = true) ||
                            extracted.endsWith(allowed, ignoreCase = true) ||
                            allowed.endsWith(extracted, ignoreCase = true)
                }

                // Prioritize URLs that match one of your allowed domains
                val primaryUrl = extractedUrls.firstOrNull { url ->
                    allowedDomains.any { allowed -> domainMatches(url, allowed) }
                } ?: extractedUrls.firstOrNull()  // Fallback if no match is found

                if (primaryUrl == null || extractedUrls.isEmpty()) {
                    Log.d("AccessibilityService", "No valid URL found")
                    return@postDelayed
                }
                val shouldBlock =
                    allowedDomains.any { allowed -> domainMatches(primaryUrl, allowed) }

                Log.d("AccessibilityService", "🔎 Checking URL: $primaryUrl → Block: $shouldBlock")

                if (shouldBlock) {
                    restrictAccess("This domain is blocked")
                }

            }, 500)
        }
    }

    fun logNodeStructure(node: AccessibilityNodeInfo?, depth: Int = 0) {
        if (node == null) return
        Log.d(
            "AccessibilityService",
            "${" ".repeat(depth * 2)}🔹 Node: ${node.className} | Text: ${node.text}"
        )

        for (i in 0 until node.childCount) {
            logNodeStructure(node.getChild(i), depth + 1)
        }
    }

    private fun normalizeDomain(url: String): String {
        return url.replace(Regex("^https?://(www\\.)?"), "") // Remove http://, https://, and www.
            .split("/")[0] // Take only the main domain
            .lowercase() // Convert to lowercase for comparison
    }

    fun AccessibilityNodeInfo.traverseAndCollectText(builder: StringBuilder) {
        if (!this.text.isNullOrBlank()) {
            builder.append(this.text.toString()).append(" ")
        }
        for (i in 0 until childCount) {
            getChild(i)?.traverseAndCollectText(builder)
        }
    }


    private fun restrictAccess(message: String) {
        performGlobalAction(GLOBAL_ACTION_BACK)
        Handler(Looper.getMainLooper()).postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
            showToast(message)
        }, 100)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onInterrupt() {
        // Handle interrupt if necessary
    }
}
