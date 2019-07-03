# ScrollViewDemo
#博客地址 
[android 卡片画廊效果及RecycleView、ViewPager、ScrollView之前的冲突解决](https://www.jianshu.com/p/71f7cda65ec9)
#话不多说，看图
![图片.gif](https://upload-images.jianshu.io/upload_images/12113295-7e03a540e5b1ea7b.gif?imageMogr2/auto-orient/strip)

#层级结构图
![image.png](https://upload-images.jianshu.io/upload_images/12113295-8250909f82ebd47f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

##层级结构初衷
1、内容需要通过卡片的形式来展现，还有支持加载更多，所以最底部使用RecyclerView，最好是做成预加载形式，提前n页加载下一页，这样体验更好。
2、为了展示更多内容卡片内要支持垂直分页，这时候我使用了ViewPager，一是可以更好的管理分页内容，二是ViewPager的垂直分页容易实现，三是可以处理不同控件之前的滑动冲突
3、ViewPager第一页使用的可回弹的ScrollView，可以在下拉的时候做一些动画之类的操作，例如关注操作等。
4、ViewPager的第二页只是一个普通的ScrollView，具体使用可以根据实际情况来处理


##关键点
1、RecycleView的分页效果基于PagerSnapHelper，RecyclerView在25.1.0版本中添加了一盒基于SnapHelper的子类PagerSnapHelper，可以使RecyclerView像ViewPager一样的效果，一次只能滑一页，而且居中显示。
```
/**
* 防止卡片在第一页和最后一页因无法"居中"而一直循环调用onScrollStateChanged-->SnapHelper.snapToTargetExistingView-->onScrollStateChanged
*/
public class CardLinearSnapHelper extends PagerSnapHelper {

public boolean mNoNeedToScroll = false;

@Override
public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
if (mNoNeedToScroll) {
return new int[]{0, 0};
} else {
return super.calculateDistanceToFinalSnap(layoutManager, targetView);
}
}

@Nullable
@Override
public View findSnapView(RecyclerView.LayoutManager layoutManager) {
return super.findSnapView(layoutManager);
}
}
```
2、卡片的效果是在滑动的时候根据RecycleView的偏移量计算缩放因子进行缩放
```
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
```
3、RecycleView的item内有一个垂直分页的VerticalViewPager，VerticalViewPager是在ViewPager上转换X，Y即可
```
private void init() {
//设置viewpager的切换动画,这里设置才能真正实现垂直滑动的viewpager
setPageTransformer(true, new VerticalPageTransformer());
setOverScrollMode(OVER_SCROLL_NEVER);
}

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

@Override
public boolean onTouchEvent(MotionEvent ev) {
return super.onTouchEvent(swapXY(ev)) && !noScroll;
}
```
4、解决ViewPager与RecycleView滑动的冲突，在ViewPager中屏蔽父视图的上下滑动事件
```
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
```
5、解决ViewPager子视图ScrollView的冲突，在ViewPager中拦截事件
```
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
```
以上就是此项目中的所有关键点。

#参考地址
[RecycleViewCardGallary](https://p.codekk.com/detail/5a1f994bfd1c9b26e2fca12a)


