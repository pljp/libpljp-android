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

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * データの保持にSortedMapを使うアダプター。
 * @param <K> 内部に保持するデータのキー。データはこのキーでコンストラクタに渡されたComparatorでソートされる。
 * @param <V> 内部に保持するデータの型。
 */
@Deprecated
abstract public class SortedMapAdapter<K, V> extends BaseAdapter {

    private final TreeMap<K, V> objects;
    private ArrayList<K> index;
    private boolean notifyOnChange = true;


    /**
     * キーを自然順序付けでソートするSortedMapAdapterを作る。
     */
    public SortedMapAdapter() {
        objects = new TreeMap<K, V>();
    }


    /**
     * キーをcomparatorでソートするSortedMapAdapterを作る。
     * @param comparator
     */
    public SortedMapAdapter(Comparator<K> comparator) {
        objects = new TreeMap<K, V>(comparator);
    }


    /**
     * アイテムを追加する。
     * @param item
     * @return 新しいアイテムに置き換えられる前に指定のキーに関連していた値。nullの場合もある。
     * @throws IllegalArgumentException keyがnullのとき。
     * @see java.util.TreeMap#put(Object, Object)
     */
    public final V put(K key, V item) {

        if ( key == null ) {
            throw new IllegalArgumentException("key == null");
        }

        V old = objects.put(key, item);

        boolean modified;
        if ( old == null ) {
            modified = item != null;
        }
        else {
            modified = !old.equals(item);
        }

        if ( modified ) {

            index = null;
            if ( notifyOnChange ) {
                notifyDataSetChanged();
            }

        }

        return old;

    }


    public final boolean containsKey(K key) {
        return objects.containsKey(key);
    }


    /**
     * キーに関連するアイテムを返す。
     * @param key
     * @return キーに関連するアイテム。無ければnull。
     * @throws IllegalArgumentException keyがnullのとき。
     */
    public final V get(K key) {

        if ( key == null ) {
            throw new IllegalArgumentException("key == null");
        }

        return objects.get(key);

    }


    /**
     * キーに関連するアイテムを削除する。
     * @param key
     * @return 削除されたアイテム。キーに関連するアイテムが無かったらnull。
     * @throws IllegalArgumentException keyがnullのとき。
     */
    public final V remove(K key) {

        if ( key == null ) {
            throw new IllegalArgumentException("key == null");
        }

        V removed = objects.remove(key);
        if ( removed != null ) {

            index = null;
            if ( notifyOnChange ) {
                notifyDataSetChanged();
            }

        }
        return removed;

    }


    /**
     * 指定の範囲のアイテムを削除する。
     * @param fromKey 削除する範囲の先頭のキー。範囲にはこのキーも含まれる。
     * @param count 削除するアイテムの最大数。0以下のときは何もせずにリターンする。
     * @throws IllegalArgumentException このマップ自体が制限された範囲を持っており、fromKey がその範囲の境界の外側にある場合
     */
    public final void remove(K fromKey, int count) {

        if ( fromKey == null ) throw new IllegalArgumentException("fromKey == null");
        if ( count <= 0 || objects.size() == 0 ) return;

        SortedMap<K, V> sub = objects.tailMap(fromKey);
        if ( sub.size() == 0 ) return;

        Iterator<Entry<K, V>> it = sub.entrySet().iterator();
        for (int i=0; i<count && it.hasNext(); i++) {
            it.next();
            it.remove();
        }
        index = null;
        if ( notifyOnChange )
            notifyDataSetChanged();

    }


    public final void clear() {

        if ( objects.size() == 0 ) return;

        objects.clear();
        if ( notifyOnChange )
            notifyDataSetChanged();
        index = null;

    }


    @Override
    public final int getCount() {
        return objects.size();
    }


    /**
     * 指定の位置のアイテムを返す。
     * @throws IllegalArgumentException positionが存在しない位置を示す場合。
     */
    @Override
    public final V getItem(int position) {

        return objects.get(getKey(position));

    }


    /**
     * 最初のアイテムを返す。
     * @return 見つかったアイテム。アイテムが無いときはnullを返す。
     */
    public final V getFirstItem() {

        if ( objects.size() == 0 ) return null;
        return objects.get(objects.firstKey());

    }


    /**
     * 最後のアイテムを返す。
     * @return 見つかったアイテム。アイテムが無いときはnullを返す。
     */
    public final V getLastItem() {

        if ( objects.size() == 0 ) return null;
        return objects.get(objects.lastKey());

    }


    /**
     * 指定の位置のキーを返す。
     * @throws IllegalArgumentException positionが存在しない位置を示す場合。
     */
    public final K getKey(int position) {

        if ( position < 0 || position >= objects.size() ) {
            throw new IllegalArgumentException("position=" + position + ", getCount()=" + getCount());
        }

        if ( index == null ) {

            index = new ArrayList<K>(objects.size());
            for (K key : objects.keySet()) {
                index.add(key);
            }

        }

        return index.get(position);

    }


    /**
     * キーの位置を返す。
     * @param key
     * @return キーの位置。キーがデータセットの中になければ-1を返す。
     */
    public final int positionOf(K key) {

        if ( key == null ) throw new IllegalArgumentException("key == null");
        if ( !objects.containsKey(key) ) return -1;

        return objects.headMap(key).size();

    }


    /**
     * このアダプターが持つデータの変更不能なSortedMapを返す。
     * 返されるSortesMapはこのアダプターの内部データにスレッドセーフではない方法でアクセスするので、
     * このアダプターを操作するスレッド以外からこのSortedMapを使用するときは外部的に同期する必要がある。
     * @return
     */
    public final SortedMap<K, V> getData() {
        return Collections.unmodifiableSortedMap(objects);
    }


    /**
     * positionに対応したアイテムIDを返す。
     * このメソッドはデフォルトではpositionを返す。
     * positionとアイテムIDのマッピングを変更するときは {@link #getItemId(int, Object)} をオーバーライドする。
     * @throws IllegalArgumentException positionが存在しない位置を示す場合。
     */
    @Override
    public final long getItemId(int position) {

        if ( position < 0 || position >= objects.size() ) {
            throw new IllegalArgumentException("position=" + position + ", getCount()=" + getCount());
        }
        return getItemId(position, getKey(position));

    }


    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {

        K key = getKey(position);
        V item = objects.get(key);

        return getView(position, key, item, convertView, parent);

    }


    /**
     * {@link #getView(int, View, ViewGroup)}をオーバーライドする代わりにこのメソッドを実装する。
     * @param key アイテムのキー。
     * @param item アイテム。
     * @return
     */
    abstract protected View getView(int position, K key, V item, View convertView, ViewGroup parent);


    /**
     * position, keyに対応するアイテムIDを返す。
     * @param position
     * @param key
     * @return
     */
    protected long getItemId(int position, K key) {
        return position;
    }


    /**
     * trueをセットするとデータセットの変更を行った時に自動的に{@link #notifyDataSetChanged()}を呼ぶ。
     * デフォルトはtrue。このメソッドで設定を変更しない限り、このフラグは変更されない。
     * @param notifyOnChange
     */
    public final void setNotifyOnChange(boolean notifyOnChange) {
        this.notifyOnChange = notifyOnChange;
    }

}
