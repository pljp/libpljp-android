package jp.programminglife.libpljp.android


/**
 * オブジェクトプール。
 * このクラスはスレッドセーフではない。
 */
class ObjectPool<T>(private val create: () -> T, private val init: ((T) -> Unit)? = null,
        private val maxPoolSize: Int = 10, private val duplicateCheck: Boolean = true) {

    private val pool = java.util.ArrayDeque<T>()
    private var acquireCount = 0
    private var hitCount = 0
    private var releaseCount = 0


    fun acquire(): T {
        acquireCount++
        return (pool.poll()?.apply { hitCount++ } ?: create())
                .also { init?.invoke(it) }
    }


    @Deprecated("")
    inline fun <R> acquire(block: (T) -> R): R {
        return acquire().let {
            try {
                block.invoke(it)
            }
            finally {
                release(it)
            }
        }
    }


    inline fun <R> use(block: (T) -> R): R {
        return acquire().let {
            try {
                block.invoke(it)
            }
            finally {
                release(it)
            }
        }
    }


    fun release(obj: T) {

        releaseCount++
        if ( pool.size < maxPoolSize && (!duplicateCheck || pool.all { it !== obj }) ) {
            pool.offer(obj)
        }

    }

}