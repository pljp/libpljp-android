package jp.programminglife.libpljp.android;


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

public final class GestureDetector {

    // Handlerのメッセージ
    static final int TAP_CONFIRMED = 1;
    static final int LONG_TAP = 2;

    static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    final Logger log = new Logger(getClass());
    private final SparseArray<Single> singleDetectors = new SparseArray<>();
    private GestureListener listener;
    final int doubleTapSlopSquare;
    final int touchSlopSquare;
    final float maxFlingVelocity;
    final float minFlingVelocitySquare;
    private final Handler_ handler;


    public GestureDetector(@NotNull Context context, @NotNull GestureListener listener) {

        this.listener = listener;
        ViewConfiguration viewConf = ViewConfiguration.get(context);
        doubleTapSlopSquare = viewConf.getScaledDoubleTapSlop() * viewConf.getScaledDoubleTapSlop();
        touchSlopSquare = viewConf.getScaledTouchSlop() * viewConf.getScaledTouchSlop();
        maxFlingVelocity = viewConf.getScaledMaximumFlingVelocity();
        minFlingVelocitySquare = viewConf.getScaledMinimumFlingVelocity() * viewConf.getScaledMinimumFlingVelocity();
        handler = new Handler_(this);
        log.v("double tap slop:%d", viewConf.getScaledDoubleTapSlop());
        log.v("touch slop:%d", viewConf.getScaledTouchSlop());
        log.v("tap timeout:%d, double tap timeout:%d", TAP_TIMEOUT, DOUBLE_TAP_TIMEOUT);

    }


    public boolean onTouchEvent(MotionEvent e) {

        int index = e.getActionIndex();
        int id = e.getPointerId(index);
        //log.v("pointer p.count:%d, action:%d, index:%d, id:%d, x:%f, y:%f", e.getPointerCount(), e.getActionMasked(), index, id, e.getX(index), e.getY(index));
        int actionMasked = e.getActionMasked();

        Single single = singleDetectors.get(id);
        if ( actionMasked == ACTION_DOWN && single == null ) {
            log.v("new Single id:%d", id);
            single = new Single(id, e);
            singleDetectors.put(id, single);
            log.v("EVENT: down - id:%d", id);
            listener.onDown(e, id);
        }

        boolean ret = false;
        if ( single != null ) {

            ret = true;
            single.onTouchEvent(e);
            if ( !single.isTracking() )
                removeSingle(single.id);

        }

        return ret;

    }


    private void removeSingle(int id) {

        log.v("remove Single id:%d", id);
        final Single single = singleDetectors.get(id);
        if ( single != null ) {
            single.dispose();
            singleDetectors.remove(id);
        }

    }


    final class Single {

        final Logger log = new Logger(getClass());
        final GestureDetector gesture;
        final int id;
        /** イベントの追跡を開始した最初のダウンイベント。nullのときはイベントの追跡をしていない。 */
        MotionEvent firstDown;
        int actionMasked;
        MotionEvent lastDown;
        private SinglePointerPhase mode;
        private int count;
        /** ドラッグで使用。直前のonDragMoveのX座標。 */
        float lastX;
        /** ドラッグで使用。直前のonDragMoveのY座標。 */
        float lastY;
        private VelocityTracker velocityTracker;

        public Single(int id, MotionEvent firstDown) {

            this.id = id;
            this.firstDown = MotionEvent.obtain(firstDown);
            gesture = GestureDetector.this;
            mode = SinglePointerPhase.UP;
            velocityTracker = VelocityTracker.obtain();

        }


        void sendMessage(int what, long time) {
            //log.v("what:%d, time:%s", what, new Date(time).toString());
            handler.sendMessageAtTime(Message.obtain(handler, what, Single.this), time);
        }


        void removeMessage(int what) {
            //log.v("what:%d", what);
            if ( handler.hasMessages(what, Single.this) ) handler.removeMessages(what, Single.this);
        }


        void onTouchEvent(MotionEvent e) {

            actionMasked = e.getActionMasked();
            if ( actionMasked == ACTION_DOWN ) {
                lastDown = MotionEvent.obtain(e);
                velocityTracker.clear();
            }
            velocityTracker.addMovement(e);

            final SinglePointerPhase newMode = mode.onTouchEvent(this, e);
            if ( newMode != null && mode != newMode ) {

                log.v("mode %s -> %s", mode, newMode);
                mode.end(this);
                mode = newMode;
                newMode.start(this);

            }

        }


        boolean isTracking() {
            return mode != SinglePointerPhase.END;
        }


        void dispose() {

            firstDown.recycle();
            if ( lastDown != null )
                lastDown.recycle();
            firstDown = lastDown = null;

            velocityTracker.recycle();
            velocityTracker = null;

            mode = SinglePointerPhase.END;
            removeMessage(TAP_CONFIRMED);
            removeMessage(LONG_TAP);

        }

    }


    private enum SinglePointerPhase  {

        DOWN {
            @Override
            void start(Single single) {
                single.removeMessage(TAP_CONFIRMED);
                single.sendMessage(LONG_TAP, single.lastDown.getEventTime() + LONG_PRESS_TIMEOUT);
            }


            @Override
            SinglePointerPhase onTouchEvent(final Single s, final MotionEvent e) {

                final Logger log = s.log;
                final GestureDetector g = s.gesture;
                final MotionEvent lastDown = s.lastDown;
                if ( s.actionMasked == ACTION_MOVE ) {

                    // ずれが大きくなったら中止
                    final float dx = lastDown.getX() - e.getX();
                    final float dy = lastDown.getY() - e.getY();
                    final float slop = dx * dx + dy * dy;
                    if ( slop > g.touchSlopSquare ) {
                        log.v("cancel Tap slop:%f, touchSlopSquare:%d, id:%d", slop, g.touchSlopSquare, s.id);
                        s.removeMessage(LONG_TAP);
                        return DRAG;
                    }

                }
                if ( s.actionMasked == ACTION_UP ) {

                    final MotionEvent firstDown = s.firstDown;
                    final float dx = firstDown.getX() - lastDown.getX();
                    final float dy = firstDown.getY() - lastDown.getY();
                    s.count++;
                    if ( s.count >= 2 && dx * dx + dy * dy > g.doubleTapSlopSquare ) {

                        log.v("EVENT: tap confirmed (連続タップ失敗) - count:%d, id:%d", s.count-1, s.id);
                        g.listener.onTapConfirmed(firstDown, s.id, s.count - 1);
                        s.count = 1;

                    }

                    log.v("EVENT: tap - count:%d, id:%d", s.count, s.id);
                    g.listener.onTap(lastDown, s.id, s.count);
                    s.sendMessage(TAP_CONFIRMED, e.getEventTime() + GestureDetector.DOUBLE_TAP_TIMEOUT);
                    s.removeMessage(LONG_TAP);
                    return UP;

                }
                return null;

            }
        },

        UP {
            @Override
            SinglePointerPhase onTouchEvent(Single single, MotionEvent e) {
                return single.actionMasked == ACTION_DOWN ? DOWN : null;
            }
        },

        DRAG {
            @Override
            void start(Single single) {

                final MotionEvent firstDown = single.firstDown;
                single.log.v("EVENT: drag start - id:%d, count:%d", single.id, single.count);
                single.gesture.listener.onDragStart(firstDown, single.id, single.count);
                single.lastX = firstDown.getX();
                single.lastY = firstDown.getY();

            }


            @Override
            SinglePointerPhase onTouchEvent(final Single single, final MotionEvent e) {

                if ( single.actionMasked == ACTION_MOVE ) {

                    float x = e.getX();
                    float y = e.getY();
                    final float dx = single.lastX - x;
                    final float dy = single.lastY - y;
                    //single.log.v("EVENT: drag move - dx:%f, dy:%f, id:%d", dx, dy, single.id);
                    single.gesture.listener.onDragMove(single.firstDown, e, single.id, dx, dy);
                    single.lastX = x;
                    single.lastY = y;

                }
                else if ( single.actionMasked == ACTION_UP ) {

                    final VelocityTracker velocityTracker = single.velocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, single.gesture.maxFlingVelocity);
                    final float vx = velocityTracker.getXVelocity(single.id);
                    final float vy = velocityTracker.getYVelocity(single.id);
                    final float velocitySquare = vx * vx + vy * vy;
                    single.log.v("id:%d, velocity:%f, vx:%f, vy:%f, minFlingVelocity:%f",
                            single.id,
                            (float)Math.sqrt(velocitySquare),
                            vx, vy,
                            (float)Math.sqrt(single.gesture.minFlingVelocitySquare));
                    if ( velocitySquare < single.gesture.minFlingVelocitySquare ) {
                        single.log.v("EVENT: drag end - id:%d", single.id);
                        single.gesture.listener.onDragEnd(e, single.id);
                    }
                    else {
                        single.log.v("EVENT: fling - id:%d", single.id, vx, vy);
                        single.gesture.listener.onFling(single.firstDown, e, single.id, vx, vy);
                    }

                    return END;

                }
                return null;

            }
        },

        END;

        /** フェーズが移ったときに呼ばれる。 */
        void start(Single single) {}
        /** 次のフェーズに移る前に呼ばれる。 */
        void end(Single single) {}
        /**
         * タッチイベントの処理。
         * @return 次のフェーズ。nullならフェーズが移らない。
         */
        SinglePointerPhase onTouchEvent(Single single, MotionEvent e) {return null;}
    }


    public static class GestureListener {

        /**
         * ゼスチャーの追跡を開始を通知する。ダブルタップの場合でも最初のdownのみを通知する。
         * @param e ACTION_DOWNイベント。
         * @param id ポインターID。
         */
        public void onDown(MotionEvent e, int id) {}

        /**
         * 連続タップが途切れた時に通知される。
         * @param e 最初のActionDownイベント。
         * @param id ポインターID。
         * @param count 連続タップした回数。
         */
        public void onTapConfirmed(MotionEvent e, int id, int count) {}

        /**
         * 1回以上の連続タップ直後で通知される。
         * @param e 最初のタップのACTION_DOWNイベント。
         * @param id ポインターID。
         * @param count 連続タップした回数。
         */
        public void onTap(MotionEvent e, int id, int count) {}

        /**
         * ドラッグが開始されたことが通知される。
         * @param e 最初のACTION_DOWNイベント。
         * @param id ポインターID。
         * @param count ドラッグの前にタップした回数。
         */
        public void onDragStart(MotionEvent e, int id, int count) {}

        /**
         * ドラッグの最中にポインタが移動したときに通知される。
         * @param e1 最初のDOWNイベント。
         * @param e2 現在のイベント。
         * @param id ポインターID。
         * @param distanceX 前回の通知からのX軸の移動量。
         * @param distanceY 前回の通知からのY軸の移動量。
         */
        public void onDragMove(MotionEvent e1, MotionEvent e2, int id, float distanceX, float distanceY) {}

        /**
         * ドラッグが終了したときに通知される。フリックで終了したときはこのメソッドではなく
         * {@link #onFling(MotionEvent, MotionEvent, int, float, float)} が呼ばれる。
         * @param e UPイベント。
         * @param id ポインターID。
         */
        public void onDragEnd(MotionEvent e, int id) {}

        /**
         * フリックしたたときに通知される。
         * @param e1 最初のDOWNイベント。
         * @param e2 UPイベント。
         * @param id ポインターID。
         * @param velocityX X方向の速度。
         * @param velocityY Y方向の速度。
         */
        public void onFling(MotionEvent e1, MotionEvent e2, int id, float velocityX, float velocityY) {}


        /**
         * 長押ししたときに通知される。
         * @param e 最初のDOWNイベント。
         * @param id ポインターID。
         * @param count タッチして離した回数。最初のタッチで長押ししたときは0。
         */
        public void onLongPress(MotionEvent e, int id, int count) {}

    }


    private static final class Handler_ extends Handler {

        private final Logger log = new Logger(getClass());
        private WeakReference<GestureDetector> gesture;


        public Handler_(GestureDetector gesture) {

            this.gesture = new WeakReference<>(gesture);
        }


        @Override
        public void handleMessage(Message msg) {

            final GestureDetector g = gesture.get();
            if ( g == null ) return;

            final Single single = msg.obj instanceof Single ? (Single)msg.obj : null;
            if ( single != null && single.isTracking() ) {
                switch (msg.what) {

                case TAP_CONFIRMED:
                    log.v("EVENT: tap confirmed - count:%d, id:%d", single.count, single.id);
                    g.listener.onTapConfirmed(single.firstDown, single.id, single.count);
                    g.removeSingle(single.id);
                    break;

                case LONG_TAP:
                    log.v("EVENT: long press - count:%d, id:%d", single.count, single.id);
                    g.listener.onLongPress(single.firstDown, single.id, single.count);
                    g.removeSingle(single.id);
                    break;
                }

            }
        }

    }

}
