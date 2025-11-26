package com.example.proyectofinalg3uthpm1;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;;
import android.view.LayoutInflater;
import android.view.View;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;


public class AdaptadorArchivos extends RecyclerView.Adapter<AdaptadorArchivos.ArchivoViewHolder> {

    private final Context contexto;
    private final List<ModeloArchivo> listaArchivos;
    private final String uidUsuarioActual;

    public AdaptadorArchivos(Context contexto, List<ModeloArchivo> listaArchivos) {
        this.contexto = contexto;
        this.listaArchivos = listaArchivos;
        this.uidUsuarioActual = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public ArchivoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(contexto).inflate(R.layout.item_archivo_feed, parent, false);
        return new ArchivoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ArchivoViewHolder holder, int position) {
        ModeloArchivo archivo = listaArchivos.get(position);

        holder.textoNombreEmisor.setText(archivo.getNombreEmisor());
        if (archivo.getFechaEnvio() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            holder.textoFecha.setText(sdf.format(archivo.getFechaEnvio()));
        } else {
            holder.textoFecha.setText("Enviando...");
        }


        String idEmisor = archivo.getIdUsuarioEmisor();

        if (idEmisor != null && !idEmisor.isEmpty()) {

            FirebaseFirestore.getInstance().collection("Usuarios").document(idEmisor).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String urlFoto = documentSnapshot.getString("urlFotoPerfil");

                            if (urlFoto != null && !urlFoto.isEmpty()) {
                                Glide.with(contexto)
                                        .load(urlFoto)
                                        .placeholder(R.drawable.outline_account_circle_24)
                                        .error(R.drawable.outline_account_circle_24)
                                        .into(holder.imagenPerfil);
                            } else {
                                holder.imagenPerfil.setImageResource(R.drawable.outline_account_circle_24);
                            }
                        } else {
                            holder.imagenPerfil.setImageResource(R.drawable.outline_account_circle_24);
                        }
                    })
                    .addOnFailureListener(e -> {
                        holder.imagenPerfil.setImageResource(R.drawable.outline_account_circle_24);
                        Log.e("AdaptadorArchivos", "Error al obtener foto de perfil", e);
                    });
        } else {
            holder.imagenPerfil.setImageResource(R.drawable.outline_account_circle_24);
        }

        String tipo = archivo.getTipoArchivo();
        if (tipo != null && tipo.startsWith("image/")) {

            holder.imagenContenido.setVisibility(View.VISIBLE);
            holder.layoutArchivoGenerico.setVisibility(View.GONE);
            Glide.with(contexto)
                    .load(archivo.getUrlArchivo())
                    .placeholder(R.drawable.rounded_imagesmode_24)
                    .into(holder.imagenContenido);
        } else {
            holder.imagenContenido.setVisibility(View.GONE);
            holder.layoutArchivoGenerico.setVisibility(View.VISIBLE);
            holder.textoNombreArchivo.setText(archivo.getNombreArchivo());

            holder.iconoTipoArchivo.setImageResource(R.drawable.ic_file);
        }

        if (archivo.getIdUsuarioEmisor().equals(uidUsuarioActual)) {
            holder.botonEliminar.setVisibility(View.VISIBLE);
            holder.botonEliminar.setOnClickListener(v -> {
                mostrarDialogoDeBorrado(archivo, position);
            });
        } else {
            holder.botonEliminar.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (tipo != null && !tipo.startsWith("image/")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(archivo.getUrlArchivo()), tipo);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    contexto.startActivity(Intent.createChooser(intent, "Abrir con..."));
                } catch (Exception e) {
                    Toast.makeText(contexto, "No se encontró una app para abrir este archivo.", Toast.LENGTH_SHORT).show();
                    Log.e("AdaptadorArchivos", "Error al abrir archivo: " + e.getMessage());
                }
            }
        });


        holder.botonDescargar.setOnClickListener(v -> {
            descargarArchivo(archivo);
        });


    }

    private void descargarArchivo(ModeloArchivo archivo) {
        String urlDeDescarga = archivo.getUrlArchivo();
        String nombreArchivo = archivo.getNombreArchivo();

        if (urlDeDescarga == null || urlDeDescarga.isEmpty()) {
            Toast.makeText(contexto, "URL de descarga no válida.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            DownloadManager downloadManager = (DownloadManager) contexto.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(urlDeDescarga);

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nombreArchivo);
            request.setTitle(nombreArchivo);
            request.setDescription("Descargando...");

            downloadManager.enqueue(request);
            Toast.makeText(contexto, "Archivo Descargado", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(contexto, "Error al iniciar la descarga.", Toast.LENGTH_SHORT).show();
            Log.e("AdaptadorArchivos", "Error al descargar: " + e.getMessage());
        }
    }

    private void mostrarDialogoDeBorrado(ModeloArchivo archivo, int position) {
        new AlertDialog.Builder(contexto)
                .setTitle("Eliminar Archivo")
                .setMessage("¿Estás seguro de que deseas eliminar este archivo? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    eliminarArchivo(archivo, position);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarArchivo(ModeloArchivo archivo, int position) {
        FirebaseFirestore.getInstance().collection("Archivos").document(archivo.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    FirebaseStorage.getInstance().getReferenceFromUrl(archivo.getUrlArchivo())
                            .delete()
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(contexto, "Archivo eliminado.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(contexto, "Archivo eliminado", Toast.LENGTH_SHORT).show();
                                Log.e("AdaptadorArchivos", "Error Storage delete: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(contexto, "Error al eliminar de Firestore.", Toast.LENGTH_SHORT).show();
                    Log.e("AdaptadorArchivos", "Error Firestore delete: " + e.getMessage());
                });
    }

    @Override
    public int getItemCount() {
        return listaArchivos.size();
    }

    static class ArchivoViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imagenPerfil;
        TextView textoNombreEmisor, textoFecha, textoNombreArchivo;
        ImageView imagenContenido, botonEliminar, iconoTipoArchivo, botonDescargar;
        LinearLayout layoutArchivoGenerico;

        public ArchivoViewHolder(@NonNull View itemView) {
            super(itemView);
            imagenPerfil = itemView.findViewById(R.id.imagenPerfilFeed);
            textoNombreEmisor = itemView.findViewById(R.id.textoNombreEmisorFeed);
            textoFecha = itemView.findViewById(R.id.textoFechaFeed);
            textoNombreArchivo = itemView.findViewById(R.id.textoNombreArchivoFeed);
            imagenContenido = itemView.findViewById(R.id.imagenContenidoFeed);
            botonEliminar = itemView.findViewById(R.id.botonEliminarArchivo);
            iconoTipoArchivo = itemView.findViewById(R.id.iconoTipoArchivo);
            layoutArchivoGenerico = itemView.findViewById(R.id.layoutArchivoGenerico);

            botonDescargar = itemView.findViewById(R.id.botonDescargarArchivo);

        }
    }
}
