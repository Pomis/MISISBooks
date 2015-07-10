package app.pomis.misisbooks.bl;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;

import java.util.ArrayList;

import app.pomis.misisbooks.views.DrawerActivity;

/**
 * Created by romanismagilov on 10.07.15.
 */
public class FileDownloader extends AsyncTask<String, String, String> {
    ArrayList<Book> downloadedBooks = new ArrayList<>();

    DownloadManager downloadManager;
    String url;
    long myDownloadReference;
    BroadcastReceiver receiverDownloadComplete;
    BroadcastReceiver receiverNotificationClicked;

    Context context;


    @Override
    protected String doInBackground(String... strings) {
        this.url = strings[0];
        context = DrawerActivity.getInstance();//govno
        downloadManager = (DownloadManager) context.getSystemService(context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request
                .setTitle(strings[1])
                .setDescription("MISIS Books")
                .setVisibleInDownloadsUi(true)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

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
                }
                DrawerActivity.getInstance().setContentView(android.support.design.R.layout.abc_dialog_title_material);
            }
        };

        context.registerReceiver(receiverNotificationClicked, filter);
        context.registerReceiver(receiverDownloadComplete, intef);

        downloadManager.enqueue(request);
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

    }
}
