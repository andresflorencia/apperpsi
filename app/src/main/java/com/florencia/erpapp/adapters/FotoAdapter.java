package com.florencia.erpapp.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.florencia.erpapp.R;
import com.florencia.erpapp.activities.DepositoActivity;
import com.florencia.erpapp.models.Foto;

import java.util.List;

public class FotoAdapter extends RecyclerView.Adapter<FotoAdapter.ImageViewHolder>{

    public static final String TAG = "TAGFOTO_ADAPTER";

    public List<Foto> listFoto;
    private Activity activity;

    public FotoAdapter(Activity activity, List<Foto> listFoto){
        this.activity = activity;
        this.listFoto = listFoto;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ImageViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_image, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        holder.bindImage(listFoto.get(position));
    }

    @Override
    public int getItemCount() {
        return listFoto.size();
    }
    class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageButton btnDelete;
        ImageView imgFoto;
        CardView cvFoto;

        ImageViewHolder(@NonNull View itemView){
            super(itemView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            imgFoto = itemView.findViewById(R.id.imgFoto);
            cvFoto = itemView.findViewById(R.id.cvFoto);
        }

        void bindImage(final Foto foto){
            if(foto.bitmap!=null)
                imgFoto.setImageBitmap(foto.bitmap);
            else if(foto.uriFoto!=null)
                imgFoto.setImageURI(foto.uriFoto);

            btnDelete.setOnClickListener(v -> EliminaFoto(getAdapterPosition()));

        }

        void EliminaFoto(int position){
            try{
                AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);
                View view = LayoutInflater.from(activity).inflate(R.layout.layout_confirmation_dialog,
                        (ConstraintLayout) activity.findViewById(R.id.lyDialogContainer));
                builder.setView(view);
                ((ImageView)view.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_delete2);
                ((TextView)view.findViewById(R.id.lblTitle)).setText("Eliminar foto");
                ((TextView)view.findViewById(R.id.lblMessage)).setText("¿Está seguro que desea eliminar esta foto?");
                ((Button)view.findViewById(R.id.btnConfirm)).setText("Si");
                ((Button)view.findViewById(R.id.btnCancel)).setText("Cancelar");

                final AlertDialog alertDialog = builder.create();
                view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
                            listFoto.remove(getAdapterPosition());
                            notifyDataSetChanged();
                            ((DepositoActivity) activity).btnCargaDocumento.setVisibility(View.VISIBLE);
                            alertDialog.dismiss();
                        });

                view.findViewById(R.id.btnCancel).setOnClickListener(v -> alertDialog.dismiss());

                alertDialog.setCancelable(false);
                if(alertDialog.getWindow()!=null)
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                alertDialog.show();
            }catch (Exception e){
                Log.d(TAG,"EliminaFoto(): " + e.getMessage());
            }
        }
    }
}

