package app.pomis.misisbooks.bl;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import app.pomis.misisbooks.views.DrawerActivity;

/**
 * Created by romanismagilov on 30.06.15.
 */
public class FaveAdder extends AsyncTask<String, String, String> {


    //ArrayList<Book> arrayList = new ArrayList<>();
    @Override
    protected String doInBackground(String... uri) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(uri[0]));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
            } else {
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }
        return responseString;
    }

    String success;
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d("435678", "Response: " + result);
        parseStuff(result);
        //DrawerActivity.getInstance().onSearchResultDownloaded();
        //DrawerActivity.getInstance().refresh();
        Toast.makeText(DrawerActivity.getInstance(), success, Toast.LENGTH_SHORT).show();
    }

    void parseStuff(String result) {
        success = "Не удалось добавить книгу в избанное";
        try {
            JSONObject jObject = new JSONObject(result.substring(result.indexOf("{")));
            JSONObject jOb = jObject.getJSONObject("response");
            String status = jOb.getString("status");
            boolean res = jOb.getBoolean("result");
            if (res&&status.equals("OK")){
                JSONArray temp = jOb.getJSONArray("inserted_ids");
                for (int j = 0; j<temp.length(); j++){
                    int id = Integer.parseInt(temp.get(j).toString());
                    success = "Книга успешно добавлена в избранное";
                }
                if (temp.length()==0)
                    success = "Книга уже в избранном";
            }
            //String tokenString = jObject.getJSONObject("response").getString("access_token");

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
