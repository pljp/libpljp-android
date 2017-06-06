package jp.programminglife.libpljp.android.kotlin

import android.databinding.BindingAdapter
import android.databinding.InverseBindingListener
import android.databinding.InverseBindingMethod
import android.databinding.InverseBindingMethods
import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import jp.programminglife.libpljp.android.Logger


// selectedItem の2-Wayバインディングに関するアダプター

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
