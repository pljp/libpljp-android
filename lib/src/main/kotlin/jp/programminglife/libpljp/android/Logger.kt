package jp.programminglife.libpljp.android

import android.util.Log
import jp.programminglife.libpljp.android.Logger.LogLevel
import java.io.PrintWriter
import java.io.StringWriter
import java.util.HashMap
import java.util.IdentityHashMap
import java.util.Locale
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * @property loggerLevel この[Logger]が出力できる最低の[LogLevel]。
 */
class Logger private constructor(
        private val loggerLevel: LogLevel,
        private val appender: Appender
) {

//    /**
//     * ロガーのインスタンスを作る。先に出力レベルの設定を行っておくこと。
//     * 設定が参照されるのはロガーのインスタンス作成時だけなので注意。
//     * @deprecated staticメソッドを使う。
//     */
//    @Deprecated
//    public AndroidLogger(@NonNull Class<?> cls) {
//        logLevel = Logger.Companion.getLogLevel(cls.getName());
//        tag = getTag(cls);
//    }


//    /**
//     * ロガーのインスタンスを作る。先に出力レベルの設定を行っておくこと。
//     * 設定が参照されるのはロガーのインスタンス作成時だけなので注意。
//     * @param prefix 無視される
//     */
//    @Deprecated
//    public AndroidLogger(@NonNull Class<?> cls, @Nullable String prefix) {
//        this(cls);
//    }


    fun wtf(message: String, vararg args: Any?) {
        appender.wtf("[" + getMethodName() + "] " + format(message, *args))
    }


    fun e(message: String, vararg args: Any?) {
        if (loggerLevel <= LogLevel.ERROR)
            appender.e("[" + getMethodName() + "] " + format(message, *args))
    }


    fun w(message: String, vararg args: Any?) {
        if (loggerLevel <= LogLevel.WARN)
            appender.w("[" + getMethodName() + "] " + format(message, *args))
    }


    fun i(message: String, vararg args: Any?) {
        if (loggerLevel <= LogLevel.INFO)
            appender.i("[" + getMethodName() + "] " + format(message, *args))
    }


    fun d() {
        if (loggerLevel <= LogLevel.DEBUG)
            appender.d("[" + getMethodName() + "] ")
    }


    fun d(message: String, vararg args: Any?) {
        if (loggerLevel <= LogLevel.DEBUG)
            appender.d("[" + getMethodName() + "] " + format(message, *args))
    }


    fun d(t: Throwable, message: String? = null, vararg args: Any?) {
        if (loggerLevel <= LogLevel.DEBUG)
            appender.d(t, "[" + getMethodName() + "] " + format(message, *args))
    }


    fun v() {
        if (loggerLevel === LogLevel.VERBOSE)
            appender.v("[" + getMethodName() + "] ")
    }


    fun v(message: String?, vararg args: Any?) {
        if (loggerLevel === LogLevel.VERBOSE)
            appender.v("[" + getMethodName() + "] " + format(message, *args))
    }


    fun v(t: Throwable, message: String? = null, vararg args: Any?) {
        if (loggerLevel === LogLevel.VERBOSE)
            appender.v(t, "[" + getMethodName() + "] " + format(message, *args))
    }


    private fun format(message: String?, vararg args: Any?): String? {
        //val message: String = message ?: return null
        return message?.let { m ->
            if (m.isNotEmpty() && args.isNotEmpty()) {
                String.format(Locale.getDefault(), m, *args)
            }
            else m
        }
    }


    private fun getMethodName(): String {
        try {
            val cur = Thread.currentThread()
            val stackTrace = cur.stackTrace
            val element = stackTrace[4]
            var methodName = element.methodName
            try {
                val cls = Class.forName(element.className)
                if (cls.isAnonymousClass) {

                    if (cls.enclosingConstructor != null)
                        methodName = "<init>:$methodName"
                    else {
                        val em = cls.enclosingMethod
                        if (em != null)
                            methodName = em.name + ":" + methodName
                    }
                }
            } catch (e: Throwable) {
                // Skip
            }

            return methodName + " (" + cur.name + ")"
        } catch (ex: Exception) {
            return ""
        }
    }


    companion object {
        private val instances = IdentityHashMap<Class<*>, Logger>()
        private val levelConfig = HashMap<String, LogLevel>()
        private val lock = ReentrantReadWriteLock(false)
        @Volatile var prefix: String? = null

        init {
            levelConfig[""] = LogLevel.INFO
        }


        fun setDefaultLogLevel(l: LogLevel) {
            setLogLevel("", l)
        }


        fun get(cls: Class<*>): Logger {
            // キャッシュにあればそれを返す

            val cachedLogger = lock.read {
                instances[cls]
            }
            if (cachedLogger != null)
                return cachedLogger

            // 新しいLoggerをキャッシュに入れる

            return lock.write {
                // ロックを得るまでにキャッシュが更新されているかもしれないので、もう一度キャッシュを確認
                instances[cls] ?: let {
                    val logLevel = getLogLevel(cls.name)
                    val appender = AndroidAppender(cls)
                    val logger = Logger(logLevel, appender)
                    //Log.v("Logger", "put: class="+cls.toString());
                    instances[cls] = logger
                    logger
                }
            }
        }


        fun getPrintLogger(cls: Class<*>): Logger {
            return Logger(
                    getLogLevel(cls.name),
                    StdoutAppender()
            )
        }


        /**
         * ログレベルを設定する。設定したログレベルはこの名前で始まるすべてのクラスに有効。
         * クラス名に対して文字列の前方一致だけで適用されることに注意。また、設定はより長くマッチする名前が優先される。
         * 例) a.b.Fooの設定は クラスa.b.FooTest にもマッチする。a.b.Fooにはaよりもa.bの方が優先的にマッチする。
         * @param packageOrClassName パッケージ名またはクラスの完全修飾名。
         * @param level 設定するログレベル。
         */
        fun setLogLevel(packageOrClassName: String, level: LogLevel) {
            lock.write {
                levelConfig[packageOrClassName] = level
            }
        }


        private fun getLogLevel(packageOrClassName: String): LogLevel {
            lock.read {
                return levelConfig.keys
                        .sorted()
                        // ログレベル設定のパッケージ名が長い方からマッチングする
                        .lastOrNull { packageOrClassName.startsWith(it) }
                        ?.let { levelConfig[it] }
                        ?: levelConfig[""] ?: LogLevel.INFO
            }
        }

    }


    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR, WTF
    }


    interface Appender {
        fun wtf(message: String)

        fun e(message: String)

        fun w(message: String)

        fun i(message: String)

        /**
         * BuildConfig.DEBUGがtrueならデフォルトのロケールでString.format()でメッセージを整形してLog.d()で出力する。
         */
        fun d(message: String)

        fun d(t: Throwable, message: String)

        fun v(message: String)

        fun v(t: Throwable, message: String)
    }


    class AndroidAppender(cls: Class<*>) : Appender {

        private val tag = getTag(cls)


        private fun getTag(cls: Class<*>): String {
            return lock.read {

                // タグ文字列を生成

                var className = ""
                var curCls: Class<*>? = cls
                while (true) {

                    if (curCls == null)
                        break
                    else if (curCls.isMemberClass) {
                        className = addClassName(curCls.simpleName, className)
                        curCls = curCls.declaringClass
                    }
                    else if (curCls.isAnonymousClass) {
                        val interfaces = curCls.interfaces
                        val superClass = if (interfaces.isNotEmpty()) interfaces[0] else curCls.superclass
                        if (superClass != null)
                            className = addClassName("(" + superClass.simpleName + ")", className)
                        curCls = curCls.enclosingClass
                    }
                    else {
                        className = addClassName(curCls.simpleName, className)
                        break
                    }
                }

                if (prefix != null)
                    "$prefix:$className"
                else
                    className
            }
        }


        private fun addClassName(enclosingClassName: String, className: String): String {
            return if (className.isNotEmpty()) "$enclosingClassName.$className" else enclosingClassName
        }


        override fun wtf(message: String) {
            Log.wtf(tag, message)
        }

        override fun e(message: String) {
            Log.e(tag, message)
        }

        override fun w(message: String) {
            Log.w(tag, message)
        }

        override fun i(message: String) {
            Log.i(tag, message)
        }

        override fun d(message: String) {
            Log.d(tag, message)
        }

        override fun d(t: Throwable, message: String) {
            Log.d(tag, message, t)
        }

        override fun v(message: String) {
            Log.v(tag, message)
        }

        override fun v(t: Throwable, message: String) {
            Log.v(tag, message, t)
        }
    }


    class StdoutAppender : Appender {
        override fun wtf(message: String) {
            println(message)
        }

        override fun e(message: String) {
            println(message)
        }

        override fun w(message: String) {
            println(message)

        }

        override fun i(message: String) {
            println(message)
        }

        override fun d(message: String) {
            println(message)
        }

        override fun d(t: Throwable, message: String) {
            println(message)
            println(getStackTrace(t))
        }

        override fun v(message: String) {
            println(message)
        }

        override fun v(t: Throwable, message: String) {
            println(message)
            println(getStackTrace(t))
        }

        private fun getStackTrace(t: Throwable): String {
            val stringWriter = StringWriter()
            t.printStackTrace(PrintWriter(stringWriter))
            return stringWriter.toString()
        }
    }

}
