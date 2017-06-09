package jp.programminglife.libpljp.android

// UUID

val java.util.UUID.epochMilli get() = jp.programminglife.libpljp.android.UUIDUtils.toEpochMilli(timestamp())


// Bundle

inline fun <reified E: Enum<E>> android.os.Bundle.getEnum(key: String) = getString(key).let { enumValueOf<E>(it) }
fun <E: Enum<E>> android.os.Bundle.putEnum(key:String, value: E) { putString(key, value.name) }
