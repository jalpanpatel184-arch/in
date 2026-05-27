package com.nova.assistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * NovaAccessibilityService
 * ────────────────────────
 * Provides programmatic control over any app's UI:
 * • Tap / long-press any node by text, ID, or class
 * • Type text into focusable fields
 * • Scroll lists
 * • Navigate Back / Home / Recents
 * • Find nodes with OCR fallback
 * • Multi-step automation sequences
 */
class NovaAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        // Singleton reference so AutomationEngine can access
        @Volatile var instance: NovaAccessibilityService? = null
            private set

        private val _currentPackage = MutableStateFlow("")
        val currentPackage: StateFlow<String> = _currentPackage

        private val _screenText = MutableStateFlow("")
        val screenText: StateFlow<String> = _screenText
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                event.packageName?.toString()?.let {
                    _currentPackage.value = it
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Update screen text for context
                serviceScope.launch {
                    _screenText.value = extractScreenText()
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        serviceScope.cancel()
        return super.onUnbind(intent)
    }

    // ══════════════════════════════════════════════════
    //  TAP OPERATIONS
    // ══════════════════════════════════════════════════

    /** Find and tap a node that contains [text] */
    fun tapByText(text: String, exact: Boolean = false): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByText(root, text, exact) ?: return false
        return performTap(node)
    }

    /** Tap a node by its content-description */
    fun tapByContentDescription(desc: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByContentDesc(root, desc) ?: return false
        return performTap(node)
    }

    /** Tap by resource ID (e.g. "com.whatsapp:id/send") */
    fun tapById(resourceId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(resourceId)
        return nodes.firstOrNull()?.let { performTap(it) } ?: false
    }

    /** Tap at absolute screen coordinates */
    fun tapAt(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun performTap(node: AccessibilityNodeInfo): Boolean {
        // Try click action first
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        // Fall back to gesture tap
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return tapAt(bounds.centerX().toFloat(), bounds.centerY().toFloat())
    }

    // ══════════════════════════════════════════════════
    //  SCROLL OPERATIONS
    // ══════════════════════════════════════════════════

    fun scrollDown(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findScrollableNode(root) ?: return false
        return scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    fun scrollUp(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findScrollableNode(root) ?: return false
        return scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    fun swipeUp(startX: Float = 540f, startY: Float = 1500f, endY: Float = 500f): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    // ══════════════════════════════════════════════════
    //  TYPE TEXT
    // ══════════════════════════════════════════════════

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findEditableNode(root)
            ?: return false

        val args = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun appendText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val current = node.text?.toString() ?: ""
        val args = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, current + text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun clearText(): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    // ══════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════

    fun pressBack()    = performGlobalAction(GLOBAL_ACTION_BACK)
    fun pressHome()    = performGlobalAction(GLOBAL_ACTION_HOME)
    fun pressRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun pressNotifications() = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    // ══════════════════════════════════════════════════
    //  SEARCH / FIND NODES
    // ══════════════════════════════════════════════════

    fun findNodeByText(
        root: AccessibilityNodeInfo,
        text: String,
        exact: Boolean = false
    ): AccessibilityNodeInfo? {
        val results = root.findAccessibilityNodeInfosByText(text)
        return if (exact) {
            results.firstOrNull { it.text?.toString()?.equals(text, ignoreCase = true) == true }
        } else {
            results.firstOrNull()
        }
    }

    private fun findNodeByContentDesc(
        root: AccessibilityNodeInfo,
        desc: String
    ): AccessibilityNodeInfo? {
        if (root.contentDescription?.toString()
                ?.contains(desc, ignoreCase = true) == true) return root
        for (i in 0 until root.childCount) {
            val result = findNodeByContentDesc(root.getChild(i) ?: continue, desc)
            if (result != null) return result
        }
        return null
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val result = findScrollableNode(root.getChild(i) ?: continue)
            if (result != null) return result
        }
        return null
    }

    private fun findEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable) return root
        for (i in 0 until root.childCount) {
            val result = findEditableNode(root.getChild(i) ?: continue)
            if (result != null) return result
        }
        return null
    }

    /** Extract all visible text from current screen for context */
    fun extractScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        val builder = StringBuilder()
        extractTextRecursive(root, builder)
        return builder.toString().trim()
    }

    private fun extractTextRecursive(node: AccessibilityNodeInfo, builder: StringBuilder) {
        node.text?.let { builder.append(it).append(" ") }
        node.contentDescription?.let { builder.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            extractTextRecursive(node.getChild(i) ?: return@extractTextRecursive, builder)
        }
    }

    /** Wait for a specific text to appear (polling) */
    suspend fun waitForText(text: String, timeoutMs: Long = 5000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val root = rootInActiveWindow
            if (root != null && findNodeByText(root, text) != null) return true
            delay(200)
        }
        return false
    }

    /** Wait for a specific package to be in foreground */
    suspend fun waitForPackage(packageName: String, timeoutMs: Long = 5000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (currentPackage.value == packageName) return true
            delay(200)
        }
        return false
    }
}
