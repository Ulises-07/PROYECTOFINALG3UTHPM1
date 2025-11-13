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

    // Vistas
    private Toolbar toolbar;
    private EditText campoNombreGrupo;
    private RecyclerView listaCompaneros;
    private Button botonCrearGrupo;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseUser usuarioActual;

    // Variables
    private AdaptadorUsuarioSeleccion adaptadorSeleccion;
    private List<ModeloUsuario> listaDeCompaneros;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_crear_grupo);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        usuarioActual = FirebaseAuth.getInstance().getCurrentUser();

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarCrearGrupo);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Crear Nuevo Grupo");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Enlazar Vistas
        campoNombreGrupo = findViewById(R.id.campoNombreGrupo);
        listaCompaneros = findViewById(R.id.listaCompanerosParaGrupo);
        botonCrearGrupo = findViewById(R.id.botonCrearGrupo);
        progressBar = findViewById(R.id.barraProgresoGrupo);

        // Configurar RecyclerView
        listaDeCompaneros = new ArrayList<>();
        adaptadorSeleccion = new AdaptadorUsuarioSeleccion(this, listaDeCompaneros);
        listaCompaneros.setLayoutManager(new LinearLayoutManager(this));
        listaCompaneros.setAdapter(adaptadorSeleccion);

        // Cargar la lista de compañeros
        cargarCompaneros();

        // Configurar botón
        botonCrearGrupo.setOnClickListener(v -> crearGrupo());
    }

    private void cargarCompaneros() {
        // REQUERIMIENTO: Opción para crear grupos desde lista de contactos UTH
        // 1. Obtener la lista de UIDs de mis compañeros
        DocumentReference refMiLista = db.collection("Compañeros").document(usuarioActual.getUid());
        refMiLista.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> uidsCompaneros = (List<String>) documentSnapshot.get("listaCompañeros");
                if (uidsCompaneros != null && !uidsCompaneros.isEmpty()) {
                    // 2. Buscar los datos de esos compañeros en la colección "Usuarios"
                    db.collection("Usuarios")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), uidsCompaneros)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    ModeloUsuario usuario = doc.toObject(ModeloUsuario.class);
                                    usuario.setUid(doc.getId()); // Guardar el UID
                                    listaDeCompaneros.add(usuario);
                                }
                                adaptadorSeleccion.notifyDataSetChanged();
                            });
                } else {
                    Toast.makeText(this, "No tienes compañeros para agregar.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Aún no has agregado compañeros.", Toast.LENGTH_SHORT).show();
            }
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

        // Añadir al creador (nosotros) a la lista de miembros
        miembrosSeleccionados.add(usuarioActual.getUid());

        // Crear el objeto ModeloGrupo
        ModeloGrupo nuevoGrupo = new ModeloGrupo(nombreGrupo, usuarioActual.getUid(), miembrosSeleccionados);

        // Guardar en Firestore
        db.collection("Grupos").add(nuevoGrupo)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ActividadCrearGrupo.this, "Grupo '" + nombreGrupo + "' creado.", Toast.LENGTH_SHORT).show();
                    finish(); // Volver a la actividad principal
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
