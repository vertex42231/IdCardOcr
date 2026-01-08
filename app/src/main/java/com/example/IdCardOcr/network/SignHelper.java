package com.example.IdCardOcr.network;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

//腾讯云API V3签名辅助类
public class SignHelper {
    //服务配置常量
    private static final String HOST = "ocr.tencentcloudapi.com";
    private static final String SERVICE = "ocr";
    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    private static final String TERMINATOR = "tc3_request";
    private static final String HTTP_METHOD = "POST";
    private static final String HTTP_URI = "/";
    private static final String CONTENT_TYPE = "application/json; charset=utf-8";
    //密钥存储
    private static String SECRET_ID = null;
    private static String SECRET_KEY = null;
    private static boolean HAS_SECRET_ID = false;
    private static boolean HAS_SECRET_KEY = false;

    //从assets/env文件加载密钥
    public static void init(Context context) {
        try {
            InputStream is = context.getAssets().open("env");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            //逐行读取配置
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains("TENCENT_SECRET_ID")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        SECRET_ID = parts[1].trim();
                        HAS_SECRET_ID = SECRET_ID != null && !SECRET_ID.isEmpty();
                    }
                } else if (line.contains("TENCENT_SECRET_KEY")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        SECRET_KEY = parts[1].trim();
                        HAS_SECRET_KEY = SECRET_KEY != null && !SECRET_KEY.isEmpty();
                    }
                }
            }
            reader.close();
            Log.d("SignHelper", "密钥加载成功");
        } catch (IOException e) {
            Log.e("SignHelper", "加载env文件失败: " + e.getMessage());
        }
    }

    //生成腾讯云API V3签名
    public static String generateSign(String secretId, String secretKey, String requestBody, long timestamp) {
        try {
            //构建规范请求串
            String canonicalRequest = buildCanonicalRequest(requestBody);
            String date = getUtcDate(timestamp);
            String credentialScope = date + "/" + SERVICE + "/" + TERMINATOR;
            String hashedCanonicalRequest = sha256Hash(canonicalRequest);
            //构建待签名字符串
            String stringToSign = buildStringToSign(timestamp, credentialScope, hashedCanonicalRequest);
            //派生签名密钥
            byte[] secretSigningKey = deriveSigningKey(secretKey, date);
            //计算签名
            String signature = hmacSha256(secretSigningKey, stringToSign);
            return buildAuthorization(secretId, credentialScope, signature);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //构建规范请求串
    private static String buildCanonicalRequest(String requestBody) {
        String hashedRequestBody = sha256Hash(requestBody);
        String canonicalHeaders = "content-type:" + CONTENT_TYPE + "\n" + "host:" + HOST + "\n";
        String signedHeaders = "content-type;host";
        return HTTP_METHOD + "\n" + HTTP_URI + "\n" + "\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedRequestBody;
    }

    //构建待签名字符串
    private static String buildStringToSign(long timestamp, String credentialScope, String hashedCanonicalRequest) {
        return ALGORITHM + "\n" + timestamp + "\n" + credentialScope + "\n" + hashedCanonicalRequest;
    }

    //派生签名密钥，三次HMAC-SHA256计算
    private static byte[] deriveSigningKey(String secretKey, String date) {
        byte[] secretDate = hmacSha256Bytes(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256Bytes(secretDate, SERVICE);
        return hmacSha256Bytes(secretService, TERMINATOR);
    }

    //构建Authorization头部
    private static String buildAuthorization(String secretId, String credentialScope, String signature) {
        String signedHeaders = "content-type;host";
        return ALGORITHM + " " + "Credential=" + secretId + "/" + credentialScope + ", " + "SignedHeaders=" + signedHeaders + ", " + "Signature=" + signature;
    }

    //SHA256哈希计算
    private static String sha256Hash(String input) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(input.getBytes());
            StringBuilder builder = new StringBuilder();
            for (byte b : sha.digest()) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) hex = '0' + hex;
                builder.append(hex);
            }
            return builder.toString().toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    //HMAC-SHA256计算，返回十六进制字符串
    private static String hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmac);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    //HMAC-SHA256计算，返回字节数组
    private static byte[] hmacSha256Bytes(byte[] key, String msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
            mac.init(secretKeySpec);
            return mac.doFinal(msg.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    //字节数组转十六进制字符串
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(String.format("%02x", b));
        return result.toString();
    }

    //获取UTC日期字符串
    private static String getUtcDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(timestamp * 1000));
    }

    //Getter方法
    public static String getSecretId() { return SECRET_ID; }
    public static String getSecretKey() { return SECRET_KEY; }
    public static String getHost() { return HOST; }
    public static String getService() { return SERVICE; }
    public static boolean hasValidCredentials() { return HAS_SECRET_ID && HAS_SECRET_KEY; }

    //获取密钥配置状态
    public static String getCredentialsStatus() {
        if (!HAS_SECRET_ID && !HAS_SECRET_KEY) return "密钥未配置（env文件未找到或内容为空）";
        String idStatus = HAS_SECRET_ID ? "已设置(" + SECRET_ID.substring(0, Math.min(10, SECRET_ID.length())) + "...)" : "未设置";
        String keyStatus = HAS_SECRET_KEY ? "已设置(" + SECRET_KEY.substring(0, Math.min(10, SECRET_KEY.length())) + "...)" : "未设置";
        return "SECRET_ID=" + idStatus + ", SECRET_KEY=" + keyStatus;
    }
}