package com.hx.scrollviewdemo;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.hx.scrollviewdemo.CardHelper.BounceScrollView;
import com.hx.scrollviewdemo.CardHelper.CardAdapterHelper;
import com.hx.scrollviewdemo.CardHelper.ConvertUtils;
import com.hx.scrollviewdemo.CardHelper.VerticalViewPager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CardAdapter extends BaseQuickAdapter<CardBean, CardAdapter.ViewPagerViewHolder> {

    private CardAdapterHelper mCardAdapterHelper = new CardAdapterHelper();
    private int mLayoutResId;
    private Activity mActivity;

    // 子视图点击事件
    public interface OnChildItemClick {
        void onChildItemClick(CardAdapter adapter, View view, CardBean item, ViewPagerViewHolder helper);
    }

    // 第一个scrollView的回弹效果
    public interface OnItemBounceChanged {
        void onItemBounceChanged(boolean isMoved, int offset, View view, CardBean item, ViewPagerViewHolder helper);
    }

    private OnChildItemClick onChildItemClick;

    public OnItemBounceChanged onItemBounceChanged;

    public void setOnChildItemClick(OnChildItemClick onChildItemClick) {
        this.onChildItemClick = onChildItemClick;
    }

    public void setOnItemBounceChanged(OnItemBounceChanged onItemBounceChanged) {
        this.onItemBounceChanged = onItemBounceChanged;
    }

    public CardAdapter(int layoutResId, Activity activity) {
        super(layoutResId);
        this.mLayoutResId = layoutResId;
        this.mActivity = activity;
    }

    @NonNull
    @Override
    public ViewPagerViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(this.mLayoutResId, viewGroup, false);
        itemView.setTag(i);
        mCardAdapterHelper.onCreateViewHolder(viewGroup, itemView);
        int itemHeight = viewGroup.getHeight() - ConvertUtils.dp2px(this.mActivity, 18);
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) itemView.getLayoutParams();
        int itemWidth = lp.width - ConvertUtils.dp2px(this.mActivity, 18);
        ViewPagerViewHolder viewHolder = new ViewPagerViewHolder(itemView, this.mActivity, itemHeight, itemWidth);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewPagerViewHolder viewHolder, int i) {
        super.onBindViewHolder(viewHolder, i);
        mCardAdapterHelper.onBindViewHolder(viewHolder.itemView, i, getItemCount());
    }

    @Override
    protected void convert(final ViewPagerViewHolder helper, final CardBean item) {
        helper.refreshData(item);
//        addOnChildClickListener(helper.oneHolder.imageView, item, helper);
        helper.oneHolder.scrollView.setOnBounceChanged(new BounceScrollView.OnBounceChanged() {
            @Override
            public void onBounceChanged(boolean isMoved, int offset) {
                if (onItemBounceChanged != null) {
                    onItemBounceChanged.onItemBounceChanged(isMoved, offset, null, item,helper);
                }
            }
        });

    }


    // 添加子视图点击事件的监听，因为重写了onCreateViewHolder方法，导致之前的BaseViewHolder中添加事件的方法无效，所以需要自己添加点击事件监听
    private void addOnChildClickListener(final View view, final CardBean item, final ViewPagerViewHolder helper) {
        final CardAdapter adapter = this;
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onChildItemClick != null) {
                    onChildItemClick.onChildItemClick(adapter, view, item, helper);
                }
            }
        });
    }


    public class ViewPagerViewHolder extends BaseViewHolder {

        private Activity mActivity;
        private List<View> views;
        private View backView;
        public VerticalViewPager viewPager;

        private View oneScrollView;
        private View twoScrollView;
        private OneScrollViewHolder oneHolder;
        private TwoScrollViewHolder twoHolder;

        public ViewPagerViewHolder(@NonNull View itemView, Activity activity, int itemHeight, int itemWidth) {
            super(itemView);
            this.mActivity = activity;
            views = new ArrayList<>();
            backView = itemView.findViewById(R.id.back_view);
            viewPager = itemView.findViewById(R.id.view_pager);
            oneScrollView = View.inflate(activity, R.layout.item_card_one_scroll_view, null);
            twoScrollView = View.inflate(activity, R.layout.item_card_two_scroll_view, null);
            oneHolder = new OneScrollViewHolder(activity, oneScrollView, viewPager);
            twoHolder = new TwoScrollViewHolder(activity, twoScrollView, viewPager);

            views.add(oneScrollView);
            views.add(twoScrollView);

            ContentPagerAdapter adapter = new ContentPagerAdapter();
            viewPager.setAdapter(adapter);
            viewPager.setTopSubScrollView(oneHolder.scrollView);
            viewPager.setBtmSubScrollView(twoHolder.scrollView);


        }

        private void refreshData(CardBean cardBean) {
            oneHolder.refreshData(cardBean);
            twoHolder.refreshData(cardBean);
        }

        class ContentPagerAdapter extends PagerAdapter {

            @Override
            public int getCount() {
                return views.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            //初始化item布局
            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View view = views.get(position);
                container.addView(view);
                return view;
            }

            //销毁item
            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        }
    }


    public class OneScrollViewHolder {

        @BindView(R.id.scroll_view)
        BounceScrollView scrollView;
        @BindView(R.id.text_view)
        TextView textView;
        @BindView(R.id.image_view)
        ImageView imageView;
        private Activity mActivity;
        private VerticalViewPager viewPager;

        public OneScrollViewHolder(Activity activity, View view, VerticalViewPager viewPager) {
//            R.layout.item_card_one_scroll_view
            ButterKnife.bind(this, view);
            this.mActivity = activity;
            this.viewPager = viewPager;

        }

        private void refreshData(CardBean cardBean) {
            textView.setText(cardBean.getContent());
            imageView.setImageResource(cardBean.getTopImg());
        }
    }


    public class TwoScrollViewHolder {

        @BindView(R.id.scroll_view)
        ScrollView scrollView;
        @BindView(R.id.image_view)
        ImageView imageView;
        private Activity mActivity;
        private VerticalViewPager viewPager;

        public TwoScrollViewHolder(Activity activity, View view, VerticalViewPager viewPager) {
//            R.layout.item_card_two_scroll_view
            ButterKnife.bind(this, view);
            this.mActivity = activity;
            this.viewPager = viewPager;

        }

        private void refreshData(CardBean cardBean) {
            imageView.setImageResource(cardBean.getBtmImg());
        }
    }
}
