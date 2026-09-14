package com.svis.model;

import java.sql.Timestamp;

public class TokenOTP {
    private int id;
    private int encuestaId;
    private int usuarioId;
    private String token;
    private String estado; // DISPONIBLE, USADO
    private Timestamp expiraEn;
    private Timestamp usadoEn;
    private Timestamp creadoEn;

    // Campos auxiliares para presentación administrativa en padrón
    private String usuarioNombre;
    private String usuarioDocumento;

    public TokenOTP() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEncuestaId() { return encuestaId; }
    public void setEncuestaId(int encuestaId) { this.encuestaId = encuestaId; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Timestamp getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Timestamp expiraEn) { this.expiraEn = expiraEn; }

    public Timestamp getUsadoEn() { return usadoEn; }
    public void setUsadoEn(Timestamp usadoEn) { this.usadoEn = usadoEn; }

    public Timestamp getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Timestamp creadoEn) { this.creadoEn = creadoEn; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public String getUsuarioDocumento() { return usuarioDocumento; }
    public void setUsuarioDocumento(String usuarioDocumento) { this.usuarioDocumento = usuarioDocumento; }
}
