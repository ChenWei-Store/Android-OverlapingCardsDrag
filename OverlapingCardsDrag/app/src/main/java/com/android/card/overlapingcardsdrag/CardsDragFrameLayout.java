package com.android.card.overlapingcardsdrag;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;


import static android.support.v4.widget.ViewDragHelper.*;

/**
 * Created by shangshicc on 2016/8/28.
 * 布局可以放多个卡片，最上层卡片可以拖动,卡片水平居中
 */
public class CardsDragFrameLayout extends FrameLayout{
    private float scaleXOffest; //沿x轴缩放偏移量
    private int cardYOffest; //沿y轴平移偏移量
    private int viewBelowCardMargin; //bottomLayout距离底层cardview距离
    //    private int notDragCardsCount; // 在没有拖动时cardview的数量
    private boolean isHasBottomLayout; // ```是否有bottomLayout

    private ViewDragHelper dragHelper;

    private View topDragView; // 保存顶部用于拖动的view
    private int topViewLeft; //topView未拖动前与parentView在x轴上的距离
    private int topViewTop;//topView未拖动前与parentView在y轴上的距离

    private int cardsCount; //布局中的卡片数量
    private int overlapingCardsCount; // 重叠的卡片数量

    private ItemCardView[] cardViews; //重叠卡片view(由底部到顶部)

    private View bottomLayout; //底部view

    private float []scaleXValues; //重叠cardviews由底部到顶部的需要设置的x轴最终缩放值
    private int []translateYValues; //重叠cardviews由底部到顶部需要设置的y轴最终平移值

    private boolean isViewReleased;
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



    private void getXmlData(TypedArray typedArray){
        cardYOffest = typedArray.getDimensionPixelOffset
                (R.styleable.CardsDragFrameLayout_card_y_offest_value, 40);

        scaleXOffest = typedArray.getFloat
                (R.styleable.CardsDragFrameLayout_scale_x_offest_value, 0.08f);

//        viewBelowCardMargin = typedArray.getDimensionPixelOffset
//                (R.styleable.CardsDragFrameLayout_view_below_card_margin, 40);

        isHasBottomLayout = typedArray.getBoolean
                (R.styleable.CardsDragFrameLayout_isHasBottomLayout, false);
    }


    /**
     * 获得设置卡片重叠效果所需的缩放/平移参数.
     */
    private void getScalesAndTranslateParams(){
        scaleXValues = new float[overlapingCardsCount];
        translateYValues = new int[overlapingCardsCount];
        //设置起始x轴缩放值,以最外层的cardview作为标准(最外层cardview scaleX = 1)
        float scaleX = 1 - scaleXOffest * (overlapingCardsCount - 1 );
        //设置起始平移值，,以最外层的cardview作为标准(最外层cardview的平移值为0)
        int translateY = cardYOffest * (overlapingCardsCount - 1);

        for(int i = 0; i < overlapingCardsCount; i++){
            scaleXValues[i] = scaleX;
            translateYValues[i] = translateY;

            //更新平移值和缩放值
            scaleX += scaleXOffest;
            translateY -= cardYOffest;
        }
    }

    /**
     * 根据是否有bottomLayout计算cardview和overlapingCardsView 的数量
     *
     */
    private void getCardsCount(){
        int count = getChildCount();
        cardsCount = count;
        if(isHasBottomLayout){
            cardsCount = count - 1;
        }
        overlapingCardsCount = cardsCount - 1;

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        getCardsCount();

        int begin = 0;
        int  differenceValue = 0;
        //如果有botomview，保存其引用值
        if(isHasBottomLayout){
            bottomLayout = getChildAt(begin++);
            differenceValue = 1;
        }

        //保存所有cardview引用
        cardViews = new ItemCardView[cardsCount];
        for(int i = begin; i <getChildCount(); i++){
            ItemCardView itemCardView =(ItemCardView)getChildAt(i);
            cardViews[i - differenceValue] = itemCardView;
            cardViews[i - differenceValue].initTextView("name"+i,"other"+i);
        }

        //只需要初始化最顶部2个view界面即可
        cardViews[cardsCount - 1].initImageView(R.drawable.archer);
        cardViews[cardsCount - 2].initImageView(R.drawable.saber);
//        cardViews[cardsCount - 1].initTextView("name1","other1");
//        cardViews[cardsCount - 2].initTextView("name2", "other2");

        getScalesAndTranslateParams();


    }


    /**
     *  为了显示出重叠卡片的界面效果，主要做2个操作，1个是沿x轴缩放，一个是沿y轴向下平移
     *  从最里层到最外层，x轴缩放逐渐值逐渐增大，y轴平移值逐渐减少.
     *
     *  注意:为了达到将最顶层cardview拖动时，未拖动部分的cardview逐渐显示出拖动前的样式，
     *  必须在布局中多写一个cardview，同时，最底层cardview与倒数第二层cardview重叠
     *
     * @param changed
     * @param l 相对于父控件的左边距
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        //设置bottomLayout位置
        if(isHasBottomLayout){
            LayoutParams flParmas = (LayoutParams)
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
        ItemCardView itemCardView = cardViews[0];
        LayoutParams flParmas = (LayoutParams)
                itemCardView.getLayoutParams();
        int height = itemCardView.getMeasuredHeight();
        int width = itemCardView.getMeasuredWidth();
        int left = flParmas.leftMargin;
        int right = left + width;
        int top = flParmas.topMargin;
        int bottom = height + top;

        //设置cardview位置.
        for(int i = 0; i < cardsCount; i++){
            ItemCardView cardView = cardViews[i];
            //设置每个cardView的默认位置
            cardView.layout(left, top, right, bottom);

            //对当前cardview进行平移和缩放(注意，最底层的cardview要与倒数第二层的cardview显示在同一个位置上)
            if(i == 0) {
                cardView.setScaleX(scaleXValues[i]);
                cardView.setTranslationY(translateYValues[i]);
            }else {
                cardView.setScaleX(scaleXValues[i - 1]);
                cardView.setTranslationY(translateYValues[i - 1]);
            }
            Log.e("onLayout","---"+i+"---");
            Log.e("onLayout","---getTop---"+cardView.getTop());
            Log.e("onLayout","---"+cardView.getTranslationY()+"---");
        }

        //保存顶部用于拖动的view以及顶部view的坐标值
        if(overlapingCardsCount > 0) {
            topDragView = cardViews[cardsCount - 1];
            topViewLeft = topDragView.getLeft();
            topViewTop = topDragView.getTop();
        }

    }


//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        //不考虑里层view沿y轴平移时的高度和宽度
//        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
//
//        //里层view要沿y轴平移，重新计算高度。(最顶层view高度+里层view的偏移值)
////        int height = getMeasuredHeight();
////        int count = getChildCount();
////        height += cardYOffest*(count - 2);
////        setMeasuredDimension(getMeasuredWidth(),height);
//    }

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


    class CardDragCallback extends ViewDragHelper.Callback{

        /**
         * 用于决定ViewGroup中的哪个子view用于拖动。在该类中应该是顶部的view被拖动，即以保存的实例topDragView
         * @param child
         * @param pointerId
         * @return
         */
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if(topDragView == null){
                return false;
            }
            return topDragView == child;
        }

        /**
         * 用于实现水平方向的拖动
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
            Log.d("drag", "lastleft" + releasedChild.getLeft());
            isViewReleased = true;

            if( left <= -width / 2 ){
                //topView距离屏幕左边移出至少一半
                int top = releasedChild.getTop();
                dragHelper.smoothSlideViewTo( releasedChild, -width , top );

            }else if( left >= getWidth() - width / 2){
                //topView距离屏幕右边移出至少一半
                int top = releasedChild.getTop();
                int parentWidth = getWidth();
                dragHelper.smoothSlideViewTo(releasedChild,parentWidth,top);

            }else{
                //还原topView位置
                for(int i = 1; i < cardViews.length - 1; i++){
                    cardViews[i].setScaleX(scaleXValues[i - 1]);
                    Log.e("---onViewReleased---", "translate:" + i);
                    Log.e("---onViewReleased---","beforeTranslate"+cardViews[i].getTranslationY());
                    cardViews[i].setTranslationY(translateYValues[i - 1]);
                    Log.e("---onViewReleased---","translate"+cardViews[i].getTranslationY());
                }

                dragHelper.smoothSlideViewTo(releasedChild, topViewLeft, topViewTop);
            }
            ViewCompat.postInvalidateOnAnimation(CardsDragFrameLayout.this);
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

            if(isViewReleased){
                isViewReleased = false;
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
            for(int i = 1; i < cardViews.length - 1; i++) {
                //沿x轴放大缩放变换，为了达到逐渐放大的效果，每次只缩放原来缩放值的 1 / 100
                float nowScaleX = cardViews[i].getScaleX();
                if (nowScaleX < scaleXValues[i]) {
                    float scaleValue = nowScaleX + scaleXOffest / 100;
//                    if(scaleValue > scaleXValues[i]){
//                        scaleValue = scaleXValues[i];
//                    }
                    cardViews[i].setScaleX(scaleValue);
                }
                //沿y轴负方向平移变换，为了达到逐渐平移的效果，每次只缩放原来平移值的 1 / 30
                float translationY = cardViews[i].getTranslationY();
                if (translationY > translateYValues[i]) {
                    float translateOffest = translationY - cardYOffest / 30;
                    cardViews[i].setTranslationY(translateOffest);
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
            ViewCompat.postInvalidateOnAnimation(CardsDragFrameLayout.this);
            isViewReleased = true;
        }
    }
}
