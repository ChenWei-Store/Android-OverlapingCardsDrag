package com.android.card.overlapingcardsdrag;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by shangshicc on 2016/8/28.
 * 布局可以放多个卡片，最上层卡片可以拖动,卡片水平居中
 */
public class CardsDragFrameLayout extends FrameLayout {
    private float scaleXOffest = 0.08f; //沿x轴缩放偏移量
    private int cardYOffest = 40; //沿y轴平移偏移量

    public CardsDragFrameLayout(Context context) {
        super(context);
    }

    public CardsDragFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CardsDragFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes
                (attrs, R.styleable.CardsDragFrameLayout);
//        cardYOffest = typedArray.getDimensionPixelOffset
//                (R.styleable.CardsDragFrameLayout_card_y_offest_value,40);
//        scaleXOffest = typedArray.getFloat
//                (R.styleable.CardsDragFrameLayout_scale_x_offest_value,0.08f);

        typedArray.recycle();
    }

    /**
     *  为了显示出重叠卡片的界面效果，主要做2个操作，1个是沿x轴缩放，一个是沿y轴平移
     *  从最里层到最外层，x轴缩放逐渐值逐渐增大，y轴平移值逐渐减少
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

}
