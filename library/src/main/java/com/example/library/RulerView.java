package com.example.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import java.math.BigDecimal;

public class RulerView extends View {

    private int indicatorColor;
    private int scaleColor;
    private int numberColor;
    private float textSize;
    private int minNumber;
    private int maxNumber;
    private float currentNumber;

    /**
     * 滑动阈值
     */
    private final int TOUCH_SLOP;
    /**
     * 惯性滑动最小、最大速度
     */
    private final int MIN_FLING_VELOCITY;
    private final int MAX_FLING_VELOCITY;

    //刻度间距
    private float scaleGapWidth;
    //短刻度长度、宽度
    private float shortScaleLength;
    private float shortScaleWidth;
    //长刻度长度、宽度
    private float longScaleLength;
    private float longScaleWidth;

    private Scroller scroller;
    private VelocityTracker tracker;
    //刻度尺左边距离屏幕中间的距离
    private float sLeftToSMDistance, totalScaleLength;

    private float mDownX, mLastX, mLastY;
    private boolean isMoved;

    private Paint scalePaint;
    private Paint numberPaint;
    private OnValueChangeListener listener;

    public RulerView(Context context) {
        this(context, null);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 初始化final常量，必须在构造中赋初值
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        TOUCH_SLOP = viewConfiguration.getScaledTouchSlop();
        MIN_FLING_VELOCITY = viewConfiguration.getScaledMinimumFlingVelocity();
        MAX_FLING_VELOCITY = viewConfiguration.getScaledMaximumFlingVelocity();
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RulerView);
        indicatorColor = array.getColor(R.styleable.RulerView_indicatorColor, Color.BLUE);
        scaleColor = array.getColor(R.styleable.RulerView_scaleColor, Color.GRAY);
        numberColor = array.getColor(R.styleable.RulerView_numberColor, Color.BLACK);
        textSize = array.getDimension(R.styleable.RulerView_textSize, sp2px(14));
        minNumber = array.getInt(R.styleable.RulerView_minNumber, 10) * 10;
        maxNumber = array.getInt(R.styleable.RulerView_maxNumber, 50) * 10;
        currentNumber = array.getFloat(R.styleable.RulerView_currentNumber, 50);
        array.recycle();
        init();
    }


    private void init() {
        scaleGapWidth = dp2px(10);
        shortScaleLength = dp2px(12);
        longScaleLength = dp2px(18);
        shortScaleWidth = dp2px(1.5f);
        longScaleWidth = dp2px(2);
        scalePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scalePaint.setColor(scaleColor);
        scalePaint.setStrokeCap(Paint.Cap.ROUND);
        numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numberPaint.setStrokeWidth(longScaleWidth);
        numberPaint.setColor(numberColor);
        numberPaint.setTextSize(textSize);
        sLeftToSMDistance = (maxNumber - minNumber) / 2 * scaleGapWidth;
        totalScaleLength = (maxNumber - minNumber) * scaleGapWidth;
        scroller = new Scroller(getContext());
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawScale(canvas);
        drawIndicatorValue(canvas);
    }

    private void drawScale(Canvas canvas) {
        //两种情况：当起始点在屏幕外时，算出起始点的坐标值，并且开始画刻度；当起始点在屏幕内时，算出起始点距离屏幕左侧的距离，
        //以及数值，开始画刻度  无论哪种情况都要起点的坐标以及数值
        scalePaint.setColor(scaleColor);
        scalePaint.setStrokeWidth(shortScaleWidth);
        float halfScreenWidth = getMeasuredWidth() / 2;
        canvas.drawLine(0, 0, getMeasuredWidth(), 0, scalePaint);
        float startDrawX = 0.0f;
        int startNum = 0;
        if (sLeftToSMDistance >= halfScreenWidth) {
            //当起始点在屏幕外时，算出起始点的数值
            startNum = (int) ((sLeftToSMDistance - halfScreenWidth) / scaleGapWidth + minNumber) - 1;
            startDrawX = -(sLeftToSMDistance - halfScreenWidth) % scaleGapWidth - scaleGapWidth;
        } else {
            //当起始点在屏幕内时，算出起始点距离屏幕左侧的距离，以及数值，开始画刻度
            startDrawX = halfScreenWidth - sLeftToSMDistance;
            startNum = minNumber;
        }
        while (startDrawX < getMeasuredWidth() + scaleGapWidth) {
            if (startNum > maxNumber) {
                break;
            }
            if (startNum % 10 == 0) {
                scalePaint.setStrokeWidth(longScaleWidth);
                canvas.drawLine(startDrawX, 0, startDrawX, longScaleLength, scalePaint);
                String text = String.valueOf(startNum / 10);
                Rect rect = new Rect();
                numberPaint.getTextBounds(text, 0, text.length(), rect);
                canvas.drawText(text, startDrawX - rect.width() / 2, longScaleLength + dp2px(8) + rect.height(), numberPaint);
            } else {
                scalePaint.setStrokeWidth(shortScaleWidth);
                canvas.drawLine(startDrawX, 0, startDrawX, shortScaleLength, scalePaint);
            }
            startDrawX = startDrawX + scaleGapWidth;
            startNum = startNum + 1;
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        final float x = (int) event.getX();
        final float y = (int) event.getY();
        if (tracker == null) {
            tracker = VelocityTracker.obtain();
        }
        tracker.addMovement(event);
        switch (action) {

            case MotionEvent.ACTION_DOWN:
                mDownX = x;
                isMoved = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final float dx = x - mLastX;

                // 判断是否已经滑动
                if (!isMoved) {
                    final float dy = y - mLastY;
                    // 滑动的触发条件：水平滑动大于垂直滑动；滑动距离大于阈值
                    if (Math.abs(dx) < Math.abs(dy) || Math.abs(x - mDownX) < TOUCH_SLOP) {
                        break;
                    }
                    isMoved = true;
                }

                sLeftToSMDistance += -dx;
                if (sLeftToSMDistance < 0) {
                    sLeftToSMDistance = 0;
                }
                if (sLeftToSMDistance > totalScaleLength) {
                    sLeftToSMDistance = totalScaleLength;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (sLeftToSMDistance < 0) {
                    sLeftToSMDistance = 0;
                }
                if (sLeftToSMDistance > totalScaleLength) {
                    sLeftToSMDistance = totalScaleLength;
                }
                tracker.computeCurrentVelocity(1000, MAX_FLING_VELOCITY);
                int xVelocity = (int) tracker.getXVelocity();
                if (Math.abs(xVelocity) >= MIN_FLING_VELOCITY) {
                    scroller.fling((int) sLeftToSMDistance, 0, -xVelocity, 0, 0,
                            (int) totalScaleLength, 0, 0);
                    invalidate();
                } else {
                    scrollToNearScale();
                }

                break;
            default:
                break;
        }
        mLastX = x;
        mLastY = y;
        return true;
    }


    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            if (scroller.getCurrX() != scroller.getFinalX()) {
                sLeftToSMDistance = scroller.getCurrX();
                invalidate();
            } else {
                scrollToNearScale();
            }
        }
    }


    //抬起手指后滑向最近的刻度值
    private void scrollToNearScale() {
        float remainDistance = sLeftToSMDistance % scaleGapWidth;
        if (remainDistance >= scaleGapWidth / 2) {
            sLeftToSMDistance = sLeftToSMDistance + scaleGapWidth - remainDistance;
        } else {
            sLeftToSMDistance = sLeftToSMDistance - remainDistance;
        }
        invalidate();
    }


    //画出中间的指示线
    private void drawIndicatorValue(Canvas canvas) {
        scalePaint.setColor(indicatorColor);
        scalePaint.setStrokeWidth(longScaleWidth);
        float startX = getMeasuredWidth() / 2;
        canvas.drawLine(startX, 0, startX, longScaleLength + dp2px(3), scalePaint);
        currentNumber= (sLeftToSMDistance/scaleGapWidth+minNumber)/10;
        BigDecimal decimal=new BigDecimal(currentNumber);
        String value=decimal.setScale(1,BigDecimal.ROUND_HALF_UP).toString();
        if (listener!=null){
            listener.onValueChange(value);
        }
    }


    //设置选中的值
    public void setCurrentValue(float currentValue){
        this.currentNumber= currentValue*10;
        if (currentNumber<minNumber||currentNumber>maxNumber){
            throw new IllegalArgumentException(String.format("The currentValue of %f is out of range: [%f, %f]",
                    currentValue, (float)minNumber/10, (float)maxNumber/10));
        }
        if (scroller.isFinished()){
            scroller.forceFinished(true);
        }
        float newDistance=(currentNumber-minNumber)*scaleGapWidth;
        int dx = (int) (newDistance - sLeftToSMDistance);
        // 最大2000ms
        final int duration = dx * 2000 / (int)totalScaleLength;
        // 滑动到目标值
        scroller.startScroll((int) sLeftToSMDistance, 0, dx, duration);
        postInvalidate();

    }



    public void setListener(OnValueChangeListener listener) {
        this.listener = listener;
    }

    public interface OnValueChangeListener{
        void onValueChange(String value);
    }




    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private int sp2px(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }


}
