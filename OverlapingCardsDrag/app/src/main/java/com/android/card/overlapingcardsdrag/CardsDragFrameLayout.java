package com.android.card.overlapingcardsdrag;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;


import java.util.ArrayList;

import static android.support.v4.widget.ViewDragHelper.*;

/**
 * author: ChenWei
 * create date: 2016/8/28.
 * description: 布局内可以放多个卡片，最上层卡片可以拖动,卡片水平居中
 */
public class CardsDragFrameLayout extends FrameLayout {
    private float scaleXOffest; //沿x轴缩放偏移量
    private int cardYOffest; //沿y轴平移偏移量
    private int viewBelowCardMargin; //bottomLayout距离底层cardview距离

    private boolean isHasBottomLayout; // 是否有bottomLayout

    private ViewDragHelper dragHelper;

    private int topViewLeft; //topView未拖动前与parentView在x轴上的距离
    private int topViewTop;//topView未拖动前与parentView在y轴上的距离

    private int cardViewsCount; //布局中的卡片数量
    private int overlapingcardViewsCount; // 用户实际看到的卡片数量
    private int cardsCount;  //cards总数

    private ArrayList<ItemCardView> cardViews; //重叠卡片view(由底部到顶部)

    private View bottomLayout; //底部view

    private float[] scaleXValues; //重叠cardviews由底部到顶部的需要设置的x轴最终缩放值
    private int[] translateYValues; //重叠cardviews由底部到顶部需要设置的y轴最终平移值

    private boolean isSmoothSlideView;

    private ItemCardView topView;

    private UpdateCardViewListener updateCardViewListener;

    private int topCardPosi;

    public CardsDragFrameLayout(Context context) {
        this(context, null);
    }

    public CardsDragFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardsDragFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes
                (attrs, R.styleable.CardsDragFrameLayout);
        getXmlData(typedArray);
        typedArray.recycle();

        dragHelper = create(this, 1f, new CardDragCallback());
    }


    private void getXmlData(TypedArray typedArray) {
        cardYOffest = typedArray.getDimensionPixelOffset
                (R.styleable.CardsDragFrameLayout_card_y_offest_value, 40);

        scaleXOffest = typedArray.getFloat
                (R.styleable.CardsDragFrameLayout_scale_x_offest_value, 0.08f);

//        viewBelowCardMargin = typedArray.getDimensionPixelOffset
//                (R.styleable.CardsDragFrameLayout_view_below_card_margin, 40);

        //是否有底部layout
        isHasBottomLayout = typedArray.getBoolean
                (R.styleable.CardsDragFrameLayout_isHasBottomLayout, false);
    }


    /**
     * 获得设置卡片重叠效果所需的缩放/平移参数.
     */
    private void getScalesAndTranslateParams() {
        scaleXValues = new float[overlapingcardViewsCount];
        translateYValues = new int[overlapingcardViewsCount];
        //设置起始x轴缩放值,以最外层的cardview作为标准(最外层cardview scaleX = 1)
        float scaleX = 1 - scaleXOffest * (overlapingcardViewsCount - 1);
        //设置起始平移值，,以最外层的cardview作为标准(最外层cardview的平移值为0)
        int translateY = cardYOffest * (overlapingcardViewsCount - 1);

        for (int i = 0; i < overlapingcardViewsCount; i++) {
            scaleXValues[i] = scaleX;
            translateYValues[i] = translateY;

            //更新平移值和缩放值
            scaleX += scaleXOffest;
            translateY -= cardYOffest;
        }
    }

    /**
     * 根据是否有bottomLayout计算cardview和overlapingCardsView 的数量
     */
    private void getcardViewsCount() {
        int count = getChildCount();
        cardViewsCount = count;
        if (isHasBottomLayout) {
            cardViewsCount = count - 1;
        }
        overlapingcardViewsCount = cardViewsCount - 1;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        getcardViewsCount();

        //保存view实例
        int begin = 0;
        //如果有botomview，保存其引用值
        if (isHasBottomLayout) {
            bottomLayout = getChildAt(begin++);
        }

        //保存所有cardview引用

        cardViews = new ArrayList<>();
        for (int i = begin; i < getChildCount(); i++) {
            ItemCardView itemCardView = (ItemCardView) getChildAt(i);
            cardViews.add(itemCardView);

        }

        topView = cardViews.get(cardViewsCount - 1);


        getScalesAndTranslateParams();

    }

    /**
     * 设置卡片的数量
     * @param cardsCount
     */
    public void setCardsCount(int cardsCount){
        this.cardsCount = cardsCount;
    }


    private void updateCardViewsUI(){
        if(updateCardViewListener == null){
            throw new IllegalArgumentException("请先设置UpdateCardViewListener监听器");
        }



        ArrayList<ItemCardView> cardViews2 =new ArrayList<>();

        int lastIndex = cardsCount;
        if(cardsCount > overlapingcardViewsCount){
            lastIndex = overlapingcardViewsCount;
        }

        for(int i = 0; i < lastIndex; i++){
            cardViews2.add(cardViews.get(cardViewsCount - i - 1));
        }

        updateCardViewListener.updateCardView(topCardPosi++,cardViews2);

    }

    /**
     * 为了显示出重叠卡片的界面效果，主要做2个操作，1个是沿x轴缩放，一个是沿y轴向下平移
     * 从最里层到最外层，x轴缩放逐渐值逐渐增大，y轴平移值逐渐减少.
     * <p/>
     * 为了达到将最顶层cardview拖动时，未拖动部分的cardview逐渐显示出拖动前的界面样式，
     * 必须在布局中多写一个cardview，同时，最底层cardview与倒数第二层cardview显示在相同的位置。
     *
     * @param changed
     * @param l       相对于父控件的左边距
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        //设置bottomLayout位置
        if (isHasBottomLayout) {
            LayoutParams flParmas = (FrameLayout.LayoutParams)
                    bottomLayout.getLayoutParams();
            int height = bottomLayout.getMeasuredHeight();
            int width = bottomLayout.getMeasuredWidth();
            int left = flParmas.leftMargin;
            int right = left + width;
            int top = flParmas.topMargin;
            int bottom = height + top;

            bottomLayout.layout(left, top, right, bottom);
        }

        //设置重叠卡片位置
        ItemCardView itemCardView = cardViews.get(0);
        LayoutParams flParmas = (LayoutParams)
                itemCardView.getLayoutParams();
        int height = itemCardView.getMeasuredHeight();
        int width = itemCardView.getMeasuredWidth();
        int left = flParmas.leftMargin;
        int right = left + width;
        int top = flParmas.topMargin;
        int bottom = height + top;

        //设置cardview位置.
        for (int i = 0; i < cardViewsCount; i++) {
            Log.e("i","i:    "+i);
            ItemCardView cardView = cardViews.get(i);
            //设置每个cardView的默认位置
            cardView.layout(left, top, right, bottom);

            //对当前cardview进行平移和缩放(注意，最底层的cardview要与倒数第二层的cardview显示在同一个位置上)
            if (i == 0){
                cardView.setScaleX(scaleXValues[i]);
                cardView.setTranslationY(translateYValues[i]);
            }else{
                cardView.setScaleX(scaleXValues[i - 1]);
                cardView.setTranslationY(translateYValues[i - 1]);
            }
        }


        //保存顶部用于拖动的view以及顶部view的坐标值
        if (overlapingcardViewsCount > 0) {
            topViewLeft = topView.getLeft();
            topViewTop = topView.getTop();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(dragHelper == null){
            return super.onInterceptTouchEvent(ev);
        }

        if(ev.getAction() == MotionEvent.ACTION_CANCEL){
            dragHelper.cancel();
            return false;
        }
        return dragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(dragHelper == null){
            return super.onTouchEvent(event);
        }

        dragHelper.processTouchEvent(event);
        return true;
    }

    private void translateLeftOutside(){
        int viewWidth = topView.getWidth();
        translateOutside(-viewWidth);
    }

    private void translateRightOutside(){
        int viewWidth = topView.getWidth();
        translateOutside(viewWidth);
    }

    /**
     * 将顶层cardview移出界面
     */
    private void  translateOutside(float endPropertyValue){

        ObjectAnimator oa = ObjectAnimator.ofFloat(
                topView, "translationX", endPropertyValue);
        oa.setDuration(800);
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                afterFly();

                cardsCount--;

                updateCardViewsUI();
            }
        });

        oa.start();
    }


    public void flyToLeftOutside(){
        int topViewWidth = topView.getWidth();
        int topViewLeft = topView.getLeft();
        float endPropertyValue = -(topViewWidth + topViewLeft);
        translateOutside(endPropertyValue);
    }

    public void flyToRightOutside(){
        int topViewWidth = topView.getWidth();
        int topViewRight = topView.getRight();
        float endPropertyValue = topViewWidth + topViewRight;
        translateOutside(endPropertyValue);
    }

    public void initCardViews(){
        if(cardsCount == 0){
            throw new IllegalArgumentException("请先调用setCardsCount()初始化卡片总数，然后在调用该方法");
        }

        updateCardViewsUI();
    }


    /**
     * 移出后还原界面
     */
    private void afterFly(){
        Log.e("cardsCount","cardsCount:  "+cardsCount);
        Log.e("cardsCount","cardViewsCount:  "+cardViewsCount);


        if(cardsCount > cardViewsCount ) {
            topView.setTranslationX(0);
            topView.invalidate();
            requestLayout();
        }else{

            cardViews.remove(cardViewsCount - 1);
            overlapingcardViewsCount -= 1;
            cardViewsCount -= 1;
            topView = cardViews.get(cardViewsCount - 1);

            requestLayout();
        }
    }

    class CardDragCallback extends ViewDragHelper.Callback{

        /**
         * 用于决定ViewGroup中的哪个子view用于拖动。在该类中应该是顶部的view被拖动，即以保存的实例topDragView
         * @param child
         * @param pointerId
         * @return
         */
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if(topView == null){
                return false;
            }
            return topView == child;
        }

        /**
         * 用于实现水平方向的拖动
         * 使用translation实现平移，不用ViewDragHelper
         * 使用ViewDragHelper的方法，在松手后的平移是从未滑动前的位置开始平移动画的。
         * PS：没找到使用ViewDragHelper中平移修改的哪个变量值
         *
         * @param child 被拖动的child
         * @param left
         * @param dx
         * @return
         */
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return left;
        }

        /**
         *用于实现竖直方向的拖动
         * @param child
         * @param top
         * @param dy
         * @return
         */
        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return top;
        }

        /**
         * 用于实现松开手指后要执行的操作,本处是指在松开手指后，topView移出屏幕或者还原到原来位置
         * 如果topView在x轴至少有一半在屏幕外，则移出屏幕，否则还原到原来的位置
         * @param releasedChild
         * @param xvel
         * @param yvel
         */
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            int width  = releasedChild.getWidth();
            int left = releasedChild.getLeft();

            if( left <= -width / 2 ){
                //topView距离屏幕左边移出至少一半
                translateLeftOutside();

            }else if( left >= getWidth() - width / 2){
                //topView距离屏幕右边移出至少一半
                translateRightOutside();
            }else {
                dragHelper.settleCapturedViewAt(topViewLeft, topViewTop);
//                dragHelper.smoothSlideViewTo(releasedChild, topViewLeft, topViewTop);
                ViewCompat.postInvalidateOnAnimation(CardsDragFrameLayout.this);
                //还原topView位置
                for (int i = 1; i < cardViewsCount - 1; i++) {
                    cardViews.get(i).setScaleX(scaleXValues[i - 1]);
                    cardViews.get(i).setTranslationY(translateYValues[i - 1]);
                }
            }

            isSmoothSlideView = true;
        }

        /**
         * 拖动时界面的更新
         * @param changedView
         * @param left
         * @param top
         * @param dx
         * @param dy
         */
        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);

            //如果是松开手指后调用smoothSlideViewTo()方法导致的该方法被调用，则不做任何处理.
            if(isSmoothSlideView){
                isSmoothSlideView = false;
                return;
            }

            /**
             * 为了达到在拖动顶部view时，其他view逐渐显示出未拖动前的界面布局，需要作出如下变换
             * 最底部view位置保持不变.
             * 倒数第二个view位置最终要移动到到未拖动前倒数第三个view的位置，
             * 倒数第三个view位置最终要移动到到未拖动前倒数第四个view的位置，
             * 。。。
             * 正数第二个view位置最终要移动到到未拖动前顶部view的位置，
             * 最顶部view正在拖动中，因此在该方法中对该view不做处理.
             */
            for(int i = 1; i < cardViewsCount - 1; i++) {
                //沿x轴放大缩放变换，为了达到逐渐放大的效果，每次只缩放原来缩放值的 1 / 100
                float nowScaleX = cardViews.get(i).getScaleX();
                if (nowScaleX < scaleXValues[i]) {
                    float scaleValue = nowScaleX + scaleXOffest / 100;
//                    if(scaleValue > scaleXValues[i]){
//                        scaleValue = scaleXValues[i];
//                    }
                    cardViews.get(i).setScaleX(scaleValue);
                }
                //沿y轴负方向平移变换，为了达到逐渐平移的效果，每次只缩放原来平移值的 1 / 30
                float translationY = cardViews.get(i).getTranslationY();
                if (translationY > translateYValues[i]) {
                    float translateOffest = translationY - cardYOffest / 30;
                    cardViews.get(i).setTranslationY(translateOffest);
                }
            }
        }

        /**
         * 手指松开后，自行滑动的速度
         * @param child
         * @return
         */
        @Override
        public int getViewHorizontalDragRange(View child) {
            return 256;
        }
    }

    /**
     * 重写该方法是因为onViewReleased()方法中要调用smoothSlideViewTo()方法.
     */
    @Override
    public void computeScroll() {
        super.computeScroll();
        if(dragHelper == null){
            return;
        }
        if(dragHelper.continueSettling(true)){
            isSmoothSlideView = true;
            ViewCompat.postInvalidateOnAnimation(CardsDragFrameLayout.this);
        }
    }

    public interface UpdateCardViewListener{
        void updateCardView(int topCardPosi,ArrayList<ItemCardView> itemCardViews);
    }

    public void addListener(UpdateCardViewListener updateCardViewListener){
        this.updateCardViewListener = updateCardViewListener;
    }
}
