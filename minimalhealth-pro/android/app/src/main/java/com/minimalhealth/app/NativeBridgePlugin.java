package com.minimalhealth.app;

import android.content.Intent;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "NativeBridge")
public class NativeBridgePlugin extends Plugin {

    @PluginMethod
    public void openAbout(PluginCall call) {
        Intent intent = new Intent(getActivity(), AboutActivity.class);
        getActivity().startActivity(intent);
        call.resolve();
    }
}
