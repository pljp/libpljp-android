package jp.programminglife.libpljp.android.kotlin

import android.databinding.BindingAdapter
import android.databinding.InverseBindingListener
import android.databinding.InverseBindingMethod
import android.databinding.InverseBindingMethods
import android.databinding.Observable
import android.databinding.ObservableList
import android.os.Bundle
import android.util.SparseArray
import android.util.SparseBooleanArray
import android.util.SparseIntArray
import android.view.View
import android.widget.AbsListView
import android.widget.Adapter
import android.widget.AdapterView
import jp.programminglife.libpljp.android.Logger

// Data Binding

fun onPropertyChangedCallback(callback: Observable.(Int) -> Unit) = object: Observable.OnPropertyChangedCallback() {
    override fun onPropertyChanged(p0: Observable, p1: Int) {
        callback.invoke(p0, p1)
    }
}

fun <O: Observable> O.addOnPropertyChangedListener(block: O.() -> Unit) =
        object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable, i: Int) {
                @Suppress("UNCHECKED_CAST")
                block(observable as O)
            }

        }.also { addOnPropertyChangedCallback(it) }


fun <T, L: ObservableList<T>> L.addOnListChangedListener(block: L.() -> Unit) {
    addOnListChangedCallback(object: ObservableList.OnListChangedCallback<L>() {
        override fun onChanged(p0: L) { block() }
        override fun onItemRangeInserted(p0: L, p1: Int, p2: Int) { block() }
        override fun onItemRangeMoved(p0: L, p1: Int, p2: Int, p3: Int) { block() }
        override fun onItemRangeChanged(p0: L, p1: Int, p2: Int) { block() }
        override fun onItemRangeRemoved(p0: L, p1: Int, p2: Int) { block() }
    })
}


// Bundle

inline fun <reified E: Enum<E>> Bundle.getEnum(key: String) = getString(key).let { enumValueOf<E>(it) }
fun <E: Enum<E>> Bundle.putEnum(key:String, value: E) { putString(key, value.name) }


// ListView

/** リストビューのチェックされたアイテムを返す。[I]で指定された型のオブジェクトだけを返す。 */
inline fun <reified I> AbsListView.checkedItems(): Sequence<I> {
    return checkedItemPositions?.let {
        it.asSequence()
                .map { getItemAtPosition(it) }
                .filter { it is I }
                .map { it as I }
    } ?: emptySequence()
}


// SparseArray

inline fun <E> SparseArray<E>.forEach(block: (key: Int, value: E?) -> Unit) {
    (0 until size()).forEach { block(it, get(keyAt(it))) }
}
fun <E> SparseArray<E>.asSequence(): Sequence<Pair<Int, E>> {
    return (0 until size()).asSequence().map { it to get(keyAt(it)) }.filter { it.second != null }
}

inline fun SparseBooleanArray.forEach(block: (key: Int, value: Boolean) -> Unit) {
    (0 until size()).forEach { keyAt(it).let { block(it, get(it)) } }
}
/** [SparseBooleanArray]から、値がtrueであるkeyのシーケンスを返す。 */
fun SparseBooleanArray.asSequence(): Sequence<Int> {
    return (0 until size()).asSequence().map { keyAt(it) }.filter { get(it) }
}

inline fun SparseIntArray.forEach(block: (key: Int, value: Int) -> Unit) {
    (0 until size()).forEach { block(it, get(keyAt(it))) }
}
fun SparseIntArray.asSequence(): Sequence<Pair<Int, Int>> {
    return (0 until size()).asSequence().map { it to get(keyAt(it)) }
}



// AdapterView #selectedItem の2-Wayバインディングに関するアダプター

@BindingAdapter("selectedItem")
fun <A: Adapter> AdapterView<A>.setSelectedItem(item: Any?) {
    val adapter = adapter ?: return
    (0 until adapter.count)
            .firstOrNull { i -> adapter.getItem(i) == item }
            ?.let { Logger.get(javaClass).v("set item %d", it); if ( selectedItemPosition != it ) setSelection(it) }
            ?: let { Logger.get(javaClass).v("set invalid position"); setSelection(AdapterView.INVALID_POSITION) }
}

@BindingAdapter("android:onItemSelected", "android:onNothingSelected", "selectedItemAttrChanged", requireAll = false)
fun <A: Adapter> AdapterView<A>.setOnItemSelectedListener(onItemSelectedListener: OnItemSelected?,
        onNothingSelectedListener: OnNothingSelected?, inverseListener: InverseBindingListener?) {

    if ( onItemSelectedListener == null && onNothingSelectedListener == null && inverseListener == null)
        this.onItemSelectedListener = null
    else
        this.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>) {
                onNothingSelectedListener?.onNothingSelected(parent)
                inverseListener?.onChange()
            }

            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                view?.let { onItemSelectedListener?.onItemSelected(parent, view, position, id) }
                inverseListener?.onChange()
            }
        }
}

interface OnItemSelected {
    fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
}

interface OnNothingSelected {
    fun onNothingSelected(parent: AdapterView<*>)
}

@InverseBindingMethods(InverseBindingMethod(type = AdapterView::class, attribute = "selectedItem"))
class AdapterViewBindingAdapters



// AbsListView #checkedItemPosition の2-wayバインディングに関するアダプター

@BindingAdapter("checkedItemPosition")
fun AbsListView.setCheckedItemPosition(position: Int) {
    if ( checkedItemPosition != position )
        setItemChecked(position, true)
}


@BindingAdapter("checkedItemPositionAttrChanged")
fun AbsListView.setOnAbsListViewItemClickListener(listener: InverseBindingListener?) {
    if ( listener != null ) {
        setOnItemClickListener { _, _, _, _ -> listener.onChange() }
    } else {
        onItemClickListener = null
    }
}

@InverseBindingMethods(
        InverseBindingMethod(type = AbsListView::class, attribute = "checkedItemPosition")
)
class AbsListViewBindingAdapters
