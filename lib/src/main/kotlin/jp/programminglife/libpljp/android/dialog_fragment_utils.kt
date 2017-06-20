package jp.programminglife.libpljp.android

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment


enum class DialogTargetType {
    ACTIVITY, FRAGMENT
}


fun android.support.v4.app.DialogFragment.setDialogResultListener(listener: Any, dialogArguments: android.os.Bundle, requestCode: Int) {

    if (listener is android.support.v4.app.Fragment) {
        setTargetFragment(listener, requestCode)
        dialogArguments.putSerializable("ViewUtils:targetType",
                jp.programminglife.libpljp.android.DialogTargetType.FRAGMENT)
    } else if (listener is android.app.Activity) {
        dialogArguments.putSerializable("ViewUtils:targetType",
                jp.programminglife.libpljp.android.DialogTargetType.ACTIVITY)
        dialogArguments.putInt("ViewUtils:requestCode", requestCode)
    } else
        throw IllegalArgumentException("listenerはFragmentかActivityを継承している必要があります。")

}

@Suppress("UNCHECKED_CAST")
fun <T> android.support.v4.app.DialogFragment.getDialogResultListener(): T {

    val type = arguments.get("ViewUtils:targetType") as? jp.programminglife.libpljp.android.DialogTargetType
            ?: throw RuntimeException("ダイアログ引数の \"ViewUtils:targetType\" がnull。")
    when (type) {
        jp.programminglife.libpljp.android.DialogTargetType.FRAGMENT -> return targetFragment as T
        jp.programminglife.libpljp.android.DialogTargetType.ACTIVITY -> return activity as T
        else -> throw RuntimeException()
    }

}

/**
 * setDialogListenerでセットしたリクエストコードを取り出す。
 */
fun android.support.v4.app.DialogFragment.getDialogRequestCode(): Int {

    val type = arguments.get("ViewUtils:targetType") as? jp.programminglife.libpljp.android.DialogTargetType
            ?: throw RuntimeException("ダイアログ引数の \"ViewUtils:targetType\" がnull。")
    when (type) {
        jp.programminglife.libpljp.android.DialogTargetType.FRAGMENT -> return targetRequestCode
        jp.programminglife.libpljp.android.DialogTargetType.ACTIVITY -> return arguments.getInt("ViewUtils:requestCode")
        else -> throw RuntimeException()
    }

}
