package jp.programminglife.libpljp.android.kotlin

import android.os.Bundle
import java.util.UUID

// UUID

val UUID.epochMilli get() = jp.programminglife.libpljp.android.UUIDUtils.toEpochMilli(timestamp())


// Bundle

inline fun <reified E: Enum<E>> Bundle.getEnum(key: String) = getString(key).let { enumValueOf<E>(it) }
fun <E: Enum<E>> Bundle.putEnum(key:String, value: E) { putString(key, value.name) }
