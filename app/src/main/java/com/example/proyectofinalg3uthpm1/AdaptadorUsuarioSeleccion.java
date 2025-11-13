package com.example.proyectofinalg3uthpm1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

// Adaptador para seleccionar usuarios (para crear grupos)
public class AdaptadorUsuarioSeleccion extends RecyclerView.Adapter<AdaptadorUsuarioSeleccion.SeleccionViewHolder> {

    private final Context contexto;
    private final List<ModeloUsuario> listaCompaneros;
    private final List<String> miembrosSeleccionados = new ArrayList<>();

    public AdaptadorUsuarioSeleccion(Context contexto, List<ModeloUsuario> listaCompaneros) {
        this.contexto = contexto;
        this.listaCompaneros = listaCompaneros;
    }

    @NonNull
    @Override
    public SeleccionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(contexto).inflate(R.layout.item_usuario_seleccion, parent, false);
        return new SeleccionViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull SeleccionViewHolder holder, int position) {
        ModeloUsuario usuario = listaCompaneros.get(position);

        holder.textoNombre.setText(usuario.getNombreCompleto());
        holder.textoCorreo.setText(usuario.getCorreo());

        if (usuario.getUrlFotoPerfil() != null && !usuario.getUrlFotoPerfil().isEmpty()) {
            Glide.with(contexto).load(usuario.getUrlFotoPerfil()).placeholder(R.drawable.ic_profile_placeholder).into(holder.imagenPerfil);
        } else {
            holder.imagenPerfil.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Manejar el estado del CheckBox
        holder.checkBox.setOnCheckedChangeListener(null); // Evitar disparos de listener al reciclar
        holder.checkBox.setChecked(miembrosSeleccionados.contains(usuario.getUid()));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!miembrosSeleccionados.contains(usuario.getUid())) {
                    miembrosSeleccionados.add(usuario.getUid());
                }
            } else {
                miembrosSeleccionados.remove(usuario.getUid());
            }
        });

        // Permitir seleccionar/deseleccionar al hacer clic en cualquier parte del item
        holder.itemView.setOnClickListener(v -> holder.checkBox.setChecked(!holder.checkBox.isChecked()));
    }

    @Override
    public int getItemCount() {
        return listaCompaneros.size();
    }

    // Método para obtener la lista de UIDs seleccionados
    public List<String> getMiembrosSeleccionados() {
        return miembrosSeleccionados;
    }

    // ViewHolder
    static class SeleccionViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imagenPerfil;
        TextView textoNombre, textoCorreo;
        CheckBox checkBox;

        public SeleccionViewHolder(@NonNull View itemView) {
            super(itemView);
            imagenPerfil = itemView.findViewById(R.id.imagenPerfilUsuarioSel);
            textoNombre = itemView.findViewById(R.id.textoNombreUsuarioSel);
            textoCorreo = itemView.findViewById(R.id.textoCorreoUsuarioSel);
            checkBox = itemView.findViewById(R.id.checkboxSeleccionarUsuario);
        }
    }
}
