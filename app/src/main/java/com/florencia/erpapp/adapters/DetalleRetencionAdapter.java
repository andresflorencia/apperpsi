package com.florencia.erpapp.adapters;

import android.app.AlertDialog;
import android.graphics.drawable.ColorDrawable;
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
import com.florencia.erpapp.activities.RetencionActivity;
import com.florencia.erpapp.models.DetalleRetencion;
import com.florencia.erpapp.services.SQLite;
import com.florencia.erpapp.utils.Utils;
import com.shasin.notificationbanner.Banner;

import java.util.List;

public class DetalleRetencionAdapter extends RecyclerView.Adapter<DetalleRetencionAdapter.RetencionViewHolder> {
    private static final String TAG = "TAGDETALLERET_ADAPTER";
    private RetencionActivity activity;
    public List<DetalleRetencion> detalleRetencion;
    private boolean visualizacion;
    View rootView;

    public DetalleRetencionAdapter(RetencionActivity activity, List<DetalleRetencion> detalleRetencion, boolean visualizacion) {
        this.detalleRetencion = detalleRetencion;
        this.activity = activity;
        this.visualizacion = visualizacion;
        this.rootView = activity.findViewById(android.R.id.content);
    }

    @NonNull
    @Override
    public RetencionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RetencionViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_retencion, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RetencionViewHolder holder, int position) {
        holder.bindRetencion(detalleRetencion.get(position));
    }

    @Override
    public int getItemCount() {
        return detalleRetencion.size();
    }

    class RetencionViewHolder extends RecyclerView.ViewHolder {

        TextView tvTipoRet, tvCodigoRet, tvBaseRet, tvPorcentajeRet, tvValorRet;
        ImageButton btnDelete;

        RetencionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTipoRet = itemView.findViewById(R.id.tvTipoRet);
            tvCodigoRet = itemView.findViewById(R.id.tvCodigoRet);
            tvBaseRet = itemView.findViewById(R.id.tvBaseRet);
            tvPorcentajeRet = itemView.findViewById(R.id.tvPorcentajeRet);
            tvValorRet = itemView.findViewById(R.id.tvValorRet);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnDelete.setVisibility(View.VISIBLE);
        }

        void bindRetencion(final DetalleRetencion detalle) {

            try {
                tvTipoRet.setText("RET: " + detalle.tipo);
                tvTipoRet.setTag(detalle.tipo);
                tvCodigoRet.setText("Código: " + detalle.codigoretencion);
                tvCodigoRet.setTag(detalle.codigoretencion);
                tvBaseRet.setText("Base imponible:\n" + Utils.FormatoMoneda(detalle.baseimponibleiva + detalle.baseimponible, 2));
                tvBaseRet.setTag(detalle.baseimponibleiva + detalle.baseimponible);
                tvPorcentajeRet.setText("Porcentaje Ret.:\n" + (detalle.porcentajereteneriva + detalle.porcentajeretener) + "%");
                tvPorcentajeRet.setTag(detalle.porcentajereteneriva + detalle.porcentajeretener);
                tvValorRet.setText("Valor Retenido:\n" + Utils.FormatoMoneda(detalle.valorretenido + detalle.valorretenidoiva, 2));
                tvValorRet.setTag(detalle.valorretenido + detalle.valorretenidoiva);

                btnDelete.setVisibility(View.VISIBLE);
                if(visualizacion)
                    btnDelete.setVisibility(View.GONE);
                btnDelete.setOnClickListener(v -> {

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);
                    View view = LayoutInflater.from(activity).inflate(R.layout.layout_warning_dialog,
                            (ConstraintLayout) activity.findViewById(R.id.lyDialogContainer));
                    builder.setView(view);
                    ((TextView) view.findViewById(R.id.lblTitle)).setText("ELIMINAR");
                    ((TextView) view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea eliminar este ítem?");
                    ((ImageView) view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_delete2);
                    ((Button) view.findViewById(R.id.btnCancel)).setText(activity.getResources().getString(R.string.Cancel));
                    ((Button) view.findViewById(R.id.btnYes)).setText(activity.getResources().getString(R.string.Confirm));
                    final AlertDialog alertDialog = builder.create();
                    view.findViewById(R.id.btnYes).setOnClickListener((vi) -> {
                        if(detalleRetencion.get(getAdapterPosition()).tipo.equals("IVA"))
                            activity.baseiva += detalleRetencion.get(getAdapterPosition()).baseimponibleiva;
                        else
                            activity.baseimponible += detalleRetencion.get(getAdapterPosition()).baseimponible;

                        detalleRetencion.remove(getAdapterPosition());
                        notifyDataSetChanged();
                        activity.updateTotal();
                        Banner.make(rootView, activity, Banner.INFO, "Ítem eliminado de la lista.", Banner.BOTTOM, 2000).show();
                        alertDialog.dismiss();
                    });

                    view.findViewById(R.id.btnCancel).setOnClickListener(vi -> alertDialog.dismiss());

                    if (alertDialog.getWindow() != null)
                        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                    alertDialog.show();
                });
            } catch (Exception ex) {
                Log.d(TAG, "bindRetencion(): " + ex.getMessage());
            }
        }
    }
}
