package com.example.proyectofinalg3uthpm1;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class ModeloGrupo {

    private String nombreGrupo;
    private String idCreador;
    private List<String> miembros;
    @ServerTimestamp
    private Date fechaCreacion;

    private String documentId;

    public ModeloGrupo() {
    }

    public ModeloGrupo(String nombreGrupo, String idCreador, List<String> miembros) {
        this.nombreGrupo = nombreGrupo;
        this.idCreador = idCreador;
        this.miembros = miembros;
    }


    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public String getIdCreador() {
        return idCreador;
    }

    public List<String> getMiembros() {
        return miembros;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public String getDocumentId() {
        return documentId;
    }


    public void setNombreGrupo(String nombreGrupo) {
        this.nombreGrupo = nombreGrupo;
    }

    public void setIdCreador(String idCreador) {
        this.idCreador = idCreador;
    }

    public void setMiembros(List<String> miembros) {
        this.miembros = miembros;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
