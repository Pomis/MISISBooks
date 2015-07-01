package app.pomis.misisbooks.bl;

import java.util.ArrayList;

/**
 * Created by romanismagilov on 30.06.15.
 */
public class BackgroundLoader {
    static public void startLoadingCats(){
        new CategoryLoader().execute("http://twosphere.ru/api/materials.getCategories?" +
                "&access_token="+Account.getInstance().twosphere_token);
        //startLoadingSearchResults("Электротехника",10,0,1);
    }

    static public ArrayList<Book> loadedBooks = new ArrayList<>();

    static public void startLoadingPopular(int id, int offset, int count){
        new PopularsLoader().execute("http://twosphere.ru/api/materials.getPopular?count="+count+"&offset="+offset+"&category="+id+"&fields=all"+
                "&access_token="+Account.getInstance().twosphere_token);
    }

    static public void startLoadingPopularForWeek(int id, int offset, int count){
        new PopularsLoader().execute("http://twosphere.ru/api/materials.getPopularForWeek?count="+count+"&offset="+offset+"&category="+id+"&fields=all"+
                "&access_token="+Account.getInstance().twosphere_token);
    }

    static public void startLoadingSearchResults(String q, int count, int offset, int category){
        new PopularsLoader().execute("http://twosphere.ru/api/materials.search?count="+count+"&q="+q+"&offset="+offset+"&category="+category+"&fields=all"+
                "&access_token="+Account.getInstance().twosphere_token);

    }
}
