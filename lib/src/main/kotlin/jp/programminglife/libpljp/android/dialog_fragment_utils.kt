package jp.programminglife.libpljp.android

import android.app.Activity
import android.os.Bundle


enum class DialogTargetType {
    ACTIVITY, FRAGMENT
}


/**
 * ダイアログの結果を受け取るFragmentを設定する。
 * このメソッドを使用するためには DialogFragment を childFragmentManager を使って表示する必要がある。
 */
fun androidx.fragment.app.DialogFragment.setDialogResultListener(listener:Any, dialogArguments: Bundle, requestCode: Int) {
    when (listener) {
        is androidx.fragment.app.Fragment -> {
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
fun <T> androidx.fragment.app.DialogFragment.getDialogResultListener(): T {

    val type = arguments?.getSerializable("ViewUtils:targetType") as? DialogTargetType
            ?: throw RuntimeException("ダイアログ引数の \"ViewUtils:targetType\" が正しくない。")
    return when (type) {
        DialogTargetType.FRAGMENT -> parentFragment as T
        DialogTargetType.ACTIVITY -> activity as T
    }

}

/**
 * setDialogListenerでセットしたリクエストコードを取り出す。
 */
fun androidx.fragment.app.DialogFragment.getDialogRequestCode(): Int {
    return (arguments?.getSerializable("ViewUtils:targetType") as? DialogTargetType)
            ?.let { arguments?.getInt("ViewUtils:requestCode") }
            ?: throw RuntimeException("ダイアログ引数の \"ViewUtils:targetType\" が正しくない。")
}
