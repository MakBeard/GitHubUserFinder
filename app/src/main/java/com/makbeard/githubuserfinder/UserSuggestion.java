package com.makbeard.githubuserfinder;

import android.os.Parcel;
import android.os.Parcelable;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

/**
 * Created by Hp on 16.05.2016.
 */
public class UserSuggestion implements SearchSuggestion {

    private String mUserName;
    private boolean mIsHistory;

    public UserSuggestion(String userName) {
        mUserName = userName;
    }

    public UserSuggestion(Parcel source) {
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

    public static final Creator<UserSuggestion> CREATOR = new Creator<UserSuggestion>() {
        @Override
        public UserSuggestion createFromParcel(Parcel source) {
            return new UserSuggestion(source);
        }

        @Override
        public UserSuggestion[] newArray(int size) {
            return new UserSuggestion[size];
        }
    };
}
