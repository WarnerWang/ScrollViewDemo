package com.hx.scrollviewdemo.CardHelper;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.ScrollView;

public class BounceScrollView extends ScrollView {

    private static final String TAG = "BounceScrollView";

    public interface OnBounceChanged{
        void onBounceChanged(boolean isMoved, int offset);
    }

    public OnBounceChanged onBounceChanged;

    // 移动因子, 是一个百分比, 比方手指移动了100px, 那么View就仅仅移动50px
    // 目的是达到一个延迟的效果
    private static final float MOVE_FACTOR = 0.5f;


    // 松开手指后, 界面回到正常位置须要的动画时间
    private static final int ANIM_TIME = 300;


    // ScrollView的子View， 也是ScrollView的唯一一个子View
    private View contentView;


    // 手指按下时的Y值, 用于在移动时计算移动距离
    // 假设按下时不能上拉和下拉。 会在手指移动时更新为当前手指的Y值
    private float startMoveY;
    private float startMoveX;


    // 用于记录正常的布局位置
    private Rect originalRect = new Rect();


    // 手指按下时记录能否够继续下拉
    private boolean canPullDown = false;


    // 手指按下时记录能否够继续上拉
    private boolean canPullUp = false;


    // 在手指滑动的过程中记录是否移动了布局
    private boolean isMoved = false;

    // 是否需要下拉回弹
    private boolean needPullDown = true;

    // 是否需要上拉回弹
    private boolean needPullUp = true;

    public BounceScrollView(Context context) {
        super(context);
        init(context);
    }

    public BounceScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BounceScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BounceScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public void setOnBounceChanged(OnBounceChanged onBounceChanged) {
        this.onBounceChanged = onBounceChanged;
    }

    private void init(Context context){
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 0) {
            contentView = getChildAt(0);
        }
    }

    // 指定此视图可以垂直滚动
    @Override
    public boolean canScrollVertically(int direction) {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);


        if (contentView == null)
            return;


        // ScrollView中的唯一子控件的位置信息, 这个位置信息在整个控件的生命周期中保持不变
        originalRect.set(contentView.getLeft(), contentView.getTop(), contentView.getRight(), contentView.getBottom());
    }


    /**
     * 在触摸事件中, 处理上拉和下拉的逻辑
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {


        if (contentView == null) {
            return super.dispatchTouchEvent(ev);
        }


        // 手指是否移动到了当前ScrollView控件之外
        boolean isTouchOutOfScrollView = ev.getY() >= this.getHeight() || ev.getY() <= 0;


        if (isTouchOutOfScrollView) { // 假设移动到了当前ScrollView控件之外
            // 计算手指移动的距离
            float nowY = ev.getY();
            int deltaY = (int) (nowY - startMoveY);
            // 计算偏移量
            int offset = (int) (deltaY * MOVE_FACTOR);
            if (isMoved) // 假设当前contentView已经被移动, 首先把布局移到原位置, 然后消费点这个事件
                boundBack(isScrollVertically(ev), offset);
            return true;
        }


        int action = ev.getAction();


        switch (action) {
            case MotionEvent.ACTION_DOWN:


                // 推断能否够上拉和下拉
                canPullDown = isCanPullDown();
                canPullUp = isCanPullUp();


                // 记录按下时的Y值
                startMoveY = ev.getY();
                startMoveX = ev.getX();


                break;


            case MotionEvent.ACTION_UP:{

                // 计算手指移动的距离
                float nowY = ev.getY();
                int deltaY = (int) (nowY - startMoveY);
                // 计算偏移量
                int offset = (int) (deltaY * MOVE_FACTOR);
                boundBack(offset != 0&&isScrollVertically(ev), offset);
            }

                break;
            case MotionEvent.ACTION_MOVE:{

                // 在移动的过程中， 既没有滚动到能够上拉的程度。 也没有滚动到能够下拉的程度
                if (!canPullDown && !canPullUp) {
                    startMoveY = ev.getY();
                    canPullDown = isCanPullDown();
                    canPullUp = isCanPullUp();


                    break;
                }


                // 计算手指移动的距离
                float nowY = ev.getY();
                int deltaY = (int) (nowY - startMoveY);


                // 是否应该移动布局
                boolean shouldMove = (canPullDown && deltaY > 0) // 能够下拉， 而且手指向下移动
                        || (canPullUp && deltaY < 0) // 能够上拉。 而且手指向上移动
                        || (canPullUp && canPullDown); // 既能够上拉也能够下拉（这样的情况出如今ScrollView包裹的控件比ScrollView还小）


                if (shouldMove) {
                    // 计算偏移量
                    int offset = (int) (deltaY * MOVE_FACTOR);


                    // 随着手指的移动而移动布局
                    contentView.layout(originalRect.left, originalRect.top + offset, originalRect.right, originalRect.bottom + offset);


                    isMoved = true; // 记录移动了布局
                    if (isScrollVertically(ev) && onBounceChanged != null) {
                        onBounceChanged.onBounceChanged(isMoved,offset);
                    }

                }

            }
                break;

            case MotionEvent.ACTION_CANCEL:{
                // 计算手指移动的距离
                float nowY = ev.getY();
                int deltaY = (int) (nowY - startMoveY);
                // 计算偏移量
                int offset = (int) (deltaY * MOVE_FACTOR);
//                boundBack(isScrollVertically(ev) && true,offset);
                boundBack(false,offset);
            }
            break;
            default:
                break;
        }

//        Logger.i(" moveAction : "+ ev.getAction()+" startY : "+startMoveY + " curY : "+ev.getY() +" startX : "+startMoveX+ " curX : "+ev.getX());
        return super.dispatchTouchEvent(ev);
    }

    private boolean isScrollVertically(MotionEvent ev){
        // 计算手指移动的距离
        float nowY = ev.getY();
        float nowX = ev.getX();
        return Math.abs(nowY - startMoveY) > Math.abs(nowX - startMoveX);
    }

    /**
     * 将内容布局移动到原位置 能够在UP事件中调用, 也能够在其它须要的地方调用, 如手指移动到当前ScrollView外时
     */
    private void boundBack(boolean canListener, int offset) {
        if (!isMoved)
            return; // 假设没有移动布局， 则跳过运行


        // 开启动画
        TranslateAnimation anim = new TranslateAnimation(0, 0, contentView.getTop(), originalRect.top);
        anim.setDuration(ANIM_TIME);


        contentView.startAnimation(anim);


        // 设置回到正常的布局位置
        contentView.layout(originalRect.left, originalRect.top, originalRect.right, originalRect.bottom);


        // 将标志位设回false
        canPullDown = false;
        canPullUp = false;
        isMoved = false;
//        Logger.i("change offset + "+offset);
        if (canListener) {
            if (onBounceChanged != null) {
                onBounceChanged.onBounceChanged(isMoved,offset);
            }
        }

    }


    /**
     * 推断是否滚动到顶部
     */
    private boolean isCanPullDown() {
        if (!needPullDown) {
            return false;
        }
        return getScrollY() == 0 || contentView.getHeight() < getHeight() + getScrollY();
    }


    /**
     * 推断是否滚动究竟部
     */
    private boolean isCanPullUp() {
        if (!needPullUp) {
            return false;
        }
        return contentView.getHeight() <= getHeight() + getScrollY();
    }

    public boolean isNeedPullDown() {
        return needPullDown;
    }

    public void setNeedPullDown(boolean needPullDown) {
        this.needPullDown = needPullDown;
    }

    public boolean isNeedPullUp() {
        return needPullUp;
    }

    public void setNeedPullUp(boolean needPullUp) {
        this.needPullUp = needPullUp;
    }
}
