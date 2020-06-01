package com.dragonfire.wildfirelauncher;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    ArrayList<String> categories;
    Context context;
    private static final String TAG = "CategoryAdapter";
    private int selectedPos = RecyclerView.NO_POSITION;
    private CategorySelectListener mCategorySelectListener;

    public CategoryAdapter(Context context, ArrayList<String> categories) {
        this.context = context;
        this.categories = categories;
    }

    public void setmCategorySelectListener(CategorySelectListener mCategorySelectListener) {
        this.mCategorySelectListener = mCategorySelectListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        holder.categoryTextView.setText(categories.get(position));
        holder.itemView.setSelected(selectedPos == position);

        if(holder.itemView.isSelected()) {
            holder.categoryTextView.setTextColor(Color.WHITE);
        }
        else
            holder.categoryTextView.setTextColor(Color.parseColor("#F50057"));

        holder.categoryTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyItemChanged(selectedPos);
                selectedPos = holder.getLayoutPosition();
                notifyItemChanged(selectedPos);
                if(mCategorySelectListener != null) {
                    mCategorySelectListener.onAppCategorySelected(categories.get(position));
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView categoryTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.category_item_textView);
        }

    }
}
