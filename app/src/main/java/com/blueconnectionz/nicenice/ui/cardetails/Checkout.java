package com.blueconnectionz.nicenice.ui.cardetails;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.blueconnectionz.nicenice.R;
import com.blueconnectionz.nicenice.utils.Common;

public class Checkout extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        // Set the status bar color to white
        Common.setStatusBarColor(getWindow(),this);
    }
}