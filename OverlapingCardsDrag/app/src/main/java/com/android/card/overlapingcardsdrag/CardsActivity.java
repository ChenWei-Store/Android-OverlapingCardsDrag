package com.android.card.overlapingcardsdrag;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;


public class CardsActivity extends AppCompatActivity {
    private int []imgvs;

    private ImageView toLeftImgv;
    private ImageView toRightImgv;
    private CardsDragFrameLayout cardsDragFrameLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgvs = new int[5];
        imgvs[0] = R.drawable.background;
        imgvs[1] = R.drawable.bg2;
        imgvs[2] = R.drawable.archer;
        imgvs[3] = R.drawable.saber;
        imgvs[4] = R.drawable.shilang2;

        toLeftImgv = (ImageView)findViewById(R.id.to_left_imgv);
        toRightImgv = (ImageView)findViewById(R.id.to_right_imgv);
        cardsDragFrameLayout = (CardsDragFrameLayout)findViewById(R.id.cards_layout);


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

}
