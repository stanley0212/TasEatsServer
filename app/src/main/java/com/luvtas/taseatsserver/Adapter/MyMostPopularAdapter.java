package com.luvtas.taseatsserver.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.luvtas.taseatsserver.Callback.IRecyclerClickListener;
import com.luvtas.taseatsserver.Model.MostPopularModel;
import com.luvtas.taseatsserver.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyMostPopularAdapter extends RecyclerView.Adapter<MyMostPopularAdapter.MyViewHolder> {

    Context context;
    List<MostPopularModel> mostPopularModelList;

    public MyMostPopularAdapter(Context context, List<MostPopularModel> mostPopularModelList) {
        this.context = context;
        this.mostPopularModelList = mostPopularModelList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_category_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(mostPopularModelList.get(position).getImage()).into(holder.category_image);
        holder.category_name.setText(new StringBuilder(mostPopularModelList.get(position).getName()));

        // EVENT
        holder.setListener((view, pos) -> {

        });
    }

    @Override
    public int getItemCount() {
        return mostPopularModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        Unbinder unbinder;

        @BindView(R.id.img_category)
        ImageView category_image;
        @BindView(R.id.txt_category)
        TextView category_name;

        IRecyclerClickListener listener;

        public IRecyclerClickListener getListener() {
            return listener;
        }

        public void setListener(IRecyclerClickListener listener) {
            this.listener = listener;
        }

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onItemClickListener(view, getAdapterPosition());
        }
    }
}
