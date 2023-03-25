package com.jsoft.diffusionpaint.helper;

public interface SdApiResponseListener {
    void onSdApiFailure(String requestType);
    void onSdApiResponse(String requestType, String responseBody);
}
