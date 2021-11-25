package jp.programminglife.libpljp.android

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.security.SecureRandom
import java.util.*

class UuidGenerator(private val repository: UuidRepository) {
    private val millis1582y10m15d: Long
    private val rnd = SecureRandom()
    private val lock = Any()


    init {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        c.clear()
        c.set(1582, Calendar.OCTOBER, 5)
        millis1582y10m15d = c.timeInMillis
    }


    /**
     * @param time ミリ秒単位の時刻
     * @param nano ミリ秒未満の時刻。ナノ秒単位。0 - 999999の範囲で指定する。
     * 1000000以上の桁と下位2桁は切り捨てられる。
     */
    fun generate(
            time: Long = System.currentTimeMillis(),
            nano: Long = SystemClock.elapsedRealtimeNanos()
    ): UUID {
        return synchronized(lock) {
            var uuid: UUID? = null
            while(uuid == null) {
                uuid = generateInternal(repository, time, nano)
                if (uuid == null) {
                    Thread.sleep(1L)
                }
            }
            uuid
        }
    }


    /**
     * UUIDを生成する。クロックシーケンスがオーバーフローしてこれ以上ユニークなIDの生成ができない場合はnullを返す。
     * この場合、100ナノ秒またはシステムの時間解像度以上経過してからもう一度このメソッドを呼び出すとUUIDの生成に
     * 成功する。
     */
    private fun generateInternal(repository: UuidRepository, time: Long, nano: Long): UUID? {
        val lastNodeId = repository.loadNodeId()
        val lastTimestamp = UuidGenerator.lastTimestamp
        val nodeId = lastNodeId ?: generateNodeId(rnd)
        val timestamp = (time - millis1582y10m15d) * 10000 + ((nano / 100) % 10000)
        val clockSeq = clockSeq ?: ClockSequence(rnd).also { UuidGenerator.clockSeq = it }
        val clockSeqValue = when {
            lastNodeId != nodeId -> clockSeq.randomize(rnd)
            lastTimestamp == null -> clockSeq.randomize(rnd)
            timestamp <= lastTimestamp -> clockSeq.incrementAndGet() ?: return null
            else -> clockSeq.get() ?: return null
        }
        val uuid = makeUUIDVersion1(timestamp, clockSeqValue, nodeId)
        UuidGenerator.lastTimestamp = timestamp
        if (lastNodeId != nodeId) {
            repository.saveNodeId(nodeId)
        }
        return uuid.takeIf { it.size == 2 }?.let { UUID(uuid[0], uuid[1]) }
    }


    fun epochMilli(uuid: UUID): Long = uuid.timestamp() / 10000L + millis1582y10m15d


    companion object {
        @Volatile
        private var clockSeq: ClockSequence? = null

        @Volatile
        private var lastTimestamp: Long? = null

        internal fun getStandardUuidStateStore(context: Context) =
                context.getSharedPreferences(context.applicationInfo.packageName+"_UuidState", Context.MODE_PRIVATE)


        private fun generateNodeId(rnd: SecureRandom) =
                rnd.nextLong() and 0xffffffffffffL or 0x010000000000L


        /**
         * Version1のUUIDを組み立てる。
         * @param uuidTime UUIDのタイムスタンプ(100ns単位)。
         * @param clockSeq 同時刻の場合に区別するためのカウント値。
         * @param node ノードID。
         */
        private fun makeUUIDVersion1(uuidTime: Long, clockSeq: Int, node: Long): LongArray {
            val timeLow = uuidTime and 0xffffffffL
            val timeMid = uuidTime shr 32 and 0xffffL
            val timeHighAndVersion = 0x1000L or (uuidTime shr 48 and 0xfffL)
            val clockSeqAndVariant = 0x8000L or (clockSeq.toLong() and 0x3fffL)

            val msb = timeLow shl 32 or (timeMid shl 16) or timeHighAndVersion
            val lsb = clockSeqAndVariant shl 48 or (node and 0xffffffffffffL)
            return longArrayOf(msb, lsb)
        }

    }


    class ClockSequence(count: Int = 0, private var value: Int) {

        constructor(rnd: SecureRandom): this(0, 0) {
            randomize(rnd)
        }

        var count: Int = count
            private set

        fun get(): Int? {
            return if (count < 0x4000) value else null
        }

        fun incrementAndGet(): Int? {
            return if (count < 0x3fff) {
                count++
                ++value
            }
            else null
        }

        fun randomize(rnd: SecureRandom): Int {
            value = rnd.nextInt(0x4000)
            count = 0
            return value
        }


        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ClockSequence) return false

            if (count != other.count) return false
            if (value != other.value) return false

            return true
        }


        override fun hashCode(): Int {
            var result = count
            result = 31 * result + value
            return result
        }

    }


    interface UuidRepository {
        fun loadNodeId(): Long?
        fun saveNodeId(nodeId: Long?)
    }


    /**
     * 指定の[SharedPreferences]にステートを保存する[UuidRepository]を作成する。
     */
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    class PreferencesUuidRepository(private val preferences: SharedPreferences) : UuidRepository {

        /**
         * UUIDステート専用の[SharedPreferences] ("<app-pkg>_UuidState")にステートを保存する
         * [UuidRepository]を作成する。
         */
        constructor(context: Context) : this(getStandardUuidStateStore(context))


        override fun loadNodeId(): Long? {
            return if (!preferences.contains(NODE_ID_KEY)) {
                null
            } else {
                preferences.getLong(NODE_ID_KEY, 0L)
            }
        }


        override fun saveNodeId(nodeId: Long?) {
            with(preferences.edit()) {
                if (nodeId != null) {
                    putLong(NODE_ID_KEY, nodeId)
                } else {
                    remove(NODE_ID_KEY)
                }
                apply()
            }
        }


        companion object {
            private const val NODE_ID_KEY = "nodeId"
        }

    }
}
