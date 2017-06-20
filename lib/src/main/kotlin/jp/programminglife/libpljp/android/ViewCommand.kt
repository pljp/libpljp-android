package jp.programminglife.libpljp.android

/**
 * ビューモデルからビューに対して指示を出すコマンドのインターフェイス。
 */
interface ViewCommand

/**
 * ViewCommandを処理するハンドラー関数の型。
 * ViewCommandを処理したらtrueを返す。このハンドラーで処理できないコマンドだったらfalseを返す。
 */
typealias ViewCommandHandler = (jp.programminglife.libpljp.android.ViewCommand) -> Boolean


/**
 * ハンドラーの集合。
 */
class ViewCommandHandlerSet(vararg val handlers: jp.programminglife.libpljp.android.ViewCommandHandler) {
    /**
     * コマンドを処理する。
     * @return コマンドを処理したらtrue。処理できないコマンドだったらfalse。
     */
    fun handle(command: jp.programminglife.libpljp.android.ViewCommand) = handlers.any { it(command) }
}


data class StartActivityCommand(val activityClass: Class<*>, val extras: android.os.Bundle) : jp.programminglife.libpljp.android.ViewCommand


fun startActivityCommandHandler(context: android.content.Context) : jp.programminglife.libpljp.android.ViewCommandHandler = { command ->
    (command as? jp.programminglife.libpljp.android.StartActivityCommand)?.run {
        val intent = android.content.Intent(context, activityClass)
        intent.putExtras(extras)
        context.startActivity(intent)
        true
    } ?: false
}


class FinishActivityCommand : jp.programminglife.libpljp.android.ViewCommand


fun finishActivityCommandHandler(activity: android.app.Activity) : jp.programminglife.libpljp.android.ViewCommandHandler = { command ->
    (command as? jp.programminglife.libpljp.android.FinishActivityCommand)?.run {
        activity.finish()
        true
    } ?: false
}
