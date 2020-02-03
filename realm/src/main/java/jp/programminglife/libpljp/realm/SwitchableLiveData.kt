package jp.programminglife.libpljp.realm

import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations

class SwitchableLiveData<T> : LiveData<T>() {

    private val source = MutableLiveData<LiveData<T>>()
    private val switchable: LiveData<T> = Transformations.switchMap(source) { it }
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