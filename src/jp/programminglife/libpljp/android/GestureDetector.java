package jp.programminglife.libpljp.android;


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import com.android.annotations.NonNull;

import java.lang.ref.WeakReference;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;

public final class GestureDetector {

    // Handlerのメッセージ
    static final int TAP_CONFIRMED = 1;
    static final int LONG_TAP = 2;

    static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    public int maxPointers;
    final Logger log = new Logger(getClass());
    final int doubleTapSlopSquare;
    final int touchSlopSquare;
    final float maxFlingVelocity;
    final float minFlingVelocitySquare;
    private final SparseArray<Detector> detectors = new SparseArray<>();
    private final Handler_ handler;
    private OnDownListener onDownListener;


    public GestureDetector(@NonNull Context context, @NonNull OnDownListener onDownListener) {

        this.onDownListener = onDownListener;
        ViewConfiguration viewConf = ViewConfiguration.get(context);
        doubleTapSlopSquare = viewConf.getScaledDoubleTapSlop() * viewConf.getScaledDoubleTapSlop();
        touchSlopSquare = viewConf.getScaledTouchSlop() * viewConf.getScaledTouchSlop();
        maxFlingVelocity = viewConf.getScaledMaximumFlingVelocity();
        minFlingVelocitySquare = viewConf.getScaledMinimumFlingVelocity() * viewConf.getScaledMinimumFlingVelocity();
        handler = new Handler_(this);
        maxPointers = 10;
        log.v("double tap slop:%d", viewConf.getScaledDoubleTapSlop());
        log.v("touch slop:%d", viewConf.getScaledTouchSlop());
        log.v("tap timeout:%d, double tap timeout:%d", TAP_TIMEOUT, DOUBLE_TAP_TIMEOUT);

    }


    public boolean onTouchEvent(@NonNull MotionEvent e) {

        int index = e.getActionIndex();
        int id = e.getPointerId(index);
        int actionMasked = e.getActionMasked();
        Detector detector = detectors.get(id);
        //log.v("pointer id:%d, action:%d, single:%s, x:%.1f, y:%.1f, index:%d, p.count:%d", id, actionMasked, single, e.getX(index), e.getY(index), index, e.getPointerCount());

        if ( (actionMasked == ACTION_DOWN || actionMasked == ACTION_POINTER_DOWN ) && detector == null ) {

            if ( detectors.size() < maxPointers ) {
                log.v("EVENT: down - id:%d", id);
                final GestureListener gestureListener = onDownListener.onDown(e, id);
                log.v("new Single id:%d", id);
                detector = new Detector(id, e, gestureListener);
                detectors.put(id, detector);
            }
            else {
                log.v("ポインターの最大数を超えた detectorの数:%d, 最大数:%d", detectors.size(), maxPointers);
            }

        }

        boolean ret = false;
        if ( detector != null ) {

            ret = true;
            detector.onTouchEvent(e);
            if ( !detector.isTracking() )
                removeSingle(detector.id);

        }

        return ret;

    }


    private void removeSingle(int id) {

        log.v("remove Single id:%d", id);
        final Detector detector = detectors.get(id);
        if ( detector != null ) {
            detector.dispose();
            detectors.remove(id);
        }

    }


    final class Detector {

        final Logger log = new Logger(getClass());
        final GestureDetector gesture;
        final int id;
        @NonNull
        final GestureListener listener;
        /** イベントの追跡を開始した最初のダウンイベント。nullのときはイベントの追跡をしていない。 */
        MotionEvent firstDown;
        int actionMasked;
        MotionEvent lastDown;
        /** ドラッグで使用。直前のonDragMoveのX座標。 */
        float lastX;
        /** ドラッグで使用。直前のonDragMoveのY座標。 */
        float lastY;
        private Mode mode;
        /** 連続タップしたカウント。 */
        private int count;
        private VelocityTracker velocityTracker;

        public Detector(int id, @NonNull MotionEvent firstDown, @NonNull GestureListener gestureListener) {

            this.id = id;
            listener = gestureListener;
            this.firstDown = MotionEvent.obtain(firstDown);
            gesture = GestureDetector.this;
            mode = Mode.UP;
            velocityTracker = VelocityTracker.obtain();

        }


        void sendMessage(int what, long time) {
            //log.v("what:%d, time:%s", what, new Date(time).toString());
            handler.sendMessageAtTime(Message.obtain(handler, what, Detector.this), time);
        }


        void removeMessage(int what) {
            //log.v("what:%d", what);
            if ( handler.hasMessages(what, Detector.this) ) handler.removeMessages(what, Detector.this);
        }


        void onTouchEvent(MotionEvent e) {

            actionMasked = e.getActionMasked();
            if ( actionMasked == ACTION_DOWN ) {
                lastDown = MotionEvent.obtain(e);
                velocityTracker.clear();
            }
            velocityTracker.addMovement(e);

            final Mode newMode = mode.onTouchEvent(this, e);
            if ( newMode != null && mode != newMode ) {

                log.v("mode %s -> %s", mode, newMode);
                mode.end(this);
                mode = newMode;
                newMode.start(this);

            }

        }


        boolean isTracking() {
            return mode != Mode.END;
        }


        void dispose() {

            firstDown.recycle();
            if ( lastDown != null )
                lastDown.recycle();
            firstDown = lastDown = null;

            velocityTracker.recycle();
            velocityTracker = null;

            mode = Mode.END;
            removeMessage(TAP_CONFIRMED);
            removeMessage(LONG_TAP);

        }

    }


    private enum Mode {

        DOWN {
            @Override
            void start(Detector detector) {
                detector.removeMessage(TAP_CONFIRMED);
                detector.sendMessage(LONG_TAP, detector.lastDown.getEventTime() + LONG_PRESS_TIMEOUT);
            }


            @Override
            Mode onTouchEvent(final Detector d, final MotionEvent e) {

                final Logger log = d.log;
                final GestureDetector g = d.gesture;
                final MotionEvent lastDown = d.lastDown;
                if ( d.actionMasked == ACTION_MOVE ) {

                    // ずれが大きくなったら中止
                    final float dx = lastDown.getX() - e.getX();
                    final float dy = lastDown.getY() - e.getY();
                    final float slop = dx * dx + dy * dy;
                    if ( slop > g.touchSlopSquare ) {
                        log.v("cancel Tap slop:%f, touchSlopSquare:%d, id:%d", slop, g.touchSlopSquare, d.id);
                        d.removeMessage(LONG_TAP);
                        return DRAG;
                    }

                }
                if ( d.actionMasked == ACTION_UP || d.actionMasked == ACTION_POINTER_UP ) {

                    final MotionEvent firstDown = d.firstDown;
                    final float dx = firstDown.getX() - lastDown.getX();
                    final float dy = firstDown.getY() - lastDown.getY();
                    d.count++;
                    if ( d.count >= 2 && dx * dx + dy * dy > g.doubleTapSlopSquare ) {

                        log.v("EVENT: tap confirmed (連続タップ失敗) - count:%d, id:%d", d.count-1, d.id);
                        d.listener.onTapConfirmed(firstDown, d.count - 1);
                        d.count = 1;

                    }

                    log.v("EVENT: tap - count:%d, id:%d", d.count, d.id);
                    d.listener.onTap(lastDown, d.count);
                    d.sendMessage(TAP_CONFIRMED, e.getEventTime() + GestureDetector.DOUBLE_TAP_TIMEOUT);
                    d.removeMessage(LONG_TAP);
                    return UP;

                }
                return null;

            }
        },

        UP {
            @Override
            Mode onTouchEvent(Detector detector, MotionEvent e) {
                return detector.actionMasked == ACTION_DOWN ? DOWN : null;
            }
        },

        DRAG {
            @Override
            void start(Detector detector) {

                final MotionEvent firstDown = detector.firstDown;
                detector.log.v("EVENT: drag start - id:%d, count:%d", detector.id, detector.count);
                detector.listener.onDragStart(firstDown, detector.count);
                detector.lastX = firstDown.getX();
                detector.lastY = firstDown.getY();

            }


            @Override
            Mode onTouchEvent(final Detector detector, final MotionEvent e) {

                if ( detector.actionMasked == ACTION_MOVE ) {

                    float x = e.getX();
                    float y = e.getY();
                    final float dx = detector.lastX - x;
                    final float dy = detector.lastY - y;
                    //single.log.v("EVENT: drag move - dx:%f, dy:%f, id:%d", dx, dy, single.id);
                    detector.listener.onDragMove(detector.firstDown, e, dx, dy);
                    detector.lastX = x;
                    detector.lastY = y;

                }
                else if ( detector.actionMasked == ACTION_UP || detector.actionMasked == ACTION_POINTER_UP ) {

                    final VelocityTracker velocityTracker = detector.velocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, detector.gesture.maxFlingVelocity);
                    final float vx = velocityTracker.getXVelocity(detector.id);
                    final float vy = velocityTracker.getYVelocity(detector.id);
                    final float velocitySquare = vx * vx + vy * vy;
                    detector.log.v("id:%d, velocity:%f, vx:%f, vy:%f, minFlingVelocity:%f",
                            detector.id,
                            (float)Math.sqrt(velocitySquare),
                            vx, vy,
                            (float)Math.sqrt(detector.gesture.minFlingVelocitySquare));
                    if ( velocitySquare < detector.gesture.minFlingVelocitySquare ) {
                        detector.log.v("EVENT: drag end - id:%d", detector.id);
                        detector.listener.onDragEnd(e);
                    }
                    else {
                        detector.log.v("EVENT: fling - id:%d", detector.id, vx, vy);
                        detector.listener.onFling(detector.firstDown, e, vx, vy);
                    }

                    return END;

                }
                return null;

            }
        },

        END;

        /** モードが移ったときに呼ばれる。 */
        void start(Detector detector) {}
        /** 次のモードに移る前に呼ばれる。 */
        void end(Detector detector) {}
        /**
         * タッチイベントの処理。
         * @return 次のモード。nullならモードが移らない。
         */
        Mode onTouchEvent(Detector detector, MotionEvent e) {return null;}
    }


    public interface OnDownListener {
        /**
         * ゼスチャーの追跡を開始を通知する。ダブルタップの場合でも最初のdownのみを通知する。
         * @param e ACTION_DOWNイベント。
         * @param id ポインターID。
         * @return 続くゼスチャーイベントを受け取るリスナー。
         */
        GestureListener onDown(MotionEvent e, int id);
    }


    public static class GestureListener {

        /**
         * 連続タップが途切れた時に通知される。
         * @param e 最初のActionDownイベント。
         * @param count 連続タップした回数。
         */
        public void onTapConfirmed(MotionEvent e, int count) {}

        /**
         * 1回以上の連続タップ直後で通知される。
         * @param e 最初のタップのACTION_DOWNイベント。
         * @param count 連続タップした回数。
         */
        public void onTap(MotionEvent e, int count) {}

        /**
         * ドラッグが開始されたことが通知される。
         * @param e 最初のACTION_DOWNイベント。
         * @param count ドラッグの前にタップした回数。
         */
        public void onDragStart(MotionEvent e, int count) {}

        /**
         * ドラッグの最中にポインタが移動したときに通知される。
         * @param e1 最初のDOWNイベント。
         * @param e2 現在のイベント。
         * @param distanceX 前回の通知からのX軸の移動量。
         * @param distanceY 前回の通知からのY軸の移動量。
         */
        public void onDragMove(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {}

        /**
         * ドラッグが終了したときに通知される。フリックで終了したときはこのメソッドではなく
         * {@link #onFling(MotionEvent, MotionEvent, float, float)} が呼ばれる。
         * @param e UPイベント。
         */
        public void onDragEnd(MotionEvent e) {}

        /**
         * フリックしたたときに通知される。
         * @param e1 最初のDOWNイベント。
         * @param e2 UPイベント。
         * @param velocityX X方向の速度。
         * @param velocityY Y方向の速度。
         */
        public void onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {}


        /**
         * 長押ししたときに通知される。
         * @param e 最初のDOWNイベント。
         * @param count タッチして離した回数。最初のタッチで長押ししたときは0。
         */
        public void onLongPress(MotionEvent e, int count) {}

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

            final Detector detector = msg.obj instanceof Detector ? (Detector)msg.obj : null;
            if ( detector != null && detector.isTracking() ) {
                switch (msg.what) {

                case TAP_CONFIRMED:
                    log.v("EVENT: tap confirmed - count:%d, id:%d", detector.count, detector.id);
                    detector.listener.onTapConfirmed(detector.firstDown, detector.count);
                    g.removeSingle(detector.id);
                    break;

                case LONG_TAP:
                    log.v("EVENT: long press - count:%d, id:%d", detector.count, detector.id);
                    detector.listener.onLongPress(detector.firstDown, detector.count);
                    g.removeSingle(detector.id);
                    break;
                }

            }
        }

    }

}
