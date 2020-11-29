package com.cloudfridge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

public class ExampleAdapter extends RecyclerView.Adapter<ExampleAdapter.ExampleViewHolder> {
    private ArrayList<ExampleItem> exampleList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(int position) throws JSONException, IOException;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public static class ExampleViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView textView1;
        public TextView textView2;
        public ImageView deleteImage;

        public ExampleViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            textView1 = itemView.findViewById(R.id.textView);
            textView2 = itemView.findViewById(R.id.textView2);
            deleteImage = itemView.findViewById(R.id.imageDelete);

            deleteImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            try {
                                listener.onDeleteClick(position);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
    }

    public ExampleAdapter(ArrayList<ExampleItem> exampleList) {
        this.exampleList = exampleList;
    }

    @NonNull
    @Override
    public ExampleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
        ExampleViewHolder exampleViewHolder = new ExampleViewHolder(view, listener);
        return exampleViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ExampleViewHolder holder, int position) {
        ExampleItem currentItem = this.exampleList.get(position);
        holder.imageView.setImageDrawable(currentItem.getImageResource());
        holder.textView1.setText(currentItem.getText1());
        holder.textView2.setText(currentItem.getText2());
    }

    @Override
    public int getItemCount() {
        return exampleList.size();
    }
}
