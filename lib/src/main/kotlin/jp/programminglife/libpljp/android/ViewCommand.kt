package jp.programminglife.libpljp.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * ビューモデルからビューに対して指示を出すコマンドのインターフェイス。
 */
@Deprecated("")
interface ViewCommand

/**
 * ViewCommandを処理するハンドラー関数の型。
 * ViewCommandを処理したらtrueを返す。このハンドラーで処理できないコマンドだったらfalseを返す。
 */
@Deprecated("")
typealias ViewCommandHandler = (ViewCommand) -> Boolean


/**
 * ハンドラーの集合。
 */
@Deprecated("")
class ViewCommandHandlerSet(vararg val handlers: ViewCommandHandler) {
    /**
     * コマンドを処理する。
     * @return コマンドを処理したらtrue。処理できないコマンドだったらfalse。
     */
    fun handle(command: ViewCommand) = handlers.any { it(command) }
}


@Deprecated("")
data class StartActivityCommand(val activityClass: Class<*>, val extras: Bundle) : ViewCommand


@Deprecated("")
fun startActivityCommandHandler(context: Context) : ViewCommandHandler = { command ->
    (command as? StartActivityCommand)?.run {
        val intent = Intent(context, activityClass)
        intent.putExtras(extras)
        context.startActivity(intent)
        true
    } ?: false
}


@Deprecated("")
class FinishActivityCommand : ViewCommand


@Deprecated("")
fun finishActivityCommandHandler(activity: Activity) : ViewCommandHandler = { command ->
    (command as? FinishActivityCommand)?.run {
        activity.finish()
        true
    } ?: false
}
