package app.pomis.misisbooks.api;

import android.util.Log;

import java.net.URLEncoder;

import app.pomis.misisbooks.bl.Utils;

public class Auth {
    
    private static final String TAG = "Kate.Auth";
    public static String redirect_url1="https://oauth.vk.com/blank.html";
    public static String redirect_url2="http://oauth.vk.com/blank.html";


    public static String[] parseRedirectUrl(String url) throws Exception {
        //url is something like http://api.vkontakte.ru/blank.html#access_token=66e8f7a266af0dd477fcd3916366b17436e66af77ac352aeb270be99df7deeb&expires_in=0&user_id=7657164
        String access_token= Utils.extractPattern(url, "access_token=(.*?)&");
        Log.i(TAG, "access_token=" + access_token);
        String user_id=Utils.extractPattern(url, "user_id=(\\d*)");
        Log.i(TAG, "user_id=" + user_id);
        if(user_id==null || user_id.length()==0 || access_token==null || access_token.length()==0)
            throw new Exception("Failed to parse redirect url "+url);
        return new String[]{access_token, user_id};
    }
}
