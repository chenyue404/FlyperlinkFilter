package com.chenyue404.flyperlinkfilter

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class WebViewHook : IXposedHookLoadPackage {
    private val PACKAGE_NAME = "com.flyperinc.flyperlink"
    private val TAG = "Flyperlink-hook-"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName
        val classLoader = lpparam.classLoader

        if (packageName != PACKAGE_NAME) {
            return
        }

        XposedBridge.log(TAG)

        XposedHelpers.findAndHookMethod(
            "android.webkit.WebView",
            classLoader,
            "setWebViewClient",
            WebViewClient::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.args[0] ?: return
                    val webViewClient: WebViewClient = param.args[0] as WebViewClient
                    XposedHelpers.findAndHookMethod(
                        webViewClient.javaClass,
                        "shouldOverrideUrlLoading",
                        WebView::class.java,
                        String::class.java,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val webview = param.args[0] as WebView
                                val url = param.args[1].toString()
                                if (shouldBlock(url)) {
                                    Toast.makeText(
                                        webview.context,
                                        "阻止打开\n$url",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    param.args[1] = ""
                                    param.result = true
                                }
                            }
                        }
                    )
                    param.args[0] = webViewClient
                }
            }
        )
    }

    private fun shouldBlock(url: String): Boolean {
//        XposedBridge.log(TAG + "shouldBlock url=$url")
        val scheme = Uri.parse(url).scheme
        val passSchemeList = listOf("http", "https")
        val result = !passSchemeList.contains(scheme)
        if (result)
            XposedBridge.log(TAG + "阻止打开\n$url")
        return result
    }
}