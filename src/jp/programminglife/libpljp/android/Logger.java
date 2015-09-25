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

import android.util.Log;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Locale;


public final class Logger {

    @Nullable
    private static String defaultPrefix;
    private static boolean debug = false;

    @NonNull
    private final String tag;


    public static void setPrefix(@Nullable String prefix) {
        defaultPrefix = prefix;
    }


    public static void setDebug(boolean debug) {
        Logger.debug = debug;
    }


    public Logger(@NonNull Class<?> cls) {
        this(cls, defaultPrefix);
    }


    /**
     * @param prefix ログのタグのプレフィックス。prefix + ":" + cls.getSimpleName() がタグになる。nullを指定するとプレフィックスなしになる。
     */
    public Logger(@NonNull Class<?> cls, @Nullable String prefix) {

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

        Log.e(tag, "[" + getMethodName() + "] " + format(message, args));

    }


    /**
     * @deprecated Throwableオブジェクトの出力はセキュリティのため {@link #d(Throwable, String, Object...)}, {@link #v(Throwable, String, Object...)}を使うこと。
     */
    @Deprecated
    public void e(@Nullable Throwable t, @Nullable String message, Object ... args) {

        Log.e(tag, format(message, args), t);

    }


    /**
     * @param message
     * @param args String.format()に渡す引数。1つもなければフォーマットせずにそのまま出力する。
     */
    public void w(@Nullable String message, Object ... args) {
        Log.w(tag, "[" + getMethodName() + "] " + format(message, args));
    }


    /**
     * @param message
     * @param args String.format()に渡す引数。1つもなければフォーマットせずにそのまま出力する。
     */
    public void i(@Nullable String message, Object ... args) {
        Log.i(tag, "[" + getMethodName() + "] " + format(message, args));
    }


    /**
     * メソッド名をLog.d()で出力する。
     */
    public void d() {

        if ( !debug ) return;
        Log.d(tag, "[" + getMethodName() + "] ");

    }


    /**
     * BuildConfig.DEBUGがtrueならデフォルトのロケールでString.format()でメッセージを整形してLog.d()で出力する。
     * @param message
     * @param args String.format()に渡す引数。1つもなければフォーマットせずにそのまま出力する。
     */
    public void d(@Nullable String message, Object ... args) {

        if ( !debug ) return;
        Log.d(tag, "[" + getMethodName() + "] " + format(message, args));

    }


    public void d(@Nullable Throwable t, @Nullable String message, Object ... args) {

        if ( !debug ) return;
        Log.d(tag, "[" + getMethodName() + "] " + format(message, args), t);

    }


    /**
     * メソッド名をLog.v()で出力する。
     */
    public void v() {

        if ( !debug ) return;
        Log.v(tag, "[" + getMethodName() + "] ");

    }


    public void v(@Nullable String message, Object ... args) {

        if ( !debug ) return;
        Log.v(tag, "[" + getMethodName() + "] " + format(message, args));

    }


    public void v(@Nullable Throwable t, @Nullable String message, Object ... args) {

        if ( !debug ) return;
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

}
