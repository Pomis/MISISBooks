package ru.twosphere.metrica.src;

import android.os.Build;

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
            connection.request(event, user_token, device_id);
        } catch (Exception err) {
            //nothing to do
        }
    }
}
