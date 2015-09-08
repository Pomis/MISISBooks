package app.pomis.misisbooks.bl;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by romanismagilov on 08.09.15.
 */
public class DatabaseInstruments {


    DBHelper dbHelper;
    SQLiteDatabase DB;
    final int CURRENT_DATABASE_VERSION = 1;
    int PREVIOUS_DATABASE_VERSION = 0;
    static private SharedPreferences preferences;
    static public DatabaseInstruments singleton;

    public DatabaseInstruments(Context context) {
        dbHelper = new DBHelper(context);
        DB = dbHelper.getWritableDatabase();

        singleton = this;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        // Считываем предыдущую версию бд
        try {
            PREVIOUS_DATABASE_VERSION = preferences.getInt("dbversion", PREVIOUS_DATABASE_VERSION);
        } catch (Exception e) {
        }
        createDB();
    }

    private void createDB() {
        if (CURRENT_DATABASE_VERSION > PREVIOUS_DATABASE_VERSION) {
            DB.execSQL("DROP TABLE IF EXISTS Downloads");
            preferences.edit().putInt("dbversion", CURRENT_DATABASE_VERSION).apply();
        }
        DB.execSQL("CREATE TABLE IF NOT EXISTS Downloads" +
                "(" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "name nvarchar(1000)," +
                "authors nvarchar(4000)," +
                "category nvarchar(1000)," +
                "filename nvarchar(255)," +
                "size nvarchar(1000)" +
                ")");
    }

    public void insertBook(Book book) {
        DB.execSQL("INSERT INTO Downloads (name,authors,category,filename,size) VALUES ("
                + quote(book.name) + ","
                + quote(book.authorsString) + ","
                + quote(book.category.categoryName) + ","
                + quote(book.fileName) + ","
                + quote(book.size)
                + ")");
    }

    public void loadBookList() {
        SearchAndLoadHistory.downloadedBooks.clear();
        ArrayList<Book> list = new ArrayList<>();
        String query = String.format("SELECT * FROM Downloads");
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, null);
        while (cursor.moveToNext()) {
            File file=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), cursor.getString(4));
            if (file.exists()) {
                Book book = new Book();
                book.name = cursor.getString(1);
                book.authorsString = cursor.getString(2);
                book.category = new Category(cursor.getString(3));
                book.fileName = cursor.getString(4);
                book.size = cursor.getString(5);

                list.add(book);
            }
        }
        cursor.close();
        SearchAndLoadHistory.downloadedBooks = list;
    }

    String quote(String string) {
        return "'" + string + "'";
    }

    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, "myDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
