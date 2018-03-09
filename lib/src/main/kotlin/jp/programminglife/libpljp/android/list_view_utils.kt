package jp.programminglife.libpljp.android

import android.databinding.InverseBindingListener
import android.databinding.InverseBindingMethod
import android.widget.AbsListView


/** リストビューのチェックされたアイテムを返す。[I]で指定された型のオブジェクトだけを返す。 */
inline fun <reified I> AbsListView.checkedItems(): Sequence<I> {
    return checkedItemPositions?.let {
        it.asSequence()
                .map { getItemAtPosition(it) }
                .filter { it is I }
                .map { it as I }
    } ?: emptySequence()
}

fun AbsListView.clearChecks() {
    checkedItemPositions.asSequence().forEach { setItemChecked(it, false) }
}


// checkedItemPosition の2-wayバインディングに関するアダプター

@android.databinding.BindingAdapter("checkedItemPosition")
fun AbsListView.setCheckedItemPosition(position: Int) {
    if ( checkedItemPosition != position )
        setItemChecked(position, true)
}

@android.databinding.BindingAdapter("checkedItemPositionAttrChanged")
fun AbsListView.setOnAbsListViewItemClickListener(listener: InverseBindingListener?) {
    if ( listener != null ) {
        setOnItemClickListener { _, _, _, _ -> listener.onChange() }
    } else {
        onItemClickListener = null
    }
}

@android.databinding.InverseBindingMethods(
        InverseBindingMethod(type = AbsListView::class, attribute = "checkedItemPosition")
)
class AbsListViewBindingAdapters
