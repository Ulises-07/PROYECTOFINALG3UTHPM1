package com.example.proyectofinalg3uthpm1;

public class ModeloUsuario {

    private String nombreCompleto;
    private String correo;
    private String carrera;
    private String urlFotoPerfil;
    private String uid;

    public ModeloUsuario() {
    }

    public ModeloUsuario(String nombreCompleto, String correo, String carrera, String urlFotoPerfil) {
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.carrera = carrera;
        this.urlFotoPerfil = urlFotoPerfil;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public String getCarrera() {
        return carrera;
    }

    public String getUrlFotoPerfil() {
        return urlFotoPerfil;
    }

    public String getUid() {
        return uid;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    public void setUrlFotoPerfil(String urlFotoPerfil) {
        this.urlFotoPerfil = urlFotoPerfil;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
