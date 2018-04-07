package jp.programminglife.libpljp.androidbinding

import android.databinding.Observable
import android.databinding.ObservableList


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
