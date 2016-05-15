package com.makbeard.githubuserfinder.model;

/**
 * Класс-модель описывающий поля пользователя в Git
 */
public class GitUser {

    private String login;
    private String html_url;
    private String avatar_url;

    public String getLogin() {
        return login;
    }

    public String getHtmlUrl() {
        return html_url;
    }

    public String getAvatarUrl() {
        return avatar_url;
    }
}
