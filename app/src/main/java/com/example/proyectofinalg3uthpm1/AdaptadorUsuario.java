package com.example.proyectofinalg3uthpm1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class AdaptadorUsuario extends RecyclerView.Adapter<AdaptadorUsuario.UsuarioViewHolder> {

    private final Context contexto;
    private final List<ModeloUsuario> listaUsuarios;
    private final OnUsuarioClickListener listener;

    public interface OnUsuarioClickListener {
        void onUsuarioClick(ModeloUsuario usuario, Button boton);
    }

    public AdaptadorUsuario(Context contexto, List<ModeloUsuario> listaUsuarios, OnUsuarioClickListener listener) {
        this.contexto = contexto;
        this.listaUsuarios = listaUsuarios;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(contexto).inflate(R.layout.item_usuario_busqueda, parent, false);
        return new UsuarioViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        ModeloUsuario usuario = listaUsuarios.get(position);

        holder.textoNombre.setText(usuario.getNombreCompleto());
        holder.textoCorreo.setText(usuario.getCorreo());

        if (usuario.getUrlFotoPerfil() != null && !usuario.getUrlFotoPerfil().isEmpty()) {
            Glide.with(contexto)
                    .load(usuario.getUrlFotoPerfil())
                    .placeholder(R.drawable.outline_account_circle_24)
                    .into(holder.imagenPerfil);
        } else {
            holder.imagenPerfil.setImageResource(R.drawable.outline_account_circle_24);
        }

        holder.botonAgregar.setOnClickListener(v -> listener.onUsuarioClick(usuario, holder.botonAgregar));
    }

    @Override
    public int getItemCount() {
        return listaUsuarios.size();
    }

    static class UsuarioViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imagenPerfil;
        TextView textoNombre, textoCorreo;
        Button botonAgregar;

        public UsuarioViewHolder(@NonNull View itemView) {
            super(itemView);
            imagenPerfil = itemView.findViewById(R.id.imagenPerfilUsuario);
            textoNombre = itemView.findViewById(R.id.textoNombreUsuario);
            textoCorreo = itemView.findViewById(R.id.textoCorreoUsuario);
            botonAgregar = itemView.findViewById(R.id.botonAgregarCompanero);
        }
    }
}