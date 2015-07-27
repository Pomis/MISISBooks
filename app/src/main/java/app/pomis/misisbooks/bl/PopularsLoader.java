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
import java.util.ArrayList;

import app.pomis.misisbooks.views.DrawerActivity;

/**
 * Created by romanismagilov on 30.06.15.
 */
public class PopularsLoader extends AsyncTask<String, String, String> {


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

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d("435678", "Response: " + result);
        parseStuff(result);
        //BackgroundLoader.loadedBooks = arrayList;
        DrawerActivity.getInstance().onSearchResultDownloaded();
    }

    void parseStuff(String result) {
        try {
            JSONObject jObject = new JSONObject(result.substring(result.indexOf("{")));
            JSONArray jArray = jObject.getJSONObject("response").getJSONArray("items");
            for (int i = 0; i<jArray.length(); i++){
                try {
                JSONObject tempObj = jArray.getJSONObject(i);
                Book book = new Book();

                book.id = tempObj.getInt("id");
                book.name = tempObj.getString("name");
                book.downloadUrl = tempObj.getString("download_url");
                book.size = tempObj.getString("size");
                book.photoBig = tempObj.getString("photo_big");
                book.photoSmall = tempObj.getString("photo_small");
                JSONArray temp = tempObj.getJSONArray("authors");
                    for (int j = 0; j<temp.length(); j++){
                        String test = temp.getString(j);
                        book.authors.add(test);
                    }
                book.category = Category.getCategoryById(tempObj.getJSONObject("category").getInt("id"));
                book.countDl = tempObj.getInt("count_dl");
                book.fave = tempObj.getBoolean("fave");
                    DrawerActivity.getInstance().addBook(book);
                }
                catch (JSONException e){e.printStackTrace();}

            }
            //String tokenString = jObject.getJSONObject("response").getString("access_token");

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
