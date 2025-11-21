package com.example.proyectofinalg3uthpm1;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        actualizarUI(usuarioActual);
    }

    private void actualizarUI(FirebaseUser usuario) {
        if (usuario != null) {

            Intent intent = new Intent(MainActivity.this, ActividadPrincipal.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(MainActivity.this, ActividadInicioSesion.class);
            startActivity(intent);
            finish();
        }
    }
}