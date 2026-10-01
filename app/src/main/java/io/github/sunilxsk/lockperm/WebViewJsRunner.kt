package io.github.sunilxsk.lockperm

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast













internal object WebViewJsRunner {

    private val mainHandler = Handler(Looper.getMainLooper())

    
    fun run(activity: Activity) {
        mainHandler.post {
            val list = runCatching { findWebViews(activity) }.getOrDefault(emptyList())
            when {
                list.isEmpty() -> toast(activity, "当前界面没有找到 WebView")
                list.size == 1 -> showJsInput(activity, list[0], describe(activity, list[0], 0))
                else -> showPicker(activity, list)
            }
        }
    }

    

    
    fun findWebViews(activity: Activity): List<WebView> {
        val out = LinkedHashSet<WebView>()
        val decor = runCatching { activity.window.decorView as? ViewGroup }.getOrNull()
        if (decor != null) traverse(decor, out)
        
        runCatching { scanDialogs(activity, out) }
        return out.toList()
    }

    private fun traverse(v: View, out: LinkedHashSet<WebView>) {
        if (v is WebView) out.add(v)
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                runCatching { traverse(v.getChildAt(i), out) }
            }
        }
    }

    private fun scanDialogs(activity: Activity, out: LinkedHashSet<WebView>) {
        
        val wm = activity.getSystemService(Activity.WINDOW_SERVICE) as? WindowManager
            ?: return
        
        val inst = runCatching {
            val g = Class.forName("android.view.WindowManagerGlobal")
            val gi = g.getDeclaredMethod("getInstance")
            gi.isAccessible = true
            gi.invoke(null)
        }.getOrNull() ?: return
        val views = runCatching {
            val f = inst.javaClass.getDeclaredField("mViews")
            f.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            f.get(inst) as? ArrayList<View>
        }.getOrNull() ?: return
        views.forEach { v -> runCatching { traverse(v, out) } }
    }

    
    private fun describe(activity: Activity, wv: WebView, index: Int): String {
        val sb = StringBuilder()
        sb.append("WebView #").append(index + 1).append('\n')
        runCatching { sb.append("标题：").append(wv.title ?: "(无)").append('\n') }
        runCatching { sb.append("地址：").append(wv.url ?: "(无)").append('\n') }
        runCatching {
            sb.append("大小：").append(wv.width).append(" x ").append(wv.height).append('\n')
        }
        runCatching {
            sb.append("进度：").append(wv.progress).append("   内容高：")
                .append(wv.contentHeight).append('\n')
        }
        runCatching {
            sb.append("可见：").append(if (wv.visibility == View.VISIBLE) "是" else "否")
                .append("   类名：").append(wv.javaClass.simpleName).append('\n')
        }
        return sb.toString().trimEnd()
    }

    

    private fun showPicker(activity: Activity, list: List<WebView>) {
        val d = activity.resources.displayMetrics.density
        val dlg = Dialog(activity)
        dlg.setTitle("选择 WebView（共 ${list.size} 个）")

        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL
        val pad = (16 * d).toInt()
        root.setPadding(pad, pad, pad, pad)

        val scroll = ScrollView(activity)
        val host = LinearLayout(activity)
        host.orientation = LinearLayout.VERTICAL
        scroll.addView(host)

        list.forEachIndexed { i, wv ->
            val card = LinearLayout(activity)
            card.orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            lp.bottomMargin = (10 * d).toInt()
            card.layoutParams = lp

            val bg = GradientDrawable()
            bg.setColor(0x3AFFFFFF.toInt())
            bg.cornerRadius = 8 * d
            card.background = bg
            card.setPadding((10 * d).toInt(), (10 * d).toInt(), (10 * d).toInt(), (10 * d).toInt())

            val tv = TextView(activity)
            tv.text = describe(activity, wv, i)
            tv.setTextColor(Color.WHITE)
            tv.textSize = 12f
            card.addView(tv)

            val btn = Button(activity)
            btn.text = "在这个里执行 JS"
            btn.isAllCaps = false
            btn.textSize = 12f
            btn.setOnClickListener {
                runCatching { dlg.dismiss() }
                showJsInput(activity, wv, describe(activity, wv, i))
            }
            card.addView(btn)

            host.addView(card)
        }

        root.addView(scroll)
        val close = Button(activity)
        close.text = "取消"
        close.isAllCaps = false
        close.setOnClickListener { runCatching { dlg.dismiss() } }
        root.addView(close)

        dlg.setContentView(root)
        showSafely(activity, dlg)
    }

    

    private fun showJsInput(activity: Activity, target: WebView, info: String) {
        val d = activity.resources.displayMetrics.density
        val dlg = Dialog(activity)
        dlg.setTitle("执行 JavaScript")

        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL
        val pad = (16 * d).toInt()
        root.setPadding(pad, pad, pad, pad)

        val infoTv = TextView(activity)
        infoTv.text = info
        infoTv.setTextColor(0xFFBBBBBB.toInt())
        infoTv.textSize = 11f
        infoTv.setPadding(0, 0, 0, (8 * d).toInt())
        root.addView(infoTv)

        val edit = EditText(activity)
        edit.hint = "输入 JavaScript 代码，例如：document.title"
        edit.setHintTextColor(0xFF888888.toInt())
        edit.setTextColor(Color.WHITE)
        edit.textSize = 13f
        edit.isSingleLine = false
        edit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        edit.setBackgroundColor(0x33000000.toInt())
        edit.gravity = Gravity.TOP
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (260 * d).toInt(),
        )
        edit.layoutParams = lp
        root.addView(edit)

        val row = LinearLayout(activity)
        row.orientation = LinearLayout.HORIZONTAL
        val run = Button(activity)
        run.text = "执行"
        run.isAllCaps = false
        run.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        run.setOnClickListener {
            val js = edit.text?.toString() ?: ""
            if (js.isBlank()) {
                toast(activity, "代码是空的")
                return@setOnClickListener
            }
            exec(activity, target, js)
            runCatching { dlg.dismiss() }
        }
        val cancel = Button(activity)
        cancel.text = "取消"
        cancel.isAllCaps = false
        cancel.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        cancel.setOnClickListener { runCatching { dlg.dismiss() } }
        row.addView(run)
        row.addView(cancel)
        root.addView(row)

        dlg.setContentView(root)
        showSafely(activity, dlg)
    }

    

    






    private fun exec(activity: Activity, wv: WebView, js: String) {
        mainHandler.post {
            var done = false
            runCatching {
                wv.evaluateJavascript(js) { value ->
                    done = true
                    val text = if (value == null || value == "null") {
                        "已执行（无返回值）"
                    } else {
                        "JS 返回：$value"
                    }
                    toastLong(activity, text)
                }
            }.onFailure {
                
                val ok = runCatching {
                    wv.loadUrl("javascript:" + Uri.encode(js))
                    true
                }.getOrDefault(false)
                if (!ok) toast(activity, "执行失败：这个 WebView 不可用")
            }
        }
    }

    
    private fun toastLong(context: Activity, msg: String) {
        val text = if (msg.length > 400) msg.take(400) + " ……（已截断）" else msg
        runCatching { Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
    }

    

    private fun showSafely(activity: Activity, dlg: Dialog) {
        runCatching {
            if (activity.isFinishing || activity.isDestroyed) return
            val bg = android.graphics.drawable.GradientDrawable()
            bg.setColor(0xF21E1E1E.toInt())
            bg.cornerRadius = 12 * activity.resources.displayMetrics.density
            dlg.window?.setBackgroundDrawable(bg)
            dlg.show()
            
            dlg.window?.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.94f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
        }.onFailure {
            toast(activity, "弹窗显示失败：${it.message}")
        }
    }

    private fun toast(context: Activity, msg: String) {
        runCatching { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }
}
