package app.pomis.misisbooks.bl;

import android.os.AsyncTask;
import android.util.Log;

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
public class CategoryLoader extends AsyncTask<String, String, String> {

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

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d("435678", "Response: " + result);
        parseCats(result);
        DrawerActivity.getInstance().onCatsDownloaded();
    }

    void parseCats(String result) {
        try {
            JSONObject jObject = new JSONObject(result.substring(result.indexOf("{")));
            JSONArray jArray = jObject.getJSONObject("response").getJSONObject("response").getJSONArray("categories");
            for (int i = 0; i<jArray.length(); i++){
                JSONObject tempObj = jArray.getJSONObject(i);
                Category category = new Category(tempObj.getInt("key"), tempObj.getString("category_name"),
                        tempObj.getString("color_hex")
                );
                Category.arrayList.add(category);
            }
            //String tokenString = jObject.getJSONObject("response").getString("access_token");

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
