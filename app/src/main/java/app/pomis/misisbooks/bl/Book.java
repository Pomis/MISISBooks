package app.pomis.misisbooks.bl;

import java.util.ArrayList;

/**
 * Created by romanismagilov on 30.06.15.
 */
public class Book {
    public int id;
    public String name;
    public String downloadUrl;
    public String size;
    public String photoBig;
    public String photoSmall;
    public ArrayList<String> authors = new ArrayList<>();
    public Category category;
    public int countDl;
    public boolean fave;
    public String fileName;
    public String authorsString="";
    public String getAuthorsToString(){
        if (authorsString.equals("")) {
            String str = "";
            for (int i = 0; i < authors.size(); i++) {
                str += (i > 0) ? ", " + authors.get(i) : authors.get(i);
            }
            authorsString = str;
            return str;
        }
        else
            return authorsString;
    }
}
