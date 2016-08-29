package jp.programminglife.libpljp.android;


import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

/**
 * OverScrollerとEdgeEffectを持ったビューの基本クラス。
 * サブクラスはsetContentSize(), setViewPortMargin(), onFling(), onScroll()を適時呼び出す必要がある。
 */
public class ScrollableView extends View {

    final Logger log = Logger.get(ScrollableView.class);
    private final ScrollableView self = this;

    // 各種サイズ

    /** コンテンツのサイズ。paddingを含む。 */
    private final Rect contentRect = new Rect();
    private int viewPortMarginLeft;
    private int viewPortMarginTop;
    private int viewPortMarginRight;
    private int viewPortMarginBottom;

    // スクロール

    private OverScroller scroller;
    private EdgeEffectCompat edgeEffectTop;
    private EdgeEffectCompat edgeEffectBottom;
    private EdgeEffectCompat edgeEffectLeft;
    private EdgeEffectCompat edgeEffectRight;
    private float coeffX;
    private float coeffY;


    public ScrollableView(Context context) {

        super(context);
        init(context);
    }


    public ScrollableView(Context context, AttributeSet attrs) {

        super(context, attrs);
        init(context);
    }


    public ScrollableView(Context context, AttributeSet attrs, int defStyleAttr) {

        super(context, attrs, defStyleAttr);
        init(context);
    }


    @TargetApi(VERSION_CODES.LOLLIPOP)
    public ScrollableView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }


    private void init(final Context context) {

        setWillNotDraw(false);
        scroller = new OverScroller(context);
        edgeEffectTop = new EdgeEffectCompat(context);
        edgeEffectBottom = new EdgeEffectCompat(context);
        edgeEffectLeft = new EdgeEffectCompat(context);
        edgeEffectRight = new EdgeEffectCompat(context);

    }


    public final void setContentSize(int width, int height) {
        contentRect.set(0, 0, width, height);
        correctScrollPosition();
    }


    public final void setViewPortMargin(int left, int top, int right, int bottom, boolean animation) {

        this.viewPortMarginLeft = left;
        this.viewPortMarginTop = top;
        this.viewPortMarginRight = right;
        this.viewPortMarginBottom = bottom;
        final int scrollX = getScrollX();
        final int scrollY = getScrollY();
        Point p = new Point(scrollX, scrollY);
        if ( correctScrollPosition(p) ) {
            if ( animation ) {
                scroller.forceFinished(true);
                scroller.startScroll(scrollX, scrollY, p.x - scrollX, p.y - scrollY);
            }
            else {
                scrollTo(p.x, p.y);
            }
        }

    }


    @Override
    @CallSuper
    public boolean onTouchEvent(@NonNull MotionEvent event) {

        int actionMasked = event.getActionMasked();
        if ( actionMasked == MotionEvent.ACTION_UP ) {

            //log.v("pointer up");
            boolean needsInvalidate = edgeEffectTop.onRelease();
            needsInvalidate |= edgeEffectBottom.onRelease();
            needsInvalidate |= edgeEffectLeft.onRelease();
            needsInvalidate |= edgeEffectRight.onRelease();
            if (needsInvalidate)
                ViewCompat.postInvalidateOnAnimation(this);

        }
        return false;

    }


    /**
     * スクロール位置がスクロール範囲を超えていたら修正する。
     * @param pointInout 現在のスクロール位置。
     * @return 修正が行われたらtrueを返す。
     */
    private boolean correctScrollPosition(Point pointInout) {

        final int x = clamp(pointInout.x, computeMinX(), computeMaxX());
        final int y = clamp(pointInout.y, computeMinY(), computeMaxY());
        if ( pointInout.x != x || pointInout.y != y ) {
            pointInout.set(x, y);
            return true;
        }
        return false;

    }


    private void correctScrollPosition() {

        correctScrollPositionPoint.set(getScrollX(), getScrollY());
        if ( correctScrollPosition(correctScrollPositionPoint) )
            scrollTo(correctScrollPositionPoint.x, correctScrollPositionPoint.y);

    }
    private Point correctScrollPositionPoint = new Point();


    private int clamp(int v, int min, int max) {

        if ( v < min ) return min;
        if ( v > max ) return max;
        return v;

    }


    private int computeMinX() {
        return 0;
    }


    private int computeMinY() {
        return 0;
    }


    private int computeMaxX() {

        final int viewPortWidth = getViewPortWidth();
        final int contentWidth = contentRect.width();
        return (contentWidth < viewPortWidth ? 0 : contentWidth - viewPortWidth);

    }


    private int computeMaxY() {

        final int viewPortHeight = getViewPortHeight();
        final int contentHeight = contentRect.height();
        return (contentHeight < viewPortHeight ? 0 : contentHeight - viewPortHeight);

    }


    private int getViewPortWidth() {
        return getWidth() - viewPortMarginLeft - viewPortMarginRight;
    }


    private int getViewPortHeight() {
        return getHeight() - viewPortMarginTop - viewPortMarginBottom;
    }


    /**
     * フリックした方向にスクロールする。
     * @param velocityX フリックした方向のX成分。
     * @param velocityY フリックした方向のY成分。
     */
    protected final void onFling(float velocityX, float velocityY) {

        int scrollX = getScrollX();
        int scrollY = getScrollY();
        scroller.forceFinished(true);
        int maxX = computeMaxX();
        int maxY = computeMaxY();
        float velocity = (float)Math.hypot(velocityX, velocityY);
        coeffX = Math.abs(-velocityX / velocity);
        coeffY = Math.abs(-velocityY / velocity);

        scroller.fling(scrollX, scrollY,
                Math.round(-velocityX), Math.round(-velocityY),
                contentRect.left, maxX,
                contentRect.top, maxY);
        //log.v("x:%d, y:%d, vx:%d, vy:%d maxX:%d, maxY:%d", scrollX, scrollY, velocityX, velocityY, maxX, maxY);
        ViewCompat.postInvalidateOnAnimation(this);

    }


    /**
     * スワイプしたときに呼び出す。
     */
    protected void onScroll(float pointerX, float pointerY, float distanceX, float distanceY) {

        int sx = getScrollX() + Math.round(distanceX);
        int sy = getScrollY() + Math.round(distanceY);
        float w = getWidth();
        float h = getViewPortHeight();
        int maxX = computeMaxX();
        int maxY = computeMaxY();
        //log.v("sx:%d, sy:%d, maxX:%d, maxY:%d pointerX:%d, pointerY:%d dx:%f, dy:%f", sx, sy, maxX, maxY, (int)pointerX, (int)pointerY, distanceX, distanceY);

        scrollTo(clamp(sx, contentRect.left, maxX), clamp(sy, contentRect.top, maxY));

        boolean needsInvalidate = sx < 0 && edgeEffectLeft.onPull(-distanceX / w, (h - pointerY) / h);
        if ( sx > maxX ) {
            needsInvalidate |= edgeEffectRight.onPull(distanceX / w, pointerY / h);
            //log.v("pull right delta:%f, disp:%f", distanceX / w, pointerY / h);
        }
        if ( sy < 0 ) {
            needsInvalidate |= edgeEffectTop.onPull(-distanceY / h, pointerX / w);
            //log.v("pull top delta:%f, disp:%f", -distanceY / h, pointerX / w);
        }
        if ( sy > maxY ) {
            needsInvalidate |= edgeEffectBottom.onPull(distanceY / h, pointerX / w);
            //log.v("pull bottom delta:%f, disp:%f", distanceY / h, pointerX / w);
        }

        if ( needsInvalidate )
            ViewCompat.postInvalidateOnAnimation(self);

    }


    /**
     * コンテンツ領域のrectの矩形範囲の位置までスクロールする。
     * @param rect スクロールして画面に表示する矩形範囲。
     * @param hAlign rectを表示する画面の位置。0で左、1.0で右に配置する。
     * @param vAlign rectを表示する画面の位置。0で上、1.0で下に配置する。
     * @param animation スクロールアニメーションするならtrue。
     */
    public final void scrollTo(@NonNull RectF rect, float hAlign, float vAlign, boolean animation) {

        scroller.forceFinished(true);
        final int x = getScrollX();
        final int y = getScrollY();
        Point p = new Point(Math.round(rect.left - (getViewPortWidth() - rect.width()) * hAlign),
                Math.round(rect.top - (getViewPortHeight() - rect.height()) * vAlign));
        correctScrollPosition(p);
        if ( animation ) {
            scroller.startScroll(x, y, p.x - x, p.y - y);
            ViewCompat.postInvalidateOnAnimation(this);
        }
        else
            scrollTo(p.x, p.y);

    }


    @Override
    @CallSuper
    public void computeScroll() {

        if ( !scroller.isFinished() && scroller.computeScrollOffset() ) {

            final int x = getScrollX();
            final int y = getScrollY();
            final int newX = scroller.getCurrX();
            final int newY = scroller.getCurrY();

            final float v = VERSION.SDK_INT >= 14 ? getCurrentVelocity14() : 0;
            computeEdgeEffect(x, y, newX, newY, v);

            if ( x != newX || y != newY )
                scrollTo(newX, newY);

        }

    }


    @TargetApi(14)
    private float getCurrentVelocity14() {
        return scroller.getCurrVelocity();
    }


    private void computeEdgeEffect(int x, int y, int newX, int newY, float velocity) {

        final int maxX = computeMaxX();
        final int maxY = computeMaxY();
        final int vx = (int) (velocity * coeffX);
        final int vy = (int) (velocity * coeffY);
        boolean needsInvalidate = false;

        if ( x != newX ) {

            if ( newX <= 0 ) {
                if ( x > 0 ) {
                    log.v("absorb left vx=%d", vx);
                    edgeEffectLeft.onAbsorb(vx);
                    needsInvalidate = true;
                }
                //newX = 0;
            }
            if ( newX >= maxX ) {
                if ( x < maxX ) {
                    log.v("absorb right vx=%d", vx);
                    edgeEffectRight.onAbsorb(vx);
                    needsInvalidate = true;
                }
                //newX = maxX;
            }

            //log.v("x:%d, y:%d, newX:%d, newY:%d", x, y, newX, newY);
            //log.v("scroll x:%d, y:%d, v:%f over:%b", newX, newY, scroller.getCurrVelocity(), scroller.isOverScrolled());
        }
        if ( y != newY ) {

            if ( newY <= 0 ) {
                if ( y > 0 ) {
                    log.v("absorb top vy=%d", vy);
                    edgeEffectTop.onAbsorb(vy);
                    needsInvalidate = true;
                }
                //newY = 0;
            }
            if ( newY >= maxY ) {
                if ( y < maxY ) {
                    log.v("absorb bottom vy=%d", vy);
                    edgeEffectBottom.onAbsorb(vy);
                    needsInvalidate = true;
                }
                //newY = maxY;
            }

            //log.v("x:%d, y:%d, newX:%d, newY:%d", x, y, newX, newY);
            //log.v("scroll x:%d, y:%d, v:%f over:%b", newX, newY, scroller.getCurrVelocity(), scroller.isOverScrolled());
        }

        if ( needsInvalidate )
            ViewCompat.postInvalidateOnAnimation(this);

    }


    @Override
    protected int computeHorizontalScrollRange() {
        return contentRect.width();
    }


    @Override
    protected int computeVerticalScrollRange() {
        return contentRect.height();
    }


    @Override
    @CallSuper
    public void draw(@NonNull Canvas canvas) {

        int w = getViewPortWidth();
        int h = getViewPortHeight();
        //log.v("x:%d, y:%d, w:%d, h:%d", x, y, w, h);

        int c1 = canvas.save();
        canvas.translate(viewPortMarginLeft, viewPortMarginTop);
        final int clipX = getScrollX();
        final int clipY = getScrollY();
        canvas.clipRect(clipX, clipY, clipX + w, clipY + h);

        super.draw(canvas);

        boolean needsInvalidate = false;

        //log.v("" + (t?"t":"") + (b?"b":"") + (l?"l":"") + (r?"r":""));

        canvas.translate(getScrollX(), getScrollY());
        if ( !edgeEffectTop.isFinished() ) {
            int c2 = canvas.save();
            canvas.translate(0, 0);
            edgeEffectTop.setSize(w, h);
            needsInvalidate = edgeEffectTop.draw(canvas);
            canvas.restoreToCount(c2);
        }

        if ( !edgeEffectBottom.isFinished() ) {
            int c2 = canvas.save();
            canvas.rotate(180);
            canvas.translate(-w, -h);
            edgeEffectBottom.setSize(w, h);
            needsInvalidate = edgeEffectBottom.draw(canvas);
            canvas.restoreToCount(c2);
        }

        if ( !edgeEffectLeft.isFinished() ) {
            int c2 = canvas.save();
            canvas.rotate(270);
            canvas.translate(-h, 0);
            edgeEffectLeft.setSize(h, w);
            needsInvalidate |= edgeEffectLeft.draw(canvas);
            canvas.restoreToCount(c2);
        }

        if ( !edgeEffectRight.isFinished() ) {
            int c2 = canvas.save();
            canvas.rotate(90);
            canvas.translate(0, -w);
            edgeEffectRight.setSize(h, w);
            needsInvalidate |= edgeEffectRight.draw(canvas);
            canvas.restoreToCount(c2);
        }

        canvas.restoreToCount(c1);

        if ( needsInvalidate )
            ViewCompat.postInvalidateOnAnimation(this);

    }

}
