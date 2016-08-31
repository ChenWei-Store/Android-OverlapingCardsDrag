package com.android.card.overlapingcardsdrag;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class CardsActivity extends AppCompatActivity {
    private int []imgvs;

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

    }

}
