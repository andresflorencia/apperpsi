package com.florencia.erpapp.adapters;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.activities.ProductoBusquedaActivity;
import com.florencia.erpapp.models.Categoria;
import com.florencia.erpapp.utils.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public class ClasificacionAdapter extends RecyclerView.Adapter<ClasificacionAdapter.ClasificacionViewHolder>{
    public static final String TAG = "TAG_CLASIFICACIONADAPTER";

    public List<Categoria> categorias;
    private ProductoBusquedaActivity activity;
    View rootView;

    public ClasificacionAdapter(ProductoBusquedaActivity activity, List<Categoria> categorias){
        this.categorias = categorias;
        this.activity = activity;
        rootView = activity.findViewById(android.R.id.content);
    }

    @NonNull
    @Override
    public ClasificacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ClasificacionViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_clasificacion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ClasificacionViewHolder holder, int position) {
        holder.bindClasificacion(categorias.get(position));
    }

    @Override
    public int getItemCount() {
        return categorias.size();
    }

    class ClasificacionViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;

        ClasificacionViewHolder(@NonNull View itemView){
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
        }

        void bindClasificacion(final Categoria categoria){
            tvNombre.setText(categoria.nombrecategoria.toUpperCase().concat(" (" + categoria.cantidad + ")"));
            if(categoria.seleccionado){
                tvNombre.setTextColor(Color.WHITE);
                itemView.setBackgroundResource(R.drawable.bg_button_confirmation);
            }else{
                tvNombre.setTextColor(Color.BLACK);
                itemView.setBackgroundResource(R.drawable.bg_white);
            }

            itemView.setOnClickListener(v -> {
                    Categoria miC = categorias.get(getAdapterPosition());
                    categorias.get(getAdapterPosition()).seleccionado = true;
                    for(Categoria c:categorias){
                        if(!c.nombrecategoria.equals(miC.nombrecategoria))
                            c.seleccionado = false;
                    }
                    notifyDataSetChanged();
                    activity.productoAdapter.filter_by_clasif(miC.categoriaid);
                });
        }

    }
}
