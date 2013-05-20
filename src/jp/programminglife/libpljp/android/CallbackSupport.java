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

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Looper;

/**
 * 複数のコールバックメソッドの一括呼び出しを簡単にするクラス。
 * @param <T> コールバックオブジェクトのインターフェイス型。
 */
public final class CallbackSupport<T> {

    public static <I> CallbackSupport<I> of(Class<I> interfaceClass) {
        return new CallbackSupport<I>(interfaceClass);
    }


    private final InvocationHandler invocationHandler = new InvocationHandler() {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws RuntimeException {

            invokeAll(method, args);
            return null;

        }

    };


    private final InvocationHandler mainThreadInvocationHandler = new InvocationHandler() {

        @Override
        public Object invoke(Object proxy, final Method method, final Object[] args) throws RuntimeException {

            handler.post(new Runnable() {

                @Override
                public void run() {
                    invokeAll(method, args);
                }

            });

            return null;

        }
    };


    private void invokeAll(Method method, Object[] args) {

        synchronized (lock) {

            errors.clear();
            for (WeakReference<T> ref : callbacks) {
                try {

                    T callback = ref.get();
                    if ( callback != null ) {
                        method.invoke(callback, args);
                    }

                }
                catch (InvocationTargetException ex) {
                    errors.add(ex.getCause());
                }
                catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

    }


    private final Object lock = new Object();
    private final Class<?>[] callbackClass;
    private final ArrayList<WeakReference<T>> callbacks;
    private final ArrayList<Throwable> errors;
    private final Handler handler;


    /**
     * コールバックリストを作る。
     * @param interfaceClass コールバックのインターフェイスクラス。
     */
    @Deprecated
    public CallbackSupport(Class<?> interfaceClass) {

        this.callbackClass = new Class[] {interfaceClass};
        callbacks = new ArrayList<WeakReference<T>>();
        errors = new ArrayList<Throwable>();
        Looper looper = Looper.getMainLooper();
        handler = new Handler(looper);

    }


    /**
     * コールバックを追加する。nullが渡されたときは何もしない。
     * リストには弱参照で登録されるのでこのリストとは別に参照を持っておかないとすぐにコールバックオブジェクトが無くなってしまうので注意。
     * @param callback リストに追加するコールバック。
     */
    public void add(T callback) {

        if ( callback == null ) {
            return;
        }

        synchronized (lock) {

            int index = indexOf(callback);
            if ( index == -1 ) {
                WeakReference<T> ref = new WeakReference<T>(callback);
                    callbacks.add(ref);
            }

            // 参照切れになったエントリを除外する。

            @SuppressWarnings("unchecked")
            WeakReference<T>[] a = callbacks.toArray(new WeakReference[0]);
            for (WeakReference<T> ref : a) {

                T listener = ref.get();
                if ( listener == null ) {
                    callbacks.remove(ref);
                }

            }

        }

    }


    /**
     * コールバックを削除する。nullを渡したときは何もしない。
     * @param callback
     */
    public void remove(T callback) {

        if ( callback == null ) {
            return;
        }

        synchronized (lock) {
            int index = indexOf(callback);
            if ( index > -1 ) {
                callbacks.remove(index);
            }
        }

    }


    /**
     * コールバックのリスト中のインデックスを返す。
     * @param callback 探したいコールバック。
     * @return コールバックのインデックス値。見つからなかった、またはcallbackにnullを指定すると-1。
     */
    private int indexOf(T callback) {

        if ( callback == null ) {
            return -1;
        }

        synchronized (lock) {
            for (int i=0; i<callbacks.size(); i++) {
                if ( callbacks.get(i).get() == callback ) {
                    return i;
                }
            }
        }
        return -1;

    }


    /**
     * 直前のプロキシメソッド呼び出しでコールバックメソッドがスローした例外のリストを返す。
     * 次のプロキシメソッドの呼び出しでこれまでの例外リストはクリアされる。
     * @return
     */
    public List<Throwable> getExceptions() {
        synchronized (lock) {
            return new ArrayList<Throwable>(errors);
        }
    }


    /**
     * コールバックのメソッドを一括で呼び出すためのプロキシを返す。
     * このプロキシのメソッドを呼ぶとリストに登録されたすべてのコールバックオブジェクトのメソッドを呼び出すことができる。
     * ただし、メソッドのリターンを受け取ることはできず、プロキシメソッドは常にnullを返す。
     * 個々のコールバックメソッドが返した例外はgetErrors()メソッドで取得できる。
     * @return
     */
    @SuppressWarnings("unchecked")
    public T getProxy() {
        return (T) Proxy.newProxyInstance(CallbackSupport.class.getClassLoader(), callbackClass, invocationHandler);
    }


    /**
     * コールバックのメソッドをメインスレッドで一括で呼び出すためのプロキシを返す。
     * プロキシメソッドを呼ぶと、Handlerを使ってメインスレッドのLooperにpostする。
     * postされた1つのタスクで登録されたコールバックオブジェクトすべてを呼び出す。
     * @return
     */
    @SuppressWarnings("unchecked")
    public T getMainThreadProxy() {
        return (T) Proxy.newProxyInstance(CallbackSupport.class.getClassLoader(), callbackClass, mainThreadInvocationHandler);
    }


}
