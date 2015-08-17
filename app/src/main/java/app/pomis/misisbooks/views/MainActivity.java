package app.pomis.misisbooks.views;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

import app.pomis.misisbooks.R;
import app.pomis.misisbooks.api.Api;
import app.pomis.misisbooks.api.KException;
import app.pomis.misisbooks.bl.Account;
import app.pomis.misisbooks.bl.TwoSphereAuth;


public class MainActivity extends ActionBarActivity {

    private final int REQUEST_LOGIN = 1;


    public static MainActivity instance;
    private MaterialDialog mMaterialDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);
        try {
            ArrayList<Long> test;
            Account.account.restore(this);

            //Если сессия есть создаём API для обращения к серверу
            if (Account.account.access_token != null) {
                Account.api = new Api(Account.account.access_token, Account.API_ID);
                // Подключение к АПИ книжечек
                new TwoSphereAuth().execute("http://twosphere.ru/api/auth.signin?vk_access_token=" + Account.account.access_token);
                //
                mMaterialDialog = new MaterialDialog.Builder(this)
                        .title("Подключение")
                        .content("Выполняется подключение к библиотеке")
                        .progress(true, 0)
                        .show();
            }
        } catch (Exception e) {

        }
    }

    public void openActivity(boolean logged){
        if (logged)
            startActivity(new Intent(this, DrawerActivity.class));
        else
            startActivity(new Intent(this, DownloadsOfflineActivity.class));
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void logIn(View v) throws JSONException, IOException, KException {
        ArrayList<Long> test;
        Account.account.restore(this);

        //Если сессия есть создаём API для обращения к серверу
        if (Account.account.access_token != null) {
            Account.api = new Api(Account.account.access_token, Account.API_ID);
            //startActivity(new Intent(this, DrawerActivity.class));
            new TwoSphereAuth().execute("http://twosphere.ru/api/auth.signin?vk_access_token=" + Account.account.access_token);
            mMaterialDialog = new MaterialDialog.Builder(this)
                    .title("Подключение")
                    .content("Выполняется подключение к библиотеке")
                    .progress(true, 0)
                    .show();
//            openActivity(Account.logged);
        } else {
            Intent intent = new Intent();
            intent.setClass(this, LoginWebActivity.class);

            startActivityForResult(intent, REQUEST_LOGIN);
        }

    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            //Получили авторизацию от контакта
            case REQUEST_LOGIN:
                if (resultCode == RESULT_OK) {
                    //авторизовались успешно
                    Account.account.access_token = data.getStringExtra("token");
                    Account.account.user_id = data.getLongExtra("user_id", 0);
                    Account.account.save(MainActivity.this);
                    Account.api = new Api(Account.account.access_token, Account.API_ID);
                    Toast.makeText(this, Account.account.access_token, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, DrawerActivity.class));
                    // Подключение к АПИ книжечек
                    new TwoSphereAuth().execute("http://twosphere.ru/api/auth.signin?vk_access_token=" + Account.account.access_token);
                }
                break;
            default:
                Log.d("rez code:", " rezult code" + resultCode);
        }
    }
}
