package com.minimalhealth.app;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AboutActivity extends AppCompatActivity {

    private static final String[] QUOTES = {
            "「没有全民健康，就没有全面小康」—— 习近平",
            "「健康是1，其他是后面的0」—— 健康中国2030",
            "「每天锻炼一小时，健康工作五十年，幸福生活一辈子」",
            "「合理膳食、适量运动、戒烟限酒、心理平衡」—— 健康四大基石",
            "「要倡导健康文明的生活方式，树立大卫生、大健康的观念」"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("关于");
        }

        TextView healthQuote = findViewById(R.id.health_quote);
        healthQuote.setText(QUOTES[(int) (System.currentTimeMillis() / 1000 / 3600) % QUOTES.length]);

        TextView appDesc = findViewById(R.id.app_description);
        appDesc.setText("MinimalHealth Pro 极简健康管家\n\n"
                + "本应用响应「健康中国2030」国家战略号召，以科技创新助力全民健康。\n\n"
                + "我们致力于通过智能化手段帮助用户建立科学的运动、饮食、饮水习惯，"
                + "从日常点滴做起，为建设健康中国贡献力量。");

        TextView techInfo = findViewById(R.id.tech_info);
        techInfo.setText("技术栈：Vue 3 + Spring Boot 3 + Capacitor 7\n"
                + "数据库：H2（服务端） + SQLite（本地）\n"
                + "版本：1.0\n"
                + "开发：MinimalHealth Team");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}