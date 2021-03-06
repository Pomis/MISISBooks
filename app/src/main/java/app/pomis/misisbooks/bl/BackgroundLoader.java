package app.pomis.misisbooks.bl;

import android.os.AsyncTask;

import java.util.ArrayList;

import app.pomis.misisbooks.views.DrawerActivity;

/**
 * Created by romanismagilov on 30.06.15.
 */
public class BackgroundLoader {

    static public AsyncTask<String, String, String> currentTask;

    static public void startLoadingCats() {
        new CategoryLoader().execute("http://twosphere.ru/api/materials.getCategories?" +
                "&access_token=" + Account.getInstance().twosphere_token);
        //startLoadingSearchResults("Электротехника",10,0,1);
    }

    static void refreshThread() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        currentTask = new PopularsLoader();
    }


    static public ArrayList<Book> loadedBooks = new ArrayList<>();

    static public void addBook(Book book) {
        loadedBooks.add(book);
    }

    static public void startLoadingPopular(int id, int offset, int count) {
        DrawerActivity.getInstance().isContinuingLoading = false;
        BackgroundLoader.loadedBooks.clear();

        currentTask.execute("http://twosphere.ru/api/materials.getPopular?count=" + count + "&offset=" + offset + "&category=" + id + "&fields=all" +
                "&access_token=" + Account.getInstance().twosphere_token);
    }

    static public void startLoadingPopularForWeek(int id, int offset, int count) {
        DrawerActivity.getInstance().isContinuingLoading = false;
        BackgroundLoader.loadedBooks.clear();
        refreshThread();
        currentTask.execute("http://twosphere.ru/api/materials.getPopularForWeek?count=" + count + "&offset=" + offset + "&category=" + id + "&fields=all" +
                "&access_token=" + Account.getInstance().twosphere_token);
    }

    static public void startLoadingSearchResults(String q, int count, int offset, int category) {
        DrawerActivity.getInstance().isContinuingLoading = false;
        BackgroundLoader.loadedBooks.clear();
        refreshThread();
        currentTask.execute("http://twosphere.ru/api/materials.search?count=" + count + "&q=" + q + "&offset=" + offset + "&category=" + category + "&fields=all" +
                "&access_token=" + Account.getInstance().twosphere_token);

    }

    static public void startLoadingFavs(int id, int offset, int count) {
        DrawerActivity.getInstance().isContinuingLoading = false;
        BackgroundLoader.loadedBooks.clear();
        refreshThread();
        currentTask.execute("http://twosphere.ru/api/fave.getDocuments?count=" + count + "&offset=" + offset + "&category=" + id + "&fields=all" +
                "&access_token=" + Account.getInstance().twosphere_token);
    }

    static public void continueLoadingPopular(int id, int offset, int count) {
        refreshThread();
        currentTask.execute("http://twosphere.ru/api/materials.getPopular?count=" + count + "&offset=" + offset + "&category=" + id + "&fields=all" +
                "&access_token=" + Account.getInstance().twosphere_token);
    }

    static public void continueLoadingPopularForWeek(int id, int offset, int count) {
        refreshThread();
        currentTask.execute("http://twosphere.ru/api/materials.getPopularForWeek?count=" + count + "&offset=" + offset + "&category=" + id + "&fields=all" +
                "&access_token=" + Account.getInstance().twosphere_token);
    }

    static public void continueLoadingSearchResults(String q, int count, int offset, int category) {
        refreshThread();
        currentTask.execute("http://twosphere.ru/api/materials.search?count=" + count + "&q=" + q + "&offset=" + offset + "&category=" + category + "&fields=all" +
                "&access_token=" + Account.getInstance().twosphere_token);

    }

    static public void continueLoadingFavs(int id, int offset, int count) {
        refreshThread();
        currentTask.execute("http://twosphere.ru/api/fave.getDocuments?count=" + count + "&offset=" + offset + "&category=" + id + "&fields=all" +
                "&access_token=" + Account.getInstance().twosphere_token);
    }

    static public void addOrRemoveFromFavs(int i, boolean fave) {
        //edition_id
        if (!fave)
            new FaveAdder().execute("http://twosphere.ru/api/fave.addDocument?edition_id=" + i + "&access_token=" + Account.getInstance().twosphere_token);
        else
            new FaveAdder().execute("http://twosphere.ru/api/fave.deleteDocument?edition_id=" + i + "&access_token=" + Account.getInstance().twosphere_token);
    }

    static public Book getBookByServerId(int i) {
        for (Book b : loadedBooks) {
            if (b.id == i) {
                b = b;
                return b;

            }
        }
        return null;
    }
}
