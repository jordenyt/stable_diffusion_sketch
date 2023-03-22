package com.jsoft.diffusionpaint.helper;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

public interface SdApiResponseListener {
    void onSdApiFailure();
    void onSdApiResponse(String requestType, String responseBody);
}
