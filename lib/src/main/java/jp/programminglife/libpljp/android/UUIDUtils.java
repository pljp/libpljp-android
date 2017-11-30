/*
Copyright (c) 2013 ProgrammingLife.jp

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
package jp.programminglife.libpljp.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;


public final class UUIDUtils {

    private static final long millis1582_10_15;
    private static final String UUID_NODE_KEY = UUIDUtils.class.getName() + ":node";
    private static final SecureRandom rnd = new SecureRandom();
    private static final Object uuidLock = new Object();
    private static long lastUUIDTime = 0L;
    private static int clockSeqCount = 0;
    /** 前回返したクロックシーケンスの値。 */
    private static int clockSeq = new SecureRandom().nextInt() & 0x3fff;

    static {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(1582, Calendar.OCTOBER, 5);
        millis1582_10_15 = c.getTimeInMillis();
    }


    @Nullable
    public static UUID create(@Nullable Long msb, @Nullable Long lsb) {
        return msb == null || lsb == null ? null : create(msb.longValue(), lsb.longValue());
    }


    @Nullable
    public static UUID create(long msb, long lsb) {
        return msb == 0 || lsb == 0 ? null : new UUID(msb, lsb);
    }


    @Nullable
    public static Long getMSB(@Nullable UUID uuid) {
        return uuid != null ? uuid.getMostSignificantBits() : null;
    }


    @Nullable
    public static Long getLSB(@Nullable UUID uuid) {
        return uuid != null ? uuid.getLeastSignificantBits() : null;
    }


    /**
     * UUIDインスタンスを作成する。
     * @throws IllegalArgumentException 引数うち一つでも0があったとき。
     */
    @NonNull
    public static UUID createNotNull(long msb, long lsb) throws IllegalArgumentException {
        if ( msb == 0 || lsb == 0 ) throw new IllegalArgumentException("msb == 0 || lsb == 0");
        return new UUID(msb, lsb);
    }


    @NonNull
    @Deprecated
    public static UUID generate(@NonNull Context context) {
        return generate(context, System.currentTimeMillis());
    }
    @NonNull
    public static UUID generate(@NonNull Context context, long time) {
        long[] arr = generateLongArray(context, time);
        return new UUID(arr[0], arr[1]);
    }


    @NonNull
    @Deprecated
    public static UUID generate(long node) {
        return generate(node, System.currentTimeMillis());
    }
    @NonNull
    public static UUID generate(long node, long time) {
        long[] arr = generateLongArray(node, time);
        return new UUID(arr[0], arr[1]);
    }


    @NonNull
    public static long[] generateLongArray(@NonNull Context context) {
        return generateLongArray(context, System.currentTimeMillis());
    }
    @NonNull
    public static long[] generateLongArray(@NonNull Context context, long time) {
        long node = getDeviceNodeId(context);
        return generateLongArray(node, time);
    }


    @NonNull
    @Deprecated
    public static long[] generateLongArray(long node) {
        long time = System.currentTimeMillis();
        return generateLongArray(node, time);
    }
    @NonNull
    public static long[] generateLongArray(long node, long time) {
        int clockSeq = nextClockSeq(time);
        return makeUUIDVersion1(time, clockSeq, node);
    }


    @NonNull
    @Deprecated
    public static String generateString(@NonNull Context context) {
        return generate(context, System.currentTimeMillis()).toString();
    }
    @NonNull
    public static String generateString(@NonNull Context context, long time) {
        return generate(context, time).toString();
    }


    /**
     * UUIDのnode値を返す。この端末のnode値が決定していればそれを返す。
     * node値はデフォルトのSharedPreferencesに記録される。
     */
    public static long getDeviceNodeId(@NonNull Context context) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        long node;

        synchronized(uuidLock) {
            node = sp.getLong(UUID_NODE_KEY, 0);
            if ( node == 0 ) {

                node = (rnd.nextLong() & 0xffffffffffffL) | 0x010000000000L;
                Editor editor = sp.edit();
                editor.putLong(UUID_NODE_KEY, node);
                editor.apply();

            }
        }
        return node;

    }


    private static int nextClockSeq(long time) {
        synchronized (uuidLock) {
            if ( time == lastUUIDTime ) {
                if ( clockSeqCount++ == 0x3fff )
                    throw new IllegalStateException("clock seq overflow.");
            }
            else {
                lastUUIDTime = time;
                clockSeqCount = 0;
            }
            return clockSeq++;
        }
    }


    /**
     * Version1のUUIDを組み立てる。
     * @param javaTime Javaの時刻(ミリ秒)。
     * @param clockSeq 同時刻の場合に区別するためのカウント値。
     * @param node ノードID。
     */
    private static long[] makeUUIDVersion1(long javaTime, int clockSeq, long node) {

        // uuidのタイムスタンプは100ns単位。
        long uuidTime = (javaTime - millis1582_10_15) * 10000;
        long time_low = uuidTime & 0xffffffffL;
        long time_mid = (uuidTime >> 32) & 0xffffL;
        long time_high_and_version = 0x1000L | ((uuidTime >> 48) & 0xfffL);
        long clock_seq_and_variant = 0x8000L | (clockSeq & 0x3fffL);

        long msb = (time_low << 32) | (time_mid << 16) | time_high_and_version;
        long lsb = (clock_seq_and_variant << 48) | (node & 0xffffffffffffL);
        return new long[] {msb, lsb};

    }


    /**
     * UUIDのタイムスタンプをJavaの時刻(ミリ秒)に変換する。
     * @deprecated UUID.epochMilli拡張関数を使う。
     * @param uuidMsb UUIDの上位64ビット。
     */
    @Deprecated
    public static long toJavaTimestamp(long uuidMsb) {
        return (
            ((uuidMsb & 0xffffffff00000000L) >>> 32) |
            ((uuidMsb & 0x00000000ffff0000L) << 16) |
            ((uuidMsb & 0x0000000000000fffL) << 48)
            ) / 10000 + millis1582_10_15;
    }


    public static long toEpochMilli(long timestamp) {
        return timestamp / 10000L + millis1582_10_15;
    }


    /**
     * @deprecated UUID.node()を使う。
     * @param uuid
     * @return
     */
    @Deprecated
    public static long getNode(UUID uuid) {
        return uuid.getLeastSignificantBits() & 0xffffffffffffL;
    }


    private UUIDUtils() {}
}
