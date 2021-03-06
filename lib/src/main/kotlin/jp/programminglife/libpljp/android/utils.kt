package jp.programminglife.libpljp.android

import android.os.Bundle
import java.util.EnumSet
import kotlin.math.sign


// Bundle

inline fun <reified E: Enum<E>> Bundle.getEnum(key: String) = getString(key)?.let { enumValueOf<E>(it) }

fun <E: Enum<E>> Bundle.putEnum(key:String, value: E) { putString(key, value.name) }

inline fun <reified E: Enum<E>> Bundle.getEnumSet(key: String): EnumSet<E>? =
        getStringArray(key)?.map { enumValueOf<E>(it) }?.let { EnumSet.copyOf(it) }

fun <E: Enum<E>> Bundle.putEnumSet(key: String, value: EnumSet<E>?) {
    putStringArray(key, value?.map { it.name }?.toTypedArray())
}


// math

@Deprecated("廃止", replaceWith = ReplaceWith("n.sign", "kotlin.math.sign"))
fun signum(n: Long) = n.sign

@Deprecated("廃止", replaceWith = ReplaceWith("n.sign", "kotlin.math.sign"))
fun signum(n: Int) = n.sign


// EnumSet

fun <E : Enum<E>> EnumSet<E>.serializeToString() = joinToString(",") { it.name }

inline fun <reified E : Enum<E>> enumSetOf(serializedValue: String): EnumSet<E> = serializedValue
        .splitToSequence(',')
        .filter { it.isNotEmpty() }
        .map { enumValueOf<E>(it) }
        .fold(EnumSet.noneOf(E::class.java)) { s, e -> s.add(e); s }

inline fun <reified T: Enum<T>> emptyEnumSet(): EnumSet<T> = EnumSet.noneOf(T::class.java)
