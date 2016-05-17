package com.makbeard.githubuserfinder.activities;

import android.app.SearchManager;
import android.content.ComponentName;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
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
import com.makbeard.githubuserfinder.model.Suggestion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MA_MAKTAG";

    private CompositeSubscription mCompositeSubscription;
    private List<GitUser> mGitUsersList = new ArrayList<>();
    private UsersRecyclerViewAdapter mRecyclerViewAdapter;
    private GitHubApi mGitHubApi;
    private Realm mRealm;

    @BindView(R.id.gitusers_recyclerview)
    RecyclerView mRecyclerView;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    SearchView mSearchView;
    private SimpleCursorAdapter mAdapter;
    private RealmConfiguration mRealmConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mGitHubApi = initRetrofit();

        setSupportActionBar(mToolbar);

        mCompositeSubscription = new CompositeSubscription();

        //Настраиваем Realm
        mRealmConfig = new RealmConfiguration.Builder(this)
                .deleteRealmIfMigrationNeeded()
                .build();
        mRealm = Realm.getInstance(mRealmConfig);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeSubscription.unsubscribe();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        //Настраиваем отображение последних запросов
        final String[] from = new String[] {"Suggestion"};
        final int[] to = new int[] {R.id.suggestion_text};

        mAdapter = new SimpleCursorAdapter(this,
                R.layout.suggestion_item,
                null,
                from,
                to,
                0);

        mSearchView.setSuggestionsAdapter(mAdapter);

        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor c = mAdapter.getCursor();
                if(c.moveToPosition(position)) {
                    String selectedItem = c.getString(1);
                    mSearchView.setQuery(selectedItem, true);
                }
                return false;
            }
        });

        mSearchView.setQueryHint("Enter User Data");

        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        mSearchView.setSearchableInfo(
                searchManager.getSearchableInfo(new ComponentName(this, MainActivity.class))
        );

        mSearchView.setIconifiedByDefault(false);

        //Обновляем Suggestions из БД
        Observable<RealmResults<Suggestion>> realmObservable = mRealm.where(Suggestion.class)
                .findAllAsync()
                .asObservable();

        //При изменении данных в БД меням Suggestion адаптер
        mCompositeSubscription.add(
                realmObservable
                .subscribe(new Action1<RealmResults<Suggestion>>() {
                    @Override
                    public void call(RealmResults<Suggestion> gitUsers) {
                        if (!gitUsers.isEmpty()) {
                            String[] temp = new String[2];
                            Iterator iterator =
                                    gitUsers.subList(gitUsers.size() - 5, gitUsers.size())
                                            .iterator();
                            int i = 0;
                            MatrixCursor matrixCursor =
                                    new MatrixCursor(new String[] {"_id", "Suggestion"});
                            do {
                                temp[0] = Integer.toString(i++);
                                temp[1] = iterator.next().toString();
                                matrixCursor.addRow(temp);
                            } while (iterator.hasNext());

                            mAdapter.changeCursor(matrixCursor);
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                })
        );

        mCompositeSubscription.add(
                //Подписка на события изменения SearchView
                RxSearchView
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

                        //Получаем доступ к Realm внутри данного потока
                        Realm realm = Realm.getInstance(mRealmConfig);

                        //Сохраняем запрос в БД
                        realm.beginTransaction();
                        Suggestion suggestion = realm.createObject(Suggestion.class);
                        suggestion.setSuggestion(charSequence.toString());
                        realm.commitTransaction();

                        mGitHubApi.getUsersList(charSequence.toString())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.newThread())
                                .doOnNext(new Action1<RootUsersResponse>() {
                                    @Override
                                    public void call(RootUsersResponse rootUsersResponse) {
                                        mRecyclerViewAdapter.updateAll(rootUsersResponse.items);
                                    }
                                })
                                .subscribe();
                    }
                })
                .subscribe()
        );

        return true;
    }
}
