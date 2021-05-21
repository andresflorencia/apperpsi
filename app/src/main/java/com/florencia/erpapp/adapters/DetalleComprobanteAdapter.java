package com.florencia.erpapp.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.activities.ComprobanteActivity;
import com.florencia.erpapp.fragments.InfoItemDialogFragment;
import com.florencia.erpapp.models.Comprobante;
import com.florencia.erpapp.models.DetalleComprobante;
import com.florencia.erpapp.models.Producto;
import com.florencia.erpapp.models.Regla;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Utils;
import com.shasin.notificationbanner.Banner;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DetalleComprobanteAdapter  extends RecyclerView.Adapter<DetalleComprobanteAdapter.ProductoViewHolder>{

    private static String TAG = "TAGDETALLECOMP_ADAPTER";
    public List<DetalleComprobante> detalleComprobante;
    ComprobanteActivity activity;
    public String categoria;
    public boolean isCredito = false;
    public boolean visualizacion = false;
    public boolean isFactura = true;
    View rootView;

    public DetalleComprobanteAdapter(ComprobanteActivity activity, List<DetalleComprobante> detalleComprobante, String categoria, boolean visualizacion) {
        this.detalleComprobante = detalleComprobante;
        this.activity = activity;
        this.categoria = (categoria.equals("") || categoria.equals("A"))?"0":categoria;
        this.visualizacion = visualizacion;
        this.rootView = activity.findViewById(android.R.id.content);
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ProductoViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_detalle_comprobante, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        holder.bindProducto(detalleComprobante.get(position));
    }
    @Override
    public int getItemCount() {
        return detalleComprobante.size();
    }

    public void CalcularTotal(){
        try {
            Double total = 0d;
            Double subtotal = 0d;
            Double subtotaliva = 0d;
            for (DetalleComprobante miDetalle:this.detalleComprobante) {
                total += miDetalle.Subtotaliva();
                if(miDetalle.producto.porcentajeiva>0)
                    subtotaliva += miDetalle.Subtotal();
                else
                    subtotal+=miDetalle.Subtotal();
            }
            this.activity.lblTotal.setText("Total: " + Utils.FormatoMoneda(total,2));
            this.activity.setSubtotales(total, subtotal, subtotaliva);
        }catch (Exception e){
            Log.d(TAG,e.getMessage());
        }
    }

    public void CambiarPrecio(String categoria, boolean credito){
        try{
            if(categoria.equals("") || categoria.equalsIgnoreCase("A"))
                categoria = "0";
            for(DetalleComprobante miDetalle:this.detalleComprobante){
                Double cant_ut = 0d;
                for (DetalleComprobante dc:this.detalleComprobante) {
                    if(miDetalle.producto.idproducto.equals(dc.producto.idproducto))
                        cant_ut += dc.cantidad;
                }
                miDetalle.getPrecio(miDetalle.producto.reglas, miDetalle.producto.precioscategoria,
                        categoria, cant_ut, miDetalle.precio, credito);
                /*miDetalle.precio = miDetalle.producto.getPrecio(categoria);
                Double ptemp = miDetalle.precio;//GUARDAMOS EL PRECIO DE LA CATEGORIA
                if(miDetalle.producto.reglas.size()>0){
                    for (Regla r:miDetalle.producto.reglas) {
                        if(miDetalle.cantidad>=r.cantidad){
                            miDetalle.precio = r.precio;
                            break;
                        }
                    }
                }
                //SI PRECIO DE CATEGORIA ES MENOR AL PRECIO DE ALGUNA REGLAPRECIO, CONSERVAMOS EL DE LA CATEGORIA
                if(ptemp<miDetalle.precio)
                    miDetalle.precio = ptemp;*/
            }
            notifyDataSetChanged();
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
        }
    }

    class ProductoViewHolder extends RecyclerView.ViewHolder{

        TextView tvNombreProducto, tvPrecio, tvCantidad, tvSubtotal;
        ImageButton btnDelete, btnInfo;

        ProductoViewHolder(@NonNull View itemView){
            super(itemView);
            tvNombreProducto = itemView.findViewById(R.id.tv_NombreProducto);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
            btnDelete = itemView.findViewById(R.id.btnDeleteProducto);
            btnInfo = itemView.findViewById(R.id.btnInfo);
            btnDelete.setVisibility(View.VISIBLE);
            btnInfo.setVisibility(View.VISIBLE);
        }

        void bindProducto(final DetalleComprobante detalle){

            try {
                tvNombreProducto.setText((detalle.producto.porcentajeiva>0?"** ":"")+ detalle.producto.nombreproducto);
                tvPrecio.setText(Utils.FormatoMoneda(detalle.precio,2));
                tvCantidad.setText(detalle.cantidad.toString());
                tvCantidad.setInputType(InputType.TYPE_CLASS_PHONE);
                tvSubtotal.setText(Utils.FormatoMoneda(detalle.Subtotal(),2));
                tvCantidad.setSelectAllOnFocus(true);
                tvCantidad.setEnabled(!visualizacion);
                if(visualizacion) {
                    btnDelete.setVisibility(View.GONE);
                    btnInfo.setVisibility(View.GONE);
                }else {
                    btnDelete.setVisibility(View.VISIBLE);
                    btnInfo.setVisibility(View.VISIBLE);
                }

                CalcularTotal();

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
                        view.findViewById(R.id.btnYes).setOnClickListener((vi)-> {
                                detalleComprobante.remove(getAdapterPosition());
                                notifyDataSetChanged();
                                CalcularTotal();
                                Banner.make(rootView,activity,Banner.INFO,"Ítem eliminado de la lista.", Banner.BOTTOM,2000).show();
                                if(SQLite.usuario.establecimientos.size() > 1 && detalleComprobante.size()==0)
                                    activity.btnCambiaEstablecimiento.setVisibility(View.VISIBLE);
                                alertDialog.dismiss();
                            });

                        view.findViewById(R.id.btnCancel).setOnClickListener(vi -> alertDialog.dismiss());

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
                                tvSubtotal.setText(Utils.FormatoMoneda(0d, 2));
                                detalleComprobante.get(getAdapterPosition()).cantidad = 0d;
                            } else {
                                DetalleComprobante det = detalleComprobante.get(getAdapterPosition());
                                Double cant = Double.parseDouble(s.toString().trim());
                                if (cant > det.producto.stock
                                        && !visualizacion && isFactura && det.producto.tipo.equalsIgnoreCase("P")) {
                                    Banner.make(rootView,activity,Banner.INFO, "«" + det.producto.nombreproducto + "» El stock disponible es: " + det.producto.stock, Banner.BOTTOM,4500).show();
                                    detalleComprobante.get(getAdapterPosition()).cantidad = det.producto.stock;
                                    tvCantidad.setText(detalleComprobante.get(getAdapterPosition()).cantidad.toString());
                                    tvCantidad.clearFocus();
                                    tvCantidad.requestFocus();
                                } else {
                                    detalleComprobante.get(getAdapterPosition()).cantidad = cant;
                                    if(!visualizacion) {
                                        Double cant_t = 0d;
                                        for(DetalleComprobante deta:detalleComprobante){
                                            if(det.producto.idproducto.equals(deta.producto.idproducto))
                                                cant_t += deta.cantidad;
                                        }
                                        Double precio = detalleComprobante.get(getAdapterPosition()).getPrecio(det.producto.reglas, det.producto.precioscategoria, categoria, cant_t,det.precio, isCredito);
                                        tvPrecio.setText(Utils.FormatoMoneda(precio,2));
                                        for(DetalleComprobante deta:detalleComprobante){
                                            if(det.producto.idproducto.equals(deta.producto.idproducto))
                                                deta.precio = precio;
                                        }
                                    }
                                    //notifyDataSetChanged();
                                    //CambiarPrecio(categoria);
                                }
                                tvSubtotal.setText(Utils.FormatoMoneda(detalleComprobante.get(getAdapterPosition()).Subtotal(), 2));
                            }
                            CalcularTotal();
                        }catch (Exception e){
                            Banner.make(rootView,activity,Banner.ERROR,"Ingrese un valor válido.", Banner.BOTTOM,2000).show();
                        }finally {
                            //notifyDataSetChanged();
                        }
                    }
                });

                btnInfo.setOnClickListener(
                    v -> {
                        DialogFragment dialogFragment = new InfoItemDialogFragment();
                        Bundle bundle = new Bundle();
                        bundle.putInt("idproducto", detalleComprobante.get(getAdapterPosition()).producto.idproducto);
                        dialogFragment.setArguments(bundle);
                        dialogFragment.show(activity.getSupportFragmentManager(), "dialog");
                    }
                );

            }catch (Exception e){
                Log.d(TAG,e.getMessage());
            }

        }
    }
}
