package com.makbeard.githubuserfinder;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.makbeard.githubuserfinder.model.GitUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для RecyclerView
 */
public class UsersRecyclerViewAdapter
        extends RecyclerView.Adapter<UsersRecyclerViewAdapter.ViewHolder> {

    private List<GitUser> mUsersList = new ArrayList<>();
    private IClickListener mListener;

    public UsersRecyclerViewAdapter(List<GitUser> usersList) {
        mUsersList.addAll(usersList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView cardView = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_cardview, parent, false);
        cardView.setClickable(true);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        TextView nameTextView = holder.mLoginTextView;
        nameTextView.setText(mUsersList.get(position).getLogin());

        CardView cardView = holder.mCardView;
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onClick(holder.getAdapterPosition());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUsersList.size();
    }

    public GitUser getItem(int position) {
        return mUsersList.get(position);
    }

    /**
     * Метод заменяет данные в адаптере на переданные
     * @param list данные для размещения в адаптере
     */
    public void updateAll(List<GitUser> list) {
        mUsersList.clear();
        mUsersList.addAll(list);
        notifyDataSetChanged();
    }

    public interface IClickListener {
        void onClick(int position);
    }

    public void setListener(IClickListener listener) {
        mListener = listener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private CardView mCardView;
        private TextView mLoginTextView;

        public ViewHolder(CardView cardView) {
            super(cardView);
            mLoginTextView = (TextView) cardView.findViewById(R.id.login_textview);
            mCardView = cardView;
        }
    }
}
