package com.example.proyectofinalg3uthpm1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ActividadRegistro extends AppCompatActivity {

    // Variables de Vistas
    EditText campoNombreCompleto, campoCorreo, campoClave, campoConfirmarClave, campoCarrera;
    Button botonRegistrar;
    TextView textoIrAInicio;
    ProgressBar barraProgreso;

    // Variables de Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_registro);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Enlazar Vistas
        campoNombreCompleto = findViewById(R.id.campoNombreCompleto);
        campoCorreo = findViewById(R.id.campoCorreoRegistro);
        campoClave = findViewById(R.id.campoClaveRegistro);
        campoConfirmarClave = findViewById(R.id.campoConfirmarClave);
        campoCarrera = findViewById(R.id.campoCarrera);
        botonRegistrar = findViewById(R.id.botonRegistrar);
        textoIrAInicio = findViewById(R.id.textoIrAInicio);
        barraProgreso = findViewById(R.id.barraProgresoRegistro);

        // Configurar Listeners
        botonRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registrarUsuario();
            }
        });

        textoIrAInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navegar de vuelta a Inicio de Sesión
                finish(); // Cierra esta actividad y vuelve a la anterior
            }
        });
    }

    private void registrarUsuario() {
        // Obtener datos (en español)
        String nombreCompleto = campoNombreCompleto.getText().toString().trim();
        String correo = campoCorreo.getText().toString().trim();
        String clave = campoClave.getText().toString().trim();
        String confirmarClave = campoConfirmarClave.getText().toString().trim();
        String carrera = campoCarrera.getText().toString().trim();

        // Validaciones
        if (nombreCompleto.isEmpty()) {
            campoNombreCompleto.setError("El nombre es obligatorio.");
            campoNombreCompleto.requestFocus();
            return;
        }

        if (correo.isEmpty()) {
            campoCorreo.setError("El correo es obligatorio.");
            campoCorreo.requestFocus();
            return;
        }

        // REQUERIMIENTO: Validación de correo UTH
        if (!correo.endsWith("@uth.hn")) {
            campoCorreo.setError("Debe ser un correo institucional (@uth.hn).");
            campoCorreo.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            campoCorreo.setError("Correo no válido.");
            campoCorreo.requestFocus();
            return;
        }

        if (clave.isEmpty()) {
            campoClave.setError("La contraseña es obligatoria.");
            campoClave.requestFocus();
            return;
        }

        if (clave.length() < 6) {
            campoClave.setError("La contraseña debe tener al menos 6 caracteres.");
            campoClave.requestFocus();
            return;
        }

        if (!clave.equals(confirmarClave)) {
            campoConfirmarClave.setError("Las contraseñas no coinciden.");
            campoConfirmarClave.requestFocus();
            return;
        }

        if (carrera.isEmpty()) {
            campoCarrera.setError("La carrera es obligatoria.");
            campoCarrera.requestFocus();
            return;
        }

        // Mostrar progreso
        barraProgreso.setVisibility(View.VISIBLE);
        botonRegistrar.setEnabled(false);

        // 1. Crear usuario en Firebase Authentication
        mAuth.createUserWithEmailAndPassword(correo, clave)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Usuario creado en Auth
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            String uid = firebaseUser.getUid();

                            // REQUERIMIENTO: Enviar correo de verificación
                            firebaseUser.sendEmailVerification();

                            // 2. Guardar datos adicionales en Firestore
                            // Nombres de campos en español
                            Map<String, Object> datosUsuario = new HashMap<>();
                            datosUsuario.put("nombreCompleto", nombreCompleto);
                            datosUsuario.put("correo", correo);
                            datosUsuario.put("carrera", carrera);
                            datosUsuario.put("fechaNacimiento", null); // El usuario lo llenará después
                            datosUsuario.put("urlFotoPerfil", null); // El usuario lo llenará después

                            db.collection("Usuarios").document(uid)
                                    .set(datosUsuario)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // Éxito al guardar en Firestore
                                            barraProgreso.setVisibility(View.GONE);
                                            Toast.makeText(ActividadRegistro.this, "Registro exitoso. Revisa tu correo para verificar la cuenta.", Toast.LENGTH_LONG).show();

                                            // Desloguear al usuario para que inicie sesión (y verifique correo)
                                            mAuth.signOut();

                                            // Redirigir a Inicio de Sesión
                                            Intent intent = new Intent(ActividadRegistro.this, ActividadInicioSesion.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Error al guardar en Firestore
                                            barraProgreso.setVisibility(View.GONE);
                                            botonRegistrar.setEnabled(true);
                                            Toast.makeText(ActividadRegistro.this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_LONG).show();

                                        }
                                    });

                        } else {
                            // Error al crear usuario en Auth
                            barraProgreso.setVisibility(View.GONE);
                            botonRegistrar.setEnabled(true);
                            Toast.makeText(ActividadRegistro.this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
