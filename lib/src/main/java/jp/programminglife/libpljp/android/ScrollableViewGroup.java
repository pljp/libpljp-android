/*
Copyright (c) 2013 ProgrammingLife.jp

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

/**
 * OverScrollerとEdgeEffectを持ったビューの基本クラス。
 * サブクラスはsetContentSize(), setViewPortMargin(), onFling(), onScroll()を適時呼び出す必要がある。
 */
public class ScrollableViewGroup extends View {

    final Logger log = Logger.get(ScrollableViewGroup.class);
    private final ScrollableViewGroup self = this;

    // 各種サイズ

    /** コンテンツのサイズ。paddingを含む。 */
    private final Rect contentRect = new Rect();

    // スクロール

    private OverScroller scroller;
    private EdgeEffect edgeEffectTop;
    private EdgeEffect edgeEffectBottom;
    private EdgeEffect edgeEffectLeft;
    private EdgeEffect edgeEffectRight;
    private float coeffX;
    private float coeffY;


    public ScrollableViewGroup(Context context) {

        super(context);
        init(context);
    }


    public ScrollableViewGroup(Context context, AttributeSet attrs) {

        super(context, attrs);
        init(context);
    }


    public ScrollableViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {

        super(context, attrs, defStyleAttr);
        init(context);
    }


    @TargetApi(VERSION_CODES.LOLLIPOP)
    public ScrollableViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }


    private void init(final Context context) {

        setWillNotDraw(false);
        scroller = new OverScroller(context);
        edgeEffectTop = new EdgeEffect(context);
        edgeEffectBottom = new EdgeEffect(context);
        edgeEffectLeft = new EdgeEffect(context);
        edgeEffectRight = new EdgeEffect(context);

    }


    public final void setContentSize(int width, int height) {
        contentRect.set(0, 0, width, height);
        correctScrollPosition(false);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int wMode = MeasureSpec.getMode(widthMeasureSpec);
        final int hMode = MeasureSpec.getMode(heightMeasureSpec);
        final int w = MeasureSpec.getSize(widthMeasureSpec);
        final int h = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(
                wMode == MeasureSpec.UNSPECIFIED ? View.MEASURED_SIZE_MASK : w,
                hMode == MeasureSpec.UNSPECIFIED ? View.MEASURED_SIZE_MASK : h);

    }


    @Override
    @CallSuper
    public boolean onTouchEvent(@NonNull MotionEvent event) {

        int actionMasked = event.getActionMasked();
        if ( actionMasked == MotionEvent.ACTION_UP ) {

            //log.v("pointer up");
            edgeEffectTop.onRelease();
            edgeEffectBottom.onRelease();
            edgeEffectLeft.onRelease();
            edgeEffectRight.onRelease();
            ViewCompat.postInvalidateOnAnimation(this);

        }
        else if ( actionMasked == MotionEvent.ACTION_DOWN ) {
            scroller.forceFinished(true);
        }
        return super.onTouchEvent(event);

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


    public void correctScrollPosition(boolean animation) {

        correctScrollPositionPoint.set(getScrollX(), getScrollY());
        if ( correctScrollPosition(correctScrollPositionPoint) ) {
            if ( animation ) {
                scroller.forceFinished(true);
                scroller.startScroll(getScrollX(), getScrollY(),
                        correctScrollPositionPoint.x - getScrollX(), correctScrollPositionPoint.y - getScrollY());
            }
            else
                scrollTo(correctScrollPositionPoint.x, correctScrollPositionPoint.y);
        }

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


    public int getViewPortWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }


    public int getViewPortHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }


    /**
     * フリックした方向にスクロールする。
     * @param velocityX フリックした方向のX成分。
     * @param velocityY フリックした方向のY成分。
     */
    protected final void onFling(float velocityX, float velocityY) {

        final int scrollX = getScrollX();
        final int scrollY = getScrollY();
        final int minX = computeMinX();
        final int minY = computeMinY();
        final int maxX = computeMaxX();
        final int maxY = computeMaxY();
        final float velocity = (float)Math.hypot(velocityX, velocityY);
        coeffX = Math.abs(-velocityX / velocity);
        coeffY = Math.abs(-velocityY / velocity);

        scroller.forceFinished(true);
        scroller.fling(scrollX, scrollY,
                Math.round(-velocityX), Math.round(-velocityY),
                minX, maxX,
                minY, maxY);
        //log.v("x:%d, y:%d, vx:%d, vy:%d maxX:%d, maxY:%d", scrollX, scrollY, velocityX, velocityY, maxX, maxY);
        ViewCompat.postInvalidateOnAnimation(this);

    }


    /**
     * スワイプしたときに呼び出す。
     */
    protected void onScroll(float pointerX, float pointerY, float distanceX, float distanceY) {

        final int sx = getScrollX() + Math.round(distanceX);
        final int sy = getScrollY() + Math.round(distanceY);
        final float w = getViewPortWidth();
        final float h = getViewPortHeight();
        final int minX = computeMinX();
        final int minY = computeMinY();
        final int maxX = computeMaxX();
        final int maxY = computeMaxY();
        //log.v("sx:%d, sy:%d, maxX:%d, maxY:%d pointerX:%d, pointerY:%d dx:%f, dy:%f", sx, sy, maxX, maxY, (int)pointerX, (int)pointerY, distanceX, distanceY);

        scrollTo(clamp(sx, minX, maxX), clamp(sy, minY, maxY));

        if ( sx < 0 )
            edgeEffectLeft.onPull(-distanceX / w);
        if ( sx > maxX ) {
            edgeEffectRight.onPull(distanceX / w);
            //log.v("pull right delta:%f, disp:%f", distanceX / w, pointerY / h);
        }
        if ( sy < 0 ) {
            edgeEffectTop.onPull(-distanceY / h);
            //log.v("pull top delta:%f, disp:%f", -distanceY / h, pointerX / w);
        }
        if ( sy > maxY ) {
            edgeEffectBottom.onPull(distanceY / h);
            //log.v("pull bottom delta:%f, disp:%f", distanceY / h, pointerX / w);
        }

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
    protected int computeHorizontalScrollExtent() {
        return getViewPortWidth();
    }


    @Override
    protected int computeVerticalScrollExtent() {
        return getViewPortHeight();
    }


    @Override
    @CallSuper
    public void draw(@NonNull Canvas canvas) {

        int w, h;
        float tx, ty;
        final int style = getScrollBarStyle();
        if ( style == SCROLLBARS_INSIDE_INSET || style == SCROLLBARS_INSIDE_OVERLAY ) {
            w = getViewPortWidth();
            h = getViewPortHeight();
            tx = getScrollX() + getPaddingLeft();
            ty = getScrollY() + getPaddingTop();
        }
        else {
            w = getWidth();
            h = getHeight();
            tx = getScrollX();
            ty = getScrollY();
        }
        //log.v("x:%d, y:%d, w:%d, h:%d", x, y, w, h);

        super.draw(canvas);

        boolean needsInvalidate = false;

        //log.v("" + (t?"t":"") + (b?"b":"") + (l?"l":"") + (r?"r":""));

        final boolean showHorizontal = contentRect.width() > w;
        final boolean showVertical = contentRect.height() > h;

        int c = canvas.save();
        canvas.translate(tx, ty);
        if ( showVertical && !edgeEffectTop.isFinished() ) {
            edgeEffectTop.setSize(w, h);
            needsInvalidate = edgeEffectTop.draw(canvas);
        }

        if ( showVertical && !edgeEffectBottom.isFinished() ) {
            canvas.rotate(180);
            canvas.translate(-w, -h);
            edgeEffectBottom.setSize(w, h);
            needsInvalidate = edgeEffectBottom.draw(canvas);
            canvas.translate(w, h);
            canvas.rotate(-180);
        }

        if ( showHorizontal && !edgeEffectLeft.isFinished() ) {
            canvas.rotate(270);
            canvas.translate(-h, 0);
            edgeEffectLeft.setSize(h, w);
            needsInvalidate |= edgeEffectLeft.draw(canvas);
            canvas.translate(h, 0);
            canvas.rotate(-270);
        }

        if ( showHorizontal && !edgeEffectRight.isFinished() ) {
            canvas.rotate(90);
            canvas.translate(0, -w);
            edgeEffectRight.setSize(h, w);
            needsInvalidate |= edgeEffectRight.draw(canvas);
            canvas.translate(0, w);
            canvas.rotate(-90);
        }

        canvas.restoreToCount(c);

        if ( needsInvalidate )
            ViewCompat.postInvalidateOnAnimation(this);

    }

}
