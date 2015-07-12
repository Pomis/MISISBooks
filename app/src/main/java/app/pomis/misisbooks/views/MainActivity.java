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

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

import app.pomis.misisbooks.R;
import app.pomis.misisbooks.api.Api;
import app.pomis.misisbooks.api.KException;
import app.pomis.misisbooks.bl.Account;


public class MainActivity extends ActionBarActivity {

    private final int REQUEST_LOGIN = 1;
    public static String API_ID="4720039";
    public static Account account = new Account();
    Api api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            logIn(findViewById(R.id.loginButton));
        } catch (Exception e){

        }
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
        account.restore(this);

        //Если сессия есть создаём API для обращения к серверу
        if( account.access_token != null ) {
            api = new Api(account.access_token, API_ID);
            startActivity(new Intent(this, DrawerActivity.class));

            //postToWall();
            //Данные юзера есть, можно постить на стену
            //api.createWallPost(1L, "test", null, null, false, false, false, null, null, null, 0L, null, null);

        } else {
            Intent intent = new Intent();
            intent.setClass(this, LoginWebActivity.class);

            startActivityForResult(intent, REQUEST_LOGIN);
        }

    }



    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch( requestCode ) {
            //Получили авторизацию от контакта
            case REQUEST_LOGIN:
                if( resultCode == RESULT_OK ) {
                    //авторизовались успешно
                    account.access_token = data.getStringExtra("token");
                    account.user_id = data.getLongExtra("user_id", 0);
                    account.save(MainActivity.this);
                    api = new Api(account.access_token, API_ID);
                    Toast.makeText(this, account.access_token, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, DrawerActivity.class));

                }
                break;
            default:
                Log.d("rez code:", " rezult code" + resultCode);
        }
    }
}
