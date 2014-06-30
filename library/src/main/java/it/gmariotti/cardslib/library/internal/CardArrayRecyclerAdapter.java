package it.gmariotti.cardslib.library.internal;

import java.util.List;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;


public class CardArrayRecyclerAdapter extends RecyclerView.Adapter<CardArrayRecyclerAdapter.ViewHolder> {
    private List<Card> items;
    private int itemLayout;
    private CardArrayAdapter adapter;

    public CardArrayRecyclerAdapter(CardArrayAdapter adapter) {
        this.adapter = adapter;
        items = adapter.getList();
    }

    @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(adapter.getRowLayoutId(), null);
        return new ViewHolder(v, parent);
    }

    @Override public void onBindViewHolder(ViewHolder holder, int position) {
        Card item = items.get(position);
        holder.itemView.setTag(item);
        adapter.getView(position, holder.itemView, holder.parent);
    }

    @Override public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View view;
        ViewGroup parent;

        public ViewHolder(View itemView, ViewGroup parent) {
            super(itemView);
            view = itemView;
            this.parent = parent;
        }
    }
}
