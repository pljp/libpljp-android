package jp.programminglife.libpljp.android


/**
 * オブジェクトプール。
 * このクラスはスレッドセーフではない。
 */
class ObjectPool<T>(private val create: () -> T, val maxPoolSize: Int = 10, val duplicateCheck: Boolean = true) {

    private val pool = java.util.ArrayDeque<T>()
    private var acquireCount = 0
    private var hitCount = 0
    private var releaseCount = 0


    fun acquire(): T {
        acquireCount++
        return pool.poll()?.apply { hitCount++ } ?: create()
    }


    fun release(obj: T) {

        releaseCount++
        if ( pool.size < maxPoolSize && (!duplicateCheck || pool.all { it !== obj }) ) {
            pool.offer(obj)
        }

    }

}