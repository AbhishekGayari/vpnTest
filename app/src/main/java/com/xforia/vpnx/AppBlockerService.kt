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
//import utility.App
//import utility.PreferenceHelper

class AppBlockerService: AccessibilityService() {

    private var blockedDomainSet = mutableListOf<String>()
    private var lastEventTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        Log.d(
                "ForegroundApp",
                "Currently in the foreground: $packageName   || "//cl: $className text: $eventText  evTypw: ${event?.eventType}
            )
        // Website blocking functionality only
        callDomainBlocking(event)
        callStateDomainBlocking(event)
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

    private fun callStateDomainBlocking(event: AccessibilityEvent) {

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) {
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
            val domainNameList = getAllAppDomains().map { normalizeDomain(it) }

            if (domainNameList.isNotEmpty()) {
                if (event.packageName.toString() in browserPackages) {
                    val rootNode = event.source ?: run {
                        return
                    }
                    if (System.currentTimeMillis() - lastEventTime < 500) return
                    lastEventTime = System.currentTimeMillis()

                    val searchBoxNodes = rootNode.findAccessibilityNodeInfosByText("Search")
                        ?: rootNode.findAccessibilityNodeInfosByViewId("android.widget.EditText")
                        ?: rootNode.findAccessibilityNodeInfosByViewId("android.widget.SearchView")

                    if (searchBoxNodes != null && searchBoxNodes.isNotEmpty()) {
                        Log.e("AccessibilityContent", "Search box detected, skipping blocking")
                        return
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        val content = buildString { rootNode.traverseAndCollectText(this) }
                        // Extract URLs using updated regular expressions
                        val urlRegex =
                            Regex("(https?://)?[\\w\\-]+(\\.[\\w\\-]+)+(/[\\w\\-./?%&=]*)?")
                        val urlMatches = urlRegex.findAll(content)

                        val validUrls = urlMatches.map { it.value }
                            .filter { url ->
                                url.contains(".com") || url.contains(".org") || url.contains(
                                    ".net"
                                )
                            }

                        for (url in validUrls) {
                            Log.d("--AccessibilityContent", "Normalized Content: $url")
                            val normalizedUrl = normalizeDomain(url)
                            if (domainNameList.any { keyword ->
                                    normalizedUrl.contains(keyword, ignoreCase = true)
                                }) {
                                restrictAccess("This domain is blocked")
                                break
                            }
                        }
                    }, 500)
                }
            }
        }
    }


    private fun getAllAppDomains(): List<String> {
        val sharedPreferences = applicationContext.getSharedPreferences("BlockedWebsites", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("blocked_domains", emptySet())?.toList() ?: emptyList()
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
//        val domainNameList =
//            PreferenceHelper.getDomainList(App.appContext).map { normalizeDomain(it) }
//        val blacklist = PreferenceHelper.getBlackListDomain(App.appContext).map { normalizeDomain(it) }
//        val uniqueCombinedList = (domainNameList + blacklist).toSet().toList()
        val domainNameList = getAllAppDomains()
        val blacklist = blockedDomainSet.map { normalizeDomain(it) }

        val uniqueCombinedList = if (blacklist.isNotEmpty()) {
            (domainNameList + blacklist).toSet().toList()
        } else {
            domainNameList
        }

        if (uniqueCombinedList.isNotEmpty()) {
            if (event.packageName.toString() in browserPackages) {
                val rootNode = event.source ?: run {
                    Log.d("AccessibilityService", "Root node is null")
                    return
                }
                if (System.currentTimeMillis() - lastEventTime < 500) return
                lastEventTime = System.currentTimeMillis()

                val searchBoxNodes = rootNode.findAccessibilityNodeInfosByText("Search")
                    ?: rootNode.findAccessibilityNodeInfosByViewId("android.widget.EditText")
                    ?: rootNode.findAccessibilityNodeInfosByViewId("android.widget.SearchView")

                if (searchBoxNodes != null && searchBoxNodes.isNotEmpty()) {
                    Log.e("AccessibilityContent", "Search box detected, skipping blocking")
                    return
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    val content = buildString { rootNode.traverseAndCollectText(this) }
                    // Extract URLs using updated regular expressions
                    val urlRegex =
                        Regex("(https?://)?[\\w\\-]+(\\.[\\w\\-]+)+(/[\\w\\-./?%&=]*)?")
                    val urlMatches = urlRegex.findAll(content)

                    val validUrls = urlMatches.map { it.value }
                        .filter { url ->
                            url.contains(".com") || url.contains(".org") || url.contains(
                                ".net"
                            )
                        }

                    Log.d("AccessibilityService", "Valid URLs: ${validUrls.joinToString()}")

                    for (url in validUrls) {
                        Log.d("AccessibilityContent", "Normalized Content: $url")
                        val normalizedUrl = normalizeDomain(url)
                        if (uniqueCombinedList.any { keyword ->
                                normalizedUrl.contains(keyword, ignoreCase = true)
                            }) {
                            restrictAccess("This domain is blocked")
                            break
                        }
                    }
                }, 500)

            }
        }
    }

    private fun normalizeDomain(url: String): String {
        val normalized = url.replace(Regex("^(https?://)?(www\\.)?"), "")
        val regex = Regex("([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}")
        val match = regex.find(normalized)
        return match?.value ?: ""
    }

    private fun AccessibilityNodeInfo.traverseAndCollectText(output: StringBuilder) {
        if (this.text != null) {
            output.append(this.text).append(" ")
        }
        for (i in 0 until childCount) {
            getChild(i)?.traverseAndCollectText(output)
        }
    }

    private fun restrictAccess(message: String) {
        performGlobalAction(GLOBAL_ACTION_BACK)
        Handler(Looper.getMainLooper()).postDelayed({
            performGlobalAction(GLOBAL_ACTION_HOME)
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
