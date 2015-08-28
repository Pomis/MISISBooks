package app.pomis.misisbooks.bl;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import app.pomis.misisbooks.api.Api;

/**
 * Created by romanismagilov on 25.04.15.
 */
public class Account {

    public static boolean logged = true;

    public static String API_ID="4720039";
    public static Account account = new Account();
    public static Api api;

    //Singleton
    static Account singleton;
    public Account(){singleton = this;}
    static public Account getInstance(){return singleton;}

    static public String name;
    static public Bitmap photo;

    private static Context ctx;
    public String access_token;
    public String twosphere_token;
    public long user_id;

    public void save(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor=prefs.edit();
        editor.putString("access_token", access_token);
        editor.putLong("user_id", user_id);
        editor.putString("twosphere_token", twosphere_token);
        editor.commit();
    }

    public void restore(Context context){
        if (ctx == null)
            ctx = context;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        twosphere_token=prefs.getString("twosphere_token", twosphere_token);
        access_token = prefs.getString("access_token", null);
        user_id=prefs.getLong("user_id", 0);
    }

    static public void clear()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor=prefs.edit();
        editor.clear();
        editor.apply();
    }
}
