package com.florencia.erpapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.Layout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.activities.ClienteActivity;
import com.florencia.erpapp.activities.ComprobanteActivity;
import com.florencia.erpapp.activities.PedidoActivity;
import com.florencia.erpapp.models.Cliente;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClienteAdapter extends RecyclerView.Adapter<ClienteAdapter.ClienteViewHolder> {

    private static String TAG = "TAGCLIENTE_ADAPTER";
    public List<Cliente> listClients;
    private List<Cliente> orginalItems = new ArrayList<>();
    Context context;

    public ClienteAdapter(Context context, List<Cliente> listClients) {
        this.context = context;
        this.listClients = listClients;
        this.orginalItems.addAll(this.listClients);
    }

    @NonNull
    @Override
    public ClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ClienteViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_cliente, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ClienteViewHolder holder, int position) {
        holder.bindCliente(listClients.get(position));
    }

    @Override
    public int getItemCount() {
        return listClients.size();
    }

    public void filter(final String busqueda) {
        listClients.clear();
        if (busqueda.length() == 0) {
            listClients.addAll(orginalItems);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                List<Cliente> collect = orginalItems.stream()
                        .filter(i -> i.nip.concat(i.razonsocial.toLowerCase())
                                .concat(i.nombrecomercial.toLowerCase())
                                .concat(i.direccion.toLowerCase()
                                        .concat(i.nombrecategoria.toLowerCase())
                                ).contains(busqueda.toLowerCase()))
                        .collect(Collectors.<Cliente>toList());
                listClients.addAll(collect);
            } else {
                for (Cliente i : orginalItems) {
                    if (i.nip.concat(i.razonsocial.toLowerCase())
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

    /*@Override
    public void onViewAttachedToWindow(ClienteViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        Utils.animateCircularReveal(holder.itemView);
    }*/

    class ClienteViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {

        TextView tvNombreComercial, tvRazonSocial, tvDireccion, tvContacto, tvEstado;
        ImageButton btnEdit, btnOptions;
        RoundedImageView imgCliente;
        CardView cvCliente;

        ClienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRazonSocial = itemView.findViewById(R.id.tv_RazonSocial);
            tvNombreComercial = itemView.findViewById(R.id.tv_NombreComercial);
            tvDireccion = itemView.findViewById(R.id.tv_Direccion);
            tvContacto = itemView.findViewById(R.id.tv_Contacto);
            imgCliente = itemView.findViewById(R.id.imgCliente);
            //btnEdit = itemView.findViewById(R.id.btnEdit);
            btnOptions = itemView.findViewById(R.id.btnNewDocument);
            cvCliente = itemView.findViewById(R.id.cvCliente);
            tvEstado = itemView.findViewById(R.id.tv_Estado);
            //cvCliente.setOnCreateContextMenuListener(this);
        }

        void bindCliente(final Cliente cliente) {
            tvRazonSocial.setText(cliente.nip + " - " + cliente.razonsocial);
            tvNombreComercial.setText(cliente.nombrecomercial);
            tvDireccion.setText(cliente.direccion);
            tvContacto.setText(cliente.fono1 + " - " + cliente.fono2 + " - Categoría: " + cliente.nombrecategoria);
            //btnEdit.setTag(cliente.idcliente);
            //btnNewDocument.setTag(cliente.idcliente);
            if ((cliente.codigosistema == 0 || cliente.actualizado == 1) && !cliente.nip.contains("999999999")) {
                tvEstado.setText("No sincronizado");
                tvEstado.setTextColor(itemView.getContext().getResources().getColor(R.color.colorend_splash));
                tvEstado.setVisibility(View.VISIBLE);
                itemView.setBackgroundResource(R.drawable.bg_btn_red);
            } else {
                tvEstado.setVisibility(View.GONE);
                itemView.setBackgroundResource(R.drawable.bg_btn_gps);
            }

            btnOptions.setOnClickListener(v -> showPopupMenu(v));

            itemView.setOnClickListener(v -> {
                if (!listClients.get(getAdapterPosition()).nip.contains("99999999")) {
                    Intent i = new Intent(context, ClienteActivity.class);
                    i.putExtra("idcliente", listClients.get(getAdapterPosition()).idcliente);
                    context.startActivity(i);
                } else
                    Utils.showMessage(v.getContext(), "No se puede editar datos de " + Constants.COMILLA_ABRE + "CONSUMIDOR FINAL" + Constants.COMILLA_CIERRA);
            });
        }

        private void showPopupMenu(View v) {
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            menu.inflate(R.menu.popup_menu_cliente);
            if ((listClients.get(this.getAdapterPosition()).codigosistema.equals(0)
                    || SQLite.usuario.VerificaPermiso(v.getContext(), Constants.REGISTRO_CLIENTE, "lectura")
                    || SQLite.usuario.VerificaPermiso(v.getContext(), Constants.REGISTRO_CLIENTE, "modificacion"))
                    && !listClients.get(this.getAdapterPosition()).nip.contains("999999999")) {
                menu.getMenu().findItem(R.id.action_editar).setVisible(true);
            } else
                menu.getMenu().findItem(R.id.action_editar).setVisible(false);

            menu.getMenu().findItem(R.id.action_comprobante).setVisible(SQLite.usuario.VerificaPermiso(v.getContext(), Constants.PUNTO_VENTA, "escritura"));
            menu.getMenu().findItem(R.id.action_pedido).setVisible(SQLite.usuario.VerificaPermiso(v.getContext(), Constants.PEDIDO, "escritura"));
            menu.setOnMenuItemClickListener(this);
            menu.show();
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Intent i;
            switch (item.getItemId()) {
                case R.id.action_editar: //MODIFICACION DE DATOS
                    i = new Intent(context, ClienteActivity.class);
                    i.putExtra("idcliente", listClients.get(getAdapterPosition()).idcliente);
                    context.startActivity(i);
                    return true;
                case R.id.action_comprobante: //NUEVO COMPROBANTE
                    i = new Intent(context, ComprobanteActivity.class);
                    i.putExtra("idcliente", listClients.get(getAdapterPosition()).idcliente);
                    context.startActivity(i);
                    return true;
                case R.id.action_pedido: //NUEVO PEDIDO
                    i = new Intent(context, PedidoActivity.class);
                    i.putExtra("idcliente", listClients.get(getAdapterPosition()).idcliente);
                    context.startActivity(i);
                    return true;
                default:
                    return false;
            }
        }
    }
}
