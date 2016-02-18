package jp.programminglife.libpljp.android;


import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

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
    /** 単体ポインタージェスチャーの最大数。スケールイベントのポインター数には影響しない。 */
    public int maxPointers;
    final Logger log = Logger.create(getClass());
    final int doubleTapSlopSquare;
    final int touchSlopSquare;
    final float maxFlingVelocity;
    final float minFlingVelocitySquare;
    private ScaleGestureDetector scaleGestureDetector;
    private final SparseArray<SinglePointerDetector> detectors = new SparseArray<>();
    private final Handler_ handler;
    private OnDownListener onDownListener;


    public GestureDetector(@NonNull Context context, @NonNull OnDownListener onDownListener) {
        this(context, onDownListener, null);
    }


    public GestureDetector(@NonNull Context context, @NonNull OnDownListener onDownListener, ScaleGestureListener scaleGestureListener) {

        this.onDownListener = onDownListener;
        if ( scaleGestureListener != null )
            scaleGestureDetector = new ScaleGestureDetector(scaleGestureListener);
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
        SinglePointerDetector detector = detectors.get(id);
        //log.v("pointer id:%d, action:%d, single:%s, x:%.1f, y:%.1f, index:%d, p.count:%d", id, actionMasked, single, e.getX(index), e.getY(index), index, e.getPointerCount());

        if ( scaleGestureDetector != null && scaleGestureDetector.onTouchEvent(e, id) ) {

            clearSingleDetectors();
            return true;

        }

        if ( (actionMasked == ACTION_DOWN || actionMasked == ACTION_POINTER_DOWN ) && detector == null ) {

            if ( detectors.size() < maxPointers ) {
                log.v("EVENT: down - id:%d", id);
                final GestureListener gestureListener = onDownListener.onDown(e, id);
                log.v("new Single id:%d", id);
                detector = new SinglePointerDetector(id, e, gestureListener);
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


    /**
     * ポインター数を返す。
     */
    public int getPointerCount() {
        return detectors.size();
    }


    private void clearSingleDetectors() {

        //log.v();
        int n = detectors.size();
        if ( n == 0 ) return;
        for (int i = 0; i < n; i++)
            detectors.get(detectors.keyAt(i)).dispose();
        detectors.clear();

    }


    private void removeSingle(int id) {

        log.v("remove Single id:%d", id);
        final SinglePointerDetector detector = detectors.get(id);
        if ( detector != null ) {
            detector.dispose();
            detectors.remove(id);
        }

    }


    final class SinglePointerDetector {

        final Logger log = Logger.create(getClass());
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

        public SinglePointerDetector(int id, @NonNull MotionEvent firstDown, @NonNull GestureListener gestureListener) {

            this.id = id;
            listener = gestureListener;
            this.firstDown = MotionEvent.obtain(firstDown);
            gesture = GestureDetector.this;
            mode = Mode.UP;
            velocityTracker = VelocityTracker.obtain();

        }


        void sendMessage(int what, long time) {
            //log.v("what:%d, time:%s", what, new Date(time).toString());
            handler.sendMessageAtTime(Message.obtain(handler, what, SinglePointerDetector.this), time);
        }


        void removeMessage(int what) {
            //log.v("what:%d", what);
            if ( handler.hasMessages(what, SinglePointerDetector.this) ) handler.removeMessages(what, SinglePointerDetector.this);
        }


        void onTouchEvent(MotionEvent e) {

            actionMasked = e.getActionMasked();
            if ( actionMasked == ACTION_DOWN ) {
                lastDown = MotionEvent.obtain(e);
                velocityTracker.clear();
            }
            velocityTracker.addMovement(e);
            changeMode(mode.onTouchEvent(this, e));

        }


        private void changeMode(@Nullable Mode newMode) {

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

            if ( mode != Mode.END ) {
                mode.cancel(this);
                mode = Mode.END;
            }
            listener.onRelease();
            firstDown.recycle();
            if ( lastDown != null )
                lastDown.recycle();
            firstDown = lastDown = null;

            velocityTracker.recycle();
            velocityTracker = null;

            removeMessage(TAP_CONFIRMED);
            removeMessage(LONG_TAP);

        }

    }


    private enum Mode {

        DOWN {
            @Override
            void start(SinglePointerDetector detector) {
                detector.removeMessage(TAP_CONFIRMED);
                detector.sendMessage(LONG_TAP, detector.lastDown.getEventTime() + LONG_PRESS_TIMEOUT);
            }


            @Override
            Mode onTouchEvent(final SinglePointerDetector d, final MotionEvent e) {

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
            Mode onTouchEvent(SinglePointerDetector detector, MotionEvent e) {
                return detector.actionMasked == ACTION_DOWN ? DOWN : null;
            }
        },

        DRAG {
            @Override
            void start(SinglePointerDetector detector) {

                final MotionEvent firstDown = detector.firstDown;
                log.v("EVENT: drag start - id:%d, count:%d", detector.id, detector.count);
                detector.listener.onDragStart(firstDown, detector.count);
                detector.lastX = firstDown.getX();
                detector.lastY = firstDown.getY();

            }


            @Override
            Mode onTouchEvent(final SinglePointerDetector detector, final MotionEvent e) {

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
                    log.v("id:%d, velocity:%f, vx:%f, vy:%f, minFlingVelocity:%f",
                            detector.id,
                            (float)Math.sqrt(velocitySquare),
                            vx, vy,
                            (float)Math.sqrt(detector.gesture.minFlingVelocitySquare));
                    if ( velocitySquare < detector.gesture.minFlingVelocitySquare ) {
                        log.v("EVENT: drag end - id:%d", detector.id);
                        detector.listener.onDragEnd(e);
                    }
                    else {
                        log.v("EVENT: fling - id:%d", detector.id, vx, vy);
                        detector.listener.onFling(detector.firstDown, e, vx, vy);
                    }

                    return END;

                }
                return null;

            }


            @Override
            void cancel(SinglePointerDetector detector) {
                log.v("cancel drag");
                detector.listener.onDragCancel(detector.firstDown);
            }
        },

        END;

        final Logger log = Logger.create(Mode.class);
        /** モードが移ったときに呼ばれる。 */
        void start(SinglePointerDetector detector) {}
        /** 次のモードに移る前に呼ばれる。 */
        void end(SinglePointerDetector detector) {}
        /** モードがキャンセルされた(次のモードに移らずに終了した)ときに呼ばれる。 */
        void cancel(SinglePointerDetector detector) {}
        /**
         * タッチイベントの処理。
         * @return 次のモード。nullならモードが移らない。
         */
        Mode onTouchEvent(SinglePointerDetector detector, MotionEvent e) {return null;}
    }


    public final class ScaleGestureDetector {

        private final Logger log = Logger.create(ScaleGestureDetector.class);
        private final ScaleGestureListener listener;
        private final SparseArray<PointF> curPoints = new SparseArray<>(2);
        private final SparseArray<PointF> prevPoints = new SparseArray<>(2);


        public ScaleGestureDetector(ScaleGestureListener listener) {
            this.listener = listener;
        }


        public boolean onTouchEvent(MotionEvent e, int id) {

            final int actionMasked = e.getActionMasked();
            boolean ret = false;
            //log.v("MotionEvent id=%d, xy=(%.1f, %.1f)", id, e.getX(), e.getY());
            if ( actionMasked == ACTION_DOWN || actionMasked == ACTION_POINTER_DOWN ) {

                // 3つめ以降のポインターは追加せずにイベントを消費する。
                if ( curPoints.size() == 2 ) {
                    ret = true;
                    log.v("3つ目のポインター id=%d", id);
                }
                // ポインターが0か1個のときは追加する。
                else {
                    curPoints.put(id, new PointF());
                    log.v("%dつめのポインター id=%d", curPoints.size(), id);
                    updatePoints(e);
                    // ポインターが2つになったらリスナーに通知する。
                    if ( curPoints.size() == 2 ) {
                        updatePoints(e); // prevPointsにcurPointsをコピーするために呼び出す。
                        listener.onScaleBegin(this);
                        ret = true;
                    }
                }

            }
            else if ( actionMasked == ACTION_UP || actionMasked == ACTION_POINTER_UP ) {

                log.v("UP id=%d", id);
                updatePoints(e);
                // ポインターの削除に成功して1つになったら通知する。
                final boolean removed = curPoints.indexOfKey(id) >= 0;
                curPoints.delete(id);
                if ( removed && curPoints.size() == 1 ) {
                    log.v("スケールジェスチャー終了");
                    listener.onScaleEnd(this);
                    // スケールジェスチャーを繰り返しても同じIDが使われるのでcurPoints, prevPointsが大きくなることはないが
                    // もし増大したならクリアする
                    if ( curPoints.size() > 2 )
                        curPoints.clear();
                    if ( prevPoints.size() > 2 )
                        prevPoints.clear();
                    ret = true;
                }

            }
            else if ( actionMasked == ACTION_MOVE ) {

                updatePoints(e);
                if ( curPoints.size() == 2 ) {
                    listener.onScale(this);
                    ret = true;
                }

            }

            return ret;

        }


        private void updatePoints(MotionEvent e) {

            // curからprevにポイントをコピーする

            for (int i = 0, n = curPoints.size(); i < n; i++) {

                final int key = curPoints.keyAt(i);
                final PointF value = curPoints.valueAt(i);
                if ( prevPoints.indexOfKey(key) < 0 )
                    prevPoints.put(key, new PointF(value.x, value.y));
                else
                    prevPoints.get(key).set(value);

            }

            // curを更新する

            for (int i=0, n=e.getPointerCount(); i<n; i++) {

                final int id = e.getPointerId(i);
                final PointF p = curPoints.get(id);
                if ( p != null ) {
                    p.set(e.getX(i), e.getY(i));
                    //log.v("point%d id=%d, %s", i, id, p);
                }

            }

            //log.v("p1=%s, p2=%s", curPoints[0].toString(), curPoints[1].toString());

        }


        public float getFocusX() {
            if ( curPoints.size() < 2 ) return 0;
            final PointF p1 = curPoints.valueAt(0);
            final PointF p2 = curPoints.valueAt(1);
            return (p1.x + p2.x) / 2.f;
        }


        public float getFocusY() {
            if ( curPoints.size() < 2 ) return 0;
            final PointF p1 = curPoints.valueAt(0);
            final PointF p2 = curPoints.valueAt(1);
            return (p1.y + p2.y) / 2.f;
        }


        public float getPreviousFocusX() {
            if ( prevPoints.size() < 2 ) return 0;
            final PointF p1 = prevPoints.valueAt(0);
            final PointF p2 = prevPoints.valueAt(1);
            return (p1.x + p2.x) / 2.f;
        }


        public float getPreviousFocusY() {
            if ( prevPoints.size() < 2 ) return 0;
            final PointF p1 = prevPoints.valueAt(0);
            final PointF p2 = prevPoints.valueAt(1);
            return (p1.y + p2.y) / 2.f;
        }


        public float getSpan() {
            if ( curPoints.size() < 2 ) return 1.f;
            final PointF p1 = curPoints.valueAt(0);
            final PointF p2 = curPoints.valueAt(1);
            return PointF.length(p2.x - p1.x, p2.y - p1.y);
        }


        public float getPreviousSpan() {
            if ( prevPoints.size() < 2 ) return 1.f;
            final PointF p1 = prevPoints.valueAt(0);
            final PointF p2 = prevPoints.valueAt(1);
            return PointF.length(p2.x - p1.x, p2.y - p1.y);
        }

    }


    public interface OnDownListener {
        /**
         * ジェスチャーの追跡を開始を通知する。ダブルタップの場合でも最初のdownのみを通知する。
         * @param e ACTION_DOWNイベント。
         * @param id ポインターID。
         * @return 続くジェスチャーイベントを受け取るリスナー。
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
         * ドラッグがキャンセルされたときに通知ばれる。
         * @param e 最初のDOWNイベント。
         */
        public void onDragCancel(MotionEvent e) {}


        /**
         * 長押ししたときに通知される。
         * @param e 最初のDOWNイベント。
         * @param count タッチして離した回数。最初のタッチで長押ししたときは0。
         */
        public void onLongPress(MotionEvent e, int count) {}


        /**
         * リスナーが解放されるときに呼ばれる。
         */
        public void onRelease() {}

    }


    public static class ScaleGestureListener {

        public void onScaleBegin(ScaleGestureDetector detector) {}

        public void onScale(ScaleGestureDetector detector) {}

        public void onScaleEnd(ScaleGestureDetector detector) {}

    }


    private static final class Handler_ extends Handler {

        private final Logger log = Logger.create(getClass());
        private WeakReference<GestureDetector> gesture;


        public Handler_(GestureDetector gesture) {

            this.gesture = new WeakReference<>(gesture);
        }


        @Override
        public void handleMessage(Message msg) {

            final GestureDetector g = gesture.get();
            if ( g == null ) return;

            final SinglePointerDetector detector = msg.obj instanceof SinglePointerDetector ? (SinglePointerDetector)msg.obj : null;
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
