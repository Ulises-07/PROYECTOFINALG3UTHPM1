package com.example.proyectofinalg3uthpm1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ActividadPrincipal extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Toolbar barraHerramientas;
    private RecyclerView listaGruposRecyclerView;
    private FloatingActionButton botonFlotanteAgregar;

    private AdaptadorGrupos adaptadorGrupos;
    private List<ModeloGrupo> listaDeGrupos;

    private ListenerRegistration oyenteDeGrupos;

    private TextView textoNombreUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_principal);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            irAInicioSesion();
            return;
        }

        db = FirebaseFirestore.getInstance();

        barraHerramientas = findViewById(R.id.toolbarPrincipal);
        setSupportActionBar(barraHerramientas);
        getSupportActionBar().setTitle("Mis Grupos");

        listaGruposRecyclerView = findViewById(R.id.listaFeedArchivos);
        botonFlotanteAgregar = findViewById(R.id.botonFlotanteAgregar);
        textoNombreUsuario = findViewById(R.id.textoNombreUsuario);

        listaDeGrupos = new ArrayList<>();
        adaptadorGrupos = new AdaptadorGrupos(this, listaDeGrupos, grupo -> {
            Intent intent = new Intent(ActividadPrincipal.this, ActividadDetalleGrupo.class);
            intent.putExtra("idGrupo", grupo.getDocumentId());
            intent.putExtra("nombreGrupo", grupo.getNombreGrupo());
            startActivity(intent);
        });

        listaGruposRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaGruposRecyclerView.setAdapter(adaptadorGrupos);

        botonFlotanteAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(ActividadPrincipal.this, ActividadCrearGrupo.class);
            startActivity(intent);
        });

    }



    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            cargarDatosUsuario(currentUser.getUid());
            cargarMisGrupos(currentUser.getUid());
        } else {
            irAInicioSesion();
        }
    }

    private void cargarDatosUsuario(String uid) {
        db.collection("Usuarios").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("nombreCompleto");
                        if (nombre != null && !nombre.isEmpty()) {
                            textoNombreUsuario.setText("Hola, " + nombre);
                        } else {
                            textoNombreUsuario.setText("Bienvenido/a");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    textoNombreUsuario.setText("Bienvenido/a");
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (oyenteDeGrupos != null) {
            oyenteDeGrupos.remove();
            oyenteDeGrupos = null;
        }
    }

    private void cargarMisGrupos(String uid) {
        if (oyenteDeGrupos != null) {
            oyenteDeGrupos.remove();
        }

        Log.d("CargarGrupos", "Iniciando carga de grupos para el UID: " + uid);
        oyenteDeGrupos = db.collection("Grupos")
                .whereArrayContains("miembros", uid)
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
                        Log.d("CargarGrupos", "Carga completa. Se encontraron " + listaDeGrupos.size() + " grupos.");
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
            startActivity(new Intent(ActividadPrincipal.this, ActividadPerfilUsuario.class));
            return true;
        } else if (id == R.id.menu_buscar) {
            startActivity(new Intent(ActividadPrincipal.this, ActividadBusqueda.class));
            return true;
        } else if (id == R.id.menu_grupos) {
            startActivity(new Intent(ActividadPrincipal.this, ActividadCrearGrupo.class));
            return true;
        } else if (id == R.id.menu_cerrar_sesion) {
            if (oyenteDeGrupos != null) {
                oyenteDeGrupos.remove();
            }
            mAuth.signOut();
            irAInicioSesion();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void irAInicioSesion() {
        if (!isFinishing()) {
            Intent intent = new Intent(ActividadPrincipal.this, ActividadInicioSesion.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}
