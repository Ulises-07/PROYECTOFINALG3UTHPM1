package com.example.proyectofinalg3uthpm1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ActividadInicioSesion extends AppCompatActivity {

    EditText campoCorreo, campoClave;
    Button botonIniciarSesion;
    TextView textoIrARegistro, textoOlvideClave;
    ProgressBar barraProgreso;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_inicio_sesion);

        mAuth = FirebaseAuth.getInstance();

        campoCorreo = findViewById(R.id.campoCorreoInicio);
        campoClave = findViewById(R.id.campoClaveInicio);
        botonIniciarSesion = findViewById(R.id.botonIniciarSesion);
        textoIrARegistro = findViewById(R.id.textoIrARegistro);
        textoOlvideClave = findViewById(R.id.textoOlvideClave);
        barraProgreso = findViewById(R.id.barraProgresoInicio);

        botonIniciarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciarSesionUsuario();
            }
        });

        textoIrARegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActividadInicioSesion.this, ActividadRegistro.class);
                startActivity(intent);
            }
        });

        textoOlvideClave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recuperarClave();
            }
        });
    }

    private void iniciarSesionUsuario() {
        String correo = campoCorreo.getText().toString().trim();
        String clave = campoClave.getText().toString().trim();

        // Validaciones
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

        barraProgreso.setVisibility(View.VISIBLE);
        botonIniciarSesion.setEnabled(false);

        mAuth.signInWithEmailAndPassword(correo, clave)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        barraProgreso.setVisibility(View.GONE);
                        botonIniciarSesion.setEnabled(true);

                        if (task.isSuccessful()) {

                            FirebaseUser usuario = mAuth.getCurrentUser();

                            if (usuario != null && usuario.isEmailVerified()) {
                                Toast.makeText(ActividadInicioSesion.this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ActividadInicioSesion.this, ActividadPrincipal.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                mAuth.signOut();
                                Toast.makeText(ActividadInicioSesion.this, "Correo no verificado. Revisa tu bandeja de entrada.", Toast.LENGTH_LONG).show();
                            }

                        } else {
                            Toast.makeText(ActividadInicioSesion.this, "Error al iniciar sesión.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void recuperarClave() {
        String correo = campoCorreo.getText().toString().trim();

        if (correo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            campoCorreo.setError("Ingresa un correo válido para recuperar la contraseña.");
            campoCorreo.requestFocus();
            return;
        }

        barraProgreso.setVisibility(View.VISIBLE);
        mAuth.sendPasswordResetEmail(correo)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        barraProgreso.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(ActividadInicioSesion.this, "Correo de recuperación enviado.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ActividadInicioSesion.this, "Error al enviar correo para recuperacion de contraseña.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}