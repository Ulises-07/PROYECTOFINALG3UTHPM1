package com.example.proyectofinalg3uthpm1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ActividadPrincipal extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser usuarioActual;

    // Vistas
    private Toolbar barraHerramientas;
    private RecyclerView listaGruposRecyclerView; // Cambiado nombre para claridad
    private FloatingActionButton botonFlotanteAgregar;
    private TextView textoVacio; // Para mostrar si no hay grupos

    // Adaptador para GRUPOS
    private AdaptadorGrupos adaptadorGrupos;
    private List<ModeloGrupo> listaDeGrupos;

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
        getSupportActionBar().setTitle("Mis Grupos"); // Cambiado título

        // Enlazar Vistas
        listaGruposRecyclerView = findViewById(R.id.listaFeedArchivos); // Reusamos el ID del XML existente o cámbialo
        botonFlotanteAgregar = findViewById(R.id.botonFlotanteAgregar);

        // Es recomendable agregar un TextView en tu XML (actividad_principal.xml) con id emptyView
        // Si no lo tienes, puedes ignorar esta línea, pero ayuda a la UX
        // textoVacio = findViewById(R.id.emptyView);

        if (usuarioActual == null) {
            irAInicioSesion();
            return;
        }

        // Configurar RecyclerView para GRUPOS
        listaDeGrupos = new ArrayList<>();
        adaptadorGrupos = new AdaptadorGrupos(this, listaDeGrupos, grupo -> {
            // AL HACER CLIC EN UN GRUPO:
            Intent intent = new Intent(ActividadPrincipal.this, ActividadDetalleGrupo.class);
            intent.putExtra("idGrupo", grupo.getDocumentId());
            intent.putExtra("nombreGrupo", grupo.getNombreGrupo());
            startActivity(intent);
        });

        listaGruposRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaGruposRecyclerView.setAdapter(adaptadorGrupos);

        // Cargar los grupos
        cargarMisGrupos();

        // Configurar botón flotante (Crear nuevo grupo)
        botonFlotanteAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(ActividadPrincipal.this, ActividadCrearGrupo.class);
            startActivity(intent);
        });
    }

    private void cargarMisGrupos() {
        // Consulta: Traer grupos donde el array "miembros" contiene mi UID
        db.collection("Grupos")
                .whereArrayContains("miembros", usuarioActual.getUid())
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("ActividadPrincipal", "Error al cargar grupos.", e);
                        return;
                    }

                    if (snapshots != null) {
                        listaDeGrupos.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ModeloGrupo grupo = doc.toObject(ModeloGrupo.class);
                            if (grupo != null) {
                                grupo.setDocumentId(doc.getId());
                                listaDeGrupos.add(grupo);
                            }
                        }
                        adaptadorGrupos.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_perfil) {
            Intent intent = new Intent(ActividadPrincipal.this, ActividadPerfilUsuario.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_buscar) {
            Intent intent = new Intent(ActividadPrincipal.this, ActividadBusqueda.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_grupos) {
            // Ya estamos en grupos, o podemos ir a crear
            Intent intent = new Intent(ActividadPrincipal.this, ActividadCrearGrupo.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_cerrar_sesion) {
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