package com.stx.xhb.enjoylife.ui.fragment;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.stx.xhb.enjoylife.R;
import com.stx.xhb.enjoylife.model.entity.VideoResponse;
import com.stx.xhb.enjoylife.presenter.video.getVideoContract;
import com.stx.xhb.enjoylife.presenter.video.getVideoIPresenterImpl;
import com.stx.xhb.enjoylife.ui.activity.VideoDetailActivity;
import com.stx.xhb.enjoylife.ui.adapter.VideoRecyclerAdapter;
import com.stx.xhb.enjoylife.ui.adapter.provider.VideoItemProvider;
import com.xhb.core.base.BaseFragment;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * 视频推荐
 */
public class VideoFragment extends BaseFragment implements getVideoContract.getVideoView {

    @BindView(R.id.recly_view)
    RecyclerView mReclyView;
    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    private List<VideoResponse.IssueListEntity.ItemListEntity> list;
    private String nextPublishTime = "";
    private VideoRecyclerAdapter mRecyclerAdapter;
    private boolean isRefresh;

    public static VideoFragment newInstance() {
        return new VideoFragment();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_common;
    }

    @Override
    protected void onInitView(Bundle savedInstanceState) {
        list = new ArrayList<>();
        mReclyView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimary);
        setLvAdapter();
        setListener();
    }

    @Override
    protected void onVisible() {
        super.onVisible();
        onRefreshData();
    }

    @Override
    protected Class getLogicClazz() {
        return getVideoContract.class;
    }

    @Override
    public void onResponse(VideoResponse response) {
        List<VideoResponse.IssueListEntity> issueList = response.getIssueList();
        if (issueList == null || issueList.isEmpty()) {
            return;
        }
        //刷新需要清除数据
        if (isRefresh) {
            list.clear();
            mSwipeRefreshLayout.setRefreshing(false);
        } else {
            mRecyclerAdapter.loadMoreComplete();
        }
        for (int i = 0; i < issueList.size(); i++) {
            list.addAll(issueList.get(i).getItemList());
        }
        String nextUrl = response.getNextPageUrl();
        nextPublishTime = Uri.parse(nextUrl).getQueryParameter("date");
        mRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onFailure(String msg) {
        if (isRefresh) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
        mRecyclerAdapter.loadMoreFail();
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    private void setListener() {
        mRecyclerAdapter.setItemClickListener(new VideoItemProvider.setOnItemClickListener() {
            @Override
            public void onItemClick(View view, VideoResponse.IssueListEntity.ItemListEntity itemListEntity) {
                Intent intent = new Intent(getActivity(), VideoDetailActivity.class);
                Bundle bundle = new Bundle();
                VideoResponse.IssueListEntity.ItemListEntity.DataEntity data = itemListEntity.getData();
                if (!"video".equals(itemListEntity.getType())) {
                    return;
                }
                bundle.putString("title", data.getTitle());
                //获取到时间
                int duration = data.getDuration();
                int mm = duration / 60;//分
                int ss = duration % 60;//秒
                String second = "";//秒
                String minute = "";//分
                if (ss < 10) {
                    second = "0" + String.valueOf(ss);
                } else {
                    second = String.valueOf(ss);
                }
                if (mm < 10) {
                    minute = "0" + String.valueOf(mm);
                } else {
                    minute = String.valueOf(mm);//分钟
                }
                bundle.putString("time", "#" + data.getCategory() + " / " + minute + "'" + second + '"');
                bundle.putString("desc", data.getDescription());//视频描述
                bundle.putString("blurred", data.getCover().getBlurred());//模糊图片地址
                bundle.putString("feed", data.getCover().getFeed());//图片地址
                bundle.putString("video", data.getPlayUrl());//视频播放地址
                bundle.putInt("collect", data.getConsumption().getCollectionCount());//收藏量
                bundle.putInt("share", data.getConsumption().getShareCount());//分享量
                bundle.putInt("reply", data.getConsumption().getReplyCount());//回复数量
                intent.putExtras(bundle);

                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(), view, VideoDetailActivity.TRANSIT_PIC);
                try {
                    ActivityCompat.startActivity(getActivity(), intent, optionsCompat.toBundle());
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    startActivity(intent);
                }
            }
        });
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRefreshData();
            }
        });
    }

    public void onRefreshData() {
        isRefresh = true;
        mSwipeRefreshLayout.setRefreshing(true);
        ((getVideoIPresenterImpl) mPresenter).getVideoInfo("", 2);
    }

    public void onLoadMore() {
        isRefresh = false;
        ((getVideoIPresenterImpl) mPresenter).getVideoInfo(nextPublishTime, 2);
    }

    private void setLvAdapter() {
        mRecyclerAdapter = new VideoRecyclerAdapter(list);
        mReclyView.setAdapter(mRecyclerAdapter);
        mRecyclerAdapter.openLoadAnimation();
        mRecyclerAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                onLoadMore();
            }
        }, mReclyView);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }
}
