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

import java.util.Locale;


public final class Logger {

    private static String defaultPrefix;
    private static boolean debug = false;
    private final String tag;


    public static void setPrefix(String prefix) {
        defaultPrefix = prefix;
    }


    public static void setDebug(boolean debug) {
        Logger.debug = debug;
    }


    public Logger(Class<?> cls) {
        this(cls, defaultPrefix);
    }


    /**
     *
     * @param cls
     * @param prefix ログのタグのプレフィックス。prefix + "." + cls.getSimpleName() がタグになる。nullを指定するとプレフィックスなしになる。
     */
    public Logger(Class<?> cls, String prefix) {

        if ( prefix != null )
            tag = prefix + "." + cls.getSimpleName();
        else
            tag = cls.getSimpleName();

    }


    public void wtf(String message, Object ... args) {
        Log.wtf(tag, "[" + getMethodName() + "] " + format(message, args));
    }


    public void wtf(Throwable t, String message, Object ... args) {
        Log.wtf(tag, format(message, args), t);
    }


    public void e(String message, Object ... args) {

        Log.e(tag, "[" + getMethodName() + "] " + format(message, args));

    }


    public void e(Throwable t, String message, Object ... args) {

        Log.e(tag, format(message, args), t);

    }


    /**
     * @param message
     * @param args String.format()に渡す引数。1つもなければフォーマットせずにそのまま出力する。
     */
    public void w(String message, Object ... args) {
        Log.w(tag, "[" + getMethodName() + "] " + format(message, args));
    }


    /**
     * @param message
     * @param args String.format()に渡す引数。1つもなければフォーマットせずにそのまま出力する。
     */
    public void i(String message, Object ... args) {
        Log.i(tag, "[" + getMethodName() + "] " + format(message, args));
    }


    /**
     * メソッド名をLog.d()で出力する。
     */
    public void d() {

        if ( !BuildConfig.DEBUG ) return;
        Log.d(tag, "[" + getMethodName() + "] ");

    }


    /**
     * BuildConfig.DEBUGがtrueならデフォルトのロケールでString.format()でメッセージを整形してLog.d()で出力する。
     * @param message
     * @param args String.format()に渡す引数。1つもなければフォーマットせずにそのまま出力する。
     */
    public void d(String message, Object ... args) {

        if ( !debug ) return;
        Log.d(tag, "[" + getMethodName() + "] " + format(message, args));

    }


    /**
     * メソッド名をLog.v()で出力する。
     */
    public void v() {

        if ( !debug ) return;
        Log.v(tag, "[" + getMethodName() + "] ");

    }


    public void v(String message, Object ... args) {

        if ( !debug ) return;
        Log.v(tag, "[" + getMethodName() + "] " + format(message, args));

    }


    private String format(String message, Object... args) {

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
            return stackTrace[4].getMethodName() + " (" + cur.getName() + ")";
        }
        catch (Exception ex) {
            return "";
        }

    }

}
