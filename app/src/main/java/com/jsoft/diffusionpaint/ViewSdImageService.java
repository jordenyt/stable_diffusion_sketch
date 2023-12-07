package com.jsoft.diffusionpaint;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ViewSdImageService extends Service {
    public ViewSdImageService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}