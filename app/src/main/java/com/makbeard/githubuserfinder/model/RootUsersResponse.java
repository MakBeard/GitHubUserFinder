package com.makbeard.githubuserfinder.model;

import java.util.List;

/**
 * Модель корневого ответа GitHub API
 */
public class RootUsersResponse {

    public int total_count;
    public List<GitUser> items;
}
