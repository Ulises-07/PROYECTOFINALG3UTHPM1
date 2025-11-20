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
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        db.collection("Usuarios")
                .whereEqualTo("correo", consultaCorreo.toLowerCase())
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(ActividadBusqueda.this, "No se encontraron usuarios.", Toast.LENGTH_SHORT).show();
                        } else {
                            listaUsuarios.clear();
                            for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                ModeloUsuario usuario = document.toObject(ModeloUsuario.class);
                                if (usuario != null) {
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
        FirebaseUser usuarioConectado = FirebaseAuth.getInstance().getCurrentUser();
        if (usuarioConectado == null) {
            Toast.makeText(this, "Tu sesión ha expirado. Por favor, inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
            return;
        }

        boton.setEnabled(false);
        boton.setText("Agregando...");

        // --- CAMBIO IMPORTANTE: Ahora apuntamos a nuestro PROPIO documento en la colección 'Usuarios' ---
        DocumentReference miPerfilRef = db.collection("Usuarios").document(usuarioConectado.getUid());

        // Usamos arrayUnion para añadir el UID del nuevo compañero a nuestra lista 'companeros'
        miPerfilRef.update("companeros", FieldValue.arrayUnion(usuario.getUid()))
                .addOnSuccessListener(aVoid -> {
                    boton.setText("Agregado");
                    Toast.makeText(ActividadBusqueda.this, "Añadido como compañero.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    boton.setEnabled(true);
                    boton.setText("Agregar");
                    Log.e(TAG, "Error al añadir compañero", e);
                    Toast.makeText(ActividadBusqueda.this, "Error al agregar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    public boolean onSupportNavigateUp() {
        // Maneja el clic en la flecha de retroceso
        finish();
        return true;
    }
}