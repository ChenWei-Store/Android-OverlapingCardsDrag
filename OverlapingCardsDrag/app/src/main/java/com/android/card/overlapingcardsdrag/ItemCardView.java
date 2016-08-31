package com.android.card.overlapingcardsdrag;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by shangshicc on 2016/8/28.
 */
public class ItemCardView extends LinearLayout {
    private TextView nameTv;
    private TextView otherTv;
    private ImageView cardImgv;
    public ItemCardView(Context context) {
        this(context, null);
    }

    public ItemCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //加载布局
        setBackgroundResource(R.drawable.card_view_bg);
        setOrientation(LinearLayout.VERTICAL);
        inflate(context, R.layout.item_card, this);
        nameTv =(TextView) findViewById(R.id.name);
        otherTv = (TextView)findViewById(R.id.other);
        cardImgv = (ImageView)findViewById(R.id.photo);
    }

    public void initTextView(String name,String other){
        nameTv.setText(name);
        otherTv.setText(other);
    }

    public void initImageView(int resId){
        cardImgv.setImageResource(resId);
    }
}
