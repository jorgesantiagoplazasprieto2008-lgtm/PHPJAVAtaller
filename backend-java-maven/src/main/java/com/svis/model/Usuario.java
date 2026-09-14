package com.svis.model;

import java.sql.Timestamp;

public class Usuario {
    private int id;
    private String documento;
    private String nombreCompleto;
    private String correo;
    private String rol;
    private Timestamp creadoEn;

    public Usuario() {}

    public Usuario(int id, String documento, String nombreCompleto, String correo, String rol) {
        this.id = id;
        this.documento = documento;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.rol = rol;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Timestamp getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Timestamp creadoEn) { this.creadoEn = creadoEn; }
}
