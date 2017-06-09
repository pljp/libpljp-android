package jp.programminglife.libpljp.android

import android.widget.AdapterView


// selectedItem の2-Wayバインディングに関するアダプター

@android.databinding.BindingAdapter("selectedItem")
fun <A: android.widget.Adapter> android.widget.AdapterView<A>.setSelectedItem(item: Any?) {
    val adapter = adapter ?: return
    (0 until adapter.count)
            .firstOrNull { i -> adapter.getItem(i) == item }
            ?.let { jp.programminglife.libpljp.android.Logger.get(javaClass).v("set item %d", it); if ( selectedItemPosition != it ) setSelection(it) }
            ?: let { jp.programminglife.libpljp.android.Logger.get(javaClass).v("set invalid position"); setSelection(
            android.widget.AdapterView.INVALID_POSITION) }
}

@android.databinding.BindingAdapter("android:onItemSelected", "android:onNothingSelected", "selectedItemAttrChanged", requireAll = false)
fun <A: android.widget.Adapter> android.widget.AdapterView<A>.setOnItemSelectedListener(onItemSelectedListener: jp.programminglife.libpljp.android.OnItemSelected?,
        onNothingSelectedListener: jp.programminglife.libpljp.android.OnNothingSelected?, inverseListener: android.databinding.InverseBindingListener?) {

    if ( onItemSelectedListener == null && onNothingSelectedListener == null && inverseListener == null)
        this.onItemSelectedListener = null
    else
        this.onItemSelectedListener = object: android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                onNothingSelectedListener?.onNothingSelected(parent)
                inverseListener?.onChange()
            }

            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                view?.let { onItemSelectedListener?.onItemSelected(parent, view, position, id) }
                inverseListener?.onChange()
            }
        }
}

interface OnItemSelected {
    fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View, position: Int, id: Long)
}

interface OnNothingSelected {
    fun onNothingSelected(parent: android.widget.AdapterView<*>)
}

@android.databinding.InverseBindingMethods(
        android.databinding.InverseBindingMethod(type = AdapterView::class, attribute = "selectedItem"))
class AdapterViewBindingAdapters
