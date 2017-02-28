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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * インターフェイスのメソッドを非同期やトランザクションで実行するProxyを作成するファクトリクラス。
 * WorkerProxyFactoryとは下記の点で異なる。
 * <dl>
 * <li>getProxyメソッドのExecutorServiceにnullを渡したときにデフォルトのExecutorが使用される。
 * <li>アノテーションをインターフェイスに付ける。
 * </dl>
 */
public class WorkerProxyFactory2 {

    private static final ExecutorService SERIAL_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * メソッドをトランザクション内で実行することを指定するアノテーション。
     */
    @Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
    public @interface Transactional {}

    /** メソッドを非同期で実行することを指定するアノテーション。これを付けたメソッドは常にnullを返す。 */
    @Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
    public @interface Async {}


    public static final class AbortedException extends Exception {

        private static final long serialVersionUID = 1L;

        public AbortedException(Throwable throwable) {
            super(throwable);
        }

    }


    public interface WorkerProxyListener {
        /** Asyncメソッドで例外がスローされた時に呼び出されるハンドラーメソッド。
         * @param t スローされた例外。
         * @param delegate メソッドが実行されたオブジェクト。
         * @param m 実行されたメソッド。
         * @param args メソッドの引数。
         */
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

            //Method delegateMethod = delegate.getClass().getMethod(method.getName(), method.getParameterTypes());
            Transactional transaction = method.getAnnotation(Transactional.class);
            final Async async = method.getAnnotation(Async.class);

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

                            ret = invoke(delegate, method, args, async != null);
                            db.setTransactionSuccessful();

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
                    public Object call() throws Exception {

                        return invoke(delegate, method, args, async != null);

                    }
                };

            }

            // 非同期ならExecutorで実行。
            if ( async != null ) {

                Future<Object> future = pool.submit(task);
                if ( method.getReturnType() == Future.class )
                    return future;
                return null;

            }

            // 同期なら直接呼び出す。
            else {
                return task.call();
            }

        }


        private Object invoke(Object delegate, Method method, Object[] args, boolean async) throws Exception {

            if ( !async ) {

                try {
                    return method.invoke(delegate, args);
                }

                // 同期メソッドの場合は例外をスローする

                catch (InvocationTargetException ex) {
                    if ( ex.getCause() instanceof Exception )
                        throw (Exception)ex.getCause();
                    throw new RuntimeException(ex);
                }
                catch (Exception ex) {
                    throw new RuntimeException(ex);
                }

            }

            Throwable t = null;
            try {

                return method.invoke(delegate, args);

            }

            // 非同期メソッドの場合は例外をリスナーに通知する

            catch (InvocationTargetException ex) {
                t = ex.getCause();
            }
            catch (Exception ex) {
                t = ex;
            }

            if ( t != null && listener != null )
                listener.thrown(t, delegate, method, args);
            return null;
        }

    }


    /**
     * <p>delegateのメソッドの呼び出しにアノテーションで機能を付加できるプロキシオブジェクトを作成する。アノテーションは実装ではなくインターフェイスに付ける。</p>
     *
     * <p>Asyncアノテーションが付いたメソッドは threadPool の submit(Callable<T>) で非同期に実行される。
     * 非同期メソッドの実行結果を受け取りたいときは、非同期メソッドの戻り型を Future にする。
     * 例外を受け取りたい場合は WorkerProxyListener で受け取ることができる。</p>
     *
     * <p>同期メソッドは呼び出し元のスレッドで実行される。</p>
     *
     * <p>Transactionが付いたメソッドはhelperから書き込み可能なSQLiteDatabaseオブジェクトでトランザクションを開始する。
     * メソッドが例外をスローした場合はロールバックする。例外が出なければコミットする。</p>
     *
     * @param threadPool Asyncメソッドを実行するスレッドプール。nullを渡すとこのクラスが静的に持っているSERIAL_EXECUTORが使用される。
     * @param helper データベースを開くためのSQLiteOpenHelperインスタンス。nullを渡すとTransactionalアノテーションが付いたメソッドを実行したときにIllegalStateExceptionをスローする。
     * @param delegate 呼び出したいメソッドの実体を持ったオブジェクト。
     * @param interfaceClass delegateの公開インターフェイス。返されるプロキシはこのインターフェイスを持つ。
     * @param listener 非同期メソッドの中で起こった例外を受け取るリスナー。
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <I, D extends I> I getProxy(ExecutorService threadPool, SQLiteOpenHelper helper, D delegate, Class<I> interfaceClass, WorkerProxyListener listener) {

        return (I)Proxy.newProxyInstance(
                WorkerProxyFactory2.class.getClassLoader(),
                new Class<?>[] {interfaceClass},
                new InvocationHandler_(threadPool == null ? SERIAL_EXECUTOR : threadPool, helper, delegate, listener));

    }


    private WorkerProxyFactory2() {}

}
