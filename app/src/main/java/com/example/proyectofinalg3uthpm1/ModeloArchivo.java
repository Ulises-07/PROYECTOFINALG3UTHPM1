package com.example.proyectofinalg3uthpm1;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

// Modelo de datos para un archivo en el feed (para Firestore)
public class ModeloArchivo {

    // Nombres de campos en español (de ESTRUCTURA_FIREBASE.md)
    private String idUsuarioEmisor;
    private String nombreEmisor;
    private String urlArchivo;
    private String nombreArchivo;
    private String tipoArchivo; // "imagen", "video", "pdf"
    private String idGrupoDestino;
    private String idUsuarioDestino;
    @ServerTimestamp
    private Date fechaEnvio;

    // ID del documento (para poder borrarlo)
    private String documentId;

    // Constructor vacío (requerido por Firestore)
    public ModeloArchivo() {
    }

    // --- Getters ---
    public String getIdUsuarioEmisor() {
        return idUsuarioEmisor;
    }

    public String getNombreEmisor() {
        return nombreEmisor;
    }

    public String getUrlArchivo() {
        return urlArchivo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public String getIdGrupoDestino() {
        return idGrupoDestino;
    }

    public String getIdUsuarioDestino() {
        return idUsuarioDestino;
    }

    public Date getFechaEnvio() {
        return fechaEnvio;
    }

    public String getDocumentId() {
        return documentId;
    }

    // --- Setters ---
    // (Firestore usa los setters para poblar el objeto)
    public void setIdUsuarioEmisor(String idUsuarioEmisor) {
        this.idUsuarioEmisor = idUsuarioEmisor;
    }

    public void setNombreEmisor(String nombreEmisor) {
        this.nombreEmisor = nombreEmisor;
    }

    public void setUrlArchivo(String urlArchivo) {
        this.urlArchivo = urlArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public void setTipoArchivo(String tipoArchivo) {
        this.tipoArchivo = tipoArchivo;
    }

    public void setIdGrupoDestino(String idGrupoDestino) {
        this.idGrupoDestino = idGrupoDestino;
    }

    public void setIdUsuarioDestino(String idUsuarioDestino) {
        this.idUsuarioDestino = idUsuarioDestino;
    }

    public void setFechaEnvio(Date fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
