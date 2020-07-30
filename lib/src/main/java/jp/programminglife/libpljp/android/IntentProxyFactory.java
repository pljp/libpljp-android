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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * メソッドの呼び出しをIntentに変換するプロキシを作るファクトリクラス。
 * getProxyメソッドでプロキシを作成する。このプロキシのメソッドを呼ぶとプロキシメソッドの名前と引数の情報がIntentに記録され、
 * CallbackのsendメソッドでそのIntentを送信する。
 * メソッド呼び出しの情報が記録されたIntentをexecuteメソッドに渡すことでメソッドの呼び出しを実施できる。
 * 作成されたIntentにはプロキシオブジェクトのクラス情報は含まれていないため、プロキシの型とexecuteメソッドに渡されたObjectの型に互換性があるかどうかは
 * このクラスのクライアント側で確認する必要がある。
 */
@Deprecated
public final class IntentProxyFactory {

    private static final String EXTRA_METHOD =                    "jp.programminglife.libpljp.android.method";
    private static final String EXTRA_METHOD_PARAMETER_TYPES =    "jp.programminglife.libpljp.android.method_parameter_types";
    private static final String EXTRA_METHOD_PARAMETERS =         "jp.programminglife.libpljp.android.method_parameters";
    private static final String EXTRA_INTERFACE =                 "jp.programminglife.libpljp.android.interface";


    public interface Callback {

        public Intent newIntent();
        public void send(Intent intent);

    }


    private static final class InvocationHandler_ implements InvocationHandler {

        private static final ArrayList<Object> EMPTY_OBJECT_LIST = new ArrayList<Object>(0);
        //        private final Logger log = new Logger(IntentProxyFactory.class);
        private final Callback callback;
        private final String interfaceName;


        private InvocationHandler_(Callback callback, Class<?> interfaceClass) {
            this.callback = callback;
            this.interfaceName = interfaceClass.getName();
        }


        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if ( args != null)
                for (Object a : args)
                    if (!(a instanceof Serializable))
                        throw new IllegalArgumentException("argsにSerializableではないオブジェクトがある");

            Intent intent = callback.newIntent();
            intent.putExtra(EXTRA_INTERFACE, interfaceName);
            intent.putExtra(EXTRA_METHOD, method.getName());

            ArrayList<Class<?>> ptypes = new ArrayList<Class<?>>(Arrays.asList(method.getParameterTypes()));
            intent.putExtra(EXTRA_METHOD_PARAMETER_TYPES, ptypes);

            ArrayList<Object> params = args != null ? new ArrayList<Object>(Arrays.asList(args)) : EMPTY_OBJECT_LIST;
            intent.putExtra(EXTRA_METHOD_PARAMETERS, params);

            callback.send(intent);
            return null;

        }
    }


    /**
     * メソッド呼び出しをIntentにパックして送信するプロキシを作成する。
     * @param callback
     * @param interfaceClass
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProxy(Callback callback, Class<T> interfaceClass) {
        return (T)Proxy.newProxyInstance(HandlerProxyFactory.class.getClassLoader(), new Class<?>[] {interfaceClass}, new InvocationHandler_(callback, interfaceClass));
    }


    /**
     * ServiceにコマンドIntentを送るプロキシを作成する。
     * @param context
     * @param serviceClass
     * @param interfaceClass
     * @return
     */
    public static <T> T getServiceProxy(final Context context, final Class<? extends Service> serviceClass, Class<T> interfaceClass) {

        return getProxy(new Callback() {

            @Override
            public Intent newIntent() {
                return new Intent(context, serviceClass);
            }

            @Override
            public void send(Intent intent) {
                context.startService(intent);
            }

        }, interfaceClass);

    }


    public static String getInteraceName(Intent intent) {

        Bundle ex = intent.getExtras();
        if ( ex == null ) {
            throw new IllegalArgumentException("extra == null");
        }

        return ex.getString(EXTRA_INTERFACE);

    }


    /**
     * Intentに記録されたメソッドの呼び出しを実行する。このメソッドはサービス側で実行する。
     * @param obj メソッドを呼び出す対象のオブジェクト。
     * @param intent プロキシメソッドの呼び出しによって作られたIntent。
     * @return 実行したメソッドの戻り値。
     * @throws IllegalArgumentException objがnull、intentのextraがnull、必要なextraの値がnull、
     *         extraに含まれるメソッドパラメーターの数がメソッドの定義と合わない、
     *         メソッドパラメーターの型がメソッドの定義と互換性が無い、メソッドが見つからない、
     *         メソッドにアクセスできない場合。
     * @throws InvocationTargetException 呼びたしたメソッドが例外をスローした。
     */
    public static Object execute(Object obj, Intent intent) throws InvocationTargetException {

        if ( obj == null ) {
            throw new IllegalArgumentException("obj == null");
        }

        Bundle ex = intent.getExtras();
        if ( ex == null ) {
            throw new IllegalArgumentException("extra == null");
        }

        String interfaceName = ex.getString(EXTRA_INTERFACE);
        String methodName = ex.getString(EXTRA_METHOD);
        @SuppressWarnings("unchecked")
        ArrayList<Class<?>> ptypes = (ArrayList<Class<?>>)ex.getSerializable(EXTRA_METHOD_PARAMETER_TYPES);
        @SuppressWarnings("unchecked")
        ArrayList<Object> params = (ArrayList<Object>) ex.getSerializable(EXTRA_METHOD_PARAMETERS);

        if ( interfaceName == null || methodName == null || ptypes == null || params == null ) {
            throw new IllegalArgumentException("extraの値にnullがある");
        }

        try {
            Class<?> interfaceClass = Class.forName(interfaceName);
            Method method = interfaceClass.getMethod(methodName, ptypes.toArray(new Class[0]));
            return method.invoke(obj, params.toArray(new Object[0]));
        }
        catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
        catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
        catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }

    }


    public static String toString(Intent intent) {

        StringBuilder s = new StringBuilder();

        Bundle ex = intent.getExtras();
        if ( ex == null ) {
            return "extra == null";
        }

        String methodName = ex.getString(EXTRA_METHOD);
        @SuppressWarnings("unchecked")
        ArrayList<Class<?>> ptypes = (ArrayList<Class<?>>)ex.getSerializable(EXTRA_METHOD_PARAMETER_TYPES);
        @SuppressWarnings("unchecked")
        ArrayList<Object> params = (ArrayList<Object>) ex.getSerializable(EXTRA_METHOD_PARAMETERS);

        if ( methodName == null ) {
            return "methodName == null";
        }
        if ( ptypes == null ) {
            return "ptypes == null";
        }
        if ( params == null ) {
            return "params == null";
        }
        if ( ptypes.size() != params.size() ) {
            return "ptypes.size() != params.size()";
        }

        s.append(methodName);
        s.append('(');
        for (int i=0, n=ptypes.size(); i<n; i++) {

            s.append('(').append(ptypes.get(i).getSimpleName()).append(')').append(params.get(i).toString());
            if ( i < n-1 ) {
                s.append(", ");
            }

        }
        s.append(')');
        return s.toString();

    }


    private IntentProxyFactory() {}

}
