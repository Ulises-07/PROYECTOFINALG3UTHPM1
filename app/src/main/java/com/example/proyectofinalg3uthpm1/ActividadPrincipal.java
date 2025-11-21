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

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser usuarioActual;

    private Toolbar barraHerramientas;
    private RecyclerView listaGruposRecyclerView;
    private FloatingActionButton botonFlotanteAgregar;
    private TextView textoVacio;

    private AdaptadorGrupos adaptadorGrupos;
    private List<ModeloGrupo> listaDeGrupos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_principal);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        usuarioActual = mAuth.getCurrentUser();

        barraHerramientas = findViewById(R.id.toolbarPrincipal);
        setSupportActionBar(barraHerramientas);
        getSupportActionBar().setTitle("Mis Grupos");

        listaGruposRecyclerView = findViewById(R.id.listaFeedArchivos);
        botonFlotanteAgregar = findViewById(R.id.botonFlotanteAgregar);

        if (usuarioActual == null) {
            irAInicioSesion();
            return;
        }

        listaDeGrupos = new ArrayList<>();
        adaptadorGrupos = new AdaptadorGrupos(this, listaDeGrupos, grupo -> {
            Intent intent = new Intent(ActividadPrincipal.this, ActividadDetalleGrupo.class);
            intent.putExtra("idGrupo", grupo.getDocumentId());
            intent.putExtra("nombreGrupo", grupo.getNombreGrupo());
            startActivity(intent);
        });

        listaGruposRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaGruposRecyclerView.setAdapter(adaptadorGrupos);

        cargarMisGrupos();

        botonFlotanteAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(ActividadPrincipal.this, ActividadCrearGrupo.class);
            startActivity(intent);
        });
    }

    private void cargarMisGrupos() {
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