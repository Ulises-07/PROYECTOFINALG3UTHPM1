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

import com.google.firebase.firestore.FieldPath;
import java.util.ArrayList;
import java.util.List;

public class ActividadDetalleGrupo extends AppCompatActivity {

    private FirebaseFirestore db;

    private Toolbar toolbar;
    private RecyclerView listaArchivosRecyclerView;
    private FloatingActionButton botonSubirArchivo;
    private TextView textoSinArchivos;
    private TextView textoNombresMiembros;


    private AdaptadorArchivos adaptadorArchivos;
    private List<ModeloArchivo> listaArchivos;

    private String idGrupoActual;
    private String nombreGrupoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_detalle_grupo);

        idGrupoActual = getIntent().getStringExtra("idGrupo");
        nombreGrupoActual = getIntent().getStringExtra("nombreGrupo");

        if (idGrupoActual == null) {
            Toast.makeText(this, "Error al cargar grupo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();


        toolbar = findViewById(R.id.toolbarDetalleGrupo);
        listaArchivosRecyclerView = findViewById(R.id.listaArchivosGrupo);
        botonSubirArchivo = findViewById(R.id.botonSubirArchivoGrupo);
        textoSinArchivos = findViewById(R.id.textoSinArchivos);
        textoNombresMiembros = findViewById(R.id.textoNombresMiembros);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(nombreGrupoActual);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        listaArchivos = new ArrayList<>();
        adaptadorArchivos = new AdaptadorArchivos(this, listaArchivos);
        listaArchivosRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaArchivosRecyclerView.setAdapter(adaptadorArchivos);

        cargarArchivosDelGrupo();
        cargarNombresDeMiembros();

        botonSubirArchivo.setOnClickListener(v -> {
            Intent intent = new Intent(ActividadDetalleGrupo.this, ActividadEnviarArchivo.class);
            intent.putExtra("idGrupoDestino", idGrupoActual);
            startActivity(intent);
        });
    }

    private void cargarArchivosDelGrupo() {
        db.collection("Archivos")
                .whereEqualTo("idGrupoDestino", idGrupoActual)
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

                        if (listaArchivos.isEmpty()) {
                            textoSinArchivos.setVisibility(View.VISIBLE);
                        } else {
                            textoSinArchivos.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void cargarNombresDeMiembros() {
        db.collection("Grupos").document(idGrupoActual).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> miembrosIds = (List<String>) documentSnapshot.get("miembros");

                        if (miembrosIds != null && !miembrosIds.isEmpty()) {
                            buscarYMostrarNombres(miembrosIds);
                        } else {
                            textoNombresMiembros.setText("Este grupo no tiene miembros.");
                        }
                    } else {
                        textoNombresMiembros.setText("Información del grupo no encontrada.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DetalleGrupo", "Error al cargar la info del grupo", e);
                    textoNombresMiembros.setText("Error al cargar miembros.");
                });
    }

    private void buscarYMostrarNombres(List<String> ids) {
        db.collection("Usuarios").whereIn(FieldPath.documentId(), ids).get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> listaDeNombres = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String nombre = doc.getString("nombreCompleto");
                        if (nombre != null && !nombre.isEmpty()) {
                            listaDeNombres.add(nombre);
                        }
                    }

                    if (!listaDeNombres.isEmpty()) {
                        String nombresFormateados = String.join(", ", listaDeNombres);
                        textoNombresMiembros.setText("Miembros: " + nombresFormateados);
                    } else {
                        textoNombresMiembros.setText("No se encontraron los nombres de los miembros.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DetalleGrupo", "Error al buscar los nombres de los usuarios", e);
                    textoNombresMiembros.setText("Error al cargar nombres.");
                });
    }



    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}