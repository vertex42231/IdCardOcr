package com.example.IdCardOcr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.IdCardOcr.model.Base64Util;
import com.example.IdCardOcr.model.IdentifyResult;
import com.example.IdCardOcr.network.OcrApiClient;
import com.example.IdCardOcr.network.SignHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

//主界面Activity：拍照或选择图片进行身份证OCR识别
public class MainActivity extends AppCompatActivity {
    //权限请求码
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    //图片URI
    private Uri photoUri;
    private Uri selectedImageUri;
    //OCR客户端
    private OcrApiClient ocrApiClient;
    //UI控件
    private TextView titleText;
    private ImageView photoView;
    private Button btnTakePhoto;
    private Button btnSelectImage;
    private Button btnUpload;
    //Activity结果启动器
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> selectImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        //初始化腾讯云密钥
        SignHelper.init(getApplicationContext());
        initViews();
        initOcrClient();
        initLaunchers();
        setupListeners();
        setupEdgeToEdge();
    }

    //初始化OCR客户端单例
    private void initOcrClient() { ocrApiClient = OcrApiClient.getInstance(); }

    //绑定UI控件
    private void initViews() {
        titleText = findViewById(R.id.titleText);
        photoView = findViewById(R.id.photoView);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnUpload = findViewById(R.id.btnUpload);
    }

    //初始化Activity结果启动器
    private void initLaunchers() {
        //拍照结果回调处理
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        selectedImageUri = null;
                        photoView.setImageDrawable(null);
                        //优先从Intent获取缩略图
                        if (data != null && data.getExtras() != null) {
                            Bitmap thumbnailBitmap = (Bitmap) data.getExtras().get("data");
                            if (thumbnailBitmap != null) {
                                photoView.setImageBitmap(thumbnailBitmap);
                                Toast.makeText(this, "照片拍摄成功", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        //从photoUri加载完整图片
                        if (photoUri != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                                photoView.setImageBitmap(bitmap);
                                Toast.makeText(this, "照片拍摄成功", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Toast.makeText(this, "加载照片失败", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "照片数据获取失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        //相册选择结果回调处理
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        photoUri = null;
                        photoView.setImageDrawable(null);
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            photoView.setImageURI(selectedImageUri);
                            Toast.makeText(this, "图片选择成功", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //设置按钮点击监听器
    private void setupListeners() {
        btnTakePhoto.setOnClickListener(v -> {
            if (checkCameraPermission()) { openCamera(); } else { requestCameraPermission(); }
        });
        btnSelectImage.setOnClickListener(v -> {
            if (checkStoragePermission()) { openImageSelector(); } else { requestStoragePermission(); }
        });
        btnUpload.setOnClickListener(v -> uploadAndRecognize());
    }

    //设置全面屏边距适配
    private void setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    //检查相机权限
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    //请求相机权限
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    //检查存储权限，Android13以上使用新权限
    private boolean checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    //请求存储权限
    private void requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_STORAGE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
    }

    //打开系统相机
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                //创建临时文件存储照片
                File photoFile = createImageFile();
                if (photoFile != null) {
                    //通过FileProvider获取安全的content://URI
                    photoUri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    cameraLauncher.launch(intent);
                }
            } catch (IOException ex) {
                Toast.makeText(this, "创建照片文件失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "没有可用的相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    //打开系统相册选择图片
    private void openImageSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        selectImageLauncher.launch(intent);
    }

    //创建临时图片文件
    private File createImageFile() throws IOException {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    //权限请求结果回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "相机权限被拒绝，无法拍照", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImageSelector();
            } else {
                Toast.makeText(this, "存储权限被拒绝，无法选择图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //上传图片并调用OCR识别
    private void uploadAndRecognize() {
        //检查密钥是否配置
        if (!SignHelper.hasValidCredentials()) {
            Toast.makeText(this, "错误：腾讯云密钥未配置，请检查assets/env文件", Toast.LENGTH_LONG).show();
            return;
        }
        //检查是否已选择图片
        if (photoUri == null && selectedImageUri == null) {
            Toast.makeText(this, "请先拍照或选择图片", Toast.LENGTH_SHORT).show();
            return;
        }
        showLoading();
        Uri imageUri = (selectedImageUri != null) ? selectedImageUri : photoUri;
        try {
            //将图片转换为Base64编码
            String base64Image = convertImageToBase64(imageUri);
            if (base64Image == null || base64Image.isEmpty()) {
                hideLoading();
                Toast.makeText(this, "图片转换失败", Toast.LENGTH_SHORT).show();
                return;
            }
            //异步调用OCR接口
            ocrApiClient.recognizeIdCard(base64Image, new OcrApiClient.Callback() {
                @Override
                public void onSuccess(IdentifyResult result) {
                    //切换到主线程更新UI
                    runOnUiThread(() -> {
                        hideLoading();
                        navigateToResultActivity(result);
                    });
                }
                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(MainActivity.this, "识别失败: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "处理图片异常: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //将URI对应的图片转换为Base64字符串
    private String convertImageToBase64(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            return bitmapToBase64(bitmap);
        } catch (IOException e) {
            return null;
        }
    }

    //Bitmap转Base64编码
    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //压缩为JPEG格式，质量80%
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            return Base64Util.encode(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    //跳转到结果展示页面
    private void navigateToResultActivity(IdentifyResult result) {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("result", result);
        startActivity(intent);
    }

    private void showLoading() { Toast.makeText(this, "正在识别中，请稍候...", Toast.LENGTH_SHORT).show(); }
    private void hideLoading() { }
}