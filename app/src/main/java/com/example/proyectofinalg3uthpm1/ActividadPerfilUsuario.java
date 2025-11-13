package com.example.proyectofinalg3uthpm1;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide; // Importar Glide
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Opcional: Para imagen circular
import de.hdodenhof.circleimageview.CircleImageView;

public class ActividadPerfilUsuario extends AppCompatActivity {

    // Constante para el selector de imágenes
    private static final int CODIGO_SELECCION_IMAGEN = 101;

    // Vistas
    private CircleImageView imagenPerfil;
    private EditText campoNombrePerfil, campoCorreoPerfil, campoCarreraPerfil, campoFechaNacimiento;
    private Button botonGuardarCambios, botonElegirFecha;
    private ProgressBar barraProgresoPerfil;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser usuarioActual;
    private DocumentReference docRefUsuario;

    // Variables
    private Uri uriImagenSeleccionada;
    private String urlFotoPerfilActual;
    private final Calendar calendario = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_perfil_usuario);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        usuarioActual = mAuth.getCurrentUser();

        if (usuarioActual == null) {
            // No debería pasar si la app está bien, pero por seguridad
            finish();
            return;
        }

        // Referencia al documento del usuario
        docRefUsuario = db.collection("Usuarios").document(usuarioActual.getUid());

        // Enlazar Vistas
        imagenPerfil = findViewById(R.id.imagenPerfil);
        campoNombrePerfil = findViewById(R.id.campoNombrePerfil);
        campoCorreoPerfil = findViewById(R.id.campoCorreoPerfil);
        campoCarreraPerfil = findViewById(R.id.campoCarreraPerfil);
        campoFechaNacimiento = findViewById(R.id.campoFechaNacimiento);
        botonGuardarCambios = findViewById(R.id.botonGuardarCambios);
        botonElegirFecha = findViewById(R.id.botonElegirFecha);
        barraProgresoPerfil = findViewById(R.id.barraProgresoPerfil);

        // El correo no se puede editar (es el identificador)
        campoCorreoPerfil.setEnabled(false);

        // Cargar datos del usuario
        cargarDatosUsuario();

        // Configurar DatePickerDialog
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendario.set(Calendar.YEAR, year);
                calendario.set(Calendar.MONTH, month);
                calendario.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                actualizarCampoFecha();
            }
        };

        botonElegirFecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(ActividadPerfilUsuario.this, dateSetListener,
                        calendario.get(Calendar.YEAR),
                        calendario.get(Calendar.MONTH),
                        calendario.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // Configurar clic en imagen de perfil
        imagenPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirSelectorDeImagen();
            }
        });

        // Configurar clic en guardar
        botonGuardarCambios.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarCambiosPerfil();
            }
        });
    }

    private void actualizarCampoFecha() {
        String formato = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(formato, Locale.US);
        campoFechaNacimiento.setText(sdf.format(calendario.getTime()));
    }

    private void cargarDatosUsuario() {
        barraProgresoPerfil.setVisibility(View.VISIBLE);
        docRefUsuario.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                barraProgresoPerfil.setVisibility(View.GONE);
                if (documentSnapshot.exists()) {
                    // Cargar datos en los campos (nombres en español)
                    campoNombrePerfil.setText(documentSnapshot.getString("nombreCompleto"));
                    campoCorreoPerfil.setText(documentSnapshot.getString("correo"));
                    campoCarreraPerfil.setText(documentSnapshot.getString("carrera"));

                    // Cargar fecha de nacimiento (Timestamp de Firestore)
                    if (documentSnapshot.getTimestamp("fechaNacimiento") != null) {
                        calendario.setTime(documentSnapshot.getTimestamp("fechaNacimiento").toDate());
                        actualizarCampoFecha();
                    }

                    // Cargar foto de perfil
                    urlFotoPerfilActual = documentSnapshot.getString("urlFotoPerfil");
                    if (urlFotoPerfilActual != null && !urlFotoPerfilActual.isEmpty()) {
                        Glide.with(ActividadPerfilUsuario.this)
                                .load(urlFotoPerfilActual)
                                .placeholder(R.drawable.ic_profile_placeholder);
                                //.into(imagenPerfil);
                    }
                } else {
                    Toast.makeText(ActividadPerfilUsuario.this, "No se encontraron datos de perfil.", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                barraProgresoPerfil.setVisibility(View.GONE);
                Toast.makeText(ActividadPerfilUsuario.this, "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void abrirSelectorDeImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, CODIGO_SELECCION_IMAGEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODIGO_SELECCION_IMAGEN && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uriImagenSeleccionada = data.getData();
            // Mostrar la imagen seleccionada en el ImageView
            Glide.with(this).load(uriImagenSeleccionada).into(imagenPerfil);
        }
    }

    private void guardarCambiosPerfil() {
        String nombre = campoNombrePerfil.getText().toString().trim();
        String carrera = campoCarreraPerfil.getText().toString().trim();

        if (nombre.isEmpty()) {
            campoNombrePerfil.setError("El nombre es obligatorio.");
            campoNombrePerfil.requestFocus();
            return;
        }

        if (carrera.isEmpty()) {
            campoCarreraPerfil.setError("La carrera es obligatoria.");
            campoCarreraPerfil.requestFocus();
            return;
        }

        barraProgresoPerfil.setVisibility(View.VISIBLE);
        botonGuardarCambios.setEnabled(false);

        // Comprobar si se seleccionó una nueva imagen
        if (uriImagenSeleccionada != null) {
            // 1. Subir la nueva imagen
            subirNuevaImagenYActualizarPerfil(nombre, carrera);
        } else {
            // 2. Solo actualizar los datos de texto
            actualizarDatosTextoPerfil(nombre, carrera, urlFotoPerfilActual);
        }
    }

    private void subirNuevaImagenYActualizarPerfil(String nombre, String carrera) {
        // Ruta en Storage: fotos_perfil/{uid}.jpg
        StorageReference refFotoPerfil = storage.getReference()
                .child("fotos_perfil")
                .child(usuarioActual.getUid() + ".jpg");

        refFotoPerfil.putFile(uriImagenSeleccionada)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Imagen subida, obtener URL de descarga
                        refFotoPerfil.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String nuevaUrlImagen = uri.toString();
                                // Actualizar perfil con la nueva URL
                                actualizarDatosTextoPerfil(nombre, carrera, nuevaUrlImagen);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        barraProgresoPerfil.setVisibility(View.GONE);
                        botonGuardarCambios.setEnabled(true);
                        Toast.makeText(ActividadPerfilUsuario.this, "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void actualizarDatosTextoPerfil(String nombre, String carrera, String urlImagen) {
        // Campos en español
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombreCompleto", nombre);
        datos.put("carrera", carrera);
        datos.put("urlFotoPerfil", urlImagen);
        datos.put("fechaNacimiento", calendario.getTime()); // Guardar como Timestamp

        docRefUsuario.set(datos, SetOptions.merge()) // SetOptions.merge() actualiza solo estos campos
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        barraProgresoPerfil.setVisibility(View.GONE);
                        botonGuardarCambios.setEnabled(true);
                        Toast.makeText(ActividadPerfilUsuario.this, "Perfil actualizado.", Toast.LENGTH_SHORT).show();
                        finish(); // Volver a la actividad principal
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        barraProgresoPerfil.setVisibility(View.GONE);
                        botonGuardarCambios.setEnabled(true);
                        Toast.makeText(ActividadPerfilUsuario.this, "Error al guardar perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
