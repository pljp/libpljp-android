package jp.programminglife.libpljp.android

import android.widget.AbsListView


/** リストビューのチェックされたアイテムを返す。[I]で指定された型のオブジェクトだけを返す。 */
inline fun <reified I : Any> AbsListView.checkedItems(): Sequence<I> {
    return checkedItemPositions?.asSequence()?.mapNotNull { getItemAtPosition(it) as? I }
            ?: emptySequence()
}

fun AbsListView.clearChecks() {
    checkedItemPositions.asSequence().forEach { setItemChecked(it, false) }
}
