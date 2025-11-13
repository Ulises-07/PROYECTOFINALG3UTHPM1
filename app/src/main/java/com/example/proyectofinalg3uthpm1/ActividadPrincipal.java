package com.example.proyectofinalg3uthpm1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.util.Log;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

/*
 * Esta es la pantalla principal (feed).
 * REQUERIMIENTO: "Interfaz tipo scroll-down estilo social media".
 * Usaremos un RecyclerView para mostrar los archivos compartidos.
 */
public class ActividadPrincipal extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser usuarioActual;

    // Vistas
    private Toolbar barraHerramientas;
    private RecyclerView listaFeedArchivos;
    private FloatingActionButton botonFlotanteAgregar;

    // (Necesitarás crear un Adaptador para el RecyclerView)
    private AdaptadorArchivos adaptadorArchivos;
    private List<Archivo> listaDeArchivos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_principal);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        usuarioActual = mAuth.getCurrentUser();

        // Configurar Toolbar
        barraHerramientas = findViewById(R.id.toolbarPrincipal);
        setSupportActionBar(barraHerramientas);
        getSupportActionBar().setTitle("Feed de Archivos");

        // Enlazar Vistas
        listaFeedArchivos = findViewById(R.id.listaFeedArchivos);
        botonFlotanteAgregar = findViewById(R.id.botonFlotanteAgregar);

        if (usuarioActual == null) {
            // Si por alguna razón el usuario no está logueado, volver a inicio
            irAInicioSesion();
            return;
        }

        // Configurar RecyclerView (Paso clave)
        // 1. Inicializar listaDeArchivos = new ArrayList<>();
        listaDeArchivos = new ArrayList<>();
        // 2. Inicializar adaptadorArchivos = new AdaptadorArchivos(this, listaDeArchivos);
        daptadorArchivos = new AdaptadorArchivos(this, listaDeArchivos);
        // 3. listaFeedArchivos.setLayoutManager(new LinearLayoutManager(this));
        listaFeedArchivos.setLayoutManager(new LinearLayoutManager(this));
        // 4. listaFeedArchivos.setAdapter(adaptadorArchivos);
        listaFeedArchivos.setAdapter(adaptadorArchivos);

        // Cargar datos del feed
        cargarArchivosDelFeed();

        // Configurar botón flotante
        botonFlotanteAgregar.setOnClickListener(v -> {
            // Lógica para enviar un nuevo archivo (abrir ActividadEnviarArchivo)
            Toast.makeText(this, "Implementar envío de archivos", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ActividadPrincipal.this, ActividadEnviarArchivo.class);
            startActivity(intent);
        });
    }

    private void cargarArchivosDelFeed() {
        // Esta es la consulta principal
        // Deberás consultar la colección "Archivos"
        // y filtrar por los grupos o compañeros del usuario actual.
        // Por ahora, solo mostraremos un placeholder.

        // Ejemplo de consulta (deberás adaptarla):

        db.collection("Archivos")
                .orderBy("fechaEnvio", Query.Direction.DESCENDING)
                // .whereIn("idGrupoDestino", listaDeMisGrupos) // <-- Necesitarás obtener la lista de grupos del usuario
                .limit(50)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("ActividadPrincipal", "Error al escuchar.", e);
                        return;
                    }

                    listaDeArchivos.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Archivo archivo = doc.toObject(Archivo.class); // Necesitas crear la clase Modelo "Archivo.java"
                        listaDeArchivos.add(archivo);
                    }
                    adaptadorArchivos.notifyDataSetChanged();
                });

    }

    // --- Manejo del Menú de Opciones (Toolbar) ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Infla el menú (agrega items a la action bar)
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Maneja los clics en los items del menú
        int id = item.getItemId();

        if (id == R.id.menu_perfil) {
            // Ir a la actividad de perfil
            Intent intent = new Intent(ActividadPrincipal.this, ActividadPerfilUsuario.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_buscar) {
            // Ir a la actividad de búsqueda de usuarios
            Intent intent = new Intent(ActividadPrincipal.this, ActividadBusqueda.class);
            startActivity(intent);
            Toast.makeText(this, "Implementar Búsqueda", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.menu_grupos) {
            // Ir a la actividad de gestión de grupos
            Intent intent = new Intent(ActividadPrincipal.this, ActividadGrupos.class);
            startActivity(intent);
            Toast.makeText(this, "Implementar Grupos", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.menu_cerrar_sesion) {
            // Cerrar sesión
            mAuth.signOut();
            irAInicioSesion();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void irAInicioSesion() {
        Intent intent = new Intent(ActividadPrincipal.this, ActividadInicioSesion.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
