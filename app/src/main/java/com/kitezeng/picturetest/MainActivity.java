package com.kitezeng.picturetest;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.io.ByteStreams;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.NameValuePair;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.message.BasicNameValuePair;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView messageText;
    private Button uploadButton, btnselectpic;
    private ImageView imageView;
    private int serverResponseCode = 0;
    private Dialog dialog;
    private String uploadServerUri = "http://localhost:8080/upload";
    private String imagepath = "";
    private static Handler mainThreadHandler;
    private static final int TIME_OUT = 10 * 1000; // 超時時間
    private static final String CHARSET = "utf-8"; // 設定編碼

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findView();
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            //驗證是否許可權限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申請權限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                }
            }
        }
    }

    private void findView() {
        uploadButton = findViewById(R.id.uploadButton);
        messageText = findViewById(R.id.IbImessage);
        btnselectpic = findViewById(R.id.button_selectpic);
        imageView = findViewById(R.id.imageView_pic);
        btnselectpic.setOnClickListener(this);
        uploadButton.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        //在相簿裡面選擇好相片之後調回到現在的這個activity中
        switch (requestCode) {
            case 1://這裡的requestCode是我自己設定的，就是確定返回到那個Activity的標誌
                if (resultCode == RESULT_OK) {//resultcode是setResult裡面設定的code值
                    try {
                        Uri selectedImage = data.getData(); //獲取系統返回的照片的Uri
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        Cursor cursor = getContentResolver().query(selectedImage,
                                filePathColumn, null, null, null);//從系統表中查詢指定Uri對應的照片
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        imagepath = cursor.getString(columnIndex);  //獲取照片路徑
                        cursor = null;
                        messageText.setText(imagepath);
                        Bitmap bitmap = BitmapFactory.decodeFile(imagepath);
                        imageView.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        // TODO Auto-generatedcatch block
                        e.printStackTrace();
                    } finally {

                    }
                }
                break;
        }
    }

    public void uploadFile(File file, String urlStr, Callback callback) {
        Boolean result = false;
        String BOUNDARY = "letv"; // 边界标识 随机生成
        String PREFIX = "--", LINE_END = "\r\n";
        //String CONTENT_TYPE = "application/json"; // json
        String CONTENT_TYPE = "multipart/form-data";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(TIME_OUT);
                    conn.setConnectTimeout(TIME_OUT);
                    conn.setDoInput(true); // 允许输入流
                    conn.setDoOutput(true); // 允许输出流
                    conn.setUseCaches(false); // 不允许使用缓存
                    conn.setRequestMethod("POST"); // 请求方式
                    conn.setRequestProperty("Charset", CHARSET); // 设置编码
                    conn.setRequestProperty("connection", "keep-alive");
                    conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary="
                            + BOUNDARY);

                    if (file != null) {
                        /**
                         * 当文件不为空，把文件包装并且上传
                         */
                        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                        StringBuffer sb = new StringBuffer();
                        sb.append(PREFIX);
                        sb.append(BOUNDARY);
                        sb.append(LINE_END);
                        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + LINE_END);
                        sb.append("Content-Type: application/octet-stream; charset=" + CHARSET + LINE_END);
                        sb.append(LINE_END);
                        dos.write(sb.toString().getBytes());


                        InputStream is = new FileInputStream(file);
                        byte[] bytes = new byte[1024 * 1024];
                        int len = 0;
                        while ((len = is.read(bytes)) != -1) {
                            dos.write(bytes, 0, len);
                        }
                        is.close();
                        dos.write(LINE_END.getBytes());
                        byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END)
                                .getBytes();
                        dos.write(end_data);
                        dos.flush();
                        /**
                         * 获取响应码 200=成功 当响应成功，获取响应的流
                         */
                        int res = conn.getResponseCode();

                        if (res == 200) {
                            if (callback != null) {
                                if (mainThreadHandler == null) {
                                    mainThreadHandler = new Handler(Looper.getMainLooper());
                                }
                                mainThreadHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.success();
//                                Log.e("response", response.toString());
                                    }
                                });
                            }
                        }

                    }
                } catch (MalformedURLException e) {
                    callback.fail(e);
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    @Override
    public void onClick(View view) {
        if (view == btnselectpic) {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 1);
        } else if (view == uploadButton) {
            Log.e("image", imagepath);
            if (imagepath.length() == 0) {
                Toast.makeText(this, "請先選擇相片", Toast.LENGTH_SHORT).show();
                return;
            }
            File file = new File(imagepath);

            uploadFile(file, "http://springbootmall-env.eba-weyjyptf.us-east-1.elasticbeanstalk.com/upload", new Callback() {
                @Override
                public void success() {
                    Toast.makeText(MainActivity.this, "成功", Toast.LENGTH_SHORT).show();
                    imagepath = "";
                    messageText.setText("");
                }

                @Override
                public void fail(Exception exception) {
                    Toast.makeText(MainActivity.this, "失敗", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
