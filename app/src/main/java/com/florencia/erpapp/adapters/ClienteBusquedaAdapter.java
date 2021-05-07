package com.florencia.erpapp.adapters;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.activities.ClienteActivity;
import com.florencia.erpapp.activities.ComprobanteActivity;
import com.florencia.erpapp.models.Cliente;
import com.florencia.erpapp.utils.Utils;
import com.makeramen.roundedimageview.RoundedImageView;
import com.shasin.notificationbanner.Banner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClienteBusquedaAdapter extends RecyclerView.Adapter<ClienteBusquedaAdapter.ClienteViewHolder> {

    private static String TAG = "TAGCLIENTE_BUSQ_ADAPTER";
    public List<Cliente> listClients;
    private List<Cliente> orginalItems = new ArrayList<>();
    Activity activity;
    public String tipobusqueda;
    View rootView;

    public ClienteBusquedaAdapter(Activity activity, List<Cliente> listClients, String tipobusqueda) {
        this.activity =activity;
        this.listClients = listClients;
        this.orginalItems.addAll(this.listClients);
        this.tipobusqueda= tipobusqueda;
        rootView = activity.findViewById(android.R.id.content);
    }

    @NonNull
    @Override
    public ClienteBusquedaAdapter.ClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ClienteBusquedaAdapter.ClienteViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_cliente, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ClienteBusquedaAdapter.ClienteViewHolder holder, int position) {
        holder.bindCliente(listClients.get(position));
    }

    @Override
    public int getItemCount() {
        return listClients.size();
    }

    public void filter(final String busqueda){
        listClients.clear();
        if(busqueda.length()==0){
            listClients.addAll(orginalItems);
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                List<Cliente> collect = orginalItems.stream()
                        .filter(i -> i.nip.concat(i.razonsocial.toLowerCase())
                                .concat(i.nombrecomercial.toLowerCase())
                                .concat(i.direccion.toLowerCase()
                                .concat(i.nombrecategoria.toLowerCase())
                                ).contains(busqueda.toLowerCase()))
                        .collect(Collectors.<Cliente>toList());
                listClients.addAll(collect);
            }else{
                for(Cliente i: orginalItems){
                    if(i.nip.concat(i.razonsocial.toLowerCase())
                            .concat(i.nombrecomercial.toLowerCase())
                            .concat(i.direccion.toLowerCase()
                            .concat(i.nombrecategoria.toLowerCase())
                            ).contains(busqueda.toLowerCase()))
                        listClients.add(i);
                }
            }
        }
        notifyDataSetChanged();
    }

    class ClienteViewHolder extends RecyclerView.ViewHolder{

        TextView tvNombreComercial, tvRazonSocial, tvDireccion, tvContacto;

        ClienteViewHolder(@NonNull View itemView){
            super(itemView);
            tvRazonSocial = itemView.findViewById(R.id.tv_RazonSocial);
            tvNombreComercial = itemView.findViewById(R.id.tv_NombreComercial);
            tvDireccion = itemView.findViewById(R.id.tv_Direccion);
            tvContacto = itemView.findViewById(R.id.tv_Contacto);
            itemView.findViewById(R.id.btnNewDocument).setVisibility(View.GONE);
            itemView.findViewById(R.id.tv_Estado).setVisibility(View.GONE);
        }

        void bindCliente(final Cliente cliente){
            tvRazonSocial.setText(cliente.nip + " - "+ cliente.razonsocial);
            tvNombreComercial.setText(cliente.nombrecomercial);
            tvDireccion.setText(cliente.direccion);
            tvContacto.setText(cliente.fono1 + " - " + cliente.fono2 +" - Categoría: " + cliente.nombrecategoria);

            itemView.setOnClickListener(v -> {
                    if(tipobusqueda.equals("PC") &&
                            (listClients.get(getAdapterPosition()).idcliente==0 || listClients.get(getAdapterPosition()).nip.contains("99999999"))){
                        Banner.make(rootView, activity, Banner.INFO,"No se puede registrar pedidos para CONSUMIDOR FINAL", Banner.BOTTOM,3000).show();
                        return;
                    }
                    activity.setResult(Activity.RESULT_OK,new Intent().putExtra("idcliente",listClients.get(getAdapterPosition()).idcliente));
                    activity.onBackPressed();
                    activity.overridePendingTransition(R.anim.right_in, R.anim.right_out);
                });

        }
    }
}
