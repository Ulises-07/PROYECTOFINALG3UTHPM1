package com.example.proyectofinalg3uthpm1;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActividadEnviarArchivo extends AppCompatActivity {

    private static final String TAG = "ActividadEnviarArchivo";

    private Toolbar toolbar;
    private Button botonSeleccionar, botonEnviar;
    private ImageView imagenVistaPrevia;
    private TextView textoInfoArchivo;
    private ProgressBar barraProgreso;

    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private FirebaseUser usuarioActual;

    private Uri uriArchivoSeleccionado;
    private String tipoArchivo;
    private String nombreArchivo;

    private String idGrupoDestino;

    private ActivityResultLauncher<String[]> lanzadorPermisos;
    private ActivityResultLauncher<Intent> lanzadorSelectorArchivos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_enviar_archivo);

        if (getIntent().hasExtra("idGrupoDestino")) {
            idGrupoDestino = getIntent().getStringExtra("idGrupoDestino");
        }

        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();
        usuarioActual = FirebaseAuth.getInstance().getCurrentUser();

        toolbar = findViewById(R.id.toolbarEnviarArchivo);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Enviar Archivo");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        botonSeleccionar = findViewById(R.id.botonSeleccionarArchivo);
        botonEnviar = findViewById(R.id.botonEnviarArchivo);
        imagenVistaPrevia = findViewById(R.id.imagenVistaPrevia);
        textoInfoArchivo = findViewById(R.id.textoInfoArchivo);
        barraProgreso = findViewById(R.id.barraProgresoEnvio);

        inicializarLanzadores();

        botonSeleccionar.setOnClickListener(v -> solicitarPermisosYContinuar());
        botonEnviar.setOnClickListener(v -> subirArchivoAFirebase());
    }

    private void inicializarLanzadores() {
        lanzadorPermisos = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
            boolean concedido;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                concedido = Boolean.TRUE.equals(resultado.get(Manifest.permission.READ_MEDIA_IMAGES)) ||
                        Boolean.TRUE.equals(resultado.get(Manifest.permission.READ_MEDIA_VIDEO));
            } else {
                concedido = Boolean.TRUE.equals(resultado.get(Manifest.permission.READ_EXTERNAL_STORAGE));
            }

            if (concedido) {
                abrirSelectorDeArchivo();
            } else {
                Toast.makeText(this, "Permiso denegado.", Toast.LENGTH_LONG).show();
            }
        });

        lanzadorSelectorArchivos = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
            if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null && resultado.getData().getData() != null) {
                uriArchivoSeleccionado = resultado.getData().getData();
                tipoArchivo = getContentResolver().getType(uriArchivoSeleccionado);
                nombreArchivo = obtenerNombreArchivo(uriArchivoSeleccionado);
                actualizarUIConArchivo();
            }
        });
    }

    private void solicitarPermisosYContinuar() {
        String[] permisos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisos = new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO};
        } else {
            permisos = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        lanzadorPermisos.launch(permisos);
    }

    private void abrirSelectorDeArchivo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimetypes = {"image/*", "video/*", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        lanzadorSelectorArchivos.launch(intent);
    }

    private void actualizarUIConArchivo() {
        botonEnviar.setEnabled(true);
        if (tipoArchivo != null && tipoArchivo.startsWith("image/")) {
            imagenVistaPrevia.setVisibility(View.VISIBLE);
            textoInfoArchivo.setVisibility(View.GONE);
            Glide.with(this).load(uriArchivoSeleccionado).into(imagenVistaPrevia);
        } else {
            imagenVistaPrevia.setVisibility(View.GONE);
            textoInfoArchivo.setVisibility(View.VISIBLE);
            textoInfoArchivo.setText("Archivo: " + nombreArchivo);
        }
    }

    private String obtenerNombreArchivo(Uri uri) {
        String nombre = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) nombre = cursor.getString(index);
                }
            } catch (Exception e) { Log.e(TAG, "Error nombre archivo", e); }
        }
        if (nombre == null) {
            nombre = uri.getPath();
            if (nombre != null) {
                int cut = nombre.lastIndexOf('/');
                if (cut != -1) nombre = nombre.substring(cut + 1);
            }
        }
        return (nombre != null) ? nombre : "archivo_desconocido";
    }

    private void subirArchivoAFirebase() {
        if (uriArchivoSeleccionado == null || usuarioActual == null) return;

        barraProgreso.setVisibility(View.VISIBLE);
        botonEnviar.setEnabled(false);
        botonSeleccionar.setEnabled(false);

        StorageReference refArchivo = storage.getReference()
                .child("archivos_compartidos/" + UUID.randomUUID().toString() + "_" + nombreArchivo);

        refArchivo.putFile(uriArchivoSeleccionado)
                .addOnSuccessListener(taskSnapshot -> {
                    refArchivo.getDownloadUrl().addOnSuccessListener(uri -> {
                        obtenerNombreUsuarioYGuardarEnFirestore(uri.toString());
                    });
                })
                .addOnFailureListener(e -> restaurarUI("Error al subir: " + e.getMessage()));
    }

    private void obtenerNombreUsuarioYGuardarEnFirestore(String urlDescarga) {
        db.collection("Usuarios").document(usuarioActual.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String nombreEmisor = usuarioActual.getEmail();
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("nombreCompleto");
                        if(nombre != null && !nombre.isEmpty()) nombreEmisor = nombre;
                    }
                    guardarMetadataEnFirestore(urlDescarga, nombreEmisor);
                })
                .addOnFailureListener(e -> guardarMetadataEnFirestore(urlDescarga, usuarioActual.getEmail()));
    }

    private void guardarMetadataEnFirestore(String urlDescarga, String nombreEmisor) {
        Map<String, Object> datosArchivo = new HashMap<>();
        datosArchivo.put("idUsuarioEmisor", usuarioActual.getUid());
        datosArchivo.put("nombreEmisor", nombreEmisor);
        datosArchivo.put("urlArchivo", urlDescarga);
        datosArchivo.put("nombreArchivo", nombreArchivo);
        datosArchivo.put("tipoArchivo", tipoArchivo);

        datosArchivo.put("idGrupoDestino", idGrupoDestino);

        datosArchivo.put("idUsuarioDestino", null);
        datosArchivo.put("fechaEnvio", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("Archivos").add(datosArchivo)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Archivo enviado al grupo.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    restaurarUI("Error al guardar datos: " + e.getMessage());
                });
    }

    private void restaurarUI(String mensajeError) {
        barraProgreso.setVisibility(View.GONE);
        botonEnviar.setEnabled(true);
        botonSeleccionar.setEnabled(true);
        Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}