package com.hx.scrollviewdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.hx.scrollviewdemo.CardHelper.CardScaleHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    @BindView(R.id.recycle_view)
    RecyclerView recycleView;
    @BindView(R.id.scroll_to_page)
    TextView scrollToPage;
    @BindView(R.id.page_info)
    TextView pageInfo;
    private CardScaleHelper mCardScaleHelper = null;
    private CardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initRecycleView();
    }

    private void initRecycleView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recycleView.setLayoutManager(linearLayoutManager);
        // mRecyclerView绑定scale效果
        mCardScaleHelper = new CardScaleHelper();
        mCardScaleHelper.attachToRecyclerView(recycleView);
        adapter = new CardAdapter(R.layout.item_card_view_pager, this);
        adapter.bindToRecyclerView(recycleView);

        int[] topImages = {R.mipmap.pic001, R.mipmap.pic002, R.mipmap.pic003, R.mipmap.pic004, R.mipmap.pic005, R.mipmap.pic006};
        int[] btmImages = {R.mipmap.pic00001, R.mipmap.pic00002, R.mipmap.pic00003, R.mipmap.pic00004, R.mipmap.pic00005, R.mipmap.pic00006};
        List array = new ArrayList();
        for (int i = 0; i < 20; i++) {
            CardBean cardBean = new CardBean();
            cardBean.setContent("第" + i + "页");
            int topImg = topImages[i % topImages.length];
            int btmImg = btmImages[i % btmImages.length];
            cardBean.setTopImg(topImg);
            cardBean.setBtmImg(btmImg);
            array.add(cardBean);
        }
        adapter.setNewData(array);
        pageInfo.setText("第1页/共"+adapter.getData().size()+"页");

        mCardScaleHelper.setOnRecycleScrollStateChanged(new CardScaleHelper.OnRecycleScrollStateChanged() {
            @Override
            public void onRecycleScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int position = mCardScaleHelper.getCurrentItemPos();
                    Log.i(TAG, "当前为第" + (position + 1) + "页");
                    pageInfo.setText("第" + (position + 1) + "页/共"+adapter.getData().size()+"页");
                }
            }
        });

        adapter.setOnChildItemClick(new CardAdapter.OnChildItemClick() {
            @Override
            public void onChildItemClick(CardAdapter adapter, View view, CardBean item, CardAdapter.ViewPagerViewHolder helper) {
                int position = adapter.getData().indexOf(item);
                Log.i(TAG, "点击了第" + (position + 1) + "页");
                Log.i(TAG, "点击了第" + (helper.getAdapterPosition() + 1) + "页");
            }
        });
//        adapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
//            @Override
//            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
////                int position = adapter.getData().indexOf(item);
//                Log.i(TAG, "点击了第" + (position + 1) + "页");
////                position = helper.getAdapterPosition();
//                Log.i(TAG, "点击了第" + (position + 1) + "页");
//            }
//        });

        adapter.setOnItemBounceChanged(new CardAdapter.OnItemBounceChanged() {
            @Override
            public void onItemBounceChanged(boolean isMoved, int offset, View view, CardBean item, CardAdapter.ViewPagerViewHolder helper) {
                if (isMoved) {
                    Log.i(TAG, "正在滑动，等待回弹");
                } else {
                    Log.i(TAG, "结束滑动，开始回弹");
                }
            }
        });
    }

    @OnClick(R.id.scroll_to_page)
    public void onViewClicked() {
        mCardScaleHelper.setCurrentItemPos(9);
    }
}
