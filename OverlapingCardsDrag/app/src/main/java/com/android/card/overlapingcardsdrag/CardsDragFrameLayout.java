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
    private float scaleXOffest = 0.08f; //沿x轴缩放偏移量
    private int cardYOffest = 40; //沿y轴平移偏移量
    private int cardRemoveOffest = 40;

    private ViewDragHelper dragHelper;

    private View topDragView; // 保存顶部用于拖动的view
    private int topViewLeft; //topView未拖动前与parentView在x轴上的距离
    private int topViewTop;//topView未拖动前与parentView在y轴上的距离



    public CardsDragFrameLayout(Context context) {
        this(context, null);
    }

    public CardsDragFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardsDragFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        dragHelper = create(this, 1f, new CardDragCallback());

        TypedArray typedArray = context.obtainStyledAttributes
                (attrs, R.styleable.CardsDragFrameLayout);
        cardYOffest = typedArray.getDimensionPixelOffset
                (R.styleable.CardsDragFrameLayout_card_y_offest_value,40);
        scaleXOffest = typedArray.getFloat
                (R.styleable.CardsDragFrameLayout_scale_x_offest_value,0.08f);

        typedArray.recycle();
    }



    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // 渲染完成，初始化卡片view列表
        int count = getChildCount();
        int []resId = {R.drawable.shilang2,R.drawable.archer,R.drawable.bg2};

        for(int i = 0; i < count;i++){
            ItemCardView itemCardView2 =(ItemCardView)getChildAt(i);
            itemCardView2.initTextView("name"+i,"other"+i);
            itemCardView2.initImageView(resId[i]);
            Log.d("cardsDragLayout", "---onFinishInflate---posi--" + i);
            Log.d("cardsDragLayout", "---onFinishInflate---id--" + itemCardView2.getId());
        }


    }

    /**
     *  为了显示出重叠卡片的界面效果，主要做2个操作，1个是沿x轴缩放，一个是沿y轴向下平移
     *  从最里层到最外层，x轴缩放逐渐值逐渐增大，y轴平移值逐渐减少】
     *
     * @param changed
     * @param l 相对于父控件的左边距
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();

        //设置起始x轴缩放值,以最外层的cardview作为标准(最外层cardview scaleX = 1)
        float scaleXValue = 1 - scaleXOffest * (count - 1);
        //设置起始平移值，,以最外层的cardview作为标准(最外层cardview的平移值为0)
        int cardOffestValue = cardYOffest * (count - 1);
        for(int i = 0; i < count; i++ ){
            View childView = getChildAt(i);
            LayoutParams flParmas = (LayoutParams)
                    childView.getLayoutParams();

            int height = childView.getMeasuredHeight();
            int width = childView.getMeasuredWidth();
            int left = flParmas.leftMargin;
            int right = left + width;
            int top = flParmas.topMargin;
            int bottom = height + top;

            //设置每个cardView的默认位置
            childView.layout(left, top, right, bottom);

            //对当前cardview进行平移和缩放
            childView.setScaleX(scaleXValue);
            childView.offsetTopAndBottom(cardOffestValue);


            //更新平移值和缩放值
            scaleXValue += scaleXOffest;
            cardOffestValue -= cardYOffest;
        }

        //保存顶部用于拖动的view以及丁部view的坐标值
        topDragView = getChildAt(count - 1);
        topViewLeft = topDragView.getLeft();
        topViewTop = topDragView.getTop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //不考虑里层view沿y轴平移时的高度和宽度
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);

        //里层view要沿y轴平移，重新计算高度。(最顶层view高度+里层view的偏移值)
        int height = getMeasuredHeight();
        int count = getChildCount();
        height += cardYOffest*(--count);
        setMeasuredDimension(getMeasuredWidth(),height);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if(ev.getAction() == MotionEvent.ACTION_CANCEL){
            dragHelper.cancel();
            return false;
        }
        return dragHelper.shouldInterceptTouchEvent(ev);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
            //
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
         * 如果topView在x轴至少有一半移出屏幕，则移出，否则还原到原来的位置
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


            if( left <= -width / 2 ){
                //topView距离屏幕左边移出至少一半
                int top = releasedChild.getTop();
                dragHelper.smoothSlideViewTo( releasedChild, -width -cardRemoveOffest , top );

            }else if( left >= getWidth() - width / 2){
                //topView距离屏幕右边移出至少一半
                int top = releasedChild.getTop();
                int parentWidth = getWidth();
                dragHelper.smoothSlideViewTo(releasedChild,parentWidth+cardRemoveOffest,top);

            }else{
                //还原topView位置
                dragHelper.smoothSlideViewTo(releasedChild, topViewLeft, topViewTop);
            }
            ViewCompat.postInvalidateOnAnimation(CardsDragFrameLayout.this);
        }
    }

    /**
     * 重写该方法是因为onViewReleased()方法中要调用smoothSlideViewTo()方法.
     */
    @Override
    public void computeScroll() {
        super.computeScroll();
        if(dragHelper.continueSettling(true)){
            ViewCompat.postInvalidateOnAnimation(CardsDragFrameLayout.this);
        }
    }
}
