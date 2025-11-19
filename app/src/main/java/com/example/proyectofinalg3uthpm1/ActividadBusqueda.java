package com.example.proyectofinalg3uthpm1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ActividadBusqueda extends AppCompatActivity {

    private static final String TAG = "ActividadBusqueda";

    // Firebase
    private FirebaseFirestore db;
    private FirebaseUser usuarioActual;

    // Vistas
    private Toolbar toolbar;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    // Variables
    private AdaptadorUsuario adaptadorUsuario;
    private List<ModeloUsuario> listaUsuarios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_busqueda);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        usuarioActual = FirebaseAuth.getInstance().getCurrentUser();

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbarBusqueda);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Buscar Compañeros");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Flecha de retroceso

        // Enlazar Vistas
        searchView = findViewById(R.id.searchViewUsuarios);
        recyclerView = findViewById(R.id.listaResultadosBusqueda);
        progressBar = findViewById(R.id.barraProgresoBusqueda);

        // Configurar RecyclerView
        listaUsuarios = new ArrayList<>();
        // Implementamos el listener del adaptador
        adaptadorUsuario = new AdaptadorUsuario(this, listaUsuarios, (usuario, boton) -> {
            // Clic en "Agregar"
            agregarCompanero(usuario, boton);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adaptadorUsuario);

        // Configurar SearchView
        configurarSearchView();
    }

    private void configurarSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // El usuario presionó "Enter"
                buscarUsuarios(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Opcional: buscar mientras escribe (puede gastar muchas lecturas de Firestore)
                // if (newText.length() > 3) {
                //     buscarUsuarios(newText);
                // }
                return false;
            }
        });
    }

    private void buscarUsuarios(String consultaCorreo) {
        if (consultaCorreo.isEmpty()) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        listaUsuarios.clear();
        adaptadorUsuario.notifyDataSetChanged();

        // REQUERIMIENTO: Motor de búsqueda para encontrar estudiantes
        // Búsqueda por rango de correo.
        // En tu método buscarUsuarios()
        db.collection("Usuarios")
                .whereEqualTo("correo", consultaCorreo.toLowerCase())
                .get()
                .addOnCompleteListener(task -> { // Usando una expresión lambda para más claridad
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(ActividadBusqueda.this, "No se encontraron usuarios.", Toast.LENGTH_SHORT).show();
                        } else {
                            // --- APLICA LA SOLUCIÓN AQUÍ ---
                            // 1. Limpia la lista antes de añadir nuevos resultados
                            listaUsuarios.clear();
                            // 2. Itera sobre los documentos, no sobre los objetos ya convertidos
                            for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                // 3. Convierte el documento a un objeto ModeloUsuario
                                ModeloUsuario usuario = document.toObject(ModeloUsuario.class);
                                if (usuario != null) {
                                    // 4. Asigna el ID del documento al objeto
                                    usuario.setUid(document.getId());

                                    // 5. Añade a la lista si no es el usuario actual
                                    if (!usuario.getCorreo().equals(usuarioActual.getEmail())) {
                                        listaUsuarios.add(usuario);
                                    }
                                }
                            }
                            adaptadorUsuario.notifyDataSetChanged();
                        }
                    } else {
                        Log.w(TAG, "Error al buscar usuarios: ", task.getException());
                        Toast.makeText(ActividadBusqueda.this, "Error al buscar.", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void agregarCompanero(ModeloUsuario usuario, Button boton) {
        // REQUERIMIENTO: Agregar como compañeros
        boton.setEnabled(false); // Deshabilitar botón para evitar doble clic
        boton.setText("Agregado");

        // Usamos la colección "Compañeros" como en ESTRUCTURA_FIREBASE.md
        DocumentReference refMiLista = db.collection("Compañeros").document(usuarioActual.getUid());

        // FieldValue.arrayUnion() asegura que no se añadan duplicados
        refMiLista.update("listaCompañeros", FieldValue.arrayUnion(usuario.getUid()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ActividadBusqueda.this, "Añadido como compañero.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Si falla, probablemente el documento no existe, hay que crearlo.
                    if (e.getMessage().contains("No document to update")) {
                        // Creamos el documento
                        List<String> primeraLista = new ArrayList<>();
                        primeraLista.add(usuario.getUid());
                        db.collection("Compañeros").document(usuarioActual.getUid())
                                .set(new java.util.HashMap<String, Object>() {{
                                    put("listaCompañeros", primeraLista);
                                }})
                                .addOnSuccessListener(v -> Toast.makeText(ActividadBusqueda.this, "Añadido como compañero.", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e2 -> Toast.makeText(ActividadBusqueda.this, "Error al añadir: " + e2.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        // Otro error
                        Toast.makeText(ActividadBusqueda.this, "Error al añadir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // (Opcional: añadirnos a la lista del otro usuario también)
        // DocumentReference refSuLista = db.collection("Compañeros").document(usuario.getUid());
        // refSuLista.update("listaCompañeros", FieldValue.arrayUnion(usuarioActual.getUid()));
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Maneja el clic en la flecha de retroceso
        finish();
        return true;
    }
}
