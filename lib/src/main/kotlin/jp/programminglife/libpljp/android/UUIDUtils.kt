/*
Copyright (c) 2019 ProgrammingLife.jp

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package jp.programminglife.libpljp.android

import android.content.Context
import android.os.SystemClock
import androidx.preference.PreferenceManager
import java.security.SecureRandom
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write


object UUIDUtils {

    internal val millis1582_10_15: Long
    private val UUID_NODE_KEY = UUIDUtils::class.java.name + ":node"
    private val rnd = SecureRandom()
    private val nodeLock = ReentrantReadWriteLock()
    private val clockSeqLock = Any()
    private var lastUUIDTime = 0L
    private var clockSeqCount = 0
    /** 前回返したクロックシーケンスの値。  */
    private var clockSeq = SecureRandom().nextInt() and 0x3fff

    init {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        c.clear()
        c.set(1582, Calendar.OCTOBER, 5)
        millis1582_10_15 = c.timeInMillis
    }


    /**
     * UUIDインスタンスを作成する。
     * @throws IllegalArgumentException 引数うち一つでも0があったとき。
     */
    fun create(msb: Long, lsb: Long): UUID {
        if (msb == 0L || lsb == 0L) throw IllegalArgumentException("msb == 0 || lsb == 0")
        return UUID(msb, lsb)
    }


    /**
     * @param time ミリ秒単位の時刻
     * @param nano ミリ秒未満の時刻。ナノ秒単位。0 - 999999の範囲で指定する。
     * 1000000以上の桁と下位2桁は切り捨てられる。
     */
    fun generate(context: Context,
            time: Long = System.currentTimeMillis(),
            nano: Long = SystemClock.elapsedRealtimeNanos()): UUID {
        val arr = generateLongArray(context, time, nano)
        return UUID(arr[0], arr[1])
    }


    fun generate(node: Long,
            time: Long = System.currentTimeMillis(),
            nano: Long = SystemClock.elapsedRealtimeNanos()): UUID {
        val arr = generateLongArray(node, time, nano)
        return UUID(arr[0], arr[1])
    }


    private fun generateLongArray(context: Context, time: Long, nano: Long): LongArray {
        val node = getDeviceNodeId(context)
        return generateLongArray(node, time, nano)
    }


    private fun generateLongArray(node: Long, time: Long, nano: Long): LongArray {
        val clockSeq = nextClockSeq(time)
        val uuidTime = (time - millis1582_10_15) * 10000 + ((nano / 100) % 10000)
        return makeUUIDVersion1(uuidTime, clockSeq, node)
    }


    /**
     * UUIDのnode値を返す。この端末のnode値が決定していればそれを返す。
     * node値はデフォルトのSharedPreferencesに記録される。
     */
    fun getDeviceNodeId(context: Context): Long {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        return nodeLock.read {
            sp.getLong(UUID_NODE_KEY, 0L).takeIf { it != 0L } ?: let {
                nodeLock.write {
                    (rnd.nextLong() and 0xffffffffffffL or 0x010000000000L).also {
                        val editor = sp.edit()
                        editor.putLong(UUID_NODE_KEY, it)
                        editor.apply()
                    }
                }
            }
        }
    }


    private fun nextClockSeq(time: Long): Int {
        synchronized(clockSeqLock) {
            if (time == lastUUIDTime) {
                if (clockSeqCount++ == 0x3fff)
                    throw IllegalStateException("clock seq overflow.")
            }
            else {
                lastUUIDTime = time
                clockSeqCount = 0
            }
            return clockSeq++
        }
    }


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


// UUID

val UUID.epochMilli get() = timestamp() / 10000L + UUIDUtils.millis1582_10_15

val UUID.nodeIdString get() = node().toString(16).toUpperCase(Locale.US)

fun nodeIdStr(context: Context) = UUIDUtils.getDeviceNodeId(context).toString(16).toUpperCase(Locale.US)
