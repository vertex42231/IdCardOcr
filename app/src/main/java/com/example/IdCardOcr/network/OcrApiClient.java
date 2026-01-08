package com.example.IdCardOcr.network;

import android.util.Log;

import com.example.IdCardOcr.model.IdentifyResult;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//腾讯云OCR API客户端
public class OcrApiClient {
    //API配置常量
    private static final String URL = "https://ocr.tencentcloudapi.com";
    private static final String ACTION = "IDCardOCR";
    private static final String VERSION = "2018-11-19";
    private static final String REGION = "ap-guangzhou";
    private static final String MEDIA_TYPE = "application/json; charset=utf-8";
    private static final String CARD_SIDE_FRONT = "FRONT";
    private static final int TIMEOUT = 30;
    //HTTP客户端和JSON解析器
    private final OkHttpClient httpClient;
    private final Gson gson;
    //单例实例
    private static OcrApiClient instance;

    //双重检查锁获取单例
    public static OcrApiClient getInstance() {
        if (instance == null) {
            synchronized (OcrApiClient.class) {
                if (instance == null) instance = new OcrApiClient();
            }
        }
        return instance;
    }

    //私有构造函数，配置OkHttp超时参数
    private OcrApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    //OCR识别回调接口
    public interface Callback {
        void onSuccess(IdentifyResult result);
        void onFailure(String error);
    }

    //异步识别身份证
    public void recognizeIdCard(String imageBase64, Callback callback) {
        try {
            String requestBody = buildRequestBody(imageBase64);
            sendRequest(requestBody, callback);
        } catch (Exception e) {
            if (callback != null) callback.onFailure("构建请求失败: " + e.getMessage());
        }
    }

    //构建请求体JSON
    private String buildRequestBody(String imageBase64) {
        RequestParams params = new RequestParams();
        params.ImageBase64 = imageBase64;
        params.CardSide = CARD_SIDE_FRONT;
        return gson.toJson(params);
    }

    //发送HTTP请求
    private void sendRequest(final String requestBodyJson, final Callback callback) {
        final long timestamp = System.currentTimeMillis() / 1000;
        String secretId = SignHelper.getSecretId();
        String secretKey = SignHelper.getSecretKey();
        //生成腾讯云API V3签名
        String authorization = SignHelper.generateSign(secretId, secretKey, requestBodyJson, timestamp);

        if (authorization == null) {
            if (callback != null) callback.onFailure("生成签名失败");
            return;
        }

        //构建HTTP请求
        MediaType mediaType = MediaType.parse(MEDIA_TYPE);
        RequestBody requestBody = RequestBody.create(requestBodyJson, mediaType);
        Request request = new Request.Builder()
                .url(URL)
                .post(requestBody)
                .addHeader("Authorization", authorization)
                .addHeader("Content-Type", MEDIA_TYPE)
                .addHeader("Host", SignHelper.getHost())
                .addHeader("X-TC-Action", ACTION)
                .addHeader("X-TC-Version", VERSION)
                .addHeader("X-TC-Timestamp", String.valueOf(timestamp))
                .addHeader("X-TC-Region", REGION)
                .build();

        //异步执行请求
        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onFailure("网络请求失败: " + e.getMessage());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        if (callback != null) callback.onFailure("HTTP错误: " + response.code());
                        return;
                    }
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (responseBody.isEmpty()) {
                        if (callback != null) callback.onFailure("响应体为空");
                        return;
                    }
                    Log.d("OCR_API", "响应内容: " + responseBody);
                    //解析JSON响应
                    OcrResponse ocrResp = gson.fromJson(responseBody, OcrResponse.class);
                    if (ocrResp == null || ocrResp.getResponse() == null) {
                        if (callback != null) callback.onFailure("API返回数据为空");
                        return;
                    }
                    //封装识别结果
                    OcrResponse.ResponseData data = ocrResp.getResponse();
                    IdentifyResult result = new IdentifyResult();
                    result.setName(data != null ? data.name : null);
                    result.setSex(data != null ? data.sex : null);
                    result.setNation(data != null ? data.nation : null);
                    result.setBirth(data != null ? data.birth : null);
                    result.setAddress(data != null ? data.address : null);
                    result.setId(data != null ? data.idNum : null);
                    if (callback != null) callback.onSuccess(result);
                } catch (Exception e) {
                    Log.e("OCR_API", "解析响应失败: " + e.getMessage());
                    if (callback != null) callback.onFailure("解析响应失败: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    //请求参数内部类
    private static class RequestParams {
        private String ImageBase64;
        private String CardSide;
    }

    //OCR响应内部类
    private static class OcrResponse {
        @SerializedName("Response")
        private ResponseData response;
        public ResponseData getResponse() { return response; }
        //响应数据字段映射
        private static class ResponseData {
            @SerializedName("Name")
            public String name;
            @SerializedName("Sex")
            public String sex;
            @SerializedName("Nation")
            public String nation;
            @SerializedName("Birth")
            public String birth;
            @SerializedName("Address")
            public String address;
            @SerializedName("IdNum")
            public String idNum;
        }
    }
}