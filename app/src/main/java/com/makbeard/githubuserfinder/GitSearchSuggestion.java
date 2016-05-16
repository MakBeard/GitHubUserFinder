package com.makbeard.githubuserfinder;

import android.os.Parcel;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

/**
 * Класс для отображения в выпадающем списке
 */
public class GitSearchSuggestion implements SearchSuggestion {

    private String mUserName;
    private boolean mIsHistory;

    public GitSearchSuggestion(String userName) {
        mUserName = userName;
    }

    public GitSearchSuggestion(Parcel source) {
        mUserName = source.readString();
    }

    public String getUserName() {
        return mUserName;
    }

    public void setIsHistory(boolean isHistory){
        this.mIsHistory = isHistory;
    }

    public boolean getIsHistory(){
        return this.mIsHistory;
    }

    @Override
    public String getBody() {
        return getUserName();
    }

    @Override
    public Creator getCreator() {
        return CREATOR;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mUserName);
    }

    public static final Creator<GitSearchSuggestion> CREATOR = new Creator<GitSearchSuggestion>() {
        @Override
        public GitSearchSuggestion createFromParcel(Parcel source) {
            return new GitSearchSuggestion(source);
        }

        @Override
        public GitSearchSuggestion[] newArray(int size) {
            return new GitSearchSuggestion[size];
        }
    };
}
