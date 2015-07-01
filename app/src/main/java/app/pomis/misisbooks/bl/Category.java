package app.pomis.misisbooks.bl;

import java.util.ArrayList;

/**
 * Created by romanismagilov on 30.06.15.
 */
public class Category {
    public int id;
    public String categoryName;
    public String colorHex;
    public Category(int key, String category_name, String color_hex){
        id = key;
        categoryName = category_name;
        colorHex = color_hex;
    }

    static public ArrayList<Category> arrayList = new ArrayList<>();

    static public Category getCategoryById(int id){
        for (Category category: arrayList)
            if (category.id == id)
                return category;
        return null;
    }
}
