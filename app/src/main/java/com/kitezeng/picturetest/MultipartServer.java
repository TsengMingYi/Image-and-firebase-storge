package com.kitezeng.picturetest;

import android.util.Log;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.NameValuePair;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

public class MultipartServer {
    private static final String TAG = "MultipartServer";
    private static String crlf = "\r\n";
    private static String twoHyphens = "--";
    private static String boundary =  "*****";
    private static String avatarPath = null;

    public static String postData(String url1, List<NameValuePair> nameValuePairs) throws IOException {
        URL url = new URL(url1);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(10000);
        connection.setConnectTimeout(15000);
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);

        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        String avatarName = null;
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (NameValuePair pair : nameValuePairs) {
            if (first)
                first = false;
            else
                query.append("&");
            query.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            query.append("=");
            query.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
            if ((avatarName = pair.getName()).equals("avatar")) {
                avatarPath = pair.getValue();
            }

        }

        FileInputStream inputStream;
        OutputStream outputStream = connection.getOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        dataOutputStream.writeBytes(query.toString());

        // Write Avatar (if any)
        if(avatarName != null && avatarPath != null) {
            dataOutputStream.writeBytes(twoHyphens + boundary + crlf);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + avatarName + "\";filename=\"" + new File(avatarPath).getName() + "\";" + crlf);
            dataOutputStream.writeBytes(crlf);

        /*Bitmap avatar = BitmapFactory.decodeFile(avatarPath);
        avatar.compress(CompressFormat.JPEG, 75, outputStream);
        outputStream.flush();*/

            inputStream = new FileInputStream(avatarPath);
            byte[] data = new byte[1024];
            int read;
            while((read = inputStream.read(data)) != -1)
                dataOutputStream.write(data, 0, read);
            inputStream.close();

            dataOutputStream.writeBytes(crlf);
            dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
        }

        dataOutputStream.flush();
        dataOutputStream.close();

        String responseMessage = connection.getResponseMessage();
        Log.d(TAG, responseMessage);

        InputStream in = connection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

        StringBuilder response = new StringBuilder();
        char []b = new char[512];
        int read;
        while((read = bufferedReader.read(b))!=-1) {
            response.append(b, 0, read);
        }

        connection.disconnect();
        Log.d(TAG, response.toString());
        return response.toString();
    }
}
