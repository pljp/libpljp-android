package jp.programminglife.libpljp.realm

import android.os.Looper
import androidx.lifecycle.LiveData
import io.realm.Case
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.kotlin.where
import io.realm.log.RealmLog
import java.util.Date

/**
 * プロパティの値に一致するオブジェクトを一つだけ返す。
 * @return プロパティの値に一致したオブジェクト。複数見つかった場合どのオブジェクトを返すかは不定。見つからなければnull。
 */
fun <T: RealmModel> Realm.get(cls: Class<T>, property: String, value: String?): T? =
        where(cls).equalTo(property, value).findFirst()

inline fun <reified T: RealmModel> Realm.get(property: String, value: String?): T? =
        where<T>().equalTo(property, value).findFirst()
inline fun <reified T: RealmModel> Realm.get(property: String, value: String?, casing: Case): T? =
        where<T>().equalTo(property, value, casing).findFirst()
inline fun <reified T: RealmModel> Realm.get(property: String, value: Byte?): T? =
        where<T>().equalTo(property, value).findFirst()
inline fun <reified T: RealmModel> Realm.get(property: String, value: ByteArray?): T? =
        where<T>().equalTo(property, value).findFirst()
inline fun <reified T: RealmModel> Realm.get(property: String, value: Short?): T? =
        where<T>().equalTo(property, value).findFirst()
inline fun <reified T: RealmModel> Realm.get(property: String, value: Int?): T? =
        where<T>().equalTo(property, value).findFirst()
inline fun <reified T: RealmModel> Realm.get(property: String, value: Long?): T? =
        where<T>().equalTo(property, value).findFirst()
inline fun <reified T: RealmModel> Realm.get(property: String, value: Double?): T? =
        where<T>().equalTo(property, value).findFirst()
inline fun <reified T: RealmModel> Realm.get(property: String, value: Float?): T? =
        where<T>().equalTo(property, value).findFirst()
inline fun <reified T: RealmModel> Realm.get(property: String, value: Boolean?): T? =
        where<T>().equalTo(property, value).findFirst()
inline fun <reified T: RealmModel> Realm.get(property: String, value: Date?): T? =
        where<T>().equalTo(property, value).findFirst()


/**
 * デフォルトのRealmインスタンスを取得して[block]を実行する。
 */
inline fun <R> realm(block: Realm.() -> R) = Realm.getDefaultInstance().use { block(it) }


@Throws(Throwable::class)
inline fun <R> Realm.tx(transaction: Realm.() -> R): R {

    beginTransaction()
    try {
        val ret = transaction(this)
        commitTransaction()
        return ret
    } catch (e: Throwable) {
        if (isInTransaction) {
            cancelTransaction()
        } else {
            RealmLog.warn("Could not cancel transaction, not currently in a transaction.")
        }
        throw e
    }

}

@Deprecated("asLiveDataを使う。", replaceWith = ReplaceWith("asLiveData()", "jp.programminglife.libpljp.realm.asLiveData"))
fun <T> RealmResults<T>.toLiveData() = asLiveData()

fun <T> RealmResults<T>.asLiveData() = object : androidx.lifecycle.LiveData<List<T>>() {

    private val changeListener = RealmChangeListener<RealmResults<T>> {
        if (Looper.myLooper() == Looper.getMainLooper())
            value = it
        else
            postValue(it)
    }

    init {
        value = this@asLiveData
    }

    override fun onActive() {
        addChangeListener(changeListener)
    }

    override fun onInactive() {
        removeChangeListener(changeListener)
    }
}


@Deprecated("asSingleLiveDataを使う。", replaceWith = ReplaceWith("asSingleLiveData()", "jp.programminglife.libpljp.realm.asSingleLiveData"))
fun <T> RealmResults<T>.toSingleLiveData() = asLiveData()

/**
 * [RealmResults]から最初の1件だけを取り出して[LiveData]でラップする。
 * 検索結果が0件になったときは[androidx.lifecycle.Observer]にnullが通知される。
 */
fun <T> RealmResults<T>.asSingleLiveData() = object : androidx.lifecycle.LiveData<T>() {

    private val changeListener = RealmChangeListener<RealmResults<T>> {
        val newValue = it.firstOrNull()
        if (Looper.myLooper() == Looper.getMainLooper())
            value = newValue
        else
            postValue(newValue)
    }

    init {
        value = this@asSingleLiveData.firstOrNull()
    }

    override fun onActive() {
        addChangeListener(changeListener)
    }

    override fun onInactive() {
        removeChangeListener(changeListener)
    }
}

@Deprecated("asLiveDataを使う", replaceWith = ReplaceWith("asLiveData()", "jp.programminglife.libpljp.realm.asLiveData"))
fun <T: RealmObject> T.toLiveData() = asLiveData()


/**
 * [RealmObject]を[LiveData]でラップする。
 * [RealmObject]が無効になった時は[androidx.lifecycle.Observer]にnull値を通知する。
 */
fun <T: RealmObject> T.asLiveData() = object : LiveData<T>() {

    private val changeListener = RealmChangeListener<T> {
        val newValue = it.takeIf { it.isValid }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            value = newValue
        }
        else {
            postValue(newValue)
        }
    }

    init {
        value = this@asLiveData.takeIf { it.isValid }
    }

    override fun onActive() {
        addChangeListener(changeListener)
    }

    override fun onInactive() {
        removeChangeListener(changeListener)
    }

}
