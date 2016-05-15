package com.makbeard.githubuserfinder.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SearchView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.rxbinding.widget.RxSearchView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent;
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
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static java.lang.String.format;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MA_MAKTAG";

    @BindView(R.id.search_edittext) EditText mSearchEditText;
    SearchView mSearchView;

    private Subscription mSubscription;
    private List<GitUser> mGitUsersList = new ArrayList<>();
    private UsersRecyclerViewAdapter mRecyclerViewAdapter;
    private RecyclerView mRecyclerView;
    private GitHubApi mGitHubApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



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

        mGitHubApi = retrofit.create(GitHubApi.class);

        mRecyclerViewAdapter = new UsersRecyclerViewAdapter(mGitUsersList);
        mRecyclerView = (RecyclerView) findViewById(R.id.gitusers_recyclerview);
        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));




        //Создаём слушателя для поля ввода
        mSubscription = RxTextView.textChangeEvents(mSearchEditText)
                .debounce(1000, TimeUnit.MILLISECONDS)
                .filter(new Func1<TextViewTextChangeEvent, Boolean>() {
                    @Override
                    public Boolean call(TextViewTextChangeEvent textViewTextChangeEvent) {
                        return textViewTextChangeEvent.text().length() > 0;
                    }
                })
                .doOnNext(new Action1<TextViewTextChangeEvent>() {
                    @Override
                    public void call(TextViewTextChangeEvent textViewTextChangeEvent) {
                        mGitHubApi.getUsersList(textViewTextChangeEvent.text().toString())
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(getUsersSubscriber());
                    }
                })
                .subscribe();



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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        RxSearchView.queryTextChanges(mSearchView)
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }
}
