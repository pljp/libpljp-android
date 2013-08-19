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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * インターフェイスのメソッドを非同期やトランザクションで実行するProxyを作成するファクトリクラス。
 */
public class WorkerProxyFactory {


    /**
     * メソッドをトランザクション内で実行することを指定するアノテーション。
     */
    @Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
    public @interface Transaction {}

    /** メソッドを非同期で実行することを指定するアノテーション。これを付けたメソッドは常にnullを返す。 */
    @Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
    public @interface Async {}


    public interface WorkerProxyListener {
        void thrown(Throwable t, Object delegate, Method m, Object[] args);
    }


    private static final class InvocationHandler_ implements InvocationHandler {

        private final Object delegate;
        private final ExecutorService pool;
        private final SQLiteOpenHelper helper;
        private final WorkerProxyListener listener;

        private InvocationHandler_(ExecutorService threadPool, SQLiteOpenHelper helper, Object delegate, WorkerProxyListener listener) {
            this.pool = threadPool;
            this.helper = helper;
            this.delegate = delegate;
            this.listener = listener;
        }


        @Override
        public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {

            Callable<Object> task;

            Method delegateMethod = delegate.getClass().getMethod(method.getName(), method.getParameterTypes());
            Transaction transaction = delegateMethod.getAnnotation(Transaction.class);
            Async async = delegateMethod.getAnnotation(Async.class);

            if ( transaction != null && helper == null )
                throw new IllegalStateException("helper == null");
            if ( async != null && pool == null )
                throw new IllegalStateException("pool == null");

            if ( transaction != null ) {

                task = new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {

                        Object ret = null;
                        SQLiteDatabase db = helper.getWritableDatabase();
                        db.beginTransaction();
                        try {

                            ret = method.invoke(delegate, args);
                            db.setTransactionSuccessful();

                        }
                        catch (InvocationTargetException ex) {
                            Exception ex2 = ex;
                            if ( ex.getCause() instanceof Exception )
                                ex2 = (Exception)ex.getCause();
                            if ( listener != null )
                                listener.thrown(ex2, delegate, method, args);
                            else
                                throw ex2;

                        }
                        finally {
                            db.endTransaction();
                        }

                        return ret;
                    }
                };

            }
            else {

                task = new Callable<Object>() {
                    @Override
                    public Object call() {

                        try {
                            return method.invoke(delegate, args);
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    }
                };

            }

            if ( async != null ) {

                Future<Object> future = pool.submit(task);
                if ( method.getReturnType() == Future.class )
                    return future;
                return null;

            }
            else {
                return task.call();
            }

        }
    }


    @SuppressWarnings("unchecked")
    public static <T> T getProxy(ExecutorService threadPool, SQLiteOpenHelper helper, T delegate, Class<T> interfaceClass) {

        return (T)Proxy.newProxyInstance(WorkerProxyFactory.class.getClassLoader(), new Class<?>[] {interfaceClass}, new InvocationHandler_(threadPool, helper, delegate, null));

    }

    /**
     * <p>delegateのメソッドの呼び出しにアノテーションで機能を付加できるプロキシオブジェクトを作成する。アノテーションはインターフェイスではなく、その実装に付ける。</p>
     * <p>Asyncアノテーションが付いたメソッドはthreadPoolのsubmit(Callable<T>)で非同期に実行される。
     * 非同期メソッドの実行結果を受け取りたいときは、非同期メソッドの戻り型をFutureにする。
     * 例外を受け取りたい場合はWorkerProxyListenerで受け取ることができる。</p>
     * <p>Transactionが付いたメソッドはhelperから書き込み可能なSQLiteDatabaseオブジェクトでトランザクションを開始する。
     * メソッドが例外をスローした場合はロールバックする。例外が出なければコミットする。</p>
     * @param threadPool Asyncメソッドを実行するスレッドプール。nullを渡すとAsyncアノテーションが付いたメソッドを実行したときにIllegalStateExceptionをスローする。
     * @param helper データベースを開くためのSQLiteOpenHelperインスタンス。nullを渡すとTransactionアノテーションが付いたメソッドを実行したときにIllegalStateExceptionをスローする。
     * @param delegate 呼び出したいメソッドの実体を持ったオブジェクト。
     * @param interfaceClass delegateの公開インターフェイス。返されるプロキシはこのインターフェイスを持つ。
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <I, D extends I> I getProxy(ExecutorService threadPool, SQLiteOpenHelper helper, D delegate, Class<I> interfaceClass, WorkerProxyListener listener) {

        return (I)Proxy.newProxyInstance(WorkerProxyFactory.class.getClassLoader(), new Class<?>[] {interfaceClass}, new InvocationHandler_(threadPool, helper, delegate, listener));

    }


    private WorkerProxyFactory() {}

}
