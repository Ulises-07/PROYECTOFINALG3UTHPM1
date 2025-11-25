package com.example.proyectofinalg3uthpm1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ActividadAnadirMiembros extends AppCompatActivity {

    private static final String TAG = "AnadirMiembros";
    private RecyclerView listaCompaneros;
    private Button botonAnadir;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String idGrupoActual;
    private AdaptadorUsuarioSeleccion adaptadorSeleccion;
    private List<ModeloUsuario> listaCompanerosNoEnGrupo = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_anadir_miembros);

        idGrupoActual = getIntent().getStringExtra("id_grupo");
        if (idGrupoActual == null || idGrupoActual.isEmpty()) {
            Toast.makeText(this, "Error: ID de grupo no encontrado.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbarAnadirMiembros);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Añadir Nuevos Miembros");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        listaCompaneros = findViewById(R.id.listaCompanerosParaAnadir);
        botonAnadir = findViewById(R.id.botonConfirmarAnadir);
        progressBar = findViewById(R.id.barraProgresoAnadir);

        adaptadorSeleccion = new AdaptadorUsuarioSeleccion(this, listaCompanerosNoEnGrupo);
        listaCompaneros.setLayoutManager(new LinearLayoutManager(this));
        listaCompaneros.setAdapter(adaptadorSeleccion);

        cargarCompanerosDisponibles();

        botonAnadir.setOnClickListener(v -> anadirMiembrosSeleccionados());
    }

    private void cargarCompanerosDisponibles() {
        String uidAdmin = FirebaseAuth.getInstance().getUid();

        db.collection("Grupos").document(idGrupoActual).get().addOnSuccessListener(grupoSnapshot -> {
            final List<String> miembrosActuales;
            if (grupoSnapshot.contains("miembros") && grupoSnapshot.get("miembros") != null) {
                miembrosActuales = (List<String>) grupoSnapshot.get("miembros");
            } else {
                miembrosActuales = new ArrayList<>();
            }


            db.collection("Usuarios").document(uidAdmin).get().addOnSuccessListener(adminSnapshot -> {
                List<String> todosMisCompaneros = (List<String>) adminSnapshot.get("companeros");
                if (todosMisCompaneros == null || todosMisCompaneros.isEmpty()) {
                    Toast.makeText(this, "No tienes compañeros para añadir.", Toast.LENGTH_SHORT).show();
                    return;
                }


                List<String> companerosParaAnadirUIDs = new ArrayList<>();
                for (String uid : todosMisCompaneros) {
                    if (!miembrosActuales.contains(uid)) {
                        companerosParaAnadirUIDs.add(uid);
                    }
                }

                if (companerosParaAnadirUIDs.isEmpty()) {
                    Toast.makeText(this, "Todos tus compañeros ya están en el grupo.", Toast.LENGTH_LONG).show();
                    return;
                }


                db.collection("Usuarios").whereIn(com.google.firebase.firestore.FieldPath.documentId(), companerosParaAnadirUIDs)
                        .get().addOnSuccessListener(usuariosSnapshot -> {
                            listaCompanerosNoEnGrupo.clear();
                            for (ModeloUsuario usuario : usuariosSnapshot.toObjects(ModeloUsuario.class)) {
                                usuario.setUid(usuariosSnapshot.getDocuments().get(listaCompanerosNoEnGrupo.size()).getId());
                                listaCompanerosNoEnGrupo.add(usuario);
                            }
                            adaptadorSeleccion.notifyDataSetChanged();
                        }).addOnFailureListener(e -> Log.e(TAG, "Error cargando perfiles", e));
            });
        }).addOnFailureListener(e -> Log.e(TAG, "Error cargando grupo", e));
    }

    private void anadirMiembrosSeleccionados() {
        List<String> nuevosMiembros = adaptadorSeleccion.getMiembrosSeleccionados();

        if (nuevosMiembros.isEmpty()) {
            Toast.makeText(this, "Debes seleccionar al menos un compañero.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        botonAnadir.setEnabled(false);

        DocumentReference grupoRef = db.collection("Grupos").document(idGrupoActual);
        grupoRef.update("miembros", FieldValue.arrayUnion(nuevosMiembros.toArray()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Miembros añadidos correctamente.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    botonAnadir.setEnabled(true);
                    Toast.makeText(this, "Error al añadir miembros: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

