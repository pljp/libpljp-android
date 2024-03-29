package jp.programminglife.libpljp.android

import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.switchMap

class SwitchableLiveData<T> : LiveData<T>() {

    private val source = MutableLiveData<LiveData<T>>()
    private val switchable: LiveData<T> = source.switchMap { it }
    private val observer = Observer<T> { value = it }

    fun switch(liveData: LiveData<T>) {
        if (Looper.myLooper() == Looper.getMainLooper())
            source.value = liveData
        else
            source.postValue(liveData)
    }


    override fun onActive() {
        switchable.observeForever(observer)
    }


    override fun onInactive() {
        switchable.removeObserver(observer)
    }

}