/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.R;
import com.hippo.util.AssertUtils;
import com.hippo.util.IntIdGenerator;
import com.hippo.util.LayoutManagerUtils;
import com.hippo.util.UiUtils;
import com.hippo.widget.Snackbar;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.widget.refreshlayout.RefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class ContentLayout extends FrameLayout {

    private ProgressBar mProgressBar;
    private ViewGroup mTipView;
    private RefreshLayout mRefreshLayout;
    private EasyRecyclerView mRecyclerView;
    private View mImageView;
    private TextView mTextView;
    private Snackbar mSnackbar;

    private ContentHelper mContentHelper;

    private int mRecyclerViewOriginTop;
    private int mRecyclerViewOriginBottom;
    private int mSnackbarOriginBottom;

    public ContentLayout(Context context) {
        super(context);
        init(context);
    }

    public ContentLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ContentLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.widget_content_layout, this);

        mProgressBar = (ProgressBar) getChildAt(0);
        mTipView = (ViewGroup) getChildAt(1);
        mRefreshLayout = (RefreshLayout) getChildAt(2);
        mSnackbar = (Snackbar) getChildAt(3);
        mRecyclerView = (EasyRecyclerView) mRefreshLayout.getChildAt(1);
        mImageView = mTipView.getChildAt(0);
        mTextView = (TextView) mTipView.getChildAt(1);

        //
        mRecyclerViewOriginTop = mRecyclerView.getPaddingTop();
        mRecyclerViewOriginBottom = mRecyclerView.getPaddingBottom();

        // Snackbar
        mSnackbarOriginBottom = mSnackbar.getPaddingBottom();
    }

    public EasyRecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public void setHelper(ContentHelper helper) {
        mContentHelper = helper;
        helper.init(this);
    }

    public void setFitPaddingTop(int fitPaddingTop) {
        // RecyclerView
        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(), mRecyclerViewOriginTop + fitPaddingTop, mRecyclerView.getPaddingRight(), mRecyclerView.getPaddingBottom());
        // RefreshLayout
        mRefreshLayout.setProgressViewOffset(false, fitPaddingTop, fitPaddingTop + UiUtils.dp2pix(getContext(), 32)); // TODO
    }

    public void setFitPaddingBottom(int fitPaddingBottom) {
        // RecyclerView
        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(),
                mRecyclerView.getPaddingTop(), mRecyclerView.getPaddingRight(),
                mRecyclerViewOriginBottom + fitPaddingBottom);
        // Snackbar
        mSnackbar.setPadding(mSnackbar.getPaddingLeft(), mSnackbar.getPaddingTop(), mSnackbar.getPaddingRight(), mSnackbarOriginBottom + fitPaddingBottom);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        return mContentHelper.onSaveInstanceState(super.onSaveInstanceState());
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(mContentHelper.onRestoreInstanceState(state));
    }

    public abstract static class ContentHelper<E, VH extends RecyclerView.ViewHolder>
            extends EasyRecyclerView.Adapter<VH>
            implements RefreshLayout.OnHeaderRefreshListener,
            RefreshLayout.OnFooterRefreshListener, View.OnClickListener {

        private static final String STATE_KEY_SUPER = "super";
        private static final String STATE_KEY_SHOWN_VIEW = "shown_view";
        private static final String STATE_KEY_RECYCLER_VIEW = "recycler_view";
        private static final String STATE_KEY_TIP_MESSAGE = "tip_message";

        public static final int TYPE_REFRESH = 0;
        public static final int TYPE_PRE_PAGE = 1;
        public static final int TYPE_PRE_PAGE_KEEP_POS = 2;
        public static final int TYPE_NEXT_PAGE = 3;
        public static final int TYPE_NEXT_PAGE_KEEP_POS = 4;
        public static final int TYPE_SOMEWHERE = 5;

        private Context mContext;

        private ProgressBar mProgressBar;
        private ViewGroup mTipView;
        private RefreshLayout mRefreshLayout;
        private EasyRecyclerView mRecyclerView;
        private View mImageView;
        private TextView mTextView;
        private Snackbar mSnackbar;
        private RecyclerView.LayoutManager mLayoutManager;

        private ViewTransition mViewTransition;

        /**
         * Store data
         */
        private List<E> mData;

        private IntIdGenerator mIdGenerator;

        /**
         * First shown page index
         */
        private int mFirstPage;
        /**
         * Last shown page index + 1
         */
        private int mLastPage;
        /**
         * First index index of current page
         */
        private int mFirstIndex;
        /**
         * Last index index of current page + 1
         */
        private int mLastIndex;
        /**
         * The size of page
         */
        private int mPageSize;
        private int mCurrentPage;
        private int mPageVolume;

        private int mCurrentTaskId;
        private int mCurrentTaskType;
        private int mCurrentTaskPage;

        private RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState){
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (mPageVolume <= 0) {
                        return;
                    }
                    if (mLastIndex == 0) {
                        mCurrentPage = 0;
                        mFirstIndex = 0;
                        mLastIndex = mPageVolume;
                    }

                    int firstVisiblePosition = LayoutManagerUtils.getFirstVisibleItemPostion(mLayoutManager);
                    int lastVisiblePosition = LayoutManagerUtils.getLastVisibleItemPostion(mLayoutManager);

                    int pageChanged = (firstVisiblePosition - mFirstIndex) / mPageVolume;
                    if (pageChanged <= 0) {
                        pageChanged = (lastVisiblePosition - mLastIndex + 1) / mPageVolume;
                    }

                    if (pageChanged != 0) {
                        mCurrentPage = mCurrentPage + pageChanged;
                        int offset = pageChanged * mPageVolume;
                        mFirstIndex += offset;
                        mLastIndex += offset;
                    }
                }
            }
        };

        private LayoutManagerUtils.OnScrollToPositionListener mOnScrollToPositionListener =
                new LayoutManagerUtils.OnScrollToPositionListener() {
                    @Override
                    public void onScrollToPosition() {
                        ContentHelper.this.onScrollToPosition();
                    }
                };

        public ContentHelper(Context context) {
            mContext = context;
            mData = new ArrayList<>();
            mIdGenerator = IntIdGenerator.create();
        }

        @SuppressWarnings("unchecked")
        protected ContentHelper(Context context, ContentHelper oldContentHelper) {
            this(context);
            mData.addAll(oldContentHelper.mData);

            mFirstPage = oldContentHelper.mFirstPage;
            mLastPage = oldContentHelper.mLastPage;
            mFirstIndex = oldContentHelper.mFirstIndex;
            mLastIndex = oldContentHelper.mLastIndex;
            mPageSize = oldContentHelper.mPageSize;
            mCurrentPage = oldContentHelper.mCurrentPage;
            mPageVolume = oldContentHelper.mPageVolume;
            mCurrentTaskId = oldContentHelper.mCurrentTaskId;
            mCurrentTaskType = oldContentHelper.mCurrentTaskType;
            mCurrentTaskPage = oldContentHelper.mCurrentTaskPage;
        }

        private void init(ContentLayout contentLayout) {
            mProgressBar = contentLayout.mProgressBar;
            mTipView = contentLayout.mTipView;
            mRefreshLayout = contentLayout.mRefreshLayout;
            mSnackbar = contentLayout.mSnackbar;
            mRecyclerView = contentLayout.mRecyclerView;
            mImageView = contentLayout.mImageView;
            mTextView = contentLayout.mTextView;
            mLayoutManager = generateLayoutManager();

            mViewTransition = new ViewTransition(mRefreshLayout, mProgressBar, mTipView);
            mViewTransition.showView(2, false);

            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(this);
            mRecyclerView.addOnScrollListener(mOnScrollListener);
            mRefreshLayout.setOnHeaderRefreshListener(this);
            mRefreshLayout.setOnFooterRefreshListener(this);

            mTipView.setOnClickListener(this);
            mSnackbar.setAction(mContext.getString(R.string.retry), this);
        }

        protected abstract RecyclerView.LayoutManager generateLayoutManager();

        protected abstract void onScrollToPosition();

        public void showContent() {
            mViewTransition.showView(0);
        }

        public void showProgressBar() {
            showProgressBar(true);
        }

        public void showProgressBar(boolean animation) {
            mViewTransition.showView(1, animation);
        }

        public void showText(CharSequence text) {
            mViewTransition.showView(2);
            mTextView.setText(text);
        }

        /**
         * @throws IndexOutOfBoundsException
         *                if {@code location < 0 || location >= size()}
         */
        public E getDataAt(int location) {
            return mData.get(location);
        }

        /**
         * Call {@link #onGetPageData(int, List)} when get data
         *
         * @param taskId task id
         * @param page the page to get
         */
        protected abstract void getPageData(int taskId, int type, int page);

        public void setPageSize(int pageSize) {
            mPageSize = pageSize;
        }

        public int getPageSize() {
            return mPageSize;
        }

        public int getCurrentPage() {
            return mCurrentPage;
        }

        public void resetPageSize() {
            mPageSize = Integer.MAX_VALUE;
        }

        public void onGetPageData(int taskId, List<E> data) {
            showContent();
            int pageVolume = data.size();
            mPageVolume = pageVolume;
            if (mCurrentTaskId == taskId) {
                switch (mCurrentTaskType) {
                    case TYPE_REFRESH:
                        mFirstPage = 0;
                        mLastPage = 1;
                        mCurrentPage = 0;
                        mFirstIndex = 0;
                        mLastIndex = pageVolume;

                        mData.clear();
                        mData.addAll(data);
                        notifyDataSetChanged();

                        mRecyclerView.stopScroll();
                        LayoutManagerUtils.scrollToPositionWithOffset(mLayoutManager, 0, 0);
                        onScrollToPosition();
                        break;
                    case TYPE_PRE_PAGE:
                    case TYPE_PRE_PAGE_KEEP_POS:
                        mData.addAll(0, data);
                        notifyItemRangeInserted(0, pageVolume);

                        mFirstPage--;
                        if (mCurrentTaskType == TYPE_PRE_PAGE_KEEP_POS) {
                            mFirstIndex += pageVolume;
                            mLastIndex += pageVolume;

                            mRecyclerView.stopScroll();
                            LayoutManagerUtils.scrollToPositionProperly(mLayoutManager, mContext, mFirstIndex - 1, mOnScrollToPositionListener);
                        } else {
                            mCurrentPage = mFirstPage;
                            mFirstIndex = 0;
                            mLastIndex = pageVolume;

                            mRecyclerView.stopScroll();
                            LayoutManagerUtils.scrollToPositionWithOffset(mLayoutManager, 0, 0);
                            onScrollToPosition();
                        }
                        break;
                    case TYPE_NEXT_PAGE:
                    case TYPE_NEXT_PAGE_KEEP_POS:
                        int oldDataSize = mData.size();
                        mData.addAll(data);
                        notifyItemRangeInserted(oldDataSize, pageVolume);

                        mLastPage++;
                        if (mCurrentTaskType != TYPE_NEXT_PAGE_KEEP_POS) {
                            mCurrentPage = mLastPage - 1;
                            mFirstIndex = mData.size() - pageVolume;
                            mLastIndex = mData.size();

                            mRecyclerView.stopScroll();
                            LayoutManagerUtils.scrollToPositionWithOffset(mLayoutManager, mFirstIndex, 0);
                            onScrollToPosition();
                        }
                        break;
                    case TYPE_SOMEWHERE:
                        mData.clear();
                        mData.addAll(data);
                        notifyDataSetChanged();

                        mFirstPage = mCurrentTaskPage;
                        mLastPage = mCurrentTaskPage + 1;
                        mCurrentPage = mCurrentTaskPage;
                        mFirstIndex = 0;
                        mLastIndex = pageVolume;

                        mRecyclerView.stopScroll();
                        LayoutManagerUtils.scrollToPositionWithOffset(mLayoutManager, 0, 0);
                        onScrollToPosition();
                        break;
                }
            }

            mRefreshLayout.setHeaderRefreshing(false);
            mRefreshLayout.setFooterRefreshing(false);
        }

        public void onGetPageData(int taskId, Exception e) {
            if (mCurrentTaskId == taskId) {
                mRefreshLayout.setHeaderRefreshing(false);
                mRefreshLayout.setFooterRefreshing(false);
                showText(e.getClass().getName());
            }
        }

        @Override
        public boolean onFooterRefresh() {
            if (mLastPage >= mPageSize) {
                return false;
            } else {
                mCurrentTaskId = mIdGenerator.nextId();
                mCurrentTaskType = TYPE_NEXT_PAGE_KEEP_POS;
                mCurrentTaskPage = mLastPage;
                getPageData(mCurrentTaskId, mCurrentTaskType, mCurrentTaskPage);
                return true;
            }
        }

        @Override
        public void onHeaderRefresh() {
            if (mFirstPage > 0) {
                mCurrentTaskId = mIdGenerator.nextId();
                mCurrentTaskType = TYPE_PRE_PAGE_KEEP_POS;
                mCurrentTaskPage = mFirstPage - 1;
                getPageData(mCurrentTaskId, mCurrentTaskType, mCurrentTaskPage);
            } else {
                doRefresh();
            }
        }

        @Override
        public void onClick(View v) {
            if (v == mTipView) {
                refreshWithSameSearch();
            }
        }

        private void doRefresh() {
            mCurrentTaskId = mIdGenerator.nextId();
            mCurrentTaskType = TYPE_REFRESH;
            mCurrentTaskPage = 0;
            getPageData(mCurrentTaskId, mCurrentTaskType, mCurrentTaskPage);
        }

        public void firstRefresh() {
            showProgressBar(false);
            doRefresh();
        }

        /**
         * Show progress bar first, than do refresh
         */
        public void refresh() {
            showProgressBar();
            doRefresh();
        }

        public void refreshWithSameSearch() {
            if (mViewTransition.getShownViewIndex() == 0) {
                // Go to top
                mRecyclerView.stopScroll();
                LayoutManagerUtils.scrollToPositionProperly(mLayoutManager,
                        mContext, 0, mOnScrollToPositionListener);
                // Show header refresh
                mRefreshLayout.setFooterRefreshing(false);
                mRefreshLayout.setHeaderRefreshing(true);
                // Do refresh
                doRefresh();
            } else {
                showProgressBar();
                doRefresh();
            }
        }

        public void cancelCurrentTask() {
            mCurrentTaskId = mIdGenerator.nextId();
        }

        public boolean canGoTo() {
            return mViewTransition.getShownViewIndex() == 0;
        }

        /**
         * Only work when data is loaded
         */
        public void goTo(int page) {
            if (page < 0 || page >= mPageSize) {
                throw new IndexOutOfBoundsException("Page size is " + mPageSize + ", page is " + page);
            } else if (page >= mFirstPage && page < mLastPage) {
                cancelCurrentTask();

                mCurrentPage = page;
                mFirstIndex = mPageVolume * (page - mFirstPage);
                mLastIndex = mFirstIndex + mPageVolume;
                int position = mFirstIndex;
                mRecyclerView.stopScroll();
                LayoutManagerUtils.scrollToPositionWithOffset(mLayoutManager, position, 0);
                onScrollToPosition();
            } else if (page == mFirstPage - 1) {
                mRefreshLayout.setFooterRefreshing(false);
                mRefreshLayout.setHeaderRefreshing(true);

                mCurrentTaskId = mIdGenerator.nextId();
                mCurrentTaskType = TYPE_PRE_PAGE;
                mCurrentTaskPage = page;
                getPageData(mCurrentTaskId, mCurrentTaskType, mCurrentTaskPage);
            } else if (page == mLastPage) {
                mRefreshLayout.setFooterRefreshing(false);
                mRefreshLayout.setHeaderRefreshing(true);

                mCurrentTaskId = mIdGenerator.nextId();
                mCurrentTaskType = TYPE_NEXT_PAGE;
                mCurrentTaskPage = page;
                getPageData(mCurrentTaskId, mCurrentTaskType, mCurrentTaskPage);
            } else {
                mRefreshLayout.setFooterRefreshing(false);
                mRefreshLayout.setHeaderRefreshing(true);

                mCurrentTaskId = mIdGenerator.nextId();
                mCurrentTaskType = TYPE_SOMEWHERE;
                mCurrentTaskPage = page;
                getPageData(mCurrentTaskId, mCurrentTaskType, mCurrentTaskPage);
            }
        }

        /**
         * Reload data to layout. Maybe do it when change item view style
         */
        public void reload() {
            notifyDataSetChanged();
            mCurrentPage = mFirstPage;
            mFirstIndex = 0;
            mLastIndex = mPageVolume;
            mRecyclerView.stopScroll();
            LayoutManagerUtils.scrollToPositionWithOffset(mLayoutManager, 0, 0);
            onScrollToPosition();
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        protected Parcelable onSaveInstanceState(Parcelable superParcelable) {
            final Bundle state = new Bundle();
            state.putParcelable(STATE_KEY_SUPER, superParcelable);
            state.putInt(STATE_KEY_SHOWN_VIEW, mViewTransition.getShownViewIndex());
            state.putParcelable(STATE_KEY_RECYCLER_VIEW, mRecyclerView.onSaveInstanceState());
            state.putString(STATE_KEY_TIP_MESSAGE, mTextView.getText().toString());
            return state;
        }

        protected Parcelable onRestoreInstanceState(Parcelable state) {
            AssertUtils.assertInstanceof("state must be Bundle", state, Bundle.class);
            final Bundle savedState = (Bundle) state;
            mViewTransition.showView(savedState.getInt(STATE_KEY_SHOWN_VIEW), false);
            mRecyclerView.onRestoreInstanceState(savedState.getParcelable(STATE_KEY_RECYCLER_VIEW));
            mTextView.setText(savedState.getString(STATE_KEY_TIP_MESSAGE));
            return savedState.getParcelable(STATE_KEY_SUPER);
        }
    }
}
