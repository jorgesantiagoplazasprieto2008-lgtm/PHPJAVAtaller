package com.svis.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Encuesta {
    private int id;
    private String titulo;
    private String descripcion;
    private String estado; // BORRADOR, ACTIVA, CERRADA
    private Timestamp creadoEn;
    private Timestamp cerradoEn;
    private List<Opcion> opciones = new ArrayList<>();

    public Encuesta() {}

    public Encuesta(int id, String titulo, String descripcion, String estado) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Timestamp getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Timestamp creadoEn) { this.creadoEn = creadoEn; }

    public Timestamp getCerradoEn() { return cerradoEn; }
    public void setCerradoEn(Timestamp cerradoEn) { this.cerradoEn = cerradoEn; }

    public List<Opcion> getOpciones() { return opciones; }
    public void setOpciones(List<Opcion> opciones) { this.opciones = opciones; }
}
