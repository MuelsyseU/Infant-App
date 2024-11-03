package com.muelsyseu.infant

//import android.app.AlertDialog
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.WebView
import android.webkit.WebView.setWebContentsDebuggingEnabled
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.muelsyseu.infant.MyChromeClient.Companion.FILE_CHOOSER_RESULT_CODE
import com.muelsyseu.infant.ui.theme.InfantTheme
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : ComponentActivity() {
    var uploadMessage: ValueCallback<Array<Uri?>?>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) Python.start(AndroidPlatform(this))
        val python = Python.getInstance()
        val pyObject = python.getModule("main")
        pyObject.callAttr("begin_init")
        enableEdgeToEdge()
        setContent {
            InfantTheme {
//              Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                  Greeting(
//                      name = "" + res,
//                      modifier = Modifier.padding(innerPadding)
//                  )
//              }
                WebViewContainer(this)
            }
        }
    }
    fun showAlert(text: String) {
        AlertDialog.Builder(this@MainActivity)
            .setTitle("提示")
            .setMessage(text)
            .create()
            .show()
    }
    fun openImageChooserActivity(fileChooserParams: FileChooserParams) {
        val i = fileChooserParams.createIntent()
        i.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(i, "选择音频文件"), FILE_CHOOSER_RESULT_CODE)
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadMessage) return
            onActivityResultAboveL(requestCode, resultCode, data)
        }
    }
    private fun onActivityResultAboveL(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessage == null) return
        var results: Array<Uri?>? = null
        if (resultCode == RESULT_OK) {
            if (intent != null) {
                val dataString = intent.dataString
                val clipData = intent.clipData
                if (clipData != null) {
                    results = arrayOfNulls(clipData.itemCount)
                    for (i in 0 until clipData.itemCount) {
                        val item = clipData.getItemAt(i)
                        results[i] = item.uri
                    }
                }
                if (dataString != null) results = arrayOf(Uri.parse(dataString))
            }
        }
        uploadMessage?.onReceiveValue(results)
        uploadMessage = null
    }
}

class MyChromeClient(active: MainActivity) : WebChromeClient() {
    private val activity = active
    companion object {
        val FILE_CHOOSER_RESULT_CODE: Int = 10000
    }
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        return super.onConsoleMessage(consoleMessage);
    }
    override fun onPermissionRequest(request: PermissionRequest?) {
        activity.requestPermissions(
            arrayOf<String>(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            ), 1111
        )
        request?.grant(request.resources);
    }
    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        result?.confirm()
        return true;
    }
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri?>?>?,
        fileChooserParams: FileChooserParams
    ): Boolean {
        activity.uploadMessage = filePathCallback
        activity.openImageChooserActivity(fileChooserParams)
        return true
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(activity: MainActivity) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            val that = this
            class JsInterface() {
                @SuppressLint("JavascriptInterface")
                @JavascriptInterface
                fun showAlert(text: String) {
                    activity.showAlert(text)
                }
                @SuppressLint("JavascriptInterface")
                @JavascriptInterface
                fun calcBase64(base64: String, callback: String) {
//                    activity.showAlert("开始...")
                    Thread {
                        val fileBytes: ByteArray =
                            Base64.decode(base64.replaceFirst("data:audio/wav;base64,", ""), 0)
                        val file = File(context.externalCacheDir, "recorded.wav")
                        val stream = FileOutputStream(file, false)
                        stream.write(fileBytes)
                        stream.flush(); stream.close()
                        val python = Python.getInstance()
                        val pyObject = python.getModule("main")
                        val res: String = pyObject.callAttr("solve", file.absolutePath).toString()
//                        that.loadUrl("javascript:$callback('$res')")
//                        activity.showAlert("结束...")
                        that.post {
                            that.evaluateJavascript(
                                "javascript:$callback('$res')",
                                null
                            )
                        }
                    }.start()
                }

            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            setWebContentsDebuggingEnabled(true);
            webChromeClient = MyChromeClient(activity)
            loadUrl("file:///android_asset/index.html")
            addJavascriptInterface(JsInterface(), "Android")
        }
    })
}