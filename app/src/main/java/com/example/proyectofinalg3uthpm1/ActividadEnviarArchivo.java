package com.example.proyectofinalg3uthpm1;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActividadEnviarArchivo extends AppCompatActivity {

    private static final int CODIGO_SELECCION_ARCHIVO = 102;
    private static final String TAG = "ActividadEnviarArchivo";

    // Vistas
    private Toolbar toolbar;
    private Button botonSeleccionar, botonEnviar;
    private ImageView imagenVistaPrevia;
    private TextView textoInfoArchivo;
    private ProgressBar barraProgreso;

    // Firebase
    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private FirebaseUser usuarioActual;

    // Variables
    private Uri uriArchivoSeleccionado;
    private String tipoArchivo;
    private String nombreArchivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_enviar_archivo);

        // Inicializar Firebase
        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();
        usuarioActual = FirebaseAuth.getInstance().getCurrentUser();

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarEnviarArchivo);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Enviar Archivo");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Enlazar Vistas
        botonSeleccionar = findViewById(R.id.botonSeleccionarArchivo);
        botonEnviar = findViewById(R.id.botonEnviarArchivo);
        imagenVistaPrevia = findViewById(R.id.imagenVistaPrevia);
        textoInfoArchivo = findViewById(R.id.textoInfoArchivo);
        barraProgreso = findViewById(R.id.barraProgresoEnvio);

        // Configurar Clics
        botonSeleccionar.setOnClickListener(v -> abrirSelectorDeArchivo());
        botonEnviar.setOnClickListener(v -> subirArchivoAFirebase());
    }

    private void abrirSelectorDeArchivo() {
        // Intent para seleccionar cualquier tipo de archivo
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        // Permitir seleccionar imágenes, videos y PDFs
        String[] mimetypes = {"image/*", "video/*", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(intent, CODIGO_SELECCION_ARCHIVO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CODIGO_SELECCION_ARCHIVO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uriArchivoSeleccionado = data.getData();
            tipoArchivo = getContentResolver().getType(uriArchivoSeleccionado);
            nombreArchivo = obtenerNombreArchivo(uriArchivoSeleccionado);

            // Actualizar UI
            botonEnviar.setEnabled(true);
            if (tipoArchivo.startsWith("image/")) {
                // Es imagen, mostrar vista previa
                imagenVistaPrevia.setVisibility(View.VISIBLE);
                textoInfoArchivo.setVisibility(View.GONE);
                Glide.with(this).load(uriArchivoSeleccionado).into(imagenVistaPrevia);
            } else {
                // No es imagen (PDF, Video), mostrar info
                imagenVistaPrevia.setVisibility(View.GONE);
                textoInfoArchivo.setVisibility(View.VISIBLE);
                textoInfoArchivo.setText("Archivo: " + nombreArchivo);
            }
        }
    }

    // Helper para obtener el nombre del archivo desde la URI
    private String obtenerNombreArchivo(Uri uri) {
        String nombre = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        nombre = cursor.getString(index);
                    }
                }
            }
        }
        if (nombre == null) {
            nombre = uri.getPath();
            int cut = nombre.lastIndexOf('/');
            if (cut != -1) {
                nombre = nombre.substring(cut + 1);
            }
        }
        return nombre;
    }

    private void subirArchivoAFirebase() {
        if (uriArchivoSeleccionado == null) {
            Toast.makeText(this, "No hay archivo seleccionado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar progreso
        barraProgreso.setVisibility(View.VISIBLE);
        barraProgreso.setIndeterminate(true);
        botonEnviar.setEnabled(false);
        botonSeleccionar.setEnabled(false);

        // 1. Subir archivo a Firebase Storage
        // Usamos UUID para un nombre único
        StorageReference refArchivo = storage.getReference()
                .child("archivos_compartidos/" + UUID.randomUUID().toString() + "_" + nombreArchivo);

        refArchivo.putFile(uriArchivoSeleccionado)
                .addOnSuccessListener(taskSnapshot -> {
                    // 2. Obtener la URL de descarga
                    refArchivo.getDownloadUrl().addOnSuccessListener(uri -> {
                        String urlDescarga = uri.toString();
                        // 3. Obtener el nombre del usuario actual
                        obtenerNombreUsuarioYGuardarEnFirestore(urlDescarga);
                    });
                })
                .addOnFailureListener(e -> {
                    // Error al subir
                    barraProgreso.setVisibility(View.GONE);
                    botonEnviar.setEnabled(true);
                    botonSeleccionar.setEnabled(true);
                    Toast.makeText(this, "Error al subir archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void obtenerNombreUsuarioYGuardarEnFirestore(String urlDescarga) {
        // Obtenemos el nombre del perfil de Firestore
        db.collection("Usuarios").document(usuarioActual.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String nombreEmisor = usuarioActual.getEmail(); // Fallback
                    if (documentSnapshot.exists()) {
                        nombreEmisor = documentSnapshot.getString("nombreCompleto");
                    }
                    // 4. Guardar metadata en Firestore
                    guardarMetadataEnFirestore(urlDescarga, nombreEmisor);
                })
                .addOnFailureListener(e -> {
                    // Si falla, usamos el email como nombre
                    guardarMetadataEnFirestore(urlDescarga, usuarioActual.getEmail());
                });
    }

    private void guardarMetadataEnFirestore(String urlDescarga, String nombreEmisor) {
        // Nombres de campos en español
        Map<String, Object> datosArchivo = new HashMap<>();
        datosArchivo.put("idUsuarioEmisor", usuarioActual.getUid());
        datosArchivo.put("nombreEmisor", nombreEmisor);
        datosArchivo.put("urlArchivo", urlDescarga);
        datosArchivo.put("nombreArchivo", nombreArchivo);
        datosArchivo.put("tipoArchivo", tipoArchivo);
        datosArchivo.put("idGrupoDestino", null); // TODO: Implementar selección de grupo
        datosArchivo.put("idUsuarioDestino", null); // TODO: Implementar selección de compañero
        datosArchivo.put("fechaEnvio", com.google.firebase.firestore.FieldValue.serverTimestamp()); // Timestamp

        db.collection("Archivos").add(datosArchivo)
                .addOnSuccessListener(documentReference -> {
                    // Éxito
                    barraProgreso.setVisibility(View.GONE);
                    Toast.makeText(this, "Archivo enviado.", Toast.LENGTH_SHORT).show();
                    finish(); // Volver a ActividadPrincipal
                })
                .addOnFailureListener(e -> {
                    // Error al guardar en Firestore
                    barraProgreso.setVisibility(View.GONE);
                    botonEnviar.setEnabled(true);
                    botonSeleccionar.setEnabled(true);
                    Toast.makeText(this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
