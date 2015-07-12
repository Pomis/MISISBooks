package app.pomis.misisbooks.bl;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.webkit.CookieManager;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import app.pomis.misisbooks.views.DrawerActivity;

/**
 * Created by romanismagilov on 10.07.15.
 */
public class FileDownloader extends AsyncTask<Book, String, String> {
    public static ArrayList<Book> downloadedBooks = new ArrayList<>();

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
        new GetFileName().execute(downloadingBook.downloadUrl);


        return null;
    }

    void onHeaderParsed(){
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

        final IntentFilter intef = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        receiverDownloadComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long ref = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (myDownloadReference == ref) {
                    //jjkkjk
                    downloadedBooks.add(downloadingBookFinal);
                }
                DrawerActivity.getInstance().setContentView(android.support.design.R.layout.abc_dialog_title_material);
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
        for (Book book : downloadedBooks)
            if (book.id == checkingBook.id)
                return true;
        return false;
    }


    // Получение названия файла с помощью хедеров
    class GetFileName extends AsyncTask<String, Integer, String>
    {
        protected String doInBackground(String... urls)
        {
            URL url;
            String filename = null;
            try {
                url = new URL(urls[0]);
                String cookie = CookieManager.getInstance().getCookie(urls[0]);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("Cookie", cookie);
                con.setRequestMethod("HEAD");
                con.setInstanceFollowRedirects(false);
                con.connect();

                // TODO: Получить название файла
                String content = con.getHeaderField("Content-Disposition");
                String contentSplit[] = content.split("filename=");
                filename = contentSplit[1].replace("filename=", "").replace("\"", "").trim();
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (IOException e) {
            }
            fullFilename = filename;
            return filename;
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
}


