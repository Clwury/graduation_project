package com.example.styletransfromapplication.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.styletransfromapplication.R;


public class RecycleViewAdapter extends RecyclerView.Adapter {
    private Drawable[] data;
    private String[] text;
    private Context context;
    private ItemClickListener itemClickListener;

    public void setOnItemClickListener(ItemClickListener itemClickListener){
        this.itemClickListener = itemClickListener;
    }

    public RecycleViewAdapter(Drawable[] data, String[] text, Context context){
        this.data = data;
        this.text = text;
        this.context = context;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView imageView;
        TextView textView;
        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.card_item);
            textView = itemView.findViewById(R.id.texts);
            imageView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (itemClickListener != null){
                itemClickListener.OnItemClick(v, (Integer) imageView.getTag());
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Log.d("风格card", String.valueOf(data[position]));
        Log.d("风格text", String.valueOf(text[position]));
        ((MyViewHolder)holder).imageView.setImageDrawable(data[position]);
        ((MyViewHolder)holder).textView.setText(text[position]);
//        if (itemClickListener != null){
//            ((MyViewHolder) holder).imageView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    int position = holder.getLayoutPosition();
//                    itemClickListener.OnItemClick(v, position);
//                }
//            });
//        }
        ((MyViewHolder) holder).imageView.setTag(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return data.length;
    }
}
