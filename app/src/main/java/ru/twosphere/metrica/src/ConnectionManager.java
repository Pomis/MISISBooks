package ru.twosphere.metrica.src;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Александр on 26.08.2015.
 */
public class ConnectionManager {

    private final String LOG_TAG = HttpRequestBuilder.class.getSimpleName();

    public void request(URL url, HashMap<String, String> requestParams) {
        try {
            HttpRequestBuilder builder = new HttpRequestBuilder();
            builder.setParams(requestParams);
            builder.setUri(url.toString());

            HttpAsyncTask task = new HttpAsyncTask();
            task.setRequestMethod(HttpAsyncTask.GET_REQUEST_NAME);

            Log.d(LOG_TAG, builder.build().getUrl().toString());
            task.execute(builder.build().getUrl());
        } catch (Exception err) {
            //nothing to do
        }
    }

    public class HttpRequestBuilder {
        private final String LOG_TAG = HttpRequestBuilder.class.getSimpleName();

        private String mReadyUrl;
        private String mUri;
        private HashMap<String, String> mRequestParams;

        public HttpRequestBuilder() {}

        public HttpRequestBuilder(HashMap<String, String> params) {
            mRequestParams = params;
        }

        public void setParams(HashMap<String, String> params) {
            mRequestParams = params;
        }

        public void setUri(String uri) {
            mUri = uri;
        }

        public String generateStringUrl() {
            try {
                Uri.Builder builtUri = Uri.parse(mUri).buildUpon();
                for (Map.Entry entry : mRequestParams.entrySet()) {
                    builtUri = builtUri.appendQueryParameter(entry.getKey().toString(),
                            entry.getValue().toString());
                }
                return builtUri.build().toString();

            } catch (Exception err) {
                Log.e(LOG_TAG, "Error occurred: " + err.getMessage());
                return mUri != null ? mUri : "";
            }
        }

        public HttpRequestBuilder build() {
            mReadyUrl = generateStringUrl();
            return HttpRequestBuilder.this;
        }

        public URL getUrl() {
            try {
                return new URL(mReadyUrl);
            } catch (MalformedURLException err) {
                Log.e(LOG_TAG, "Error occurred: " + err.getMessage());
                return null;
            }
        }
    }

    public class HttpAsyncTask extends AsyncTask<URL, Integer, String> {

        /**
         * <URL, Integer, String> — URL::URL, Progress Report::Integer, Result::String
         */

        private final String LOG_TAG = HttpAsyncTask.class.getSimpleName();

        public static final String POST_REQUEST_NAME = "POST";
        public static final String GET_REQUEST_NAME = "GET";
        public static final String HEAD_REQUEST_NAME = "HEAD";

        private String mSelectedMethod = POST_REQUEST_NAME;

        public void setRequestMethod(String selected) {
            if (!selected.equals(POST_REQUEST_NAME) && !selected.equals(GET_REQUEST_NAME) &&
                    !selected.equals(HEAD_REQUEST_NAME)) {
                return;
            }
            mSelectedMethod = selected;
        }

        @Override
        protected void onPreExecute() {
            // выполнение кода в UI потоке до запуска HTTP запроса
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // Обновите индикатор хода выполнения, уведомления или другой
            // элемент пользовательского интерфейса.

            // Выполняется в UI потоке после каждого изменения
        }

        protected String doInBackground(URL... urls) {
            // Выполняется не в UI потоке. Создается отдельный поток с помощью менеджера.

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String responseJsonStr = null;

            try {
                URL url = urls[0];

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(mSelectedMethod);
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty. No point in parsing.
                    return null;
                }

                return buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                Log.d(LOG_TAG, "[onPostExecute] Response has been received");
                // Сюда приходит ответ после завершения HTTP запроса
                // Выполняется в UI потоке
                if (response == null) {
                    Log.d(LOG_TAG, "Internet connection problem");
                }
            } catch (Exception err) {
                Log.d(LOG_TAG, "Processing error");
            }
        }
    }
}
