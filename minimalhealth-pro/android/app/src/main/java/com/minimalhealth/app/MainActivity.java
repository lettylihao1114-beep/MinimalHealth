package com.minimalhealth.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        registerPlugin(NativeBridgePlugin.class); // 必须在 super.onCreate() 之前，否则 Bridge 已创建完毕
        super.onCreate(savedInstanceState);
    }
}
