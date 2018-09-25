package com.example.silence.rulerview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.library.RulerView;

public class MainActivity extends AppCompatActivity {


    private RulerView rulerView;
    private TextView valueTV;
    private EditText valueET;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        valueTV=findViewById(R.id.valueTV);
        rulerView=findViewById(R.id.ruleView);
        rulerView.setListener(new RulerView.OnValueChangeListener() {
            @Override
            public void onValueChange(String value) {
                valueTV.setText(value);
            }
        });


        valueET=findViewById(R.id.valueET);
        button=findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String value=valueET.getText().toString();
                if (TextUtils.isEmpty(value)){
                    return;
                }
                float valueF=Float.parseFloat(value);
                rulerView.setCurrentValue(valueF);
            }
        });



    }












}
