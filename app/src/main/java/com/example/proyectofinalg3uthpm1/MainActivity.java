package com.example.proyectofinalg3uthpm1;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/*
 * MainActivity es la actividad lanzadora (LAUNCHER).
 * Su ÚNICA función es verificar si el usuario ya ha iniciado sesión (persistencia).
 * Si SÍ ha iniciado sesión, lo redirige a ActividadPrincipal.
 * Si NO ha iniciado sesión, lo redirige a ActividadInicioSesion.
 */
public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // No necesitamos un layout (setContentView) porque esta actividad solo redirige.

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Comprobar si el usuario ya está autenticado
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        actualizarUI(usuarioActual);
    }

    private void actualizarUI(FirebaseUser usuario) {
        if (usuario != null) {
            // El usuario SÍ está autenticado
            // (Aquí podrías añadir la lógica de verificación de correo)
            // if(usuario.isEmailVerified()){ ... }

            Intent intent = new Intent(MainActivity.this, ActividadPrincipal.class);
            startActivity(intent);
            finish(); // Cierra MainActivity para que el usuario no pueda volver atrás
        } else {
            // El usuario NO está autenticado
            Intent intent = new Intent(MainActivity.this, ActividadInicioSesion.class);
            startActivity(intent);
            finish(); // Cierra MainActivity
        }
    }
}