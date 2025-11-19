package com.example.proyectofinalg3uthpm1;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;import android.os.Build;
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

    // --- NUEVOS LANZADORES DE ACTIVIDADES (Forma moderna) ---
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

        // Inicializar los lanzadores de resultados
        inicializarLanzadores();

        // Configurar Clics
        botonSeleccionar.setOnClickListener(v -> solicitarPermisosYContinuar());
        botonEnviar.setOnClickListener(v -> subirArchivoAFirebase());
    }

    private void inicializarLanzadores() {
        // 1. Lanzador para solicitar permisos
        lanzadorPermisos = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
            boolean concedido = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                concedido = Boolean.TRUE.equals(resultado.get(Manifest.permission.READ_MEDIA_IMAGES)) ||
                        Boolean.TRUE.equals(resultado.get(Manifest.permission.READ_MEDIA_VIDEO));
            } else {
                concedido = Boolean.TRUE.equals(resultado.get(Manifest.permission.READ_EXTERNAL_STORAGE));
            }

            if (concedido) {
                Log.d(TAG, "Permisos de lectura concedidos.");
                abrirSelectorDeArchivo(); // Si se conceden, abrimos el selector
            } else {
                Log.d(TAG, "Permisos de lectura denegados.");
                Toast.makeText(this, "Permiso necesario para seleccionar archivos.", Toast.LENGTH_LONG).show();
            }
        });

        // 2. Lanzador para el selector de archivos (reemplaza a onActivityResult)
        lanzadorSelectorArchivos = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
            if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null && resultado.getData().getData() != null) {
                uriArchivoSeleccionado = resultado.getData().getData();
                tipoArchivo = getContentResolver().getType(uriArchivoSeleccionado);
                nombreArchivo = obtenerNombreArchivo(uriArchivoSeleccionado);

                // Actualizar UI
                botonEnviar.setEnabled(true);
                if (tipoArchivo != null && tipoArchivo.startsWith("image/")) {
                    // Es imagen, mostrar vista previa
                    imagenVistaPrevia.setVisibility(View.VISIBLE);
                    textoInfoArchivo.setVisibility(View.GONE);
                    Glide.with(this).load(uriArchivoSeleccionado).into(imagenVistaPrevia);
                } else {
                    // No es imagen (PDF, Video, etc.), mostrar info
                    imagenVistaPrevia.setVisibility(View.GONE);
                    textoInfoArchivo.setVisibility(View.VISIBLE);
                    textoInfoArchivo.setText("Archivo: " + nombreArchivo);
                }
            }
        });
    }

    private void solicitarPermisosYContinuar() {
        String[] permisos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            // Para Android 13+, pedimos permisos más específicos.
            permisos = new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO};
        } else { // Versiones anteriores
            permisos = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        // Lanza la solicitud de permisos
        lanzadorPermisos.launch(permisos);
    }

    private void abrirSelectorDeArchivo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        // Permitir seleccionar imágenes, videos y PDFs
        String[] mimetypes = {"image/*", "video/*", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        // Usar el nuevo lanzador
        lanzadorSelectorArchivos.launch(intent);
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
        // Si sigue siendo nulo, ponemos un nombre genérico
        return (nombre != null) ? nombre : "archivo_desconocido";
    }

    private void subirArchivoAFirebase() {
        if (uriArchivoSeleccionado == null) {
            Toast.makeText(this, "No hay archivo seleccionado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Comprobación de seguridad para el usuario actual
        if (usuarioActual == null) {
            Toast.makeText(this, "Error de sesión. Por favor, inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
            return;
        }

        // Mostrar progreso
        barraProgreso.setVisibility(View.VISIBLE);
        barraProgreso.setIndeterminate(true);
        botonEnviar.setEnabled(false);
        botonSeleccionar.setEnabled(false);

        // 1. Subir archivo a Firebase Storage
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
                    restaurarUI("Error al subir archivo: " + e.getMessage());
                });
    }

    private void obtenerNombreUsuarioYGuardarEnFirestore(String urlDescarga) {
        // Obtenemos el nombre del perfil de Firestore
        db.collection("Usuarios").document(usuarioActual.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String nombreEmisor = usuarioActual.getEmail(); // Fallback por defecto
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Asegúrate de que el campo se llama "nombreCompleto" en tu Firestore
                        String nombreCompleto = documentSnapshot.getString("nombreCompleto");
                        if (nombreCompleto != null && !nombreCompleto.isEmpty()) {
                            nombreEmisor = nombreCompleto;
                        }
                    }
                    // 4. Guardar metadata en Firestore
                    guardarMetadataEnFirestore(urlDescarga, nombreEmisor);
                })
                .addOnFailureListener(e -> {
                    // Si falla, usamos el email como nombre y continuamos
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
        datosArchivo.put("idGrupoDestino", null); // TODO: Implementar selección de grupo
        datosArchivo.put("idUsuarioDestino", null); // TODO: Implementar selección de compañero
        datosArchivo.put("fechaEnvio", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("Archivos").add(datosArchivo)
                .addOnSuccessListener(documentReference -> {
                    // Éxito final
                    Toast.makeText(this, "Archivo enviado.", Toast.LENGTH_SHORT).show();
                    finish(); // Volver a ActividadPrincipal
                })
                .addOnFailureListener(e -> {
                    restaurarUI("Error al guardar datos: " + e.getMessage());
                });
    }

    // Método de ayuda para restaurar la UI en caso de error
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
