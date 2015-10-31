package com.studio.arm.wink;

import android.os.AsyncTask;
import android.util.Log;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.api.VKApiConst;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexander on 31.10.15.
 */
public class ProgressTask extends AsyncTask {

    @Override
    protected Object doInBackground(Object[] params) {

        String response = request("https://api.vk.com/method/photos.getWallUploadServer?user_id=" + VKApiConst.OWNER_ID + "&access_token=" + VKAccessToken.currentToken().accessToken + "&v=5.37").toString();
        /*String response = request("https://api.vk.com/method/chronicle.getUploadServer&access_token=" +
                    VKAccessToken.currentToken().accessToken + "&v=5.37").toString();*/
        //String response = request("https://api.vk.com/method/wall.get?owner_id=11472575&access_token=" + VKAccessToken.currentToken().accessToken + "&v=5.37").toString();
        try {
            JSONObject json = new JSONObject(response);
            Log.d("----", (String)json.getJSONObject("response").get("upload_url"));
        }
        catch (JSONException e) {

        }

        downloadPhoto(params);

        Log.d("------", response);
        return null;
    }

    private void downloadPhoto(Object[] params) {
        String url = "http://example.com";
        String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()
        String param1 = "value1";
        String param2 = "value2";

        try {
            String query = String.format("param1=%s&param2=%s",
                    URLEncoder.encode(param1, charset),
                    URLEncoder.encode(param2, charset));


            URLConnection connection = new URL(url).openConnection();
            connection.setDoOutput(true); // Triggers POST.
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(query.getBytes(charset));
            }

            InputStream response = connection.getInputStream();
        }
        catch (Exception e) {


        }
    }

    private StringBuffer request(String urlString) {


        StringBuffer chaine = new StringBuffer("");
        try{
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            InputStream response = connection.getInputStream();


            BufferedReader rd = new BufferedReader(new InputStreamReader(response));
            String line = "";
            while ((line = rd.readLine()) != null) {
                chaine.append(line);
            }

        } catch (IOException e) {
            // writing exception to log
            e.printStackTrace();
        }

        return chaine;
    }
}
