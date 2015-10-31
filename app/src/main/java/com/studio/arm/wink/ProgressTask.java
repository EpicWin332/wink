package com.studio.arm.wink;

import android.os.AsyncTask;
import android.util.Log;

import com.vk.sdk.VKAccessToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by alexander on 31.10.15.
 */
public class ProgressTask extends AsyncTask {

    @Override
    protected Object doInBackground(Object[] params) {
            /*String response = request("https://api.vk.com/method/chronicle.getUploadServer&access_token=" +
                    VKAccessToken.currentToken().accessToken + "&v=5.37").toString();*/
        String response = request("https://api.vk.com/method/wall.get?owner_id=11472575&access_token=" + VKAccessToken.currentToken().accessToken + "&v=5.37").toString();

        Log.d("------", response);
        return null;
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
