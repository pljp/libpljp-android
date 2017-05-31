package jp.programminglife.libpljp.realm

import io.realm.Realm
import io.realm.RealmModel
import io.realm.log.RealmLog

/**
 * プロパティの値に一致するオブジェクトを一つだけ返す。
 * @return プロパティの値に一致したオブジェクト。複数見つかった場合どのオブジェクトを返すかは不定。見つからなければnull。
 */
fun <T: RealmModel> Realm.get(cls: Class<T>, property: String, value: String): T? {
    return this.where(cls).equalTo(property, value).findFirst()
}


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
