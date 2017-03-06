package com.clam.stickycircleview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private StickyCircleView scvRefresh;
    private Button btStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scvRefresh = (StickyCircleView)findViewById(R.id.scv_refresh);
        btStop = (Button)findViewById(R.id.bt_stop);

        scvRefresh.setOnReloadListener(new StickyCircleView.OnReloadListener() {
            @Override
            public void onReload() {
                Toast.makeText(MainActivity.this,"开始刷新",Toast.LENGTH_SHORT).show();
            }
        });
        btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scvRefresh.stopReload();
            }
        });
    }
}
