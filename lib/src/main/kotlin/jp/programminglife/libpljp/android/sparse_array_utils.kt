package jp.programminglife.libpljp.android

import android.util.LongSparseArray
import android.util.SparseArray
import android.util.SparseBooleanArray
import android.util.SparseIntArray
import android.util.SparseLongArray

// SparseArray

inline fun <E> SparseArray<E>.forEach(block: (key: Int, value: E) -> Unit) {
    for (i in 0 until size()) {
        block(keyAt(i), valueAt(i))
    }
}

inline fun <E> SparseArray<E>.forEachValue(block: (value: E) -> Unit) {
    for (i in 0 until size()) {
        block(valueAt(i))
    }
}

fun <E> SparseArray<E>.asSequence(): Sequence<Pair<Int, E>> {
    return sequence {
        for (i in 0 until size()) {
            yield(keyAt(i) to valueAt(i))
        }
    }
}

fun <E> SparseArray<E>.valueSequence(): Sequence<E> {
    return sequence {
        for (i in 0 until size()) {
            yield(valueAt(i))
        }
    }
}

// LongSparseArray

inline fun <E> LongSparseArray<E>.forEach(block: (key: Long, value: E) -> Unit) {
    for (i in 0 until size()) {
        block(keyAt(i), valueAt(i))
    }
}

inline fun <E> LongSparseArray<E>.forEachValue(block: (value: E) -> Unit) {
    for (i in 0 until size()) {
        block(valueAt(i))
    }
}

fun <E> LongSparseArray<E>.asSequence(): Sequence<Pair<Long, E>> {
    return sequence {
        for (i in 0 until size()) {
            yield(keyAt(i) to valueAt(i))
        }
    }
}

fun <E> LongSparseArray<E>.valueSequence(): Sequence<E> {
    return sequence {
        for (i in 0 until size()) {
            yield(valueAt(i))
        }
    }
}

// SparseBooleanArray

inline fun SparseBooleanArray.forEach(block: (key: Int, value: Boolean) -> Unit) {
    for (i in 0 until size()) {
        block(keyAt(i), valueAt(i))
    }
}

@Deprecated(message = "trueKeySequenceにリネーム", replaceWith = ReplaceWith("trueKeySequence()"))
fun SparseBooleanArray.asSequence() = trueKeySequence()

/** [SparseBooleanArray]から、値がtrueであるkeyのシーケンスを返す。 */
fun SparseBooleanArray.trueKeySequence(): Sequence<Int> {
    return sequence {
        for (i in 0 until size()) {
            if (valueAt(i)) {
                yield(keyAt(i))
            }
        }
    }
}

// SparseIntArray

inline fun SparseIntArray.forEach(block: (key: Int, value: Int) -> Unit) {
    for (i in 0 until size()) {
        block(keyAt(i), valueAt(i))
    }
}

inline fun SparseIntArray.forEachValue(block: (value: Int) -> Unit) {
    for (i in 0 until size()) {
        block(valueAt(i))
    }
}

fun SparseIntArray.asSequence(): Sequence<Pair<Int, Int>> {
    return sequence {
        for (i in 0 until size()) {
            yield(keyAt(i) to valueAt(i))
        }
    }
}

fun SparseIntArray.valueSequence(): Sequence<Int> {
    return sequence {
        for (i in 0 until size()) {
            yield(valueAt(i))
        }
    }
}

// SparseLongArray

inline fun SparseLongArray.forEach(block: (key: Int, value: Long) -> Unit) {
    for (i in 0 until size()) {
        block(keyAt(i), valueAt(i))
    }
}

inline fun SparseLongArray.forEachValue(block: (value: Long) -> Unit) {
    for (i in 0 until size()) {
        block(valueAt(i))
    }
}

fun SparseLongArray.asSequence(): Sequence<Pair<Int, Long>> {
    return sequence {
        for (i in 0 until size()) {
            yield(keyAt(i) to valueAt(i))
        }
    }
}

fun SparseLongArray.valueSequence(): Sequence<Long> {
    return sequence {
        for (i in 0 until size()) {
            yield(valueAt(i))
        }
    }
}
