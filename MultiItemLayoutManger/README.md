# MultiItemLayoutManger
# 简介

随着 RecyclerView 控件的发布，已经有越来越多的人开始放弃 ListView 而转向 RecyclerView。
RecyclerView 已经被人玩出了各种花样，但是关于多种布局的 ItemView 的实现网上的资料还是很少，多种布局的 ItemView 其实使用场景非常多，只不过因为有很多代替方案所以可能会很少有人会使用 RecyclerView 来实现，比如下面这样：

<div align="center">
<img src="https://img-blog.csdn.net/20180503234312141?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3UwMTM4NzI4NTc=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70" width = "300" height = "550"  alt="预览图" align=center/>
</div>
包括图中的 Banner ，子标题，按钮等所有的元素都是通过一个 RecyclerView 来实现的，苦于没找到相关博客只能自己写一个了，其实还是很简单的，下面来看一下实现方式。
# 实现方式
在 Adapter 中有关于设置 ItemViewType 的方法，但是仅仅通过设置 Adapter 并不能实现上面的功能，还需要结合 LayoutManager 来实现，这是因为子 View 的大小及位置是由 LayoutManager 负责管理。按照上面的样式需要三个 ItemViewType，如下：
```java
public static final int BANNER_ITEM_TYPE = 0;
public static final int TITLE_ITEM_TYPE = 1;
public static final int MENU_ITEM_TYPE = 2;
```
这里之所以使用 public static 修饰是因为这三个属性 Adapter 中也需要使用。</p>
下面需要根据这三种 Type 来分别设置子 View：
```java
@Override
public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
    if (getItemCount() <= 0 || state.isPreLayout()) {
        return;
    }
    detachAndScrapAttachedViews(recycler);

    int curWidth = 0, curLineTop = 0;
    int horizontalCount = 0;//横向已经摆放的个数
    int widthDivider = -1;//横向间隔
    int lastViewType = MENU_ITEM_TYPE;//上一个 View 的类型
    int lastHeight = 0;//上一个 View 的高度
    for (int i = 0; i < getItemCount(); i++) {
        //遍历所有的子 View 进行计算处理
        View view = recycler.getViewForPosition(i);
        addView(view);

        measureChildWithMargins(view, 0, 0);

        //获取当前 View 的大小
        int width = getDecoratedMeasuredWidth(view);
        int height = getDecoratedMeasuredHeight(view);

        int viewType = getItemViewType(view);

        if (viewType == TITLE_ITEM_TYPE || viewType == BANNER_ITEM_TYPE) {
            //Banner 和子标题宽度及摆放方式其实是相同的，这里不做区分
            if (i != 0) {
                curLineTop += lastHeight;
            }
            layoutDecorated(view, 0, curLineTop, width, curLineTop + height);
            horizontalCount = 0;
            curWidth = 0;
            lastHeight = height;
            lastViewType = viewType;
        } else {
            if (widthDivider == -1) {
                widthDivider = (getWidth() - width * spanCount) / (spanCount + 1);
            }
            if (horizontalCount >= spanCount) {
                //需要换行
                curLineTop += lastHeight;//高度需要改变
                layoutDecorated(view, widthDivider, curLineTop, widthDivider + width, curLineTop + height);
                horizontalCount = 1;
                curWidth = width + widthDivider * 2;
                lastHeight = height;
                lastViewType = viewType;
            } else {
                //未换行，高度不变，横向距离变化
                if (curWidth == 0) {
                    curWidth = widthDivider;
                }
                if(i != 0 && lastViewType != MENU_ITEM_TYPE){
                    curLineTop += lastHeight;
                }
                layoutDecorated(view, curWidth, curLineTop, curWidth + width, curLineTop + height);
                curWidth += width + widthDivider;
                horizontalCount++;
                lastHeight = height;
                lastViewType = viewType;
            }
        }
        if(i == getItemCount() - 1){
            curLineTop += lastHeight;
        }
    }
    //计算总高度，滑动时需要使用
    totalHeight = Math.max(curLineTop, getVerticalSpace());
}
```
上面的注释写的很清楚了，还是比较简单的，除此之外还需要使 RecyclerView 可以上下滑动：
```java
@Override
public boolean canScrollVertically() {
    return true;
}

@Override
public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
    int travel = dy;
    if (verticalScrollOffset + dy < 0) {
        travel = -verticalScrollOffset;
    } else if (verticalScrollOffset + dy > totalHeight - getVerticalSpace()) {//如果滑动到最底部
        travel = totalHeight - getVerticalSpace() - verticalScrollOffset;
    }
    verticalScrollOffset += travel;
    offsetChildrenVertical(-travel);
    return travel;
}
```
下面当上全部代码：
```java
package com.zhangke.widget;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * 多种 ItemView 布局的 LayoutManger
 * <p>
 * Created by ZhangKe on 2018/4/8.
 */
public class MultiItemLayoutManger extends RecyclerView.LayoutManager {

    public static final int BANNER_ITEM_TYPE = 0;
    public static final int TITLE_ITEM_TYPE = 1;
    public static final int MENU_ITEM_TYPE = 2;

    private final int spanCount;//横向按钮摆放个数

    private int verticalScrollOffset = 0;
    private int totalHeight = 0;

    public MultiItemLayoutManger(int spanCount) {
        this.spanCount = spanCount;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() <= 0 || state.isPreLayout()) {
            return;
        }
        detachAndScrapAttachedViews(recycler);

        int curWidth = 0, curLineTop = 0;
        int horizontalCount = 0;//横向已经摆放的个数
        int widthDivider = -1;//横向间隔
        int lastViewType = MENU_ITEM_TYPE;//上一个 View 的类型
        int lastHeight = 0;//上一个 View 的高度
        for (int i = 0; i < getItemCount(); i++) {
            //遍历所有的子 View 进行计算处理
            View view = recycler.getViewForPosition(i);
            addView(view);

            measureChildWithMargins(view, 0, 0);

            //获取当前 View 的大小
            int width = getDecoratedMeasuredWidth(view);
            int height = getDecoratedMeasuredHeight(view);

            int viewType = getItemViewType(view);

            if (viewType == TITLE_ITEM_TYPE || viewType == BANNER_ITEM_TYPE) {
                //Banner 和子标题宽度及摆放方式其实是相同的，这里不做区分
                if (i != 0) {
                    curLineTop += lastHeight;
                }
                layoutDecorated(view, 0, curLineTop, width, curLineTop + height);
                horizontalCount = 0;
                curWidth = 0;
                lastHeight = height;
                lastViewType = viewType;
            } else {
                if (widthDivider == -1) {
                    widthDivider = (getWidth() - width * spanCount) / (spanCount + 1);
                }
                if (horizontalCount >= spanCount) {
                    //需要换行
                    curLineTop += lastHeight;//高度需要改变
                    layoutDecorated(view, widthDivider, curLineTop, widthDivider + width, curLineTop + height);
                    horizontalCount = 1;
                    curWidth = width + widthDivider * 2;
                    lastHeight = height;
                    lastViewType = viewType;
                } else {
                    //未换行，高度不变，横向距离变化
                    if (curWidth == 0) {
                        curWidth = widthDivider;
                    }
                    if(i != 0 && lastViewType != MENU_ITEM_TYPE){
                        curLineTop += lastHeight;
                    }
                    layoutDecorated(view, curWidth, curLineTop, curWidth + width, curLineTop + height);
                    curWidth += width + widthDivider;
                    horizontalCount++;
                    lastHeight = height;
                    lastViewType = viewType;
                }
            }
            if(i == getItemCount() - 1){
                curLineTop += lastHeight;
            }
        }
        //计算总高度，滑动时需要使用
        totalHeight = Math.max(curLineTop, getVerticalSpace());
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int travel = dy;
        if (verticalScrollOffset + dy < 0) {
            travel = -verticalScrollOffset;
        } else if (verticalScrollOffset + dy > totalHeight - getVerticalSpace()) {//如果滑动到最底部
            travel = totalHeight - getVerticalSpace() - verticalScrollOffset;
        }
        verticalScrollOffset += travel;
        offsetChildrenVertical(-travel);
        return travel;
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }
}

```
上面的代码直接拷贝过去就能使用，使用起来也很容易。
# 使用
使用时需要注意，除了设置 RecyclerView 的 LayoutManager 之外还需要相应的设置 Adapter：
RecyclerView：
```java
recyclerView.setLayoutManager(new MultiItemLayoutManger(4));//设置横向排列有四个按钮
```
Adapter：
```java
@NonNull
@Override
public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == MultiItemLayoutManger.BANNER_ITEM_TYPE) {
        return new BannerViewHolder(inflater.inflate(R.layout.adapter__banner, parent, false));
    } else if (viewType == MultiItemLayoutManger.TITLE_ITEM_TYPE) {
        return new TitleViewHolder(inflater.inflate(R.layout.adapter_title, parent, false));
    } else {
        return new MenuViewHolder(inflater.inflate(R.layout.adapter_menu, parent, false));
    }
}

@Override
public int getItemViewType(int position) {
	//我是把 ItemViewType 直接保存在了List 数据中，这里直接取出来就行
    return listData.get(position).getItemType();
}
```
OK,这样就好了。
