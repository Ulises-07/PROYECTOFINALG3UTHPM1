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

    EditText campoNombreCompleto, campoCorreo, campoClave, campoConfirmarClave, campoCarrera;
    Button botonRegistrar;
    TextView textoIrAInicio;
    ProgressBar barraProgreso;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_registro);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        campoNombreCompleto = findViewById(R.id.campoNombreCompleto);
        campoCorreo = findViewById(R.id.campoCorreoRegistro);
        campoClave = findViewById(R.id.campoClaveRegistro);
        campoConfirmarClave = findViewById(R.id.campoConfirmarClave);
        campoCarrera = findViewById(R.id.campoCarrera);
        botonRegistrar = findViewById(R.id.botonRegistrar);
        textoIrAInicio = findViewById(R.id.textoIrAInicio);
        barraProgreso = findViewById(R.id.barraProgresoRegistro);


        botonRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registrarUsuario();
            }
        });

        textoIrAInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void registrarUsuario() {
        String nombreCompleto = campoNombreCompleto.getText().toString().trim();
        String correo = campoCorreo.getText().toString().trim();
        String clave = campoClave.getText().toString().trim();
        String confirmarClave = campoConfirmarClave.getText().toString().trim();
        String carrera = campoCarrera.getText().toString().trim();

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

        barraProgreso.setVisibility(View.VISIBLE);
        botonRegistrar.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(correo, clave)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            String uid = firebaseUser.getUid();

                            firebaseUser.sendEmailVerification();

                            Map<String, Object> datosUsuario = new HashMap<>();
                            datosUsuario.put("nombreCompleto", nombreCompleto);
                            datosUsuario.put("correo", correo);
                            datosUsuario.put("carrera", carrera);
                            datosUsuario.put("fechaNacimiento", null);
                            datosUsuario.put("urlFotoPerfil", null);

                            db.collection("Usuarios").document(uid)
                                    .set(datosUsuario)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            barraProgreso.setVisibility(View.GONE);
                                            Toast.makeText(ActividadRegistro.this, "Registro exitoso. Revisa tu correo para verificar la cuenta.", Toast.LENGTH_LONG).show();

                                            mAuth.signOut();

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
                            barraProgreso.setVisibility(View.GONE);
                            botonRegistrar.setEnabled(true);
                            Toast.makeText(ActividadRegistro.this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
