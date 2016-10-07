package com.android.card.overlapingcardsdrag;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;


public class CardsActivity extends AppCompatActivity {
    private int []imgvs;

    private ImageView toLeftImgv;
    private ImageView toRightImgv;
    private CardsDragFrameLayout cardsDragFrameLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();

        findViewId();

        bindDataToCardsLayout();

        toLeftImgv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardsDragFrameLayout.flyToLeftOutside();
            }
        });

        toRightImgv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardsDragFrameLayout.flyToRightOutside();
            }
        });
    }


    private void initData(){
        imgvs = new int[5];
        imgvs[0] = R.drawable.background;
        imgvs[1] = R.drawable.bg2;
        imgvs[2] = R.drawable.archer;
        imgvs[3] = R.drawable.saber;
        imgvs[4] = R.drawable.shilang2;
    }


    private void findViewId(){
        toLeftImgv = (ImageView)findViewById(R.id.to_left_imgv);
        toRightImgv = (ImageView)findViewById(R.id.to_right_imgv);
        cardsDragFrameLayout = (CardsDragFrameLayout)findViewById(R.id.cards_layout);
    }

    private void bindDataToCardsLayout(){
        cardsDragFrameLayout.addListener(new CardsDragFrameLayout.UpdateCardViewListener() {
            @Override
            public void updateCardView(int topCardPosi,ArrayList<ItemCardView> itemCardViews) {
                int imgvsPosi = topCardPosi;

                for(int i = 0; i < itemCardViews.size(); i++){
                    ItemCardView itemCardView = itemCardViews.get(i);
                    itemCardView.initTextView("name:" + imgvsPosi, String.valueOf(imgvsPosi));
                    itemCardView.getCardImgv().setImageResource(imgvs[imgvsPosi++]);
                }
            }
        });

        //先设置cardsCount,在初始化cardview
        cardsDragFrameLayout.setCardsCount(imgvs.length);
        //初始化cardview布局前请先设置监听器和cardsCount
        cardsDragFrameLayout.initCardViews();

    }


}
