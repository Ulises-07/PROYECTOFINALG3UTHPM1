package com.example.proyectofinalg3uthpm1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ActividadDetalleGrupo extends AppCompatActivity {

    private FirebaseFirestore db;

    private Toolbar toolbar;
    private RecyclerView listaArchivosRecyclerView;
    private FloatingActionButton botonSubirArchivo;
    private TextView textoSinArchivos;

    private AdaptadorArchivos adaptadorArchivos;
    private List<ModeloArchivo> listaArchivos;

    private String idGrupoActual;
    private String nombreGrupoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_detalle_grupo);

        // Obtener datos del Intent
        idGrupoActual = getIntent().getStringExtra("idGrupo");
        nombreGrupoActual = getIntent().getStringExtra("nombreGrupo");

        if (idGrupoActual == null) {
            Toast.makeText(this, "Error al cargar grupo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configurar Firebase
        db = FirebaseFirestore.getInstance();

        // Vistas
        toolbar = findViewById(R.id.toolbarDetalleGrupo);
        listaArchivosRecyclerView = findViewById(R.id.listaArchivosGrupo);
        botonSubirArchivo = findViewById(R.id.botonSubirArchivoGrupo);
        textoSinArchivos = findViewById(R.id.textoSinArchivos);

        // Configurar Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(nombreGrupoActual);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Configurar Recycler y Adaptador (reusamos AdaptadorArchivos)
        listaArchivos = new ArrayList<>();
        adaptadorArchivos = new AdaptadorArchivos(this, listaArchivos);
        listaArchivosRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaArchivosRecyclerView.setAdapter(adaptadorArchivos);

        // Cargar Archivos del Grupo
        cargarArchivosDelGrupo();

        // Botón para subir archivo a ESTE grupo
        botonSubirArchivo.setOnClickListener(v -> {
            Intent intent = new Intent(ActividadDetalleGrupo.this, ActividadEnviarArchivo.class);
            // IMPORTANTE: Pasamos el ID del grupo para que se guarde correctamente
            intent.putExtra("idGrupoDestino", idGrupoActual);
            startActivity(intent);
        });
    }

    private void cargarArchivosDelGrupo() {
        db.collection("Archivos")
                .whereEqualTo("idGrupoDestino", idGrupoActual) // FILTRO CLAVE
                .orderBy("fechaEnvio", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("DetalleGrupo", "Error al cargar archivos", e);
                        return;
                    }

                    if (snapshots != null) {
                        listaArchivos.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ModeloArchivo archivo = doc.toObject(ModeloArchivo.class);
                            if (archivo != null) {
                                archivo.setDocumentId(doc.getId());
                                listaArchivos.add(archivo);
                            }
                        }

                        adaptadorArchivos.notifyDataSetChanged();

                        // Mostrar/Ocultar mensaje de vacío
                        if (listaArchivos.isEmpty()) {
                            textoSinArchivos.setVisibility(View.VISIBLE);
                        } else {
                            textoSinArchivos.setVisibility(View.GONE);
                        }
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}