package com.hs_augsburg_example.lightscatcher;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class DatenschutzActivity extends AppCompatActivity {

    private WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datenschutz);

        webview = (WebView) findViewById(R.id.webview);
        webview.loadUrl("http://www.google.com");
    }
}
