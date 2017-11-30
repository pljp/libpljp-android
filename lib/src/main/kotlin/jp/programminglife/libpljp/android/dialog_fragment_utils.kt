package jp.programminglife.libpljp.android

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment


enum class DialogTargetType {
    ACTIVITY, FRAGMENT
}


fun DialogFragment.setDialogResultListener(listener: Any, dialogArguments: Bundle, requestCode: Int) {

    when (listener) {
        is Fragment -> {
            setTargetFragment(listener, requestCode)
            dialogArguments.putSerializable("ViewUtils:targetType", DialogTargetType.FRAGMENT)
        }
        is Activity -> {
            dialogArguments.putSerializable("ViewUtils:targetType", DialogTargetType.ACTIVITY)
            dialogArguments.putInt("ViewUtils:requestCode", requestCode)
        }
        else -> throw IllegalArgumentException("listenerはFragmentかActivityを継承している必要があります。")
    }

}

@Suppress("UNCHECKED_CAST")
fun <T> DialogFragment.getDialogResultListener(): T {

    val type = arguments.get("ViewUtils:targetType") as? DialogTargetType
            ?: throw RuntimeException("ダイアログ引数の \"ViewUtils:targetType\" がnull。")
    return when (type) {
        DialogTargetType.FRAGMENT -> targetFragment as T
        DialogTargetType.ACTIVITY -> activity as T
    }

}

/**
 * setDialogListenerでセットしたリクエストコードを取り出す。
 */
fun DialogFragment.getDialogRequestCode(): Int {

    val type = arguments.get("ViewUtils:targetType") as? DialogTargetType
            ?: throw RuntimeException("ダイアログ引数の \"ViewUtils:targetType\" がnull。")
    return when (type) {
        DialogTargetType.FRAGMENT -> targetRequestCode
        DialogTargetType.ACTIVITY -> arguments.getInt("ViewUtils:requestCode")
    }

}
