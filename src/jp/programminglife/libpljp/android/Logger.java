/*
Copyright (c) 2013 ProgrammingLife.jp

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package jp.programminglife.libpljp.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * logcatに出力するためのユーティリティクラス。staticメソッドもインスタンスもスレッドセーフ。
 */
public final class Logger {

    @Nullable
    private static String defaultPrefix;
    private static final Map<String, LogLevel> levelMap = new HashMap<>();
    private static List<String> names;
    private static final ReadWriteLock lock = new ReentrantReadWriteLock(false);

    @NonNull
    private final String tag;
    @NonNull
    private final LogLevel logLevel;


    static {
        levelMap.put("", LogLevel.INFO);
    }


    public static void setPrefix(@Nullable String prefix) {
        defaultPrefix = prefix;
    }


    @Deprecated
    public static void setDebug(boolean debug) {
        setDefaultLogLevel(debug ? LogLevel.DEBUG : LogLevel.INFO);
    }


    public static void setDefaultLogLevel(@NonNull LogLevel l) {
        setLogLevel("", l);
    }


    /**
     * ログレベルを設定する。設定したログレベルはこの名前で始まるすべてのクラスに有効。
     * クラス名に対して文字列の前方一致だけで適用されることに注意。また、設定はより長くマッチする名前が優先される。
     * 例) a.b.Fooの設定は クラスa.b.FooTest にもマッチする。a.b.Fooにはaよりもa.bの方が優先的にマッチする。
     * @param packageOrClassName パッケージ名またはクラスの完全修飾名。
     * @param level 設定するログレベル。
     */
    public static void setLogLevel(@NonNull String packageOrClassName, @NonNull LogLevel level) {

        final Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            levelMap.put(packageOrClassName, level);
            names = null;
        }
        finally {
            writeLock.unlock();
        }

    }


    private static LogLevel getLogLevel(String packageOrClassName) {

        final Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            if ( names == null ) {
                names = new ArrayList<>(levelMap.keySet());
                Collections.sort(names);
                Collections.reverse(names);
            }
        }
        finally {
            writeLock.unlock();
        }

        final Lock readLock = lock.readLock();
        readLock.lock();
        try {
            for (String name : names) {
                if ( packageOrClassName.startsWith(name) )
                    return levelMap.get(name);
            }
            return levelMap.get("");
        }
        finally {
            readLock.unlock();
        }

    }


    public Logger(@NonNull Class<?> cls) {
        this(cls, defaultPrefix);
    }


    /**
     * ロガーのインスタンスを作る。先に出力レベルの設定を行っておくこと。
     * 設定が参照されるのはロガーのインスタンス作成時だけなので注意。
     * @param prefix ログのタグのプレフィックス。prefix + ":" + cls.getSimpleName() がタグになる。nullを指定するとプレフィックスなしになる。
     */
    public Logger(@NonNull Class<?> cls, @Nullable String prefix) {

        logLevel = getLogLevel(cls.getName());
        String className = "";
        while (true) {

            if ( cls.isMemberClass() ) {
                className = addClassName(cls.getSimpleName(), className);
                cls = cls.getDeclaringClass();
            }
            else if ( cls.isAnonymousClass() ) {

                Class<?>[] interfaces = cls.getInterfaces();
                Class<?> superClass = interfaces.length > 0 ? interfaces[0] : cls.getSuperclass();
                className = addClassName("("+superClass.getSimpleName()+")", className);
                cls = cls.getEnclosingClass();
            }
            else {
                className = addClassName(cls.getSimpleName(), className);
                break;
            }
        }

        if ( prefix != null )
            tag = prefix + ":" + className;
        else
            tag = className;
    }


    private String addClassName(String enclosingClassName, String className) {
        return className.length() > 0 ? enclosingClassName + "." + className : enclosingClassName;
    }


    public void wtf(@Nullable String message, Object ... args) {
        Log.wtf(tag, "[" + getMethodName() + "] " + format(message, args));
    }


    /**
     * @deprecated Throwableオブジェクトの出力はセキュリティのため {@link #d(Throwable, String, Object...)}, {@link #v(Throwable, String, Object...)}を使うこと。
     */
    @Deprecated
    public void wtf(@Nullable Throwable t, @Nullable String message, Object ... args) {
        Log.wtf(tag, format(message, args), t);
    }


    public void e(@Nullable String message, Object ... args) {

        if ( logLevel.compareTo(LogLevel.ERROR) >= 0 )
            Log.e(tag, "[" + getMethodName() + "] " + format(message, args));

    }


    /**
     * @deprecated Throwableオブジェクトの出力はセキュリティのため {@link #d(Throwable, String, Object...)}, {@link #v(Throwable, String, Object...)}を使うこと。
     */
    @Deprecated
    public void e(@Nullable Throwable t, @Nullable String message, Object ... args) {

        if ( logLevel.compareTo(LogLevel.ERROR) >= 0 )
            Log.e(tag, format(message, args), t);

    }


    /**
     * @param args String.format()に渡す引数。1つもなければフォーマットせずにそのまま出力する。
     */
    public void w(@Nullable String message, Object ... args) {
        if ( logLevel.compareTo(LogLevel.WARN) >= 0 )
            Log.w(tag, "[" + getMethodName() + "] " + format(message, args));
    }


    /**
     * @param args String.format()に渡す引数。1つもなければフォーマットせずにそのまま出力する。
     */
    public void i(@Nullable String message, Object ... args) {
        if ( logLevel.compareTo(LogLevel.INFO) >= 0 )
            Log.i(tag, "[" + getMethodName() + "] " + format(message, args));
    }


    /**
     * メソッド名をLog.d()で出力する。
     */
    public void d() {

        if ( logLevel.compareTo(LogLevel.DEBUG) >= 0 )
            Log.d(tag, "[" + getMethodName() + "] ");

    }


    /**
     * BuildConfig.DEBUGがtrueならデフォルトのロケールでString.format()でメッセージを整形してLog.d()で出力する。
     * @param args String.format()に渡す引数。1つもなければフォーマットせずにそのまま出力する。
     */
    public void d(@Nullable String message, Object ... args) {

        if ( logLevel.compareTo(LogLevel.DEBUG) >= 0 )
            Log.d(tag, "[" + getMethodName() + "] " + format(message, args));

    }


    public void d(@Nullable Throwable t, @Nullable String message, Object ... args) {

        if ( logLevel.compareTo(LogLevel.DEBUG) >= 0 )
            Log.d(tag, "[" + getMethodName() + "] " + format(message, args), t);

    }


    /**
     * メソッド名をLog.v()で出力する。
     */
    public void v() {

        if ( logLevel == LogLevel.VERBOSE )
            Log.v(tag, "[" + getMethodName() + "] ");

    }


    public void v(@Nullable String message, Object ... args) {

        if ( logLevel == LogLevel.VERBOSE )
            Log.v(tag, "[" + getMethodName() + "] " + format(message, args));

    }


    public void v(@Nullable Throwable t, @Nullable String message, Object ... args) {

        if ( logLevel == LogLevel.VERBOSE )
            Log.v(tag, "[" + getMethodName() + "] " + format(message, args), t);

    }


    private String format(@Nullable String message, Object... args) {

        if ( message == null ) return null;

        if ( message.length() > 0 && args.length > 0 ) {
            message = String.format(Locale.getDefault(), message, args);
        }

        return message;

    }


    private String getMethodName() {

        try {
            Thread cur = Thread.currentThread();
            StackTraceElement[] stackTrace = cur.getStackTrace();
            StackTraceElement element = stackTrace[4];
            String methodName = element.getMethodName();
            try {

                Class<?> cls = Class.forName(element.getClassName());
                if ( cls.isAnonymousClass() ) {

                    if ( cls.getEnclosingConstructor() != null )
                        methodName = "<init>" + ":" + methodName;
                    else {
                        Method em = cls.getEnclosingMethod();
                        if ( em != null )
                            methodName = em.getName() + ":" + methodName;
                    }
                }
            }
            catch (Exception e) {
                // Skip
            }

            return methodName + " (" + cur.getName() + ")";
        }
        catch (Exception ex) {
            return "";
        }

    }


    public enum LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR, WTF;
    }

}
