package jp.programminglife.libpljp.android.kotlin

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment


enum class DialogTargetType {
    ACTIVITY, FRAGMENT
}


fun DialogFragment.setDialogResultListener(listener: Any, dialogArguments: Bundle, requestCode: Int) {

    if (listener is Fragment) {
        setTargetFragment(listener, requestCode)
        dialogArguments.putSerializable("ViewUtils:targetType", DialogTargetType.FRAGMENT)
    } else if (listener is Activity) {
        dialogArguments.putSerializable("ViewUtils:targetType", DialogTargetType.ACTIVITY)
        dialogArguments.putInt("ViewUtils:requestCode", requestCode)
    } else
        throw IllegalArgumentException("listenerはFragmentかActivityを継承している必要があります。")

}

@Suppress("UNCHECKED_CAST")
fun <T> DialogFragment.getDialogResultListener(): T {

    val type = arguments.get("ViewUtils:targetType") as? DialogTargetType
            ?: throw RuntimeException("ダイアログ引数の \"ViewUtils:targetType\" がnull。")
    when (type) {
        DialogTargetType.FRAGMENT -> return targetFragment as T
        DialogTargetType.ACTIVITY -> return activity as T
        else -> throw RuntimeException()
    }

}

/**
 * setDialogListenerでセットしたリクエストコードを取り出す。
 */
fun DialogFragment.getDialogRequestCode(): Int {

    val type = arguments.get("ViewUtils:targetType") as? DialogTargetType
            ?: throw RuntimeException("ダイアログ引数の \"ViewUtils:targetType\" がnull。")
    when (type) {
        DialogTargetType.FRAGMENT -> return targetRequestCode
        DialogTargetType.ACTIVITY -> return arguments.getInt("ViewUtils:requestCode")
        else -> throw RuntimeException()
    }

}
