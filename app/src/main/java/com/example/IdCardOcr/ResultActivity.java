package com.example.IdCardOcr;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.IdCardOcr.model.IdentifyResult;

//身份证识别结果展示页面
public class ResultActivity extends AppCompatActivity {
    //UI控件
    private TextView tvName;
    private TextView tvGender;
    private TextView tvNation;
    private TextView tvBirthDate;
    private TextView tvAddress;
    private TextView tvIdNumber;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initViews();
        displayResult();
        //点击返回上一页
        btnBack.setOnClickListener(v -> finish());
    }

    //绑定UI控件
    private void initViews() {
        tvName = findViewById(R.id.tvName);
        tvGender = findViewById(R.id.tvGender);
        tvNation = findViewById(R.id.tvNation);
        tvBirthDate = findViewById(R.id.tvBirthDate);
        tvAddress = findViewById(R.id.tvAddress);
        tvIdNumber = findViewById(R.id.tvIdNumber);
        btnBack = findViewById(R.id.btnBack);
    }

    //从Intent获取识别结果并显示
    private void displayResult() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("result")) {
            //反序列化获取结果对象
            IdentifyResult result = (IdentifyResult) intent.getSerializableExtra("result");
            if (result != null) {
                tvName.setText("姓名：" + result.getName());
                tvGender.setText("性别：" + result.getSex());
                tvNation.setText("民族：" + result.getNation());
                tvBirthDate.setText("出生日期：" + result.getBirth());
                tvAddress.setText("地址：" + result.getAddress());
                tvIdNumber.setText("身份证号：" + result.getId());
            }
        }
    }
}