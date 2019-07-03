package com.hx.scrollviewdemo.CardHelper;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import java.lang.reflect.Field;

public class VerticalViewPager extends ViewPager {

    public static final String TAG = "VerticalViewPager";

    private Context mContext;

    private boolean noScroll;

    private ScrollView topSubScrollView;

    private ScrollView btmSubScrollView;


    public void setNoScroll(boolean noScroll) {
        this.noScroll = noScroll;
    }

    public void setTopSubScrollView(ScrollView topSubScrollView) {
        this.topSubScrollView = topSubScrollView;
    }

    public void setBtmSubScrollView(ScrollView btmSubScrollView) {
        this.btmSubScrollView = btmSubScrollView;
    }

    public VerticalViewPager(Context context) {
        this(context, null);
    }

    public VerticalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
        fixTouchSlop();
    }

    private void init() {
        //设置viewpager的切换动画,这里设置才能真正实现垂直滑动的viewpager
        setPageTransformer(true, new VerticalPageTransformer());
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    /**
     * 这个方法是通过反射，修改viewpager的触发切换的最小滑动速度（还是距离？姑
     * 且是速度吧！滑了10dp就给它切换）
     **/
    private void fixTouchSlop() {
        Field field = null;
        try {
            field = ViewPager.class.getDeclaredField("mMinimumVelocity");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        try {
            if (null != field) {
                field.setAccessible(true);
                field.setInt(this, ConvertUtils.px2dp(mContext, 10));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    private class VerticalPageTransformer implements ViewPager.PageTransformer {

        @Override
        public void transformPage(View view, float position) {

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]
                view.setAlpha(1);

                // Counteract the default slide transition
                view.setTranslationX(view.getWidth() * -position);

                //set Y position to swipe in from top
                float yPosition = position * view.getHeight();
                view.setTranslationY(yPosition);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    /**
     * Swaps the X and Y coordinates of your touch event.
     */
    private MotionEvent swapXY(MotionEvent ev) {
        //获取宽高
        float width = getWidth();
        float height = getHeight();

        //将Y轴的移动距离转变成X轴的移动距离
        float newX = (ev.getY() / height) * width;
        //将X轴的移动距离转变成Y轴的移动距离
        float newY = (ev.getX() / width) * height;
        //重设event的位置
        ev.setLocation(newX, newY);

        return ev;
    }

    private float startY;
    private float endY;
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:{
                startY = event.getY();
            }
            break;
            case MotionEvent.ACTION_MOVE:{
                float curY = event.getY();
                if (getCurrentItem() == 0) {
                    //当第一页的scrollView在顶端且向上拉时，拦截事件
                    if (topSubScrollView != null && topSubScrollView.getScrollY() == 0 && curY < startY) {//topScrollView向上拉
                        return true;
                    }

                }else if (getCurrentItem() == 1) {
                    //btmScrollView滑动到最顶部且还向下拉
                    if (btmSubScrollView != null && btmSubScrollView.getScrollY() == 0 && curY > startY) {
                        return true;
                    }
                }


            }
            break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:{
                endY = event.getY();
                if (getCurrentItem() == 0) {
                    //当第一页的scrollView在顶端且向上拉时，拦截事件
                    if (topSubScrollView != null && topSubScrollView.getScrollY() == 0 && endY < startY) {//topScrollView向上拉
                        return true;
                    }
                }else if (getCurrentItem() == 1) {
                    //btmScrollView滑动到最顶部且还向下拉
                    if (btmSubScrollView != null && btmSubScrollView.getScrollY() == 0 && endY > startY) {
                        return true;
                    }
                }
            }
            break;
        }
        boolean intercepted = super.onInterceptTouchEvent(swapXY(event)) && !noScroll;
        swapXY(event); // return touch coordinates to original reference frame for any child views
        return intercepted;
    }

    private float xDispatchLast;
    private float yDispatchLast;
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:{
                xDispatchLast = event.getX();
                yDispatchLast = event.getY();
            }
            break;
            case MotionEvent.ACTION_MOVE:{
                float curX = event.getX();
                float curY = event.getY();

                float xDiff = curX - xDispatchLast;
                float yDiff = curY - yDispatchLast;
                float xAbsDiff = Math.abs(xDiff);
                float yAbsDiff = Math.abs(yDiff);
                if (yAbsDiff > xAbsDiff) {//上下滑动时拦截父控件的事件
                    getParent().requestDisallowInterceptTouchEvent(true);
                }

            }
            break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:{
                xDispatchLast = event.getX();
                yDispatchLast = event.getY();
            }
            break;
        }
        return super.dispatchTouchEvent(event) && !noScroll;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(swapXY(ev)) && !noScroll;
    }
}
