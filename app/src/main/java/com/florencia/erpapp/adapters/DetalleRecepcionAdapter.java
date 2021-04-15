package com.florencia.erpapp.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.activities.RecepcionActivity;
import com.florencia.erpapp.models.DetalleComprobante;
import com.florencia.erpapp.utils.Utils;
import com.shasin.notificationbanner.Banner;

import java.util.List;

public class DetalleRecepcionAdapter extends RecyclerView.Adapter<DetalleRecepcionAdapter.ProductoViewHolder>{

    private static String TAG = "TAGDETALLERECEP_ADAPTER";
    public List<DetalleComprobante> detalleComprobante;
    Activity activity;
    String categoria, tipotransaccion;
    public boolean visualizacion = false;
    View rootView;

    public DetalleRecepcionAdapter(Activity activity, List<DetalleComprobante> detalleComprobante, String categoria, boolean visualizacion, String tipotransaccion) {
        this.detalleComprobante = detalleComprobante;
        this.activity = activity;
        this.categoria = categoria.equals("")?"0":categoria;
        this.visualizacion = visualizacion;
        this.tipotransaccion = tipotransaccion;
        this.rootView = activity.findViewById(android.R.id.content);
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ProductoViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_recepcion_inv, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        holder.bindProducto(detalleComprobante.get(position));
    }
    @Override
    public int getItemCount() {
        return detalleComprobante.size();
    }


    class ProductoViewHolder extends RecyclerView.ViewHolder{

        TextView tvNombreProducto, tvLote, tvCantidad, tvFechaVencimiento;
        ImageButton btnDelete;

        ProductoViewHolder(@NonNull View itemView){
            super(itemView);
            tvNombreProducto = itemView.findViewById(R.id.tv_NombreProducto);
            tvLote = itemView.findViewById(R.id.tvLote);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            tvFechaVencimiento = itemView.findViewById(R.id.tvFechaVencimiento);
            btnDelete = itemView.findViewById(R.id.btnDeleteProducto);
        }

        void bindProducto(final DetalleComprobante detalle){

            try {
                tvNombreProducto.setText(detalle.producto.codigoproducto + " - " + detalle.producto.nombreproducto);
                tvLote.setText(detalle.numerolote);
                tvFechaVencimiento.setText(detalle.fechavencimiento);
                tvCantidad.setText(Utils.RoundDecimal(detalle.cantidad,2).toString());
                tvCantidad.setInputType(InputType.TYPE_CLASS_PHONE);
                tvCantidad.setSelectAllOnFocus(true);
                if(tipotransaccion.equals("4,20") || tipotransaccion.equals("20,4")) {
                    tvCantidad.setEnabled(!visualizacion);
                    btnDelete.setVisibility(visualizacion?View.GONE:View.VISIBLE);
                }else{
                    tvCantidad.setEnabled(false);
                    btnDelete.setVisibility(View.GONE);
                }

                btnDelete.setOnClickListener(v -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);
                        View view = LayoutInflater.from(activity).inflate(R.layout.layout_warning_dialog,
                                (ConstraintLayout) activity.findViewById(R.id.lyDialogContainer));
                        builder.setView(view);
                        ((TextView)view.findViewById(R.id.lblTitle)).setText(detalleComprobante.get(getAdapterPosition()).producto.nombreproducto);
                        ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea eliminar este ítem?");
                        ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_delete2);
                        ((Button)view.findViewById(R.id.btnCancel)).setText(activity.getResources().getString(R.string.Cancel));
                        ((Button)view.findViewById(R.id.btnYes)).setText(activity.getResources().getString(R.string.Confirm));
                        final AlertDialog alertDialog = builder.create();
                        view.findViewById(R.id.btnYes).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                detalleComprobante.remove(getAdapterPosition());
                                notifyDataSetChanged();
                                Banner.make(rootView,activity,Banner.INFO,"Ítem eliminado de la lista.", Banner.BOTTOM,2000).show();
                                alertDialog.dismiss();
                            }
                        });

                        view.findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) { alertDialog.dismiss();}
                        });

                        if(alertDialog.getWindow()!=null)
                            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                        alertDialog.show();
                    });

                tvCantidad.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }
                    @Override
                    public void afterTextChanged(Editable s) {
                        try {
                            if (s.length() == 0) {
                                //detalleComprobante.get(getAdapterPosition()).cantidad = 0d;
                            } else {
                                Double cant = Double.parseDouble(s.toString().trim());
                                if ((tipotransaccion.equals("4,20") || tipotransaccion.equals("20,4")) && !visualizacion) {
                                    if (cant <= 0 || cant > detalleComprobante.get(getAdapterPosition()).producto.lotes.get(0).stock) {
                                        Banner.make(rootView,activity,Banner.ERROR,
                                                "La cantidad de transferencia debe estar entre 0 y " + detalleComprobante.get(getAdapterPosition()).producto.lotes.get(0).stock,
                                                Banner.BOTTOM,2500).show();
                                        tvCantidad.setText(Utils.RoundDecimal(detalleComprobante.get(getAdapterPosition()).producto.lotes.get(0).stock,2).toString());
                                        tvCantidad.clearFocus();
                                        tvCantidad.requestFocus();
                                    } else {
                                        detalleComprobante.get(getAdapterPosition()).cantidad = cant;
                                    }
                                }
                            }
                        }catch (Exception e){
                            Banner.make(rootView,activity,Banner.ERROR,"Ingrese un valor válido.", Banner.BOTTOM,2000).show();
                        }
                    }
                });

            }catch (Exception e){
                Log.d(TAG,e.getMessage());
            }

        }
    }
}
