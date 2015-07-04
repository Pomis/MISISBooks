package app.pomis.misisbooks.bl;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;

import java.util.ArrayList;
import java.util.Arrays;

import app.pomis.misisbooks.R;

/**
 * Created by romanismagilov on 03.07.15.
 */
public class SearchHistory {

    private SharedPreferences preferences;
    private Context context;
    public SearchHistory(Context appContext) {
        context = appContext;
        preferences = PreferenceManager.getDefaultSharedPreferences(appContext);
    }
    void putListString(String key, ArrayList<String> stringList) {
        String[] myStringList = stringList.toArray(new String[stringList.size()]);
        preferences.edit().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();
    }
    ArrayList<String> getListString(String key) {
        return new ArrayList<String>(Arrays.asList(TextUtils.split(preferences.getString(key, ""), "‚‗‚")));
    }

    public void loadAdd(SearchBox search){
        search.clearSearchable();
        ArrayList<String> list = getListString("keys");
        for (String value: list){
            search.addSearchable(new SearchResult(value, context.getResources().getDrawable(R.drawable.ic_history)));
        }
    }

    public void saveAll(SearchBox search){
        ArrayList<SearchResult> results = search.getSearchables();
        ArrayList<String> stringList = new ArrayList<>();
        for (SearchResult result: results)
            stringList.add(result.title);
        putListString("keys", stringList);
    }
}