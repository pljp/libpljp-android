package jp.programminglife.libpljp.android

import android.util.SparseArray
import android.util.SparseBooleanArray
import android.util.SparseIntArray


inline fun <E> SparseArray<E>.forEach(block: (key: Int, value: E?) -> Unit) {
    (0 until size()).forEach { block(it, get(keyAt(it))) }
}
fun <E> SparseArray<E>.asSequence(): Sequence<Pair<Int, E>> {
    return (0 until size()).asSequence().map { it to get(keyAt(it)) }.filter { it.second != null }
}

inline fun SparseBooleanArray.forEach(block: (key: Int, value: Boolean) -> Unit) {
    (0 until size()).forEach { keyAt(it).let { block(it, get(it)) } }
}
/** [SparseBooleanArray]から、値がtrueであるkeyのシーケンスを返す。 */
fun SparseBooleanArray.asSequence(): Sequence<Int> {
    return (0 until size()).asSequence().map { keyAt(it) }.filter { get(it) }
}

inline fun SparseIntArray.forEach(block: (key: Int, value: Int) -> Unit) {
    (0 until size()).forEach { block(it, get(keyAt(it))) }
}
fun SparseIntArray.asSequence(): Sequence<Pair<Int, Int>> {
    return (0 until size()).asSequence().map { it to get(keyAt(it)) }
}
