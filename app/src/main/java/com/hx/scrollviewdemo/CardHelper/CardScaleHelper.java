package com.hx.scrollviewdemo.CardHelper;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

public class CardScaleHelper {

    private static final int FLING_MIN_DISTANCE = 15;// 移动最小距离

    private RecyclerView mRecyclerView;
    private Context mContext;

    private float mScale = 0.9f; // 两边视图scale
    private int mPagePadding = 15; // 卡片的padding, 卡片间的距离等于2倍的mPagePadding
    private int mShowLeftCardWidth = 15;   // 左边卡片显示大小

    private int mCardWidth; // 卡片宽度
    private int mOnePageWidth; // 滑动一页的距离
    private int mCardGalleryWidth;

    private int mCurrentItemPos;
    private int mCurrentItemOffset;

    public static int scrollToUP = 1;//向上滑
    public static int scrollToDown = 2;//向下滑
    public static int scrollToLeft = 3;//向左滑
    public static int scrollToRight = 4;//向右滑

    private int scrollDirection;//滑动方向

    public CardScaleHelper(){
    }

    public interface OnRecycleScrollStateChanged{
        void onRecycleScrollStateChanged(RecyclerView recyclerView, int newState);
    }

    private OnRecycleScrollStateChanged onRecycleScrollStateChanged;

    private CardLinearSnapHelper mLinearSnapHelper = new CardLinearSnapHelper();

    public void setOnRecycleScrollStateChanged(OnRecycleScrollStateChanged onRecycleScrollStateChanged) {
        this.onRecycleScrollStateChanged = onRecycleScrollStateChanged;
    }

    public void attachToRecyclerView(final RecyclerView mRecyclerView) {
        this.mRecyclerView = mRecyclerView;
        mContext = mRecyclerView.getContext();
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mLinearSnapHelper.mNoNeedToScroll = mCurrentItemOffset == 0 || mCurrentItemOffset == getDestItemOffset(mRecyclerView.getAdapter().getItemCount() - 1);

                    View itemView = mLinearSnapHelper.findSnapView(recyclerView.getLayoutManager());
                    int currentPosition = 0;
                    if (itemView != null) {
                        currentPosition = recyclerView.getLayoutManager().getPosition(itemView);
                    }
                    mCurrentItemPos = currentPosition;
                    mCurrentItemOffset = mCurrentItemPos * mOnePageWidth;
                    onScrolledChangedCallback();
                } else {
                    mLinearSnapHelper.mNoNeedToScroll = false;
                }
                if (onRecycleScrollStateChanged != null) {
                    onRecycleScrollStateChanged.onRecycleScrollStateChanged(recyclerView,newState);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // dx>0则表示右滑, dx<0表示左滑, dy<0表示上滑, dy>0表示下滑
                if (dx > 0) {
                    scrollDirection = scrollToRight;
                }else if (dx < 0) {
                    scrollDirection = scrollToLeft;
                }else if (dy < 0) {
                    scrollDirection = scrollToUP;
                }else if (dy > 0) {
                    scrollDirection = scrollToDown;
                }
                if(dx != 0){//去掉奇怪的内存疯涨问题
                    mCurrentItemOffset += dx;
//                    Logger.v("CardScaleHelper",String.format("dx=%s, dy=%s, mScrolledX=%s", dx, dy, mCurrentItemOffset));
                    onScrolledChangedCallback();
                }
            }
        });

        initWidth();
        mLinearSnapHelper.attachToRecyclerView(mRecyclerView);
    }

    public int getScrollDirection(){
        return scrollDirection;
    }

    /**
     * 初始化卡片宽度
     */
    private void initWidth() {
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mCardGalleryWidth = mRecyclerView.getWidth();
                mCardWidth = mCardGalleryWidth - ConvertUtils.dp2px(mContext, 2 * (mPagePadding + mShowLeftCardWidth));
                mOnePageWidth = mCardWidth;
                mRecyclerView.smoothScrollToPosition(mCurrentItemPos);
                onScrolledChangedCallback();
            }
        });
    }

    /**
     * 指定滑动到第几页
     * @param currentItemPos
     */
    public void setCurrentItemPos(final int currentItemPos) {
        this.mCurrentItemPos = currentItemPos;
        if (mOnePageWidth == 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCurrentItemOffset = currentItemPos * mOnePageWidth + ConvertUtils.dp2px(mContext, (mPagePadding + mShowLeftCardWidth));
                    mRecyclerView.scrollToPosition(currentItemPos);
                    //
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mRecyclerView.smoothScrollBy(2,0);
                        }
                    },200);
                }
            }, 100);
        }else {
            mCurrentItemOffset = currentItemPos * mOnePageWidth + ConvertUtils.dp2px(mContext, (mPagePadding + mShowLeftCardWidth));
            mRecyclerView.scrollToPosition(currentItemPos);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.smoothScrollBy(2,0);
                }
            },200);
//            onScrolledChangedCallback();
        }

    }

    public int getCurrentItemPos() {
        return mCurrentItemPos;
    }

    private int getDestItemOffset(int destPos) {
        return mOnePageWidth * destPos;
    }

    /**
     * RecyclerView位移事件监听, view大小随位移事件变化
     */
    private void onScrolledChangedCallback() {
        int offset = mCurrentItemOffset - mCurrentItemPos * mOnePageWidth;
        float percent = (float) Math.max(Math.abs(offset) * 1.0 / mOnePageWidth, 0.0001);

//        Logger.d(String.format("offset=%s, percent=%s", offset, percent));
        View leftView = null;
        View currentView;
        View rightView = null;
        if (mCurrentItemPos > 0) {
            leftView = mRecyclerView.getLayoutManager().findViewByPosition(mCurrentItemPos - 1);
        }
        currentView = mRecyclerView.getLayoutManager().findViewByPosition(mCurrentItemPos);
        if (mCurrentItemPos < mRecyclerView.getAdapter().getItemCount() - 1) {
            rightView = mRecyclerView.getLayoutManager().findViewByPosition(mCurrentItemPos + 1);
        }

        if (leftView != null) {
            // y = (1 - mScale)x + mScale
            leftView.setScaleY((1 - mScale) * percent + mScale);
        }
        if (currentView != null) {
            // y = (mScale - 1)x + 1
            currentView.setScaleY((mScale - 1) * percent + 1);
        }
        if (rightView != null) {
            // y = (1 - mScale)x + mScale
            rightView.setScaleY((1 - mScale) * percent + mScale);
        }
    }

    public void setScale(float scale) {
        mScale = scale;
    }

    public void setPagePadding(int pagePadding) {
        mPagePadding = pagePadding;
    }

    public void setShowLeftCardWidth(int showLeftCardWidth) {
        mShowLeftCardWidth = showLeftCardWidth;
    }

}
