package app.pomis.misisbooks.views;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import app.pomis.misisbooks.BuildConfig;
import app.pomis.misisbooks.R;
import app.pomis.misisbooks.api.Auth;


public class LoginWebActivity extends Activity {
    private static final String TAG = "Tracker.LoginActivity";

    WebView webview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_web);

        webview = (WebView) findViewById(R.id.wv);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.clearCache(true);

        //Чтобы получать уведомления об окончании загрузки страницы
        webview.setWebViewClient(new VkontakteWebViewClient());

        //otherwise CookieManager will fall with java.lang.IllegalStateException: CookieSyncManager::createInstance() needs to be called before CookieSyncManager::getInstance()
        CookieSyncManager.createInstance(this);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        //String url= Auth.getUrl(LogInActivity.API_ID, Auth.getSettings());
        String url = "https://oauth.vk.com/authorize?client_id=4720039&scope=65536&display=mobile&redirect_uri=https%3A//oauth.vk.com/blank.html&response_type=token&revoke=1&v=5.29";
        //System.out.print(url);
        webview.loadUrl(url);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
            findViewById(R.id.statusBarLollipop).setVisibility(View.GONE);
        }
    }

    class VkontakteWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            parseUrl(url);
        }
    }

    private void parseUrl(String url) {
        try {
            if(url==null)
                return;
            Log.i(TAG, "url=" + url);
            if(url.startsWith(Auth.redirect_url1) || url.startsWith(Auth.redirect_url2))
            {
                if(!url.contains("error=")){
                    String[] auth=Auth.parseRedirectUrl(url);
                    Intent intent=new Intent();
                    intent.putExtra("token", auth[0]);
                    intent.putExtra("user_id", Long.parseLong(auth[1]));
                    setResult(Activity.RESULT_OK, intent);
                }
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
