package com.makbeard.githubuserfinder;

import com.makbeard.githubuserfinder.model.RootUsersResponse;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Интерфейс запосов к GitHub для Retrofit
 */
public interface GitHubApi {

    String ROOT_URL = "https://api.github.com";

    @GET("/search/users")
    Observable<RootUsersResponse> getUsersList(@Query("q") String q);
}
