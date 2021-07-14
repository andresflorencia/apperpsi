package com.florencia.erpapp.adapters;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.florencia.erpapp.MainActivity;
import com.florencia.erpapp.R;
import com.florencia.erpapp.activities.ClienteActivity;
import com.florencia.erpapp.activities.ComprobanteActivity;
import com.florencia.erpapp.activities.DepositoActivity;
import com.florencia.erpapp.activities.ListaComprobantesActivity;
import com.florencia.erpapp.activities.PedidoActivity;
import com.florencia.erpapp.activities.PedidoInventarioActivity;
import com.florencia.erpapp.activities.RecepcionActivity;
import com.florencia.erpapp.activities.TransferenciaActivity;
import com.florencia.erpapp.fragments.ClienteFragment;
import com.florencia.erpapp.utils.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.shasin.notificationbanner.Banner;


public class ResumenAdapter extends RecyclerView.Adapter<ResumenAdapter.ResumenViewHolder> {

    public static final String TAG = "TAG_RESUMENADAPTER";

    public JsonArray jDatos;
    private Activity activity;
    private String fecha;
    View rootView;

    public ResumenAdapter(Activity activity, JsonArray jDatos, String fecha) {
        this.jDatos = jDatos;
        this.activity = activity;
        this.fecha = fecha;
        rootView = activity.findViewById(android.R.id.content);
    }

    @NonNull
    @Override
    public ResumenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ResumenViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_resumen, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ResumenViewHolder holder, int position) {
        holder.bindResumen(jDatos.get(position).getAsJsonObject());
    }

    @Override
    public int getItemCount() {
        return jDatos.size();
    }

    class ResumenViewHolder extends RecyclerView.ViewHolder {

        TextView tvDocumento, tvCantidad, tvTotal, tvCantidadNS;
        Button btnNewDocument;

        ResumenViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDocumento = itemView.findViewById(R.id.tvDocumento);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            btnNewDocument = itemView.findViewById(R.id.btnNewDocument);
            tvCantidadNS = itemView.findViewById(R.id.tvCantidadNS);
        }

        void bindResumen(final JsonObject jDocumento) {
            tvDocumento.setText(jDocumento.get("documento").getAsString());
            if (jDocumento.get("documento").getAsString().equalsIgnoreCase("CLIENTES"))
                tvCantidad.setText(jDocumento.get("cantidad").getAsInt() + "/" + jDocumento.get("total").getAsInt());
            else
                tvCantidad.setText(jDocumento.get("cantidad").getAsString());
            if (jDocumento.get("documento").getAsString().equalsIgnoreCase("CLIENTES"))
                tvTotal.setText("Nuevos/Totales");
            else
                tvTotal.setText(jDocumento.get("total").getAsDouble() == 0 ? "" : "Total: " + Utils.FormatoMoneda(jDocumento.get("total").getAsDouble(), 2));

            tvCantidadNS.setText(jDocumento.get("cantidadns").getAsString());
            if (jDocumento.get("cantidadns").getAsInt() > 0)
                tvCantidadNS.setVisibility(View.VISIBLE);
            else
                tvCantidadNS.setVisibility(View.GONE);

            btnNewDocument.setOnClickListener(
                    v -> {
                        JsonObject miDoc = jDatos.get(getAdapterPosition()).getAsJsonObject();
                        if (miDoc != null) {
                            Intent i = null;
                            switch (miDoc.get("documento").getAsString()) {
                                case "FACTURAS":
                                    i = new Intent(activity, ComprobanteActivity.class);
                                    break;
                                case "PEDIDOS CLIENTE":
                                    i = new Intent(activity, PedidoActivity.class);
                                    break;
                                case "RECEPCIONES":
                                    i = new Intent(activity, RecepcionActivity.class);
                                    break;
                                case "TRANSFERENCIAS":
                                    i = new Intent(activity, TransferenciaActivity.class);
                                    break;
                                case "PEDIDOS INVENTARIO":
                                    i = new Intent(activity, PedidoInventarioActivity.class);
                                    break;
                                case "CLIENTES":
                                    i = new Intent(activity, ClienteActivity.class);
                                    break;
                                case "DEPOSITOS":
                                    i = new Intent(activity, DepositoActivity.class);
                                    break;
                            }
                            if (i != null) {
                                activity.startActivity(i);
                                activity.overridePendingTransition(R.anim.zoom_back_in, R.anim.zoom_back_out);
                            }
                        }
                    }
            );

            itemView.setOnClickListener(
                    v -> {
                        JsonObject miDoc = jDatos.get(getAdapterPosition()).getAsJsonObject();
                        if (miDoc != null) {
                            if (!miDoc.get("documento").getAsString().equalsIgnoreCase("CLIENTES")
                                    && miDoc.get("cantidad").getAsInt() == 0) {
                                String[] fecA = fecha.split("-");
                                Banner.make(rootView, activity, Banner.INFO,
                                        "No existen " + miDoc.get("documento").getAsString() + " para la fecha: "
                                                + fecA[2] + "-" + Utils.getMes(Integer.valueOf(fecA[1]) - 1, true) + "-" + fecA[0]
                                        , Banner.BOTTOM, 2000).show();
                                return;
                            }

                            Intent i = new Intent(activity, ListaComprobantesActivity.class);
                            i.putExtra("fechadesde", fecha);
                            i.putExtra("fechahasta", fecha);
                            i.putExtra("retornar", false);
                            switch (miDoc.get("documento").getAsString()) {
                                case "FACTURAS":
                                    i.putExtra("tipobusqueda", "01");
                                    break;
                                case "PEDIDOS CLIENTE":
                                    i.putExtra("tipobusqueda", "PC");
                                    break;
                                case "RECEPCIONES":
                                    i.putExtra("tipobusqueda", "8,23");
                                    break;
                                case "TRANSFERENCIAS":
                                    i.putExtra("tipobusqueda", "4,20");
                                    break;
                                case "PEDIDOS INVENTARIO":
                                    i.putExtra("tipobusqueda", "PI");
                                    break;
                                case "DEPOSITOS":
                                    i.putExtra("tipobusqueda", "DE");
                                    break;
                            }
                            if (miDoc.get("documento").getAsString().equalsIgnoreCase("CLIENTES")) {
                                ((MainActivity) activity).fragment = new ClienteFragment(miDoc.get("cantidad").getAsInt() == 0 ? "" : fecha);
                                //Log.d("TAG", ((MainActivity)activity).fragment.getClass().getName());

                                ((MainActivity) activity).agregaFragment(((MainActivity) activity).fragment.getClass().getName());
                            } else {
                                activity.startActivity(i);
                                activity.overridePendingTransition(R.anim.left_in, R.anim.left_out);
                            }
                        }

                    });
        }
    }
}
