package app.pomis.misisbooks.views;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
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
import app.pomis.misisbooks.bl.EncodingUtil;
import app.pomis.misisbooks.bl.FileDownloader;
import app.pomis.misisbooks.bl.MediaFileFunctions;
import app.pomis.misisbooks.bl.ResourcesLoader;
import app.pomis.misisbooks.bl.SearchAndLoadHistory;
import app.pomis.misisbooks.bl.TwoSphereAuth;
import ru.twosphere.metrica.src.Metrica;


public class DrawerActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
    static DrawerActivity singleton;
    Drawer mDrawer;
    Fragment fragment;
    public ContentAdapter mContentAdapter;
    MaterialDialog mMaterialDialog;
    public int mode = 0; // 1 поиск
    public int downloadMode = 1;
    public boolean isContinuingLoading = false;
    Metrica metrica = new Metrica();

    public void addBook(Book book) {
        BackgroundLoader.addBook(book);
        if (mContentAdapter != null)
            mContentAdapter.notifyDataSetChanged();
    }

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

    public Toolbar toolbar;

    // Запуск активности
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        metrica.track("RUN_APPLICATION");

        String test = "http://twosphere.ru/api/auth.signin?vk_access_token=" + Account.account.access_token;
        singleton = this;

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

        // спрятать подложку статусбара для лоллипопа, если запущено на старом ведре
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            findViewById(R.id.statusBarLollipop).setVisibility(View.INVISIBLE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) toolbar.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
            toolbar.setLayoutParams(params);
            params = (RelativeLayout.LayoutParams) search.getLayoutParams();
            params.setMargins(-6, -4, -6, 0);
            search.setLayoutParams(params);
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT)
            findViewById(R.id.statusBarLollipop).setBackgroundColor(getResources().getColor(R.color.primaryColorDark));
        // Подрузка загруженных файлов


        BackgroundLoader.startLoadingCats();
        // Мы получили токен. Грузим аву и имя
        new ResourcesLoader().execute("http://twosphere.ru/api/account.getInfo?access_token=" + Account.getInstance().twosphere_token);


        createNavigationDrawer();
        search.bringToFront();
        mSearchAndLoadHistory = new SearchAndLoadHistory(this);
        mSearchAndLoadHistory.loadAdd(search);
        mSearchAndLoadHistory.loadDownloadList();
        setToolbarTitle();
        MainActivity.instance.finish();

        mMaterialDialog = new MaterialDialog.Builder(this)
                .title("Подключение")
                .content("Выполняется подключение к библиотеке")
                .progress(true, 0)
                .cancelable(false)
                .show();

    }


    boolean wasClosedByBackButton = false;

    // Кнопка назад
    @Override
    public void onBackPressed() {
        metrica.track("BACK_BUTTON_PRESSED");

        if (isSearchOpened) {
            wasClosedByBackButton = true;
            closeSearch();
            isSearchOpened = false;
            mSearchAction.setEnabled(true);
            return;
        }
        super.onBackPressed();
    }

    //region Поиск
    private MenuItem mSearchAction;
    SearchAndLoadHistory mSearchAndLoadHistory;
    public boolean isSearchOpened = false;
    public int catId = 1;


    public void doSearch() throws UnsupportedEncodingException {
        findViewById(R.id.materialsNone).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.headerTitle)).setText("Идёт загрузка, пожалуйста подождите");
        if (search.getSearchText().length() > 0) {
            BackgroundLoader.startLoadingSearchResults(EncodingUtil.encodeURIComponent(search.getSearchText()), 10, 0, catId);
            mode = Modes.SEARCH;
        } else {
            mode = Modes.POPULAR_WEEK;
            BackgroundLoader.startLoadingPopularForWeek(catId, 0, 10);
        }
        //
        hideKeyboard();
    }


    // Выдача результатов поиска (или загрузки)
    public void onSearchResultDownloaded(int countOfNewItems) {
        mContentAdapter = new ContentAdapter(this, R.layout.book_layout,
                (mode == Modes.DOWNLOADS) ? SearchAndLoadHistory.downloadedBooks : BackgroundLoader.loadedBooks);

        ListView lv = ((ListView) findViewById(R.id.search_result));
//            lv.addHeaderView(view);
//            ((TextView)lv.findViewById(R.id.headerTitle)).setText("Результаты поиска");
        if (lv.getAdapter() == null) {

            lv.setAdapter(mContentAdapter);
            lv.setOnItemClickListener(this);
            mContentAdapter.notifyDataSetChanged();
        } else mContentAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(lv);

        if (countOfNewItems > 9)
            findViewById(R.id.footerContainer).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.footerContainer).setVisibility(View.GONE);
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
                break;
        }
        mContentAdapter.notifyDataSetChanged();
        if (!isContinuingLoading) {
            ((ScrollView) findViewById(R.id.scrollViewId)).fullScroll(ScrollView.FOCUS_UP);
        } else {
            mMaterialDialog.hide();
            ((ScrollView) findViewById(R.id.scrollViewId)).fullScroll(ScrollView.FOCUS_DOWN);
        }

        toolbar.getMenu().getItem(0).setTitle("Поиск");
        if ((mode == Modes.SEARCH || mode == Modes.FAVS || mode == Modes.POPULAR_WEEK || mode == Modes.POPULAR) &&
                BackgroundLoader.loadedBooks.size() == 0) {
            findViewById(R.id.materialsNone).setVisibility(View.VISIBLE);
            findViewById(R.id.materialsNone).bringToFront();
        } else
            findViewById(R.id.materialsNone).setVisibility(View.GONE);
    }

    // Список загрузок
    public void showDownloadsList() {
        metrica.track("DOWNLOADS_SHOW_LIST");

        SearchAndLoadHistory.getInstance().loadDownloadList();
        mContentAdapter = new ContentAdapter(this, R.layout.book_layout, SearchAndLoadHistory.downloadedBooks);
        ListView lv = ((ListView) findViewById(R.id.search_result));
        lv.setAdapter(mContentAdapter);
        lv.setOnItemClickListener(this);
        mContentAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(lv);
        ((ScrollView) findViewById(R.id.scrollViewId)).smoothScrollTo(0, 0);

        if (SearchAndLoadHistory.downloadedBooks.size() == 0) {
            findViewById(R.id.materialsNone).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.materialsNone).setVisibility(View.GONE);

        }
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

    public void deleteBook(int position) {
        SearchAndLoadHistory.downloadedBooks.remove(position);
        metrica.track("BOOK_DELETED_FROM_STORAGE");
        mContentAdapter.notifyDataSetChanged();
    }


    public void loadMore(View view) {
        isContinuingLoading = true;
        switch (mode) {
            case Modes.POPULAR:
                metrica.track("POPULAR_MORE_LOADED");
                BackgroundLoader.continueLoadingPopular(catId, BackgroundLoader.loadedBooks.size(), 10);
                break;
            case Modes.FAVS:
                metrica.track("FAVE_MORE_LOADED");
                BackgroundLoader.continueLoadingFavs(catId, BackgroundLoader.loadedBooks.size(), 10);
                break;
            case Modes.POPULAR_WEEK:
                metrica.track("POPULAR_WEEK_MORE_LOADED");
                BackgroundLoader.continueLoadingPopularForWeek(catId, BackgroundLoader.loadedBooks.size(), 10);
                break;
            case Modes.SEARCH:
                metrica.track("SEARCH_MORE_LOADED");
                BackgroundLoader.continueLoadingSearchResults(search.getSearchText(), 10, BackgroundLoader.loadedBooks.size(), catId);
                break;
        }
        mMaterialDialog = new MaterialDialog.Builder(this)
                .title("Загрузка...")
                .content("Подождите")
                .progress(true, 0)
                .show();
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

    private void hideKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    SearchBox search;

    //
    // Гмейловый поиск
    //
    public void openSearch() {
        //setTitle("");
        setToolbarTitle();
        search.setLogoText("");
        search.bringToFront();
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
                metrica.track("SEARCH_TOOLBAR_OPENED");

                // Use this to tint the screen
                mSearchAction.setEnabled(false);
                //((ImageView)findViewById(R.id.mic)).setMaxHeight(0);
                //((ImageView)findViewById(R.id.mic)).setMaxWidth(0);
                ((Spinner) findViewById(R.id.spinnerToolbar)).setEnabled(false);
                findViewById(R.id.mic).setVisibility(View.GONE);
                ((EditText) findViewById(R.id.search)).addTextChangedListener(new TextWatcher() {


                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
            }

            @Override
            public void onSearchClosed() {
                metrica.track("SEARCH_TOOLBAR_CLOSED");

                // Use this to un-tint the screen
                closeSearch();
                mSearchAction.setEnabled(true);
                if (!wasClosedByBackButton) {
                    try {
                        if (search.getSearchText().length() == 0) doSearch();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else {
                    wasClosedByBackButton = false;
                }
                ((Spinner) findViewById(R.id.spinnerToolbar)).setEnabled(true);

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
                search.setSearchString(search.getSearchText().trim());
                search.addSearchable(new SearchResult(search.getSearchText().trim(), getResources().getDrawable(R.drawable.ic_history)));
                ((ArrayAdapter) ((ListView) search.findViewById(R.id.results)).getAdapter()).notifyDataSetChanged();
                mSearchAndLoadHistory.saveAll(search);
                mSearchAndLoadHistory.loadAdd(search);
                ((Spinner) findViewById(R.id.spinnerToolbar)).setEnabled(false);

            }

            @Override
            public void onSearchCleared() {
                // findViewById(R.id.mic).setVisibility(View.GONE);
                // ((ImageView)findViewById(R.id.mic)).setEnabled(false);
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
        if (search.getSearchText().isEmpty()) setToolbarTitle();
    }

    public void setToolbarTitle() {
        if (mode == Modes.DOWNLOADS) {
            toolbar.setTitle("Загрузки");
            //setTitle("MISIS Books");
        } else
            toolbar.setTitle("");
    }

    //
    // Нажатие на книжку, диалог скачивания
    //
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        metrica.track("ITEM_CLICKED");

        final int index = i;
        String descr = "";
        switch (mode) {
            case Modes.DOWNLOADS:
                descr = "Авторы: " + SearchAndLoadHistory.downloadedBooks.get(i).getAuthorsToString() +
                        "\nКатегория: " + SearchAndLoadHistory.downloadedBooks.get(i).category.categoryName +
                        "\nРазмер: " + SearchAndLoadHistory.downloadedBooks.get(i).size;
                new MaterialDialog.Builder(this)
                        .title(SearchAndLoadHistory.downloadedBooks.get(i).name)
                        .content(descr)
                        .positiveText("Открыть")
                        .negativeText("Удалить")
                        .negativeColorAttr(Color.parseColor("#ffffff"))
                        .positiveColorRes(R.color.primaryColor)
                        .neutralColorAttr(Color.parseColor("#ffffff"))

                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            ///
                            /// Открытие скачанного файла
                            ///
                            public void onPositive(MaterialDialog dialog) {
                                metrica.track("OPEN_DOWNLOADED_FILE");

                                super.onPositive(dialog);
                                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                        SearchAndLoadHistory.downloadedBooks.get(index).fileName);
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
                                        Toast.makeText(DrawerActivity.getInstance(), "Невозможно открыть файл с данным типом", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }

                            @Override
                            ///
                            /// Удаление файла
                            ///
                            public void onNegative(MaterialDialog dialog) {
                                metrica.track("DELETE_DOWNLOADED_FILE");

                                super.onNegative(dialog);
                                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                        SearchAndLoadHistory.downloadedBooks.get(index).fileName);

                                if (file.delete() ||
                                        MediaFileFunctions.deleteViaContentProvider(DrawerActivity.getInstance(), SearchAndLoadHistory.downloadedBooks.get(index).fileName)) {
                                    Toast.makeText(DrawerActivity.getInstance(), "Файл удалён", Toast.LENGTH_SHORT).show();
                                    //FileDownloader.downloadedBooks.remove(index);
                                    DrawerActivity.getInstance().deleteBook(index);//refresh();
                                    if (SearchAndLoadHistory.downloadedBooks.size() == 0) {
                                        findViewById(R.id.materialsNone).setVisibility(View.VISIBLE);
                                    } else {
                                        findViewById(R.id.materialsNone).setVisibility(View.GONE);

                                    }
                                } else {
                                    Toast.makeText(DrawerActivity.getInstance(), "У данного приложения нет прав на удаление файлов", Toast.LENGTH_SHORT).show();

                                }

                            }

                        })
                        .show();

                break;
            default:
                descr = "Авторы: " + BackgroundLoader.loadedBooks.get(i).getAuthorsToString() +
                        "\nКатегория: " + BackgroundLoader.loadedBooks.get(i).category.categoryName +
                        "\nРазмер: " + BackgroundLoader.loadedBooks.get(i).size +
                        "\nСкачано " + BackgroundLoader.loadedBooks.get(i).countDl + " раз.";
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

                                    metrica.track("START_DOWNLOAD");

                                    String url = BackgroundLoader.loadedBooks.get(index).downloadUrl;

                                    if (downloadMode == Settings.BROWSER_DOWNLOAD) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                        startActivity(Intent.createChooser(intent, "Выберите браузер"));
                                    } else if (downloadMode == Settings.CUSTOM_DOWNLOAD) {
                                        new FileDownloader().execute(BackgroundLoader.loadedBooks.get(index));
                                    }
                                }
                            }

                            @Override
                            public void onNeutral(MaterialDialog dialog) {
                                super.onNeutral(dialog);

                                metrica.track("TOGGLE_FAVE");

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
                Book item = SearchAndLoadHistory.downloadedBooks.get(position);
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
                        //new PrimaryDrawerItem().withName(R.string.drawer_item_6).withIcon(getResources().getDrawable(R.drawable.ic_thumb_up_black_24dp)).withBadge("").withIdentifier(7),
                        //new PrimaryDrawerItem().withName(R.string.drawer_item_1).withIcon(getResources().getDrawable(R.drawable.ic_search_black_24dp)).withBadge("").withIdentifier(1),
                        new PrimaryDrawerItem().withName(R.string.drawer_item_3).withIcon(getResources().getDrawable(R.drawable.ic_star_border_black_24dp)).withBadge("").withIdentifier(3),
                        new PrimaryDrawerItem().withName(R.string.drawer_item_2).withIcon(getResources().getDrawable(R.drawable.ic_file_download_black_24dp)).withIdentifier(2),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName("Выход").withIcon(getResources().getDrawable(R.drawable.ic_exit_to_app_black_24dp)).withIdentifier(666)
                        //new SecondaryDrawerItem().withName(R.string.drawer_item_4).withIcon(FontAwesome.Icon.faw_question).setEnabled(false).withIdentifier(5),
                        //new DividerDrawerItem()
                        //,                        new SecondaryDrawerItem().withName(R.string.drawer_item_contact).withIcon(FontAwesome.Icon.faw_github).withBadge("12+").withIdentifier(1)
                )
                .withOnDrawerListener(new Drawer.OnDrawerListener() {
                    @Override
                    public void onDrawerOpened(View drawerView) {
                        // Скрываем клавиатуру при открытии Navigation Drawer

                        metrica.track("NAVIGATION_DRAWER_OPENED");

                        InputMethodManager inputMethodManager = (InputMethodManager) DrawerActivity.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(DrawerActivity.this.getCurrentFocus().getWindowToken(), 0);
                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {
                        metrica.track("NAVIGATION_DRAWER_CLOSED");

                        if (mode == Modes.DOWNLOADS) {
                            showDownloadsList();
                            (findViewById(R.id.headerLayout)).setVisibility(View.GONE);
                        }

                        //if (((ScrollView) findViewById(R.id.scrollViewId))!=null)
                        //    ((ScrollView) findViewById(R.id.scrollViewId)).smoothScrollTo(0, 0);
                    }
                })
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    // Обработка клика
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        if (drawerItem instanceof Nameable) {
                            //   Toast.makeText(DrawerActivity.this, DrawerActivity.this.getString(((Nameable) drawerItem).getNameRes()), Toast.LENGTH_SHORT).show();
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
                        DrawerActivity.getInstance().toolbar.getMenu().getItem(0)
                                .setVisible(true);
                        if (drawerItem != null && drawerItem.getIdentifier() != 0)
                            switch (drawerItem.getIdentifier()) {
                                case 1:
                                    metrica.track("GO_TO_SEARCH");

                                    fragment = new SearchFragment();
                                    mode = Modes.SEARCH;
                                    break;
                                case 3:
                                    metrica.track("GO_TO_FAVES");

                                    fragment = new SearchFragment();
                                    mode = Modes.FAVS;
                                    //if (mContentAdapter != null && fragment != null)
                                    //    BackgroundLoader.startLoadingFavs(1, 0, 10);
                                    break;
                                case 6:
                                    metrica.track("GO_TO_POPULAR_FOR_WEEK");

                                    fragment = new SearchFragment();
                                    mode = Modes.POPULAR_WEEK;
                                    //if (mContentAdapter != null && fragment != null)
                                    //    BackgroundLoader.startLoadingPopularForWeek(1, 0, 10);
                                    break;
                                case 7:
                                    metrica.track("GO_TO_POPULAR");

                                    fragment = new SearchFragment();
                                    mode = Modes.POPULAR;
                                    //if (mContentAdapter != null && fragment != null)
                                    //    BackgroundLoader.startLoadingPopular(1, 0, 10);
                                    break;
                                case 2:
                                    metrica.track("GO_TO_DOWNLOADS");

                                    fragment = new SearchFragment();
                                    mode = Modes.DOWNLOADS;
                                    DrawerActivity.getInstance().toolbar.getMenu().getItem(0)
                                            .setVisible(false);
                                    //if (mContentAdapter != null && fragment != null)
                                    //BackgroundLoader.startLoadingPopular(1, 0, 10);
                                    break;
                                case 666:
                                    quit();
                                    break;
                                default:
                                    break;

                            }
                        if (fragment != null) {
                            FragmentManager fragmentManager = getSupportFragmentManager();
                            fragmentManager.beginTransaction()
                                    .replace(R.id.content_frame, fragment).commit();

                            // Highlight the selected item, update the title, and close the drawer

                            //setTitle(DrawerActivity.this.getString(((Nameable) drawerItem).getNameRes()));


                            //
                        }
                        if (mode == Modes.DOWNLOADS) {
                            ((Spinner) findViewById(R.id.spinnerToolbar)).setVisibility(View.GONE);
                            (findViewById(R.id.headerLayout)).setVisibility(View.GONE);
                            setToolbarTitle();
                        } else {
                            ((Spinner) findViewById(R.id.spinnerToolbar)).setVisibility(View.VISIBLE);
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
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int dpWidth = (int) (displayMetrics.widthPixels / displayMetrics.density);
        int width = (dpWidth - 30 > 300) ? 300 : dpWidth - 30;
        mDrawer.withDrawerWidthDp(width);

        mDrawer.build();


    }

    private void quit() {
        metrica.track("ACCOUNT_QUIT");
        Account.account.twosphere_token = null;
        Account.clear();
        if (MainActivity.instance != null) MainActivity.instance.finish();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    //
    //  Обновление Navi Drawer
    //
    boolean loaded = false;

    public void onResDownloaded() {
        if (((ImageView) findViewById(R.id.header)) != null) {
            ((ImageView) findViewById(R.id.header)).setImageBitmap(Account.photo);
            ((TextView) findViewById(R.id.headerText)).setText(Account.name);
        }
        // Открыть фрагмент поиска по умолчанию
        fragment = new SearchFragment();
        mode = Modes.POPULAR_WEEK;
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment).commit();
        if (!loaded) {
            BackgroundLoader.startLoadingPopularForWeek(1, 0, 10);
        }
        loaded = true;

        mMaterialDialog.hide();
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
