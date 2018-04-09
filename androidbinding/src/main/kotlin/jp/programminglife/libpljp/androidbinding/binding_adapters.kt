package jp.programminglife.libpljp.androidbinding

import android.databinding.InverseBindingListener
import android.databinding.InverseBindingMethod
import android.view.View
import android.widget.AbsListView
import android.widget.Adapter
import android.widget.AdapterView
import jp.programminglife.libpljp.android.Logger


private class adapter_view_utils
private val log = Logger.get(
        adapter_view_utils::class.java)

// AdapterView.selectedItem の2-Wayバインディングに関するアダプター

@android.databinding.BindingAdapter("selectedItem")
fun <A: Adapter> AdapterView<A>.setSelectedItem(item: Any?) {
    val adapter = adapter ?: return
    (0 until adapter.count)
            .firstOrNull { i -> adapter.getItem(i) == item }
            ?.let { log.v("set item %d", it); if ( selectedItemPosition != it ) setSelection(it) }
            ?: let { log.v("set invalid position"); setSelection(AdapterView.INVALID_POSITION) }
}

@android.databinding.BindingAdapter("android:onItemSelected", "android:onNothingSelected", "selectedItemAttrChanged", requireAll = false)
fun <A: Adapter> AdapterView<A>.setOnItemSelectedListener(
        onItemSelectedListener: OnItemSelected?,
        onNothingSelectedListener: OnNothingSelected?,
        inverseListener: InverseBindingListener?) {

    if ( onItemSelectedListener == null && onNothingSelectedListener == null && inverseListener == null)
        this.onItemSelectedListener = null
    else
        this.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>) {
                log.v()
                onNothingSelectedListener?.onNothingSelected(parent)
                inverseListener?.onChange()
            }

            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                log.v("position=%d, view=%s, inverseListener=%s", position, view, inverseListener)
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

@android.databinding.InverseBindingMethods(InverseBindingMethod(type = AdapterView::class, attribute = "selectedItem"))
class AdapterViewBindingAdapters



// ListView.checkedItemPosition の2-wayバインディングに関するアダプター

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
