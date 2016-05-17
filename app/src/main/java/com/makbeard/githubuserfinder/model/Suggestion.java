package com.makbeard.githubuserfinder.model;

import io.realm.RealmObject;

/**
 * Класс-модель, описывающий запрос
 */
public class Suggestion extends RealmObject {

    private String mSuggestion;

    public Suggestion() {

    }

    public Suggestion(String suggestion) {
        mSuggestion = suggestion;
    }

    public void setSuggestion(String suggestion) {
        mSuggestion = suggestion;
    }

    @Override
    public String toString() {
        return mSuggestion;
    }
}
