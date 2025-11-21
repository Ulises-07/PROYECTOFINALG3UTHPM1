package com.example.proyectofinalg3uthpm1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActividadCrearGrupo extends AppCompatActivity {

    private static final String TAG = "ActividadCrearGrupo";

    private Toolbar toolbar;
    private EditText campoNombreGrupo;
    private RecyclerView listaCompaneros;
    private Button botonCrearGrupo;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseUser usuarioActual;

    private AdaptadorUsuarioSeleccion adaptadorSeleccion;
    private List<ModeloUsuario> listaDeCompaneros;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_crear_grupo);

        db = FirebaseFirestore.getInstance();
        usuarioActual = FirebaseAuth.getInstance().getCurrentUser();

        toolbar = findViewById(R.id.toolbarCrearGrupo);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Crear Nuevo Grupo");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        campoNombreGrupo = findViewById(R.id.campoNombreGrupo);
        listaCompaneros = findViewById(R.id.listaCompanerosParaGrupo);
        botonCrearGrupo = findViewById(R.id.botonCrearGrupo);
        progressBar = findViewById(R.id.barraProgresoGrupo);

        listaDeCompaneros = new ArrayList<>();
        adaptadorSeleccion = new AdaptadorUsuarioSeleccion(this, listaDeCompaneros);
        listaCompaneros.setLayoutManager(new LinearLayoutManager(this));
        listaCompaneros.setAdapter(adaptadorSeleccion);

        cargarCompaneros();

        botonCrearGrupo.setOnClickListener(v -> crearGrupo());
    }

    private void cargarCompaneros() {
        if (usuarioActual == null) {
            Toast.makeText(this, "No se pudo verificar tu sesión.", Toast.LENGTH_LONG).show();
            return;
        }

        DocumentReference miPerfilRef = db.collection("Usuarios").document(usuarioActual.getUid());

        miPerfilRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> uidsCompaneros = (List<String>) documentSnapshot.get("companeros");

                if (uidsCompaneros != null && !uidsCompaneros.isEmpty()) {
                    db.collection("Usuarios")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), uidsCompaneros)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                listaDeCompaneros.clear();
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    ModeloUsuario usuario = doc.toObject(ModeloUsuario.class);
                                    if (usuario != null) {
                                        usuario.setUid(doc.getId());
                                        listaDeCompaneros.add(usuario);
                                    }
                                }
                                adaptadorSeleccion.notifyDataSetChanged();

                                if(listaDeCompaneros.isEmpty()){
                                    Toast.makeText(this, "No se encontraron los perfiles de tus compañeros.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error al buscar los perfiles de los compañeros", e);
                                Toast.makeText(this, "Error al cargar los perfiles de compañeros.", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(this, "Aún no has agregado compañeros.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No se pudo encontrar tu perfil de usuario.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error al obtener tu perfil", e);
            Toast.makeText(this, "Error al cargar tu lista de compañeros.", Toast.LENGTH_SHORT).show();
        });
    }


    private void crearGrupo() {
        String nombreGrupo = campoNombreGrupo.getText().toString().trim();
        List<String> miembrosSeleccionados = adaptadorSeleccion.getMiembrosSeleccionados();

        if (nombreGrupo.isEmpty()) {
            campoNombreGrupo.setError("El nombre es obligatorio.");
            campoNombreGrupo.requestFocus();
            return;
        }

        if (miembrosSeleccionados.isEmpty()) {
            Toast.makeText(this, "Debes seleccionar al menos un compañero.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        botonCrearGrupo.setEnabled(false);

        miembrosSeleccionados.add(usuarioActual.getUid());

        ModeloGrupo nuevoGrupo = new ModeloGrupo(nombreGrupo, usuarioActual.getUid(), miembrosSeleccionados);

        db.collection("Grupos").add(nuevoGrupo)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ActividadCrearGrupo.this, "Grupo '" + nombreGrupo + "' creado.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    botonCrearGrupo.setEnabled(true);
                    Toast.makeText(ActividadCrearGrupo.this, "Error al crear grupo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}