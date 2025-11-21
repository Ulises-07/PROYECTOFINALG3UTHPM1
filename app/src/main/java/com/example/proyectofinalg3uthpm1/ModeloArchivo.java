package com.example.proyectofinalg3uthpm1;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ModeloArchivo {

    private String idUsuarioEmisor;
    private String nombreEmisor;
    private String urlArchivo;
    private String nombreArchivo;
    private String tipoArchivo;
    private String idGrupoDestino;
    private String idUsuarioDestino;
    @ServerTimestamp
    private Date fechaEnvio;

    private String documentId;

    public ModeloArchivo() {
    }

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
