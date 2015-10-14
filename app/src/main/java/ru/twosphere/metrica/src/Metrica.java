package ru.twosphere.metrica.src;

import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import java.net.URL;
import java.util.HashMap;

import app.pomis.misisbooks.bl.Account;

/**
 * Created by Александр on 26.08.2015.
 */
public class Metrica {

    public static final String METRICA_URI = "http://twosphere.ru/stats/track";

    public void track(String event) {
        if (BuildVars.METRICA_API_KEY.isEmpty()) {
            return;
        }
        try {
            ConnectionManager connection = new ConnectionManager();

            String user_token = Account.account.twosphere_token;
            if (user_token == null || user_token.isEmpty()) {
                user_token = "NOT_AUTHORIZED";
            }
            if (event.isEmpty()) {
                event = "UNKNOWN_EVENT";
            }
            String device_id = Build.SERIAL;
            String android_version = Build.VERSION.RELEASE;
            String type = "UI_INTERPLAY";

            HashMap<String, String> requestParams = new HashMap<>();
            requestParams.put("type", type);
            requestParams.put("event", event);
            requestParams.put("user_token", user_token);
            requestParams.put("device_id", device_id);
            requestParams.put("android_version", android_version);
            requestParams.put("api_key", BuildVars.METRICA_API_KEY);

            connection.request(new URL(Metrica.METRICA_URI), requestParams);
        } catch (Exception err) {
            //nothing to do
        }
    }

    public void trackLocation(Context context, String latitude, String longitude) {
//        if (BuildVars.METRICA_API_KEY.isEmpty()) {
//            return;
//        }
//        try {
//            ConnectionManager connection = new ConnectionManager();
//
//            if (Account.account.twosphere_token == null) {
//                Account.account.restore(context);
//            }
//
//            String user_token = Account.account.twosphere_token;
//            if (user_token == null || user_token.isEmpty()) {
//                user_token = "NOT_AUTHORIZED";
//            }
//
//            String coords = latitude + "|" + longitude;
//            String encodedCoords = Base64.encodeToString(coords.getBytes(), Base64.DEFAULT);
//            String type = "RECOMMEND_REQUEST";
//
//            HashMap<String, String> requestParams = new HashMap<>();
//            requestParams.put("type", type);
//            requestParams.put("l", encodedCoords);
//            requestParams.put("user_token", user_token);
//            requestParams.put("api_key", BuildVars.METRICA_API_KEY);
//
//            connection.request(new URL(Metrica.METRICA_URI), requestParams);
//        } catch (Exception err) {
//            //nothing to do
//            //err.printStackTrace();
//        }
    }
}
