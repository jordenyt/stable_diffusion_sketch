package com.jsoft.diffusionpaint.helper;

public interface SdApiResponseListener {
    void onSdApiFailure(String requestType, String errorMessage);
    void onSdApiResponse(String requestType, String responseBody);
}
