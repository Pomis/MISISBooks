package app.pomis.misisbooks.bl;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app.pomis.misisbooks.views.DrawerActivity;

/**
 * Created by romanismagilov on 10.07.15.
 */
public class FileDownloader extends AsyncTask<Book, String, String> {

    DownloadManager downloadManager;
    String url;
    long myDownloadReference;
    BroadcastReceiver receiverDownloadComplete;
    BroadcastReceiver receiverNotificationClicked;

    Context context;
    Book downloadingBook;
    String fullFilename;

    @Override
    protected String doInBackground(Book... book) {
        downloadingBook = book[0];
        this.url = book[0].downloadUrl;
        context = DrawerActivity.getInstance();
        fullFilename = book[0].name;
        new GetExt().execute(downloadingBook.downloadUrl);


        return null;
    }

    void onHeaderParsed() {
        final Book downloadingBookFinal = downloadingBook;

        downloadManager = (DownloadManager) context.getSystemService(context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request
                .setTitle(downloadingBook.name)
                .setDescription("MISIS Books")
                .setVisibleInDownloadsUi(true)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fullFilename);

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        receiverNotificationClicked = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String extraID = DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;
                long[] references = intent.getLongArrayExtra(extraID);
                for (long reference : references) {
                    if (reference == myDownloadReference) {
                        // dsf //
                    }
                }
            }
        };

        ///
        /// Книга загружена
        ///
        final IntentFilter intef = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        receiverDownloadComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long ref = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                //if (myDownloadReference == ref) {
                downloadingBookFinal.fileName = fullFilename;
                SearchAndLoadHistory.downloadedBooks.add(downloadingBookFinal);
                //SearchAndLoadHistory.getInstance().saveDownloadList();
                DatabaseInstruments.singleton.insertBook(downloadingBookFinal);
                //}
            }
        };

        context.registerReceiver(receiverNotificationClicked, filter);
        context.registerReceiver(receiverDownloadComplete, intef);

        downloadManager.enqueue(request);
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        Toast.makeText(context, "Начата загрузка...", Toast.LENGTH_SHORT).show();
    }

    public static boolean checkIfDownloaded(Book checkingBook) {
        for (Book book : SearchAndLoadHistory.downloadedBooks)
            if (book.id == checkingBook.id)
                return true;
        return false;
    }


    // Получение названия файла с помощью хедеров
    class GetExt extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... urls) {
            URL url;
            String ext = null;
            try {
                url = new URL(urls[0]);
                List<Header> headers = new HeadersReceiver(urls[0]).getHeaders();
                Header filetype = headers.get(4);
                String headerOfContentType = filetype.toString();
                String contentSplit[] = headerOfContentType.split("application/");
                ext = contentSplit[1];
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (IOException e) {
            }
            fullFilename += "." + ext;
            return ext;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            onHeaderParsed();
        }
    }

    class Header {

        private Map.Entry<String, List<String>> header;

        public Header(Map.Entry<String, List<String>> header) {
            this.header = header;
        }

        public String toString() {
            StringBuilder result = new StringBuilder();

            if (header.getKey() != null) {

                result
                        .append(header.getKey())
                        .append(":");

                if (header.getValue().size() > 1) {
                    for (String val : header.getValue()) {
                        result.append("\n   ").append(val);
                    }
                } else {
                    result.append("  ").append(header.getValue().get(0));
                }
            }

            return result.toString();
        }

    }

    class HeadersReceiver {

        private String url;

        public HeadersReceiver(String url) {
            this.url = url;
        }

        public List<Header> getHeaders() throws IOException {
            URL url = new URL(this.url);
            URLConnection con = url.openConnection();

            List<Header> result = new ArrayList<Header>();

            for (Map.Entry<String, List<String>> header : con.getHeaderFields().entrySet()) {
                result.add(new Header(header));
            }

            return result;
        }

    }
}


