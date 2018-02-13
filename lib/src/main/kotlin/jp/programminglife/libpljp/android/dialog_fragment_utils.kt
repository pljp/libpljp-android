package jp.programminglife.libpljp.android

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment


enum class DialogTargetType {
    ACTIVITY, FRAGMENT
}


/**
 * ダイアログの結果を受け取るFragmentを設定する。
 * このメソッドを使用するためには DialogFragment を childFragmentManager を使って表示する必要がある。
 */
fun DialogFragment.setDialogResultListener(listener:Any, dialogArguments: Bundle, requestCode: Int) {
    when (listener) {
        is Fragment -> {
            dialogArguments.putSerializable("ViewUtils:targetType", DialogTargetType.FRAGMENT)
            dialogArguments.putInt("ViewUtils:requestCode", requestCode)
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

    val type = arguments.getSerializable("ViewUtils:targetType") as? DialogTargetType
            ?: throw RuntimeException("ダイアログ引数の \"ViewUtils:targetType\" がnull。")
    return when (type) {
        DialogTargetType.FRAGMENT -> parentFragment as T
        DialogTargetType.ACTIVITY -> activity as T
    }

}

/**
 * setDialogListenerでセットしたリクエストコードを取り出す。
 */
fun DialogFragment.getDialogRequestCode(): Int {
    return (arguments.getSerializable("ViewUtils:targetType") as? DialogTargetType)
            ?.let { arguments.getInt("ViewUtils:requestCode") }
            ?: throw RuntimeException("ダイアログ引数の \"ViewUtils:targetType\" がnull。")
}
