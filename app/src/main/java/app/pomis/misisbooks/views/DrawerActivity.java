package app.pomis.misisbooks.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.iconics.typeface.FontAwesome;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.Badgeable;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import app.pomis.misisbooks.R;
import app.pomis.misisbooks.bl.Account;
import app.pomis.misisbooks.bl.BackgroundLoader;
import app.pomis.misisbooks.bl.Book;
import app.pomis.misisbooks.bl.FileDownloader;
import app.pomis.misisbooks.bl.SearchAndLoadHistory;
import app.pomis.misisbooks.bl.TwoSphereAuth;


public class DrawerActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
    static DrawerActivity singleton;
    Drawer mDrawer;
    Fragment fragment;
    ContentAdapter mContentAdapter;
    public int mode = 0; // 1 поиск
    public int downloadMode = 1;
    public boolean isContinuingLoading = false;

    class Modes {
        static public final int SEARCH = 1;
        static public final int POPULAR = 2;
        static public final int POPULAR_WEEK = 3;
        static public final int FAVS = 4;
        static public final int DOWNLOADS = 5;
    }

    class Settings {
        static public final int CUSTOM_DOWNLOAD = 1;
        static public final int BROWSER_DOWNLOAD = 2;
    }

    private Toolbar toolbar;

    // Запуск активности
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);
        String test = "http://twosphere.ru/api/auth.signin?vk_access_token=" + MainActivity.account.access_token;
        singleton = this;
        // Подключение к АПИ книжечек
        new TwoSphereAuth().execute("http://twosphere.ru/api/auth.signin?vk_access_token=" + MainActivity.account.access_token);
        BackgroundLoader.startLoadingCats();


        // Поиск
        search = (SearchBox) findViewById(R.id.searchbox);
        search.enableVoiceRecognition(this);

        // Отрисовка тулбара
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                openSearch();
                return true;
            }
        });

        // Подрузка загруженных файлов


        createNavigationDrawer();
        search.bringToFront();
        mSearchAndLoadHistory = new SearchAndLoadHistory(this);
        mSearchAndLoadHistory.loadAdd(search);
        mSearchAndLoadHistory.loadDownloadList();
        setTitle("");
    }


    // Кнопка назад
    @Override
    public void onBackPressed() {
        if (isSearchOpened) {
            closeSearch();
            return;
        }
        super.onBackPressed();
    }

    //region Поиск
    private MenuItem mSearchAction;
    SearchAndLoadHistory mSearchAndLoadHistory;
    private boolean isSearchOpened = false;
    public int catId = 1;


    public void doSearch() throws UnsupportedEncodingException {
        BackgroundLoader.startLoadingSearchResults(URLEncoder.encode(search.getSearchText(), "UTF-8"), 10, 0, catId);
    }


    // Выдача результатов поиска (или загрузки)
    public void onSearchResultDownloaded() {
        if (mContentAdapter == null) {
            mContentAdapter = new ContentAdapter(this, R.layout.book_layout,
                    (mode == Modes.DOWNLOADS) ? FileDownloader.downloadedBooks : BackgroundLoader.loadedBooks);
        }
        ListView lv = ((ListView) findViewById(R.id.search_result));
//            lv.addHeaderView(view);
//            ((TextView)lv.findViewById(R.id.headerTitle)).setText("Результаты поиска");
        if (lv.getAdapter() == null) {

            lv.setAdapter(mContentAdapter);
            lv.setOnItemClickListener(this);
            mContentAdapter.notifyDataSetChanged();
        } else mContentAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(lv);

        if (BackgroundLoader.loadedBooks.size() > 9)
            findViewById(R.id.footerContainer).setVisibility(View.VISIBLE);
        switch (mode) {
            case Modes.SEARCH:
                ((TextView) findViewById(R.id.headerTitle)).setText("Результаты поиска");
                break;
            case Modes.FAVS:
                ((TextView) findViewById(R.id.headerTitle)).setText("Избранное");
                break;
            case Modes.POPULAR_WEEK:
                ((TextView) findViewById(R.id.headerTitle)).setText("Популярное за неделю");
                break;
            case Modes.POPULAR:
                ((TextView) findViewById(R.id.headerTitle)).setText("Популярное за всё время");
                break;
            case Modes.DOWNLOADS:
                ((TextView) findViewById(R.id.headerTitle)).setText("Загрузки");
                break;
        }
        if (!isContinuingLoading)
            ((ScrollView) findViewById(R.id.scrollViewId)).smoothScrollTo(0, 0);//fullScroll(ScrollView.FOCUS_UP);
        else
            ((ScrollView) findViewById(R.id.scrollViewId)).fullScroll(ScrollView.FOCUS_DOWN);

    }

    // Список загрузок
    public void showDownloadsList() {
        mContentAdapter = new ContentAdapter(this, R.layout.book_layout, FileDownloader.downloadedBooks);
        ListView lv = ((ListView) findViewById(R.id.search_result));
        lv.setAdapter(mContentAdapter);
        lv.setOnItemClickListener(this);
        mContentAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(lv);
        ((ScrollView) findViewById(R.id.scrollViewId)).smoothScrollTo(0, 0);
    }

    // Обновление содержимого
    public void refresh() {
        //mContentAdapter.clear();
        mContentAdapter = new ContentAdapter(this, R.layout.book_layout, BackgroundLoader.loadedBooks);
        ListView lv = ((ListView) findViewById(R.id.search_result));
        lv.setAdapter(mContentAdapter);
        lv.setOnItemClickListener(this);
        mContentAdapter.notifyDataSetChanged();
    }


    public void loadMore(View view) {
        isContinuingLoading = true;
        switch (mode) {
            case Modes.POPULAR:
                BackgroundLoader.continueLoadingPopular(catId, BackgroundLoader.loadedBooks.size(), 10);
            case Modes.FAVS:
                BackgroundLoader.continueLoadingPopular(catId, BackgroundLoader.loadedBooks.size(), 10);
            case Modes.POPULAR_WEEK:
                BackgroundLoader.continueLoadingPopularForWeek(catId, BackgroundLoader.loadedBooks.size(), 10);
            case Modes.SEARCH:
                BackgroundLoader.continueLoadingSearchResults(search.getSearchText(), 10, BackgroundLoader.loadedBooks.size(), catId);
        }
        //((ScrollView) findViewById(R.id.scrollViewId)).fullScroll(ScrollView.FOCUS_DOWN);
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0) {
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ListView.LayoutParams.WRAP_CONTENT));
            }
            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);

            // Применение Ripple-effect
//            MaterialRippleLayout.on(view)
//                    .rippleColor(Color.BLACK)
//                    .create();

            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    SearchBox search;

    //
    // Гмейловый поиск
    //
    public void openSearch() {
        toolbar.setTitle("");
        search.setLogoText("");
        isSearchOpened = true;
        search.revealFromMenuItem(R.id.action_search, this);

        //.setDrawerLogo(getDrawable(R.drawable.ic_drawer));
        search.setMenuListener(new SearchBox.MenuListener() {

            @Override
            public void onMenuClick() {
                // Hamburger has been clicked
                Toast.makeText(DrawerActivity.this, "Menu click",
                        Toast.LENGTH_LONG).show();
            }

        });
        search.setSearchListener(new SearchBox.SearchListener() {

            @Override
            public void onSearchOpened() {
                // Use this to tint the screen

            }

            @Override
            public void onSearchClosed() {
                // Use this to un-tint the screen
                closeSearch();
            }

            @Override
            public void onSearchTermChanged() {
                // React to the search term changing
                // Called after it has updated results
//                notifyAll();
            }

            @Override
            public void onSearch(String searchTerm) {
                mode = Modes.SEARCH;
                try {

                    doSearch();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                search.addSearchable(new SearchResult(search.getSearchText(), getResources().getDrawable(R.drawable.ic_history)));
                ((ArrayAdapter) ((ListView) search.findViewById(R.id.results)).getAdapter()).notifyDataSetChanged();
                mSearchAndLoadHistory.saveAll(search);
            }

            @Override
            public void onSearchCleared() {

            }

        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234 && resultCode == RESULT_OK) {
            ArrayList<String> matches = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            search.populateEditText(matches);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void closeSearch() {
        search.hideCircularly(this);
        if (search.getSearchText().isEmpty()) toolbar.setTitle("");
    }

    //
    // Нажатие на книжку, диалог скачивания
    //
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final int index = i;
        String descr = "";
        descr = "Авторы: " + BackgroundLoader.loadedBooks.get(i).getAuthorsToString() +
                "\nКатегория: " + BackgroundLoader.loadedBooks.get(i).category.categoryName +
                "\nРазмер: " + BackgroundLoader.loadedBooks.get(i).size +
                "\nСкачано " + BackgroundLoader.loadedBooks.get(i).countDl + " раз.";
        switch (mode) {
            case Modes.DOWNLOADS:
                new MaterialDialog.Builder(this)
                        .title(BackgroundLoader.loadedBooks.get(i).name)
                        .content(descr)
                        .positiveText("Открыть")
                        .neutralText("Удалить")
                        .negativeColorAttr(Color.parseColor("#ffffff"))
                        .positiveColorRes(R.color.primaryColor)
                        .neutralColorAttr(Color.parseColor("#ffffff"))
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            ///
                            /// Удаление файла
                            ///
                            public void onNeutral(MaterialDialog dialog) {
                                super.onNeutral(dialog);


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
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(file), "application/pdf");
                                startActivity(intent);
                            }
                        })
                        .show();
                break;
            default:
                new MaterialDialog.Builder(this)
                        .title(BackgroundLoader.loadedBooks.get(i).name)
                        .content(descr)
                        .positiveText(FileDownloader.checkIfDownloaded(BackgroundLoader.loadedBooks.get(i)) ? "Уже загружено" : "Скачать")
                        .neutralText(BackgroundLoader.loadedBooks.get(i).fave ? "Из избранного" : "В избранное")
                        .negativeColorAttr(Color.parseColor("#ffffff"))
                        .positiveColorRes(R.color.primaryColor)
                        .neutralColorAttr(Color.parseColor("#ffffff"))
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                // Если книга не скачана, то начинаем-с скачивать её
                                if (!FileDownloader.checkIfDownloaded(BackgroundLoader.loadedBooks.get(index))) {

                                    String url = BackgroundLoader.loadedBooks.get(index).downloadUrl;

                                    if (downloadMode == Settings.BROWSER_DOWNLOAD) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                        startActivity(Intent.createChooser(intent, "Выберите браузер"));
                                    } else if (downloadMode == Settings.CUSTOM_DOWNLOAD) {
                                        new FileDownloader().execute(BackgroundLoader.loadedBooks.get(index));
                                    }
                                }
                            }
                        })
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onNeutral(MaterialDialog dialog) {
                                super.onNeutral(dialog);
                                if (!BackgroundLoader.loadedBooks.get(index).fave) {
                                    BackgroundLoader.loadedBooks.get(index).fave = true;
                                    BackgroundLoader.addOrRemoveFromFavs(BackgroundLoader.loadedBooks.get(index).id, false);
                                    refresh();
                                } else {
                                    BackgroundLoader.loadedBooks.get(index).fave = false;
                                    BackgroundLoader.addOrRemoveFromFavs(BackgroundLoader.loadedBooks.get(index).id, true);
                                    refresh();
                                }

                            }
                        })
                        .show();
                break;
        }
    }

    public void setCatId(int id) {
        catId = id;
        if (mContentAdapter != null && mode == Modes.FAVS) {
            mContentAdapter.clear();
            BackgroundLoader.startLoadingFavs(catId, 0, 10);
        }
        if (mContentAdapter != null && mode == Modes.POPULAR_WEEK) {
            mContentAdapter.clear();
            BackgroundLoader.startLoadingPopularForWeek(catId, 0, 10);
        }
        if (mContentAdapter != null && mode == Modes.POPULAR) {
            mContentAdapter.clear();
            BackgroundLoader.startLoadingPopular(catId, 0, 10);
        }
    }


    //
    // Адаптер книжек
    //
    class ContentAdapter extends ArrayAdapter {
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
            if (mode == Modes.DOWNLOADS) {
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
            // Из поиска
            else {
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
                    view.faveStar = (ImageView) rowView.findViewById(R.id.faveStar);
                    rowView.setTag(view);

                } else {
                    view = (ViewHolder) rowView.getTag();
                }


                /** Set data to your Views. */
                Book item = BackgroundLoader.loadedBooks.get(position);
                view.textView.setText(item.name);

                view.authorsTextView.setText(item.getAuthorsToString());
                if (view.sizeView != null)
                    view.sizeView.setText("Размер: " + item.size);
                // Круголь цветной
                Drawable drawable = getResources().getDrawable(R.drawable.circle);
                drawable.setColorFilter(Color.parseColor("#" + item.category.colorHex), PorterDuff.Mode.SRC_ATOP);
                if (view.imageView != null)
                    view.imageView.setImageDrawable(drawable);

                if (view.faveStar != null && item.fave) {
                    view.faveStar.setVisibility(View.VISIBLE);
                    view.faveStar.setImageResource(R.drawable.ic_star_border_black_24dp);
                }
                return rowView;
            }
        }

        protected class ViewHolder {
            protected TextView textView;
            protected ImageView imageView;
            protected TextView authorsTextView;
            protected TextView sizeView;
            protected ImageView faveStar;
        }
    }
//endregion


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mSearchAction = menu.findItem(R.id.action_search);
        return super.onPrepareOptionsMenu(menu);
    }

    static public DrawerActivity getInstance() {
        return singleton;
    }

    private Drawer.Result drawerResult = null;


    public void onCatsDownloaded() {
    }

    //
    // Navigation Drawer, фрагмент по умолчанию
    //
    public void createNavigationDrawer() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View vi = inflater.inflate(R.layout.drawer_header, null); //log.xml is your file.
        ImageView imgg = (ImageView) vi.findViewById(R.id.header);
        if (Account.photo != null)
            imgg.setImageBitmap(Account.photo);
        if (Account.name != null)
            ((TextView) vi.findViewById(R.id.headerText)).setText(Account.name);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDrawer = new Drawer()
                .withActivity(this)
                .withToolbar(toolbar)
                .withActionBarDrawerToggle(true)
                .withHeader(vi)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.drawer_item_5).withIcon(getResources().getDrawable(R.drawable.ic_search_black_24dp)).withBadge("").withIdentifier(6),
                        new PrimaryDrawerItem().withName(R.string.drawer_item_6).withIcon(getResources().getDrawable(R.drawable.ic_search_black_24dp)).withBadge("").withIdentifier(7),
                        new PrimaryDrawerItem().withName(R.string.drawer_item_1).withIcon(getResources().getDrawable(R.drawable.ic_search_black_24dp)).withBadge("").withIdentifier(1),
                        new PrimaryDrawerItem().withName(R.string.drawer_item_2).withIcon(getResources().getDrawable(R.drawable.ic_file_download_black_24dp)).withIdentifier(2),
                        new PrimaryDrawerItem().withName(R.string.drawer_item_3).withIcon(getResources().getDrawable(R.drawable.ic_star_border_black_24dp)).withBadge("").withIdentifier(3),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_help).withIcon(getResources().getDrawable(R.drawable.ic_settings_black_24dp)).withIdentifier(4),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_4).withIcon(FontAwesome.Icon.faw_question).setEnabled(false).withIdentifier(5),
                        new DividerDrawerItem()
                        //,                        new SecondaryDrawerItem().withName(R.string.drawer_item_contact).withIcon(FontAwesome.Icon.faw_github).withBadge("12+").withIdentifier(1)
                )
                .withOnDrawerListener(new Drawer.OnDrawerListener() {
                    @Override
                    public void onDrawerOpened(View drawerView) {
                        // Скрываем клавиатуру при открытии Navigation Drawer
                        InputMethodManager inputMethodManager = (InputMethodManager) DrawerActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(DrawerActivity.this.getCurrentFocus().getWindowToken(), 0);
                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {
                        if (mode == Modes.DOWNLOADS)
                            showDownloadsList();
                        if (((ScrollView) findViewById(R.id.scrollViewId))!=null)
                            ((ScrollView) findViewById(R.id.scrollViewId)).smoothScrollTo(0, 0);
                    }
                })
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    // Обработка клика
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        if (drawerItem instanceof Nameable) {
                            Toast.makeText(DrawerActivity.this, DrawerActivity.this.getString(((Nameable) drawerItem).getNameRes()), Toast.LENGTH_SHORT).show();
                        }
                        if (drawerItem instanceof Badgeable) {
                            Badgeable badgeable = (Badgeable) drawerItem;
                            if (badgeable.getBadge() != null) {
                                // учтите, не делайте так, если ваш бейдж содержит символ "+"
                                try {
                                    int badge = Integer.valueOf(badgeable.getBadge());
                                    if (badge > 0) {
                                        drawerResult.updateBadge(String.valueOf(badge - 1), position);
                                    }
                                } catch (Exception e) {
                                    Log.d("test", "Не нажимайте на бейдж, содержащий плюс! :)");
                                }
                            }
                        }
                        Fragment fragment = null;
                        if (drawerItem != null && drawerItem.getIdentifier() != 0)
                            switch (drawerItem.getIdentifier()) {
                                case 1:
                                    fragment = new SearchFragment();
                                    mode = Modes.SEARCH;
                                    break;
                                case 3:
                                    fragment = new SearchFragment();
                                    mode = Modes.FAVS;
                                    if (mContentAdapter != null && fragment != null)
                                        BackgroundLoader.startLoadingFavs(1, 0, 10);
                                    break;
                                case 6:
                                    fragment = new SearchFragment();
                                    mode = Modes.POPULAR_WEEK;
                                    if (mContentAdapter != null && fragment != null)
                                        BackgroundLoader.startLoadingPopularForWeek(1, 0, 10);
                                    break;
                                case 7:
                                    fragment = new SearchFragment();
                                    mode = Modes.POPULAR;
                                    if (mContentAdapter != null && fragment != null)
                                        BackgroundLoader.startLoadingPopular(1, 0, 10);
                                    break;
                                case 2:
                                    fragment = new SearchFragment();
                                    mode = Modes.DOWNLOADS;

                                    //if (mContentAdapter != null && fragment != null)
                                    //BackgroundLoader.startLoadingPopular(1, 0, 10);
                                    break;
                                default:
                                    break;

                            }
                        if (fragment != null) {
                            FragmentManager fragmentManager = getSupportFragmentManager();
                            fragmentManager.beginTransaction()
                                    .replace(R.id.content_frame, fragment).commit();

                            // Highlight the selected item, update the title, and close the drawer

                            setTitle(DrawerActivity.this.getString(((Nameable) drawerItem).getNameRes()));


                            //
                        }
                    }
                })
                .withOnDrawerItemLongClickListener(new Drawer.OnDrawerItemLongClickListener() {
                    @Override
                    // Обработка длинного клика, например, только для SecondaryDrawerItem
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        if (drawerItem instanceof SecondaryDrawerItem) {
                            Toast.makeText(DrawerActivity.this, DrawerActivity.this.getString(((SecondaryDrawerItem) drawerItem).getNameRes()), Toast.LENGTH_SHORT).show();
                        }
                        return false;
                    }
                });
        mDrawer.build();


    }

    //
    //  Обновление Navi Drawer
    //
    public void onResDownloaded() {
        ((ImageView) findViewById(R.id.header)).setImageBitmap(Account.photo);
        ((TextView) findViewById(R.id.headerText)).setText(Account.name);

        // Открыть фрагмент поиска по умолчанию
        fragment = new SearchFragment();
        mode = Modes.POPULAR_WEEK;
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment).commit();
        BackgroundLoader.startLoadingPopularForWeek(1, 0, 10);
    }

    @Override
    public void onAttachFragment(android.app.Fragment fragment) {
        super.onAttachFragment(fragment);
        if (mode == Modes.SEARCH)
            ((TextView) findViewById(R.id.headerTitle)).setText("Выберите категорию и начните поиск");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_search) {
            //handleMenuSearch();
            openSearch();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_drawer, container, false);
            return rootView;
        }


    }

}
