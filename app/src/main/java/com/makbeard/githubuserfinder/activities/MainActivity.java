package com.makbeard.githubuserfinder.activities;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.makbeard.githubuserfinder.GitHubApi;
import com.makbeard.githubuserfinder.R;
import com.makbeard.githubuserfinder.GitSearchSuggestion;
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
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MA_MAKTAG";

    private Subscription mSubscription;
    private List<GitUser> mGitUsersList = new ArrayList<>();
    private UsersRecyclerViewAdapter mRecyclerViewAdapter;
    private GitHubApi mGitHubApi;

    @BindView(R.id.floatingSearchView)
    FloatingSearchView mFloatingSearchView;

    @BindView(R.id.gitusers_recyclerview)
    RecyclerView mRecyclerView;

    @BindView(R.id.app_bar)
    AppBarLayout mAppBarLayout;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mGitHubApi = initRetrofit();

        //Создаём Observable из кастомного SearchBox
        Observable<String> searchBoxObservable =
                Observable.create(new rx.Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                mFloatingSearchView.setOnQueryChangeListener(
                        new FloatingSearchView.OnQueryChangeListener() {
                    @Override
                    public void onSearchTextChanged(String oldQuery, String newQuery) {
                        subscriber.onNext(newQuery);
                    }
                });
            }
        });

        final List<GitSearchSuggestion> gitSearchSuggestionList = new ArrayList<>();


        mFloatingSearchView.setOnFocusChangeListener(
                new FloatingSearchView.OnFocusChangeListener() {
            @Override
            public void onFocus() {

                //Выпадающий список
                GitSearchSuggestion gitSearchSuggestion;
                for(int i=0; i<10; i++){
                    gitSearchSuggestion = new GitSearchSuggestion("User " + i);
                    gitSearchSuggestion.setIsHistory(true);
                    gitSearchSuggestionList.add(gitSearchSuggestion);
                }

                mFloatingSearchView.swapSuggestions(gitSearchSuggestionList);
            }

            @Override
            public void onFocusCleared() {

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
}
