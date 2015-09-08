package app.pomis.misisbooks.bl;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import app.pomis.misisbooks.R;

/**
 * Created by romanismagilov on 03.07.15.
 */
public class SearchAndLoadHistory {

    private SharedPreferences preferences;
    private Context context;

    public static ArrayList<Book> downloadedBooks = new ArrayList<>();

    // Singleton для вызова из широковещательного приёмника
    static public SearchAndLoadHistory instance;

    static public SearchAndLoadHistory getInstance() {
        if (instance != null) {
            return instance;
        } else Log.d("ОШИПКА", "не изициализорован SearchAndLoadHistory");
        return null
                ;
    }

    public SearchAndLoadHistory(Context appContext) {
        context = appContext;
        instance = this;
        preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    void putListString(String key, ArrayList<String> stringList) {
        String[] myStringList = stringList.toArray(new String[stringList.size()]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();
    }

    ArrayList<String> getListString(String key) {
        return new ArrayList<String>(Arrays.asList(TextUtils.split(preferences.getString(key, ""), "‚‗‚")));
    }

    public void loadAdd(SearchBox search) {
        search.clearSearchable();
        ArrayList<String> list = getListString("keys");
        list = new ArrayList<String>(new HashSet<String>(list));
        for (String value : list) {
            search.addSearchable(new SearchResult(value, context.getResources().getDrawable(R.drawable.ic_history)));
        }

    }

    // сохранить поиски в шаредпрефс
    public void saveAll(SearchBox search) {
        ArrayList<SearchResult> results = search.getSearchables();
        ArrayList<String> stringList = new ArrayList<>();
        for (SearchResult result : results)
            stringList.add(result.title);
        putListString("keys", stringList);
    }

    public void loadDownloadList() {
        //FileDownloader.downloadedBooks.clear();
//        ArrayList<String> names = new ArrayList<>(new HashSet<>(new ArrayList<>(Arrays.asList(TextUtils.split(preferences.getString("names", ""), "‚‗‚")))));
//        ArrayList<String> authors = new ArrayList<>(new HashSet<>(new ArrayList<>(Arrays.asList(TextUtils.split(preferences.getString("authors", ""), "‚‗‚")))));
//        ArrayList<String> categories = new ArrayList<>(new ArrayList<>(Arrays.asList(TextUtils.split(preferences.getString("categories", ""), "‚‗‚"))));
//        ArrayList<String> filenames = new ArrayList<>(new HashSet<>(new ArrayList<>(Arrays.asList(TextUtils.split(preferences.getString("filenames", ""), "‚‗‚")))));
//        ArrayList<String> sizes = new ArrayList<>(new HashSet<>(new ArrayList<>(Arrays.asList(TextUtils.split(preferences.getString("sizes", ""), "‚‗‚")))));
//        for (int i = 0; i < names.size(); i++) {
//            File file=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filenames.get(i));
//
//            //dir.mkdirs();
//
//           // Uri downloadLocation=Uri.fromFile(new File(dir, filenames.get(i)));
//           // File file = new File(String.valueOf(downloadLocation));
//            if (file.exists()) {
//                Book book = new Book();
//                book.authorsString = authors.get(i);
//                book.category = new Category(categories.get(i));
//                book.fileName = filenames.get(i);
//                book.name = names.get(i);
//                book.size = sizes.get(i);
//                boolean ok = true;
//                for (Book bookie : downloadedBooks)
//                    if (bookie.name.equals(book.name))
//                        ok = false;
//                if (ok) downloadedBooks.add(book);
//            }
//        }
        DatabaseInstruments.singleton.loadBookList();
    }

//    public void saveDownloadList() {
//        //loadDownloadList();
//        ArrayList<String> names = new ArrayList<>();
//        ArrayList<String> authors = new ArrayList<>();
//        ArrayList<String> categories = new ArrayList<>();
//        ArrayList<String> filenames = new ArrayList<>();
//        ArrayList<String> sizes = new ArrayList<>();
//        for (Book book : downloadedBooks) {
//            names.add(book.name);
//            authors.add(book.getAuthorsToString());
//            categories.add(book.category.categoryName);
//            filenames.add(book.fileName);
//            sizes.add(book.size);
//        }
//        preferences.edit()
//                .putString("names", TextUtils.join("‚‗‚", names))
//                .putString("authors", TextUtils.join("‚‗‚", authors))
//                .putString("categories", TextUtils.join("‚‗‚", categories))
//                .putString("filenames", TextUtils.join("‚‗‚", filenames))
//                .putString("sizes", TextUtils.join("‚‗‚", sizes))
//                .apply();
//    }

}
