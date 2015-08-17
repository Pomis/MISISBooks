package app.pomis.misisbooks.bl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import app.pomis.misisbooks.views.DrawerActivity;

/**
 * Created by romanismagilov on 30.06.15.
 */
public class ResourcesLoader extends AsyncTask<String, String, String> {


    ArrayList<Book> arrayList = new ArrayList<>();
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
        if (result!=null) {
            parseStuff(result);
        }

    }

    void parseStuff(String result) {
        try {
            JSONObject jObject = new JSONObject(result.substring(result.indexOf("{")));
            JSONObject jResponse = jObject.getJSONObject("response").getJSONObject("user");

            Account.name=jResponse.getString("view_name");

            String urldisplay = jResponse.getString("photo");
            Bitmap mIcon11 = null;
            new DownloadImageTask().execute(jResponse.getString("photo"));
        }
            //String tokenString = jObject.getJSONObject("response").getString("access_token");

         catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        public DownloadImageTask() {
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }


            Account.photo = getRoundedShape(mIcon11);
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {

            DrawerActivity.getInstance().onResDownloaded();
        }
    }

    public Bitmap getRoundedShape(Bitmap scaleBitmapImage) {
        int targetWidth = 200;
        int targetHeight = 200;
        Bitmap targetBitmap = Bitmap.createBitmap(targetWidth,
                targetHeight,Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(targetBitmap);
        Path path = new Path();
        path.addCircle(((float) targetWidth - 1) / 2,
                ((float) targetHeight - 1) / 2,
                (Math.min(((float) targetWidth),
                        ((float) targetHeight)) / 2),
                Path.Direction.CCW);

        canvas.clipPath(path);
        Bitmap sourceBitmap = scaleBitmapImage;
        canvas.drawBitmap(sourceBitmap,
                new Rect(0, 0, sourceBitmap.getWidth(),
                        sourceBitmap.getHeight()),
                new Rect(0, 0, targetWidth, targetHeight), null);
        return targetBitmap;
    }
}
