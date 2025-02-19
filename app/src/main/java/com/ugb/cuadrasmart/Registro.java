package com.ugb.cuadrasmart;

public class Registro {
    private int id;
    private String fecha;
    private String horaInicio;
    private String horaCierre;
    private int numeroCaja;
    private String cajero;
    private double billetes;
    private double monedas;
    private double cheques;
    private double discrepancia;
    private String justificacion;
    private String evidencia; // URI (en formato String) de la imagen de la justificación

    public Registro(int id, String fecha, String horaInicio, String horaCierre, int numeroCaja, String cajero,
                    double billetes, double monedas, double cheques, double discrepancia,
                    String justificacion, String evidencia) {
        this.id = id;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaCierre = horaCierre;
        this.numeroCaja = numeroCaja;
        this.cajero = cajero;
        this.billetes = billetes;
        this.monedas = monedas;
        this.cheques = cheques;
        this.discrepancia = discrepancia;
        this.justificacion = justificacion;
        this.evidencia = evidencia;
    }

    // Getters
    public int getId() { return id; }
    public String getFecha() { return fecha; }
    public String getHoraInicio() { return horaInicio; }
    public String getHoraCierre() { return horaCierre; }
    public int getNumeroCaja() { return numeroCaja; }
    public String getCajero() { return cajero; }
    public double getBilletes() { return billetes; }
    public double getMonedas() { return monedas; }
    public double getCheques() { return cheques; }
    public double getDiscrepancia() { return discrepancia; }
    public String getJustificacion() { return justificacion; }
    public String getEvidencia() { return evidencia; }

    // Método para determinar si el cajero "cuadro" o "no cuadro"
    public String getEstado() {
        return (Math.abs(discrepancia) <= 1) ? "Cuadro" : "No Cuadro";
    }
}