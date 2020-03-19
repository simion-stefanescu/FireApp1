package com.example.fireapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button mRegisterButton,mLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRegisterButton = findViewById(R.id.register_btn);
        mLoginButton = findViewById(R.id.login_btn);

        //handle register button click
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Register Activity
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            }
        });
    }
}
