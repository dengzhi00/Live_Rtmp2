package com.dzm.rtmp2;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private FVDialog fvDialog;
    private TextView ej_tv_title;
    private EditText rtmp_path;
    private Camera.Size mSize;
    private int encodeType;
    private EditText ej_tv_ml;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ej_tv_title = (TextView) findViewById(R.id.ej_tv_title);
        rtmp_path = (EditText) findViewById(R.id.rtmp_path);
        ej_tv_ml = (EditText) findViewById(R.id.ej_tv_ml);
        fvDialog = new FVDialog(this, new FVDialog.FVDialogListener() {
            @Override
            public void fVListener(Camera.Size size) {
                ej_tv_title.setText(size.width+"*"+size.height);
                mSize = size;
            }
        });
        findViewById(R.id.lv_ll_fv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fvDialog.show();
            }
        });

        RadioGroup RadioGroup01 = (RadioGroup) findViewById(R.id.RadioGroup01);
        RadioGroup01.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.RadioButton1){
                    encodeType = 100;
                }else if(checkedId == R.id.RadioButton2){
                    encodeType = 200;
                }
            }
        });

        findViewById(R.id.go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null == mSize){
                    return;
                }
                String bitrateStr = ej_tv_ml.getText().toString();
                if(TextUtils.isEmpty(bitrateStr)){
                    return;
                }
                Intent intent = new Intent(MainActivity.this,PushActivity.class);
                String path = rtmp_path.getText().toString();
                Bundle bundle = new Bundle();
                bundle.putString("path",path);
                bundle.putInt("width",mSize.width);
                bundle.putInt("height",mSize.height);
                bundle.putInt("encodeType",encodeType);
                bundle.putInt("bitrate",Integer.valueOf(bitrateStr));
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

    }
}
