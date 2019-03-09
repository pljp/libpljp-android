package jp.programminglife.libpljp.realm

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

class SwitchableLiveData<T> : LiveData<T>() {

    private var source: LiveData<T>? = null
    private val observer = Observer<T> { this@SwitchableLiveData.value = it }


    fun switch(liveData: LiveData<T>?) {
        source?.removeObserver(observer)
        source = liveData
        liveData?.observeForever(observer)
    }


    override fun onActive() {
        source?.observeForever(observer)
    }


    override fun onInactive() {
        source?.removeObserver(observer)
    }

//    private val source = MutableLiveData<LiveData<T>>()
//    private val switchable: LiveData<T> = Transformations.switchMap(source) { it }
//    private val observer = Observer<T> { value = it }
//
//    fun switch(liveData: LiveData<T>) {
//        source.value = liveData
//    }
//
//
//    override fun onActive() {
//        switchable.observeForever(observer)
//    }
//
//
//    override fun onInactive() {
//        switchable.removeObserver(observer)
//    }

}