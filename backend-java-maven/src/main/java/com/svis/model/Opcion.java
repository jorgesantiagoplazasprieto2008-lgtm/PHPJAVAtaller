package com.svis.model;

public class Opcion {
    private int id;
    private int encuestaId;
    private String textoOpcion;
    private int votos;

    public Opcion() {}

    public Opcion(int id, int encuestaId, String textoOpcion, int votos) {
        this.id = id;
        this.encuestaId = encuestaId;
        this.textoOpcion = textoOpcion;
        this.votos = votos;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEncuestaId() { return encuestaId; }
    public void setEncuestaId(int encuestaId) { this.encuestaId = encuestaId; }

    public String getTextoOpcion() { return textoOpcion; }
    public void setTextoOpcion(String textoOpcion) { this.textoOpcion = textoOpcion; }

    public int getVotos() { return votos; }
    public void setVotos(int votos) { this.votos = votos; }
}
