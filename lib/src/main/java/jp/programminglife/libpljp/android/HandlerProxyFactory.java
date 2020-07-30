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

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


/**
 * インターフェイスのメソッドをLooperに関連付けられたスレッドで実行するProxyを作成するファクトリクラス。
 */
@Deprecated
public class HandlerProxyFactory {

    private static final class InvocationHandler_ implements InvocationHandler {

        private final Object delegate;
        private final Handler handler;


        private InvocationHandler_(Looper looper, Object delegate) {
            this.delegate = delegate;
            handler = new Handler(looper);
        }


        @Override
        public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {

            handler.post(new Runnable() {

                @Override
                public void run() {
                    try {
                        method.invoke(delegate, args);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }
            });

            return null;

        }
    }


    /**
     * delegateメソッドの呼び出しを遅延実行するプロキシオブジェクトを作成する。返されるプロキシのメソッドは非同期で実行されるため、値を返すことはできない。
     * また、プロキシメソッドに例外が宣言されていても、例外を返すことはできない。delegateのメソッドが例外をスローするとLooperがRuntimeExceptionを処理することになるので
     * delegateのメソッドはすべての例外を内部的にキャッチして対処する必要がある。
     * @param looper メソッドの実行を行うスレッドに関連付けられたLooper。
     * @param delegate 呼び出したいメソッドの実体を持ったオブジェクト。
     * @param interfaceClass delegateの公開インターフェイス。返されるプロキシはこのインターフェイスを持つ。
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProxy(Looper looper, T delegate, Class<T> interfaceClass) {

        return (T)Proxy.newProxyInstance(HandlerProxyFactory.class.getClassLoader(), new Class<?>[] {interfaceClass}, new InvocationHandler_(looper, delegate));

    }


    private HandlerProxyFactory() {}

}
