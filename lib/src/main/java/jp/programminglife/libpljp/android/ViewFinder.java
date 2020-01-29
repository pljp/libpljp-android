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

import android.app.Activity;
import android.view.View;


/**
 * Viewの検索を簡略化するユーティリティクラス。
 * インスタンスはスレッドセーフではない。
 */
@Deprecated
abstract public class ViewFinder {

    /**
     * ViewFinderの新しいインスタンスを返す。
     * @param parent
     * @return
     * @throws IllegalArgumentException 引数がnull。
     */
    public static ViewFinder of(View parent) {

        if ( parent == null ) {
            throw new IllegalArgumentException();
        }
        return new ViewFinderImpl(parent);

    }


    public static ViewFinder of(Activity activity) {

        if ( activity == null ) {
            throw new IllegalArgumentException();
        }
        return new ActivityViewFinder(activity);

    }


    /**
     * parentビューからidで指定されたビューを探す。
     * @param parent
     * @param id
     * @return
     * @throws IllegalArgumentException parent引数がnull。
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T find(View parent, int id) {

        if ( parent == null ) {
            throw new IllegalArgumentException();
        }
        return (T)parent.findViewById(id);

    }


    @SuppressWarnings("unchecked")
    public static <T extends View> T find(Activity activity, int id) {

        if ( activity == null ) {
            throw new IllegalArgumentException();
        }
        return (T)activity.findViewById(id);

    }


    abstract public <T extends View> T find(int id);


    private static final class ViewFinderImpl extends ViewFinder {

        private final View parent;

        private ViewFinderImpl(View parent) {
            this.parent = parent;
        }


        @Override
        public <T extends View> T find(int id) {
            return find(parent, id);
        }

    }


    private static final class ActivityViewFinder extends ViewFinder {

        private final Activity activity;

        private ActivityViewFinder(Activity activity) {
            this.activity = activity;
        }

        @Override
        public <T extends View> T find(int id) {
            return find(activity, id);
        }

    }

}
