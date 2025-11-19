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

    // --- LANZADORES DE ACTIVIDADES (Forma moderna y correcta) ---
    private ActivityResultLauncher<String[]> lanzadorPermisos;
    private ActivityResultLauncher<Intent> lanzadorSelectorArchivos;

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Enviar Archivo");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Enlazar Vistas
        botonSeleccionar = findViewById(R.id.botonSeleccionarArchivo);
        botonEnviar = findViewById(R.id.botonEnviarArchivo);
        imagenVistaPrevia = findViewById(R.id.imagenVistaPrevia);
        textoInfoArchivo = findViewById(R.id.textoInfoArchivo);
        barraProgreso = findViewById(R.id.barraProgresoEnvio);

        // Inicializar los lanzadores de resultados de actividad
        inicializarLanzadores();

        // Configurar Clics
        botonSeleccionar.setOnClickListener(v -> solicitarPermisosYContinuar());
        botonEnviar.setOnClickListener(v -> subirArchivoAFirebase());
    }

    private void inicializarLanzadores() {
        // 1. Lanzador para la solicitud de permisos en tiempo de ejecución
        lanzadorPermisos = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
            boolean concedido;
            // Comprueba el permiso correcto según la versión de Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
                concedido = Boolean.TRUE.equals(resultado.get(Manifest.permission.READ_MEDIA_IMAGES)) ||
                        Boolean.TRUE.equals(resultado.get(Manifest.permission.READ_MEDIA_VIDEO));
            } else { // Versiones anteriores
                concedido = Boolean.TRUE.equals(resultado.get(Manifest.permission.READ_EXTERNAL_STORAGE));
            }

            if (concedido) {
                Log.d(TAG, "Permiso de lectura concedido.");
                abrirSelectorDeArchivo(); // Si se conceden los permisos, ahora sí abrimos el selector.
            } else {
                Log.d(TAG, "Permiso de lectura denegado.");
                Toast.makeText(this, "El permiso para acceder a archivos es necesario.", Toast.LENGTH_LONG).show();
            }
        });

        // 2. Lanzador para el resultado del selector de archivos (reemplaza a onActivityResult)
        lanzadorSelectorArchivos = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
            if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null && resultado.getData().getData() != null) {
                uriArchivoSeleccionado = resultado.getData().getData();
                tipoArchivo = getContentResolver().getType(uriArchivoSeleccionado);
                nombreArchivo = obtenerNombreArchivo(uriArchivoSeleccionado);

                // Actualizar la interfaz de usuario con la información del archivo seleccionado
                actualizarUIConArchivo();
            }
        });
    }

    private void solicitarPermisosYContinuar() {
        String[] permisos;
        // Elige qué permisos solicitar según la versión del SDK de Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisos = new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO};
        } else {
            permisos = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        // Lanza el diálogo para solicitar los permisos al usuario
        lanzadorPermisos.launch(permisos);
    }

    private void abrirSelectorDeArchivo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimetypes = {"image/*", "video/*", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        // Usa el nuevo lanzador para abrir el selector de archivos
        lanzadorSelectorArchivos.launch(intent);
    }

    // Este método ya no es necesario porque usamos lanzadorSelectorArchivos
    // @Override
    // protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { ... }

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

    // Helper para obtener el nombre del archivo desde la URI
    private String obtenerNombreArchivo(Uri uri) {
        String nombre = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        nombre = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al obtener el nombre del archivo", e);
            }
        }
        if (nombre == null) {
            nombre = uri.getPath();
            if (nombre != null) {
                int cut = nombre.lastIndexOf('/');
                if (cut != -1) {
                    nombre = nombre.substring(cut + 1);
                }
            }
        }
        return (nombre != null) ? nombre : "archivo_desconocido";
    }

    private void subirArchivoAFirebase() {
        if (uriArchivoSeleccionado == null) {
            Toast.makeText(this, "No hay archivo seleccionado.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (usuarioActual == null) {
            Toast.makeText(this, "Error de sesión. Por favor, inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
            return;
        }

        // Mostrar progreso
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
                .addOnFailureListener(e -> {
                    // Impresión de error detallada para depuración
                    Log.e(TAG, "Error al subir a Firebase Storage", e);
                    restaurarUI("Error al subir: " + e.getMessage());
                });
    }

    private void obtenerNombreUsuarioYGuardarEnFirestore(String urlDescarga) {
        db.collection("Usuarios").document(usuarioActual.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String nombreEmisor = usuarioActual.getEmail(); // Fallback
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String nombreCompleto = documentSnapshot.getString("nombreCompleto");
                        if(nombreCompleto != null && !nombreCompleto.isEmpty()) {
                            nombreEmisor = nombreCompleto;
                        }
                    }
                    guardarMetadataEnFirestore(urlDescarga, nombreEmisor);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "No se pudo obtener el nombre del perfil, usando email como fallback.", e);
                    guardarMetadataEnFirestore(urlDescarga, usuarioActual.getEmail());
                });
    }

    private void guardarMetadataEnFirestore(String urlDescarga, String nombreEmisor) {
        Map<String, Object> datosArchivo = new HashMap<>();
        datosArchivo.put("idUsuarioEmisor", usuarioActual.getUid());
        datosArchivo.put("nombreEmisor", nombreEmisor);
        datosArchivo.put("urlArchivo", urlDescarga);
        datosArchivo.put("nombreArchivo", nombreArchivo);
        datosArchivo.put("tipoArchivo", tipoArchivo);
        datosArchivo.put("idGrupoDestino", null);
        datosArchivo.put("idUsuarioDestino", null);
        datosArchivo.put("fechaEnvio", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("Archivos").add(datosArchivo)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Archivo enviado.", Toast.LENGTH_SHORT).show();
                    finish(); // Volver a la actividad anterior
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar metadata en Firestore", e);
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
