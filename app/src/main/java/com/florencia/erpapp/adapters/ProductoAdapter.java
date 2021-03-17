package com.florencia.erpapp.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.activities.ComprobanteActivity;
import com.florencia.erpapp.activities.PedidoActivity;
import com.florencia.erpapp.activities.ProductoBusquedaActivity;
import com.florencia.erpapp.fragments.InfoDialogFragment;
import com.florencia.erpapp.fragments.InfoItemDialogFragment;
import com.florencia.erpapp.models.Cliente;
import com.florencia.erpapp.models.DetalleComprobante;
import com.florencia.erpapp.models.DetallePedido;
import com.florencia.erpapp.models.DetallePedidoInv;
import com.florencia.erpapp.models.Producto;
import com.florencia.erpapp.utils.Utils;
import com.shasin.notificationbanner.Banner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>{
    public List<Producto> listProductos;
    private List<Producto> orginalItems = new ArrayList<>();
    public List<DetalleComprobante> productosSelected = new ArrayList<>(); //LISTA PARA COMPROBANTE
    public List<DetallePedido> productosSelectedP = new ArrayList<>(); //LISTA PARA PEDIDO CLIENTE
    public List<DetallePedidoInv> productosSelectedPI = new ArrayList<>(); //LISTA PARA PEDIDO INVENTARIO
    androidx.appcompat.widget.Toolbar toolbar;
    String tipobusqueda;
    ProductoBusquedaActivity activity;
    View rootView;
    Integer clasificacionid = -1; //TODOS

    public ProductoAdapter(Toolbar toolbar, List<Producto> listProductos, String tipobusqueda, ProductoBusquedaActivity activity) {
        this.toolbar = toolbar;
        this.listProductos = listProductos;
        this.orginalItems.addAll(this.listProductos);
        this.tipobusqueda = tipobusqueda;
        this.activity = activity;
        this.rootView = activity.findViewById(android.R.id.content);
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ProductoViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_producto, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        holder.bindProducto(listProductos.get(position));
    }
    @Override
    public int getItemCount() {
        return listProductos.size();
    }

    public void filter(final String busqueda){
        listProductos.clear();
        if(busqueda.length()==0){
            //listProductos.addAll(orginalItems);
            filter_by_clasif(clasificacionid);
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                List<Producto> collect = orginalItems.stream()
                        .filter(i -> i.nombreproducto.toLowerCase()
                                .concat(i.codigoproducto.toLowerCase())
                                .concat(i.numerolote.toLowerCase())
                                .contains(busqueda.toLowerCase()) &&
                                (clasificacionid != -1?i.clasificacionid:1) == (clasificacionid != -1?clasificacionid:1))
                        .collect(Collectors.<Producto>toList());
                listProductos.addAll(collect);
            }else{
                for(Producto i: orginalItems){
                    if(i.nombreproducto.toLowerCase()
                            .concat(i.codigoproducto.toLowerCase())
                            .concat(i.numerolote.toLowerCase())
                            .contains(busqueda.toLowerCase()) &&
                            (clasificacionid != -1?i.clasificacionid:1) == (clasificacionid != -1?clasificacionid:1))
                        listProductos.add(i);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void filter_by_clasif(final Integer clasificacionid){
        try{
            this.clasificacionid = clasificacionid;
            listProductos.clear();
            if(clasificacionid.equals(-1)){
                listProductos.addAll(orginalItems);
            }else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    List<Producto> collect = orginalItems.stream()
                            .filter(i -> i.clasificacionid
                                    .equals(clasificacionid))
                            .collect(Collectors.<Producto>toList());
                    listProductos.addAll(collect);
                } else {
                    for (Producto i : orginalItems) {
                        if (i.clasificacionid.equals(clasificacionid))
                            listProductos.add(i);
                    }
                }
            }
            notifyDataSetChanged();
        }catch (Exception e){
            Log.d("TAGPRODUCTOADAPTER", "filter_by_clasif(): " + e.getMessage());
        }
    }

    class ProductoViewHolder extends RecyclerView.ViewHolder{

        TextView tvNombreProducto, tvPrecio, tvStock;
        ImageButton btnInfo;
        CheckBox ckIva;

        ProductoViewHolder(@NonNull View itemView){
            super(itemView);
            tvNombreProducto = itemView.findViewById(R.id.tv_NombreProducto);
            tvPrecio = itemView.findViewById(R.id.tv_Precio);
            tvStock = itemView.findViewById(R.id.tv_Stock);
            ckIva = itemView.findViewById(R.id.ckIva);
            btnInfo = itemView.findViewById(R.id.btnInfo);
        }

        void bindProducto(final Producto producto){

            try {
                tvNombreProducto.setText(producto.nombreproducto);
                ckIva.setChecked(producto.porcentajeiva > 0);

                if(tipobusqueda.equals("4,20") || tipobusqueda.equals("20,4")) {//TRANSFERENCIAS
                    ckIva.setVisibility(View.GONE);
                    btnInfo.setVisibility(View.GONE);
                    tvPrecio.setText(("P. Costo: " + Utils.FormatoMoneda(producto.lotes.get(0).preciocosto,2))
                            .concat("\n")
                            .concat("Vence: " + producto.lotes.get(0).fechavencimiento));
                    tvStock.setText(("Stock: " + Utils.RoundDecimal(producto.lotes.get(0).stock,2))
                            .concat("\n")
                            .concat("Lote: " + producto.lotes.get(0).numerolote));
                }else {
                    ckIva.setVisibility(View.VISIBLE);
                    btnInfo.setVisibility(View.VISIBLE);
                    tvPrecio.setText("PVP: " + Utils.FormatoMoneda(producto.getPrecioSugerido(),2));
                    if(producto.stock>0 && producto.tipo.equalsIgnoreCase("P")) {
                        tvStock.setText("Stock: " + Utils.RoundDecimal(producto.stock, 2));
                        tvStock.setTextColor(Color.BLACK);
                    }else if(producto.tipo.equalsIgnoreCase("S")) {
                        tvStock.setText("SERVICIO");
                        tvStock.setTextColor(Color.BLACK);
                    }else{
                        tvStock.setText("Sin stock");
                        tvStock.setTextColor(itemView.getContext().getResources().getColor(R.color.texthintenabled));
                    }
                }


                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        IngresarCantidad(v.getContext(),listProductos.get(getAdapterPosition()));
                    }
                });
                btnInfo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogFragment dialogFragment = new InfoItemDialogFragment();
                        Bundle bundle = new Bundle();
                        bundle.putInt("idproducto", listProductos.get(getAdapterPosition()).idproducto);
                        dialogFragment.setArguments(bundle);
                        dialogFragment.show(activity.getSupportFragmentManager(), "dialog");
                    }
                });
            }catch (Exception e){
                Log.d("TAGPRODUCTO",e.getMessage());
            }

        }

        void IngresarCantidad(Context context, Producto producto){
            try {

                if(tipobusqueda.equals("01")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (productosSelected.stream().filter(i -> i.producto.equals(producto)).
                                collect(Collectors.toList()).size() > 0) {
                            Banner.make(rootView,activity,Banner.WARNING, "Item ya seleccionado...", Banner.BOTTOM,2000).show();
                            return;
                        }
                    } else {
                        for (DetalleComprobante detalle : productosSelected) {
                            if (detalle.producto.equals(producto)) {
                                Banner.make(rootView,activity,Banner.WARNING, "Item ya seleccionado...", Banner.BOTTOM,2000).show();
                                return;
                            }
                        }
                    }
                }else if(tipobusqueda.equals("PC") || tipobusqueda.equals("PI")){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if(tipobusqueda.equals("PC")) {
                            if (productosSelectedP.stream().filter(i -> i.producto.equals(producto)).
                                    collect(Collectors.toList()).size() > 0) {
                                Banner.make(rootView, activity, Banner.WARNING, "Item ya seleccionado...", Banner.BOTTOM, 2000).show();
                                return;
                            }
                        }else if(tipobusqueda.equals("PI")){
                            if (productosSelectedPI.stream().filter(i -> i.producto.equals(producto)).
                                    collect(Collectors.toList()).size() > 0) {
                                Banner.make(rootView, activity, Banner.WARNING, "Item ya seleccionado...", Banner.BOTTOM, 2000).show();
                                return;
                            }
                        }
                    } else {
                        if(tipobusqueda.equals("PC")) {
                            for (DetallePedido detalle : productosSelectedP) {
                                if (detalle.producto.equals(producto)) {
                                    Banner.make(rootView, activity, Banner.WARNING, "Item ya seleccionado...", Banner.BOTTOM, 2000).show();
                                    return;
                                }
                            }
                        }else if(tipobusqueda.equals("PI")){
                            for (DetallePedidoInv detalle : productosSelectedPI) {
                                if (detalle.producto.equals(producto)) {
                                    Banner.make(rootView, activity, Banner.WARNING, "Item ya seleccionado...", Banner.BOTTOM, 2000).show();
                                    return;
                                }
                            }
                        }
                    }
                }

                if(tipobusqueda.equals("01") && producto.stock <= 0 && producto.tipo.equalsIgnoreCase("P")){
                    Banner.make(rootView,activity,Banner.WARNING, "Producto sin stock disponible.",Banner.BOTTOM,2000).show();
                    return;
                }else if((tipobusqueda.equals("4,20") || tipobusqueda.equals("20,4")) && producto.lotes.get(0).stock <= 0){
                    Banner.make(rootView,activity,Banner.WARNING, "Lote del producto sin stock disponible.", Banner.BOTTOM,2000).show();
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(producto.nombreproducto);
                builder.setMessage("Ingrese la cantidad: ");
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_PHONE);
                input.setText("1");
                input.setSelectAllOnFocus(true);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (input.getText().toString().equals("")) {
                            Banner.make(rootView,activity,Banner.ERROR, "Debe especificar una cantidad.",Banner.BOTTOM,2000).show();
                            return;
                        }else{
                            Double cantidad = Double.parseDouble(input.getText().toString().trim());
                            if(tipobusqueda.equals("01") && cantidad > producto.stock && producto.tipo.equalsIgnoreCase("P")){
                                Banner.make(rootView,activity,Banner.ERROR,"La cantidad máxima de venta es: " + producto.stock, Banner.BOTTOM,2000).show();
                                return;
                            }else if((tipobusqueda.equals("4,20") || tipobusqueda.equals("20,4")) && cantidad > producto.lotes.get(0).stock){
                                Banner.make(rootView,activity,Banner.ERROR,"La cantidad máxima de transferencia del lote «"
                                        + producto.lotes.get(0).numerolote +"» es: " + producto.lotes.get(0).stock, Banner.BOTTOM,2000).show();
                                return;
                            }
                            if(tipobusqueda.equals("01")) { //FACTURA
                                DetalleComprobante midetalle = new DetalleComprobante();
                                midetalle.producto = producto;
                                midetalle.cantidad = cantidad;
                                midetalle.precio = producto.pvp1;
                                midetalle.total = midetalle.cantidad * midetalle.precio;
                                productosSelected.add(midetalle);
                            }else if(tipobusqueda.equals("PC")){ //PEDIDO CLIENTE
                                DetallePedido midetalle = new DetallePedido();
                                midetalle.producto = producto;
                                midetalle.cantidad = cantidad;
                                midetalle.precio = producto.pvp1;
                                productosSelectedP.add(midetalle);
                            }else if(tipobusqueda.equals("4,20") || tipobusqueda.equals("20,4")){ //TRANSFERENCIA
                                DetalleComprobante midetalle = new DetalleComprobante();
                                midetalle.producto = producto;
                                midetalle.cantidad = cantidad;
                                midetalle.precio = producto.lotes.get(0).preciocosto;
                                midetalle.preciocosto = producto.lotes.get(0).preciocosto;
                                midetalle.numerolote = producto.lotes.get(0).numerolote;
                                midetalle.stock = producto.lotes.get(0).stock - cantidad;
                                midetalle.fechavencimiento = producto.lotes.get(0).fechavencimiento;
                                productosSelected.add(midetalle);
                            }else if(tipobusqueda.equals("PI")){ //PEDIDO INVENTARIO
                                DetallePedidoInv midetalle = new DetallePedidoInv();
                                midetalle.producto = producto;
                                midetalle.cantidadpedida = cantidad;
                                midetalle.cantidadautorizada = cantidad;
                                midetalle.stockactual = producto.stock;
                                productosSelectedPI.add(midetalle);
                            }
                            toolbar.getMenu().findItem(R.id.option_select).setVisible(true);
                            Banner.make(rootView,activity,Banner.INFO,"Item agregado a la lista", Banner.BOTTOM,2000).show();
                            dialog.dismiss();
                        }
                    }
                });

                builder.setNegativeButton("Cancelar", null);
                builder.show();
                input.clearFocus();
                //input.requestFocus();
            }catch (Exception e){
                Log.d("TAGPRODUCTO", e.getMessage());
            }

        }
    }
}
