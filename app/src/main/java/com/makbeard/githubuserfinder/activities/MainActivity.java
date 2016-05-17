package com.makbeard.githubuserfinder.activities;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.rxbinding.support.v7.widget.RxSearchView;
import com.makbeard.githubuserfinder.GitHubApi;
import com.makbeard.githubuserfinder.R;
import com.makbeard.githubuserfinder.UsersRecyclerViewAdapter;
import com.makbeard.githubuserfinder.model.GitUser;
import com.makbeard.githubuserfinder.model.RootUsersResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity
        implements SearchView.OnSuggestionListener {

    private static final String TAG = "MA_MAKTAG";

    private Subscription mSubscription;
    private List<GitUser> mGitUsersList = new ArrayList<>();
    private UsersRecyclerViewAdapter mRecyclerViewAdapter;
    private GitHubApi mGitHubApi;

    @BindView(R.id.gitusers_recyclerview)
    RecyclerView mRecyclerView;

    @BindView(R.id.app_bar)
    AppBarLayout mAppBarLayout;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    SearchView mSearchView;
    private SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mGitHubApi = initRetrofit();

        setSupportActionBar(mToolbar);

        String[] columnNames = {"_id", "text"};
        final MatrixCursor cursor = new MatrixCursor(columnNames);

        String[] array = {"Bauru", "Sao Paulo", "Rio de Janeiro",
                "Bahia", "Mato Grosso", "Minas Gerais",
                "Tocantins", "Rio Grande do Sul"};

        String[] temp = new String[2];
        int id = 0;
        for(String item : array){
            temp[0] = Integer.toString(id++);
            temp[1] = item;
            cursor.addRow(temp);
        }

        final String[] from = new String[] {"text"};
        final int[] to = new int[] {android.R.id.text1};

        mAdapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                null,
                from,
                to,
                0);


        /*
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
        mSearchView.setSearchableInfo(searchableInfo);

        searchView.setOnQueryTextListener(this);
        searchAdapter = new SimpleCursorAdapter(this, R.layout.listentry, null, new String[] { "name" }, new int[] { R.id.name_entry }, 0);
        searchView.setSuggestionsAdapter(searchAdapter)

        Then, in the onQueryTextChanged method I have this:

        searchAdapter.changeCursor(cursor);
        */

        //Настраиваем отображение RecyclerView
        mRecyclerViewAdapter = new UsersRecyclerViewAdapter(mGitUsersList);

        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
            mRecyclerView.setLayoutManager(
                    new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        }
    }

    /**
     * Метод возвращает настроеный Retrofit для работы с GitHubApi
     */
    private GitHubApi initRetrofit(){
        //Создаём interceptor для анализа логов
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Gson gson = new GsonBuilder()
                .create();

        //Настраиваем Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GitHubApi.ROOT_URL)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(GitHubApi.class);
    }

    // TODO: 15.05.2016 Сохранять ответы в кэш с помощью Realm на 1 минуту
    // TODO: 15.05.2016 Сделать SearchView history
    // TODO: 15.05.2016 Сделать поток чтения из БД c помощью Rx

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSubscription.unsubscribe();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        // TODO: 16.05.2016 Перенести SearchView в Menu
        mSearchView.setSuggestionsAdapter(mAdapter);
        mSearchView.setOnSuggestionListener(this);
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setQueryHint("Enter User Data");

        mSubscription = RxSearchView
                .queryTextChanges(mSearchView)
                .debounce(1000, TimeUnit.MILLISECONDS)
                .filter(new Func1<CharSequence, Boolean>() {
                    @Override
                    public Boolean call(CharSequence charSequence) {
                        return !TextUtils.isEmpty(charSequence);
                    }
                })
                .doOnNext(new Action1<CharSequence>() {
                    @Override
                    public void call(CharSequence charSequence) {
                        // TODO: 16.05.2016 Сохраняем запрос в БД
                        mGitHubApi.getUsersList(charSequence.toString())
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNext(new Action1<RootUsersResponse>() {
                                    @Override
                                    public void call(RootUsersResponse rootUsersResponse) {
                                        mRecyclerViewAdapter.updateAll(rootUsersResponse.items);
                                    }
                                })
                                .subscribe();
                    }
                })
                .subscribe();

        return true;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        Log.d(TAG, "onSuggestionSelect: " + mAdapter.getItem(position).toString());
        Cursor c = mAdapter.getCursor();
        if(c.moveToPosition(position)) {
            String selectedItem = c.getString(1);
            Log.d(TAG, "onSuggestionSelect: " + selectedItem);
        }
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        Log.d(TAG, "onSuggestionSelect: " + mAdapter.getItem(position).toString());
        return false;
    }
}
