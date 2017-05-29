package jp.programminglife.rxandroid;


import android.database.Cursor;

import jp.programminglife.libpljp.android.Logger;
import rx.Observer;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.SyncOnSubscribe;

public final class CursorOnSubscribe<T> extends SyncOnSubscribe<Cursor, T> {

    private final Logger log = Logger.get(CursorOnSubscribe.class);
    private final Func0<Cursor> queryRunner;
    private final Func1<Cursor, T> cursorReader;


    /**
     * Cursorから値を読んで出力するOnSubscriberオブジェクトを作成する。
     * @param queryRunner クエリーを実行してCursorを返す関数。
     * @param cursorReader Cursorの値を読んで返す関数。引数のCursorはmoveToNext()してあるので現在位置から読み込むこと。
     * @param <T> 出力する値の型。
     * @return OnSubscribeインスタンス。
     */
    public static <T> CursorOnSubscribe<T> create(Func0<Cursor> queryRunner, Func1<Cursor, T> cursorReader) {
        return new CursorOnSubscribe<>(queryRunner, cursorReader);
    }


    private CursorOnSubscribe(Func0<Cursor> queryRunner, Func1<Cursor, T> cursorReader) {
        this.queryRunner = queryRunner;
        this.cursorReader = cursorReader;
    }


    @Override
    protected Cursor generateState() {
        return queryRunner.call();
    }


    @Override
    protected Cursor next(Cursor cursor, Observer<? super T> observer) {

        if ( cursor.moveToNext() )
            observer.onNext(cursorReader.call(cursor));
        else
            observer.onCompleted();
        return cursor;

    }


    @Override
    protected void onUnsubscribe(Cursor cursor) {
        cursor.close();
    }

}
