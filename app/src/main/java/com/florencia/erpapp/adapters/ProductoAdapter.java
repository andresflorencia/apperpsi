package com.florencia.erpapp.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import com.florencia.erpapp.utils.Constants;
import com.florencia.erpapp.utils.Utils;
import com.shasin.notificationbanner.Banner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder> {
    private static String TAG = "TAGDETALLEPRODUCTO_ADAPTER";
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

    public void filter(final String busqueda) {
        listProductos.clear();
        if (busqueda.length() == 0) {
            //listProductos.addAll(orginalItems);
            filter_by_clasif(clasificacionid);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                List<Producto> collect = orginalItems.stream()
                        .filter(i -> i.nombreproducto.toLowerCase()
                                .concat(i.codigoproducto.toLowerCase())
                                .concat(i.numerolote.toLowerCase())
                                .contains(busqueda.toLowerCase()) &&
                                (clasificacionid != -1 ? i.clasificacionid : 1) == (clasificacionid != -1 ? clasificacionid : 1))
                        .collect(Collectors.<Producto>toList());
                listProductos.addAll(collect);
            } else {
                for (Producto i : orginalItems) {
                    if (i.nombreproducto.toLowerCase()
                            .concat(i.codigoproducto.toLowerCase())
                            .concat(i.numerolote.toLowerCase())
                            .contains(busqueda.toLowerCase()) &&
                            (clasificacionid != -1 ? i.clasificacionid : 1) == (clasificacionid != -1 ? clasificacionid : 1))
                        listProductos.add(i);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void filter_by_clasif(final Integer clasificacionid) {
        try {
            this.clasificacionid = clasificacionid;
            listProductos.clear();
            if (clasificacionid.equals(-1)) {
                listProductos.addAll(orginalItems);
            } else {
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
        } catch (Exception e) {
            Log.d(TAG, "filter_by_clasif(): " + e.getMessage());
        }
    }

    /*@Override
    public void onViewAttachedToWindow(ProductoViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        Utils.animateCircularReveal(holder.itemView);
    }*/

    class ProductoViewHolder extends RecyclerView.ViewHolder {

        TextView tvNombreProducto, tvPrecio, tvStock, tvPercentDesc;
        ImageButton btnInfo;
        CheckBox ckIva;

        ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreProducto = itemView.findViewById(R.id.tv_NombreProducto);
            tvPrecio = itemView.findViewById(R.id.tv_Precio);
            tvStock = itemView.findViewById(R.id.tv_Stock);
            ckIva = itemView.findViewById(R.id.ckIva);
            btnInfo = itemView.findViewById(R.id.btnInfo);
            tvPercentDesc = itemView.findViewById(R.id.tv_PercentDesc);
        }

        void bindProducto(final Producto producto) {

            try {
                tvNombreProducto.setText(producto.nombreproducto);
                ckIva.setChecked(producto.porcentajeiva > 0);

                if (tipobusqueda.equals("4,20") || tipobusqueda.equals("20,4")) {//TRANSFERENCIAS
                    ckIva.setVisibility(View.GONE);
                    btnInfo.setVisibility(View.GONE);
                    tvPrecio.setText(//("P. Costo: " + Utils.FormatoMoneda(producto.lotes.get(0).preciocosto, 2))
                            ("\n")
                            .concat("Vence: " + producto.lotes.get(0).fechavencimiento));
                    tvStock.setText(("Stock: " + Utils.RoundDecimal(producto.lotes.get(0).stock, 2))
                            .concat("\n")
                            .concat("Lote: " + producto.lotes.get(0).numerolote));
                } else {
                    ckIva.setVisibility(View.VISIBLE);
                    btnInfo.setVisibility(View.VISIBLE);
                    tvPercentDesc.setVisibility(View.GONE);
                    tvPrecio.setText("PVP: " + Utils.FormatoMoneda(producto.getPrecioSugerido(false), 2));

                    if(producto.descuento>0) {
                        tvPercentDesc.setText("-" + producto.descuento + "%");
                        tvPercentDesc.setVisibility(View.VISIBLE);
                    }

                    if (producto.stock > 0 && producto.tipo.equalsIgnoreCase("P")) {
                        tvStock.setText("Stock: " + Utils.RoundDecimal(producto.stock, 2));
                        tvStock.setTextColor(Color.BLACK);
                    } else if (producto.tipo.equalsIgnoreCase("S")) {
                        tvStock.setText("SERVICIO");
                        tvStock.setTextColor(Color.BLACK);
                    } else {
                        tvStock.setText("Sin stock");
                        tvStock.setTextColor(itemView.getContext().getResources().getColor(R.color.texthintenabled));
                    }
                }


                itemView.setOnClickListener(v -> IngresarCantidad(v.getContext(), listProductos.get(getAdapterPosition())));
                btnInfo.setOnClickListener(v -> {
                    DialogFragment dialogFragment = new InfoItemDialogFragment();
                    Bundle bundle = new Bundle();
                    bundle.putInt("idproducto", listProductos.get(getAdapterPosition()).idproducto);
                    dialogFragment.setArguments(bundle);
                    dialogFragment.show(activity.getSupportFragmentManager(), "dialog");
                });
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }

        }

        @SuppressLint("RestrictedApi")
        void IngresarCantidad(Context context, Producto producto) {
            try {

                if (tipobusqueda.equals("01") || tipobusqueda.equals("PR")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (productosSelected.stream().filter(i -> i.producto.equals(producto)).
                                collect(Collectors.toList()).size() > 0) {
                            Banner.make(rootView, activity, Banner.WARNING, "Item ya seleccionado...", Banner.BOTTOM, 2000).show();
                            return;
                        }
                    } else {
                        for (DetalleComprobante detalle : productosSelected) {
                            if (detalle.producto.equals(producto)) {
                                Banner.make(rootView, activity, Banner.WARNING, "Item ya seleccionado...", Banner.BOTTOM, 2000).show();
                                return;
                            }
                        }
                    }
                } else if (tipobusqueda.equals("PC") || tipobusqueda.equals("PI")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (tipobusqueda.equals("PC")) {
                            if (productosSelectedP.stream().filter(i -> i.producto.equals(producto)).
                                    collect(Collectors.toList()).size() > 0) {
                                Banner.make(rootView, activity, Banner.WARNING, "Item ya seleccionado...", Banner.BOTTOM, 2000).show();
                                return;
                            }
                        } else if (tipobusqueda.equals("PI")) {
                            if (productosSelectedPI.stream().filter(i -> i.producto.equals(producto)).
                                    collect(Collectors.toList()).size() > 0) {
                                Banner.make(rootView, activity, Banner.WARNING, "Item ya seleccionado...", Banner.BOTTOM, 2000).show();
                                return;
                            }
                        }
                    } else {
                        if (tipobusqueda.equals("PC")) {
                            for (DetallePedido detalle : productosSelectedP) {
                                if (detalle.producto.equals(producto)) {
                                    Banner.make(rootView, activity, Banner.WARNING, "Item ya seleccionado...", Banner.BOTTOM, 2000).show();
                                    return;
                                }
                            }
                        } else if (tipobusqueda.equals("PI")) {
                            for (DetallePedidoInv detalle : productosSelectedPI) {
                                if (detalle.producto.equals(producto)) {
                                    Banner.make(rootView, activity, Banner.WARNING, "Item ya seleccionado...", Banner.BOTTOM, 2000).show();
                                    return;
                                }
                            }
                        }
                    }
                }

                if (tipobusqueda.equals("01") && producto.stock <= 0 && producto.tipo.equalsIgnoreCase("P")) {
                    Banner.make(rootView, activity, Banner.WARNING, "Producto sin stock disponible.", Banner.BOTTOM, 2000).show();
                    return;
                } else if ((tipobusqueda.equals("4,20") || tipobusqueda.equals("20,4")) && producto.lotes.get(0).stock <= 0) {
                    Banner.make(rootView, activity, Banner.WARNING, "Lote del producto sin stock disponible.", Banner.BOTTOM, 2000).show();
                    return;
                }

                EditText txtCantidad;
                TextView lblMessage;
                Button btnOk;
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context, R.style.AlertDialogTheme);
                View view = LayoutInflater.from(context).inflate(R.layout.layout_cantidad_dialog,
                        (ConstraintLayout) activity.findViewById(R.id.lyDialogContainer));
                builder.setView(view);
                txtCantidad = view.findViewById(R.id.txtCantidad);
                lblMessage = (TextView) view.findViewById(R.id.lblMessage);
                btnOk = (Button) view.findViewById(R.id.btnConfirm);
                ((Button) view.findViewById(R.id.btnCancel)).setText("Cancelar");

                btnOk.setText("Confirmar");
                txtCantidad.setText("1");
                txtCantidad.setSelectAllOnFocus(true);

                txtCantidad.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        try {
                            Double c = Double.parseDouble(s.toString().trim());
                            lblMessage.setText("");
                            btnOk.setEnabled(true);
                        } catch (Exception e) {
                            btnOk.setEnabled(false);
                            lblMessage.setText("Ingrese una cantidad válida.");
                        }
                    }
                });

                final android.app.AlertDialog alertDialog = builder.create();
                btnOk.setOnClickListener(
                        v -> {
                            if (txtCantidad.getText().toString().equals("")) {
                                Banner.make(rootView, activity, Banner.ERROR, "Debe especificar una cantidad.", Banner.BOTTOM, 2000).show();
                                return;
                            } else {
                                Double cantidad = Double.parseDouble(txtCantidad.getText().toString().trim());
                                if (tipobusqueda.equals("01") && cantidad > producto.stock && producto.tipo.equalsIgnoreCase("P")) {
                                    Banner.make(rootView, activity, Banner.ERROR, "La cantidad máxima de venta es: " + producto.stock, Banner.BOTTOM, 2000).show();
                                    return;
                                } else if ((tipobusqueda.equals("4,20") || tipobusqueda.equals("20,4")) && cantidad > producto.lotes.get(0).stock) {
                                    Banner.make(rootView, activity, Banner.ERROR, "La cantidad máxima de transferencia del lote " + Constants.COMILLA_ABRE
                                            + producto.lotes.get(0).numerolote + Constants.COMILLA_CIERRA + " es: " + producto.lotes.get(0).stock, Banner.BOTTOM, 2000).show();
                                    return;
                                }
                                Integer counter = 0;
                                if (tipobusqueda.equals("01") || tipobusqueda.equals("PR")) { //FACTURA -PROFORMA
                                    DetalleComprobante midetalle = new DetalleComprobante();
                                    midetalle.producto = producto;
                                    midetalle.cantidad = cantidad;
                                    midetalle.precio = producto.pvp1;
                                    midetalle.total = midetalle.cantidad * midetalle.precio;
                                    productosSelected.add(midetalle);
                                    counter = productosSelected.size();
                                } else if (tipobusqueda.equals("PC")) { //PEDIDO CLIENTE
                                    DetallePedido midetalle = new DetallePedido();
                                    midetalle.producto = producto;
                                    midetalle.cantidad = cantidad;
                                    midetalle.precio = producto.pvp1;
                                    productosSelectedP.add(midetalle);
                                    counter = productosSelectedP.size();
                                } else if (tipobusqueda.equals("4,20") || tipobusqueda.equals("20,4")) { //TRANSFERENCIA
                                    DetalleComprobante midetalle = new DetalleComprobante();
                                    midetalle.producto = producto;
                                    midetalle.cantidad = cantidad;
                                    midetalle.precio = producto.lotes.get(0).preciocosto;
                                    midetalle.preciocosto = producto.lotes.get(0).preciocosto;
                                    midetalle.numerolote = producto.lotes.get(0).numerolote;
                                    midetalle.stock = producto.lotes.get(0).stock - cantidad;
                                    midetalle.fechavencimiento = producto.lotes.get(0).fechavencimiento;
                                    productosSelected.add(midetalle);
                                    counter = productosSelected.size();
                                } else if (tipobusqueda.equals("PI")) { //PEDIDO INVENTARIO
                                    DetallePedidoInv midetalle = new DetallePedidoInv();
                                    midetalle.producto = producto;
                                    midetalle.cantidadpedida = cantidad;
                                    midetalle.cantidadautorizada = cantidad;
                                    midetalle.stockactual = producto.stock;
                                    productosSelectedPI.add(midetalle);
                                    counter = productosSelectedPI.size();
                                }
                                activity.btnConfirmar.setVisibility(View.VISIBLE);
                                activity.txtCounter.setVisibility(View.VISIBLE);
                                activity.txtCounter.setText(counter.toString());
                                //toolbar.getMenu().findItem(R.id.option_select).setVisible(true);
                                Banner.make(rootView, activity, Banner.INFO,
                                        "Item agregado a la lista", Banner.TOP, 2000).show();
                                alertDialog.dismiss();
                            }
                        }
                );

                view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

                if (alertDialog.getWindow() != null)
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                alertDialog.show();
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }

        }
    }
}
