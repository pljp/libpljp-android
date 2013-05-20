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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * メソッド呼び出しをトランザクションの中で実行するプロキシを作成するファクトリクラス。
 */
public class TransactionProxyFactory {

    /**
     * メソッド呼び出しをトランザクション内で実行するプロキシオブジェクトを作成する。
     * メソッドからスローされた例外はそのままスローし、トランザクションはロールバックする。
     * @param helper
     * @param delegate プロキシを通して呼び出されるオブジェクト。1メソッドが1トランザクションになる。
     * @param interfaceClass プロキシとdelegateの公開インターフェイス。
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProxy(final SQLiteOpenHelper helper, final Object delegate, Class<T> interfaceClass) {

        return (T)Proxy.newProxyInstance(HandlerProxyFactory.class.getClassLoader(), new Class<?>[] {interfaceClass}, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {

                Object ret = null;
                SQLiteDatabase db = helper.getWritableDatabase();
                db.beginTransaction();
                try {

                    ret = method.invoke(delegate, args);
                    db.setTransactionSuccessful();

                }
                catch (InvocationTargetException ex) {
                    throw ex.getCause();
                }
                finally {
                    db.endTransaction();
                }

                return ret;
            }
        });
    }

}
