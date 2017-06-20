package jp.programminglife.libpljp.android


fun onPropertyChangedCallback(callback: android.databinding.Observable.(Int) -> Unit) = object: android.databinding.Observable.OnPropertyChangedCallback() {
    override fun onPropertyChanged(p0: android.databinding.Observable, p1: Int) {
        callback.invoke(p0, p1)
    }
}

fun <O: android.databinding.Observable> O.addOnPropertyChangedListener(block: O.() -> Unit) =
        object : android.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: android.databinding.Observable, i: Int) {
                @Suppress("UNCHECKED_CAST")
                block(observable as O)
            }

        }.also { addOnPropertyChangedCallback(it) }


fun <T, L: android.databinding.ObservableList<T>> L.addOnListChangedListener(block: L.() -> Unit) {
    addOnListChangedCallback(object: android.databinding.ObservableList.OnListChangedCallback<L>() {
        override fun onChanged(p0: L) { block() }
        override fun onItemRangeInserted(p0: L, p1: Int, p2: Int) { block() }
        override fun onItemRangeMoved(p0: L, p1: Int, p2: Int, p3: Int) { block() }
        override fun onItemRangeChanged(p0: L, p1: Int, p2: Int) { block() }
        override fun onItemRangeRemoved(p0: L, p1: Int, p2: Int) { block() }
    })
}
