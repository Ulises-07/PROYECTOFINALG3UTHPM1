package com.example.proyectofinalg3uthpm1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;

public class AdaptadorGrupos extends RecyclerView.Adapter<AdaptadorGrupos.GrupoViewHolder> {

    private final Context contexto;
    private final List<ModeloGrupo> listaGrupos;
    private final OnGrupoClickListener listener;

    public interface OnGrupoClickListener {
        void onGrupoClick(ModeloGrupo grupo);
    }

    public AdaptadorGrupos(Context contexto, List<ModeloGrupo> listaGrupos, OnGrupoClickListener listener) {
        this.contexto = contexto;
        this.listaGrupos = listaGrupos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GrupoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(contexto).inflate(R.layout.item_grupo, parent, false);
        return new GrupoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GrupoViewHolder holder, int position) {
        ModeloGrupo grupo = listaGrupos.get(position);

        holder.textoNombre.setText(grupo.getNombreGrupo());

        if (grupo.getFechaCreacion() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.textoInfo.setText("Creado el: " + sdf.format(grupo.getFechaCreacion()));
        } else {
            holder.textoInfo.setText("Cargando...");
        }

        holder.imagenGrupo.setImageResource(R.drawable.rounded_contacts_product_24);

        holder.itemView.setOnClickListener(v -> listener.onGrupoClick(grupo));
    }

    @Override
    public int getItemCount() {
        return listaGrupos.size();
    }

    static class GrupoViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imagenGrupo;
        TextView textoNombre, textoInfo;

        public GrupoViewHolder(@NonNull View itemView) {
            super(itemView);
            imagenGrupo = itemView.findViewById(R.id.imagenGrupoItem);
            textoNombre = itemView.findViewById(R.id.textoNombreGrupoItem);
            textoInfo = itemView.findViewById(R.id.textoInfoGrupoItem);
        }
    }
}
