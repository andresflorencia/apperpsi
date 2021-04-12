package com.florencia.erpapp.adapters;

import android.app.Activity;
import android.app.AlertDialog;
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
import com.florencia.erpapp.models.DetallePedidoInv;
import com.florencia.erpapp.utils.Utils;
import com.google.android.material.textfield.TextInputLayout;
import com.shasin.notificationbanner.Banner;

import java.util.List;

public class DetallePedidoInvAdapter extends RecyclerView.Adapter<DetallePedidoInvAdapter.ProductoViewHolder>{
    public List<DetallePedidoInv> detallePedido;
    Activity activity;
    String tipotransaccion;
    public boolean visualizacion = false;
    View rootView;

    public DetallePedidoInvAdapter(Activity activity, List<DetallePedidoInv> detallePedido, boolean visualizacion, String tipotransaccion) {
        this.detallePedido = detallePedido;
        this.activity = activity;
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
        holder.bindProducto(detallePedido.get(position));
    }
    @Override
    public int getItemCount() {
        return detallePedido.size();
    }

    class ProductoViewHolder extends RecyclerView.ViewHolder{
        TextView tvNombreProducto, tvStock, tvCantidad, tvFechaVencimiento;
        TextInputLayout tilFechaVenc, tilLote, tilCantidad;
        ImageButton btnDelete;

        ProductoViewHolder(@NonNull View itemView){
            super(itemView);
            tvNombreProducto = itemView.findViewById(R.id.tv_NombreProducto);
            tvStock = itemView.findViewById(R.id.tvLote);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            tvFechaVencimiento = itemView.findViewById(R.id.tvFechaVencimiento);
            tilFechaVenc = itemView.findViewById(R.id.tilFechaVenc);
            tilLote = itemView.findViewById(R.id.tilLote);
            tilCantidad = itemView.findViewById(R.id.tilCantidad);
            btnDelete = itemView.findViewById(R.id.btnDeleteProducto);
        }

        void bindProducto(final DetallePedidoInv detalle){
            try {
                tvNombreProducto.setText(detalle.producto.codigoproducto + " - " + detalle.producto.nombreproducto);
                tvStock.setText(Utils.RoundDecimal(detalle.stockactual, 2).toString());
                tilLote.setHint("Stock");
                tilCantidad.setHint("Cantidad solicitada");
                tvCantidad.setText(Utils.RoundDecimal(detalle.cantidadpedida, 2).toString());
                tvCantidad.setInputType(InputType.TYPE_CLASS_PHONE);
                tvCantidad.setSelectAllOnFocus(true);
                tvCantidad.setEnabled(!visualizacion);
                tilFechaVenc.setVisibility(View.GONE);
                btnDelete.setVisibility(visualizacion ? View.GONE : View.VISIBLE);

                btnDelete.setOnClickListener(v -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);
                        View view = LayoutInflater.from(activity).inflate(R.layout.layout_warning_dialog,
                                (ConstraintLayout) activity.findViewById(R.id.lyDialogContainer));
                        builder.setView(view);
                        ((TextView)view.findViewById(R.id.lblTitle)).setText(detallePedido.get(getAdapterPosition()).producto.nombreproducto);
                        ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea eliminar este ítem?");
                        ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_delete2);
                        ((Button)view.findViewById(R.id.btnCancel)).setText(v.getContext().getResources().getString(R.string.Cancel));
                        ((Button)view.findViewById(R.id.btnYes)).setText(v.getContext().getResources().getString(R.string.Confirm));
                        final AlertDialog alertDialog = builder.create();
                        view.findViewById(R.id.btnYes).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                detallePedido.remove(getAdapterPosition());
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
                            if (s.length() == 0 || s.equals("0")) {
                                detallePedido.get(getAdapterPosition()).cantidadpedida = 0d;
                                detallePedido.get(getAdapterPosition()).cantidadautorizada = 0d;
                            } else {
                                Double cant = Double.parseDouble(s.toString().trim());
                                detallePedido.get(getAdapterPosition()).cantidadpedida = cant;
                                detallePedido.get(getAdapterPosition()).cantidadautorizada = cant;
                            }
                        }catch (Exception e){
                            Banner.make(rootView,activity,Banner.ERROR,"Ingrese un valor válido.", Banner.BOTTOM,2000).show();
                        }
                    }
                });
            }catch (Exception e){
                Log.d("TAG", e.getMessage());
            }
        }
    }


}
