package com.florencia.erpapp.adapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.activities.PedidoActivity;
import com.florencia.erpapp.fragments.InfoItemDialogFragment;
import com.florencia.erpapp.models.DetallePedido;
import com.florencia.erpapp.utils.Utils;
import com.shasin.notificationbanner.Banner;

import java.util.List;

public class DetallePedidoAdapter extends RecyclerView.Adapter<DetallePedidoAdapter.ProductoViewHolder>{

    private static String TAG = "TAGDETALLEPEDIDO_ADAPTER";
    public List<DetallePedido> detallePedido;
    PedidoActivity activity;
    public String categoria;
    public boolean visualizacion = false;
    View rootView;

    public DetallePedidoAdapter(PedidoActivity activity, List<DetallePedido> detallePedido, String categoria, boolean visualizacion) {
        this.detallePedido = detallePedido;
        this.activity = activity;
        this.categoria = categoria.equals("")?"0":categoria;
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
        holder.bindProducto(detallePedido.get(position));
    }
    @Override
    public int getItemCount() {
        return detallePedido.size();
    }

    public void CalcularTotal(){
        try {
            Double total = 0d;
            Double subtotal = 0d;
            Double subtotaliva = 0d;
            for (DetallePedido miDetalle:this.detallePedido) {
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

    public void CambiarPrecio(String categoria){
        try{
            if(categoria.equals("") || categoria.equalsIgnoreCase("A"))
                categoria = "0";
            for(DetallePedido miDetalle:this.detallePedido){
                //miDetalle.precio = miDetalle.producto.getPrecio(categoria);
                miDetalle.getPrecio(miDetalle.producto.reglas, miDetalle.producto.precioscategoria,
                        categoria, miDetalle.cantidad, miDetalle.precio);
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

        void bindProducto(final DetallePedido detalle){

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
                        ((TextView)view.findViewById(R.id.lblTitle)).setText(detallePedido.get(getAdapterPosition()).producto.nombreproducto);
                        ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea eliminar este ítem?");
                        ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_delete2);
                        ((Button)view.findViewById(R.id.btnCancel)).setText(activity.getResources().getString(R.string.Cancel));
                        ((Button)view.findViewById(R.id.btnYes)).setText(activity.getResources().getString(R.string.Confirm));
                        final AlertDialog alertDialog = builder.create();
                        view.findViewById(R.id.btnYes).setOnClickListener(vi -> {
                                detallePedido.remove(getAdapterPosition());
                                notifyDataSetChanged();
                                CalcularTotal();
                                Banner.make(rootView,activity,Banner.INFO,"Ítem eliminado de la lista.",Banner.BOTTOM,2000).show();
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
                        try {
                            if (s.length() == 0 || s.equals("0")) {
                                tvSubtotal.setText(Utils.FormatoMoneda(0d, 2));
                                detallePedido.get(getAdapterPosition()).cantidad = 0d;
                            } else {
                                DetallePedido det = detallePedido.get(getAdapterPosition());
                                Double cant = Double.parseDouble(s.toString().trim());
                                if (cant <= 0 && !visualizacion) {
                                    //Utils.showMessage(activity, "Especifique una cantidad mayor a 0");
                                    Banner.make(rootView,activity,Banner.ERROR,"Especifique una cantidad mayor a 0", Banner.BOTTOM,2000).show();
                                    detallePedido.get(getAdapterPosition()).cantidad = 0d;
                                    //tvCantidad.setText(detallePedido.get(getAdapterPosition()).cantidad.toString());
                                    tvCantidad.clearFocus();
                                    tvCantidad.requestFocus();
                                } else {
                                    detallePedido.get(getAdapterPosition()).cantidad = cant;
                                    if(!visualizacion) {
                                        Double precio = detallePedido.get(getAdapterPosition()).getPrecio(det.producto.reglas, det.producto.precioscategoria, categoria, det.cantidad, det.precio);
                                        tvPrecio.setText(Utils.FormatoMoneda(precio, 2));
                                    }
                                }
                                tvSubtotal.setText(Utils.FormatoMoneda(detallePedido.get(getAdapterPosition()).Subtotal(), 2));
                            }
                            CalcularTotal();
                        }catch (Exception e) {
                            Banner.make(rootView,activity,Banner.ERROR,"Ingrese un valor válido.",Banner.BOTTOM,2000).show();
                        }
                    }
                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });

                btnInfo.setOnClickListener(v -> {
                        DialogFragment dialogFragment = new InfoItemDialogFragment();
                        Bundle bundle = new Bundle();
                        bundle.putInt("idproducto", detallePedido.get(getAdapterPosition()).producto.idproducto);
                        dialogFragment.setArguments(bundle);
                        dialogFragment.show(activity.getSupportFragmentManager(), "dialog");
                    });

            }catch (Exception e){
                Log.d(TAG, e.getMessage());
            }

        }
    }
}
