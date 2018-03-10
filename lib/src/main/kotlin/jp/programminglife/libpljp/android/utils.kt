package jp.programminglife.libpljp.android

import android.content.Context
import android.os.Bundle
import java.util.EnumSet
import java.util.Locale
import java.util.UUID


// UUID

val UUID.epochMilli get() = UUIDUtils.toEpochMilli(timestamp())

val UUID.nodeId get() = node().toString(16).toUpperCase(Locale.US)

fun nodeIdStr(context: Context) = UUIDUtils.getDeviceNodeId(context).toString(16).toUpperCase(Locale.US)


// Bundle

inline fun <reified E: Enum<E>> Bundle.getEnum(key: String) = getString(key).let { enumValueOf<E>(it) }

fun <E: Enum<E>> Bundle.putEnum(key:String, value: E) { putString(key, value.name) }

inline fun <reified E: Enum<E>> Bundle.getEnumSet(key: String): EnumSet<E>? =
        getStringArray(key)?.map { enumValueOf<E>(it) }?.let { EnumSet.copyOf(it) }

fun <E: Enum<E>> Bundle.putEnumSet(key: String, value: EnumSet<E>?) {
    putStringArray(key, value?.map { it.name }?.toTypedArray())
}


// math

fun signum(n: Long) = (n / Math.abs(n)).toInt()

fun signum(n: Int) = n / Math.abs(n)


// EnumSet

fun <E : Enum<E>> EnumSet<E>.serializeToString() = joinToString(",") { it.name }

inline fun <reified E : Enum<E>> enumSetOf(serializedValue: String): EnumSet<E> = serializedValue
        .splitToSequence(',')
        .filter { it.isNotEmpty() }
        .map { enumValueOf<E>(it) }
        .fold(EnumSet.noneOf(E::class.java)) { s, e -> s.add(e); s }
