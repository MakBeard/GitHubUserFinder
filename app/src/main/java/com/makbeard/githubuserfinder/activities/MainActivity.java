package com.makbeard.githubuserfinder.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.animation.Interpolator;
import android.widget.ArrayAdapter;
import android.widget.SearchView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.rxbinding.widget.RxSearchView;
import com.makbeard.githubuserfinder.GitHubApi;
import com.makbeard.githubuserfinder.R;
import com.makbeard.githubuserfinder.UsersRecyclerViewAdapter;
import com.makbeard.githubuserfinder.model.GitUser;
import com.makbeard.githubuserfinder.model.RootUsersResponse;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;

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
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MA_MAKTAG";

    SearchView mSearchView;

    private Subscription mSubscription;
    private List<GitUser> mGitUsersList = new ArrayList<>();
    private UsersRecyclerViewAdapter mRecyclerViewAdapter;
    private RecyclerView mRecyclerView;
    private GitHubApi mGitHubApi;

    @BindView(R.id.searchbox)
    SearchBox mSearchBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mGitHubApi = initRetrofit();

        //Создаём Observable из кастомного SearchBox
        Observable<String> searchBoxObservable = Observable.create(new rx.Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                mSearchBox.setSearchListener(new SearchBox.SearchListener() {
                    @Override
                    public void onSearchOpened() {

                    }

                    @Override
                    public void onSearchCleared() {

                    }

                    @Override
                    public void onSearchClosed() {

                    }

                    @Override
                    public void onSearchTermChanged(String s) {
                        subscriber.onNext(s);

                    }

                    @Override
                    public void onSearch(String s) {
                        // TODO: 15.05.2016 Сохранять запросы в кэш с помощью Realm
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onResultClick(SearchResult searchResult) {

                    }
                });
            }
        });

        mSubscription = searchBoxObservable
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
                        mGitHubApi.getUsersList(charSequence.toString())
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(getUsersSubscriber());
                    }
                })
                .subscribe();


        //Настраиваем отображение RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.gitusers_recyclerview);
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
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mSearchBox.revealFromMenuItem(R.id.action_search, this);
/*
        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        mSubscription = RxSearchView.queryTextChanges(mSearchView)
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
                        mGitHubApi.getUsersList(charSequence.toString())
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(getUsersSubscriber());
                    }
                })
                .subscribe();
*/

        // TODO: 15.05.2016 Сохранять ответы в кэш с помощью Realm на 1 минуту

        // TODO: 15.05.2016 Сделать SearchView history
        // TODO: 15.05.2016 Сделать поток чтения из БД c помощью Rx

        return true;
    }

    private Subscriber<RootUsersResponse> getUsersSubscriber() {
        return new Subscriber<RootUsersResponse>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(RootUsersResponse rootUsersResponse) {
                mRecyclerViewAdapter.updateAll(rootUsersResponse.items);
                for (GitUser gitUser : rootUsersResponse.items) {
                    Log.d(TAG, "onNext: " + gitUser.getLogin() + " " + gitUser.getAvatarUrl() + " " + gitUser.getHtmlUrl());
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSubscription.unsubscribe();
    }
}
