package app.pomis.misisbooks.views;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.List;

import app.pomis.misisbooks.R;
import app.pomis.misisbooks.bl.BackgroundLoader;
import app.pomis.misisbooks.bl.Book;
import app.pomis.misisbooks.bl.FileDownloader;
import app.pomis.misisbooks.bl.SearchAndLoadHistory;

public class DownloadsOfflineActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    Toolbar mToolbar;
    ContentAdapter mContentAdapter;
    static public DownloadsOfflineActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads_offline);

        instance = this;

        SearchAndLoadHistory mSearchAndLoadHistory = new SearchAndLoadHistory(this);
        mSearchAndLoadHistory.loadDownloadList();

        mToolbar = ((Toolbar) findViewById(R.id.downloadsToolbar));

        showDownloadsList();
        mToolbar.setTitle("Загрузки");
        Toast.makeText(this, "Нет подключения к сети. Доступен только список загрузок", Toast.LENGTH_LONG).show();
        MainActivity.instance.finish();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            findViewById(R.id.statusBarLollipop).setVisibility(View.GONE);
        }
    }

    public void showDownloadsList() {
        mContentAdapter = new ContentAdapter(this, R.layout.book_layout, FileDownloader.downloadedBooks);
        ListView lv = ((ListView) findViewById(R.id.search_result));
        lv.setAdapter(mContentAdapter);
        lv.setOnItemClickListener(this);
        mContentAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_downloads_offline, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final int index = i;
        String descr = "";
        descr = "Авторы: " + FileDownloader.downloadedBooks.get(i).getAuthorsToString() +
                "\nКатегория: " + FileDownloader.downloadedBooks.get(i).category.categoryName +
                "\nРазмер: " + FileDownloader.downloadedBooks.get(i).size +
                "\nСкачано " + FileDownloader.downloadedBooks.get(i).countDl + " раз.";

        new MaterialDialog.Builder(this)
                .title(FileDownloader.downloadedBooks.get(i).name)
                .content(descr)
                .positiveText("Открыть")
                .negativeColorAttr(Color.parseColor("#ffffff"))
                .positiveColorRes(R.color.primaryColor)
                .neutralColorAttr(Color.parseColor("#ffffff"))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    ///
                    /// Удаление файла
                    ///
                    public void onNeutral(MaterialDialog dialog) {
                        super.onNegative(dialog);


                    }
                })
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    ///
                    /// Открытие скачанного файла
                    ///
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                FileDownloader.downloadedBooks.get(index).fileName);
                        if (file.exists()) {
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(".pdf");
                            intent.setDataAndType(Uri.fromFile(file), "application/pdf");
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            try {
                                startActivity(intent);
                                //finish();
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(DownloadsOfflineActivity.instance, "Нечем открывать файл этого типа", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                })
                .show();
    }

    //
    // Адаптер книжек
    //
    public class ContentAdapter extends ArrayAdapter {
        private final Activity activity;
        private final List<String> list;

        public ContentAdapter(Context context, int resource, List objects) {
            super(context, resource, objects);
            activity = (Activity) context;
            list = objects;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Загрузка
            View rowView = convertView;
            ViewHolder view;

            if (rowView == null) {
                // Get a new instance of the row layout view
                LayoutInflater inflater = activity.getLayoutInflater();
                rowView = inflater.inflate(R.layout.book_layout, null);

                // Hold the view objects in an object, that way the don't need to be "re-  finded"
                view = new ViewHolder();
                view.textView = (TextView) rowView.findViewById(R.id.title);
                view.imageView = (ImageView) rowView.findViewById(R.id.imageView);
                view.authorsTextView = (TextView) rowView.findViewById(R.id.subText);
                rowView.setTag(view);

            } else {
                view = (ViewHolder) rowView.getTag();
            }
            Book item = FileDownloader.downloadedBooks.get(position);
            view.textView.setText(item.name);

            view.authorsTextView.setText(item.getAuthorsToString());
            if (view.sizeView != null)
                view.sizeView.setText("Размер: " + item.size);
            // Иконка загрузки
            if (view.imageView != null) {
                view.imageView.setImageResource(R.drawable.pdf);

            }
            return rowView;
        }

        protected class ViewHolder {
            protected TextView textView;
            protected ImageView imageView;
            protected TextView authorsTextView;
            protected TextView sizeView;
            protected ImageView faveStar;
        }
    }
}
