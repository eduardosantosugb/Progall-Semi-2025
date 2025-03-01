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
    private double ventasEsperadas;
    private double discrepancia;
    private String justificacion;
    private String evidencia;  // URI de la imagen (en forma de String)
    private String tienda;

    public Registro(int id, String fecha, String horaInicio, String horaCierre, int numeroCaja, String cajero,
                    double billetes, double monedas, double cheques, double ventasEsperadas, double discrepancia,
                    String justificacion, String evidencia, String tienda) {
        this.id = id;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaCierre = horaCierre;
        this.numeroCaja = numeroCaja;
        this.cajero = cajero;
        this.billetes = billetes;
        this.monedas = monedas;
        this.cheques = cheques;
        this.ventasEsperadas = ventasEsperadas;
        this.discrepancia = discrepancia;
        this.justificacion = justificacion;
        this.evidencia = evidencia;
        this.tienda = tienda;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getFecha() {
        return fecha;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public String getHoraCierre() {
        return horaCierre;
    }

    public int getNumeroCaja() {
        return numeroCaja;
    }

    public String getCajero() {
        return cajero;
    }

    public double getBilletes() {
        return billetes;
    }

    public double getMonedas() {
        return monedas;
    }

    public double getCheques() {
        return cheques;
    }

    public double getVentasEsperadas() {
        return ventasEsperadas;
    }

    public double getDiscrepancia() {
        return discrepancia;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public String getEvidencia() {
        return evidencia;
    }

    public String getTienda() {
        return tienda;
    }

    /**
     * Retorna el estado del registro basado en la discrepancia.
     * Si la discrepancia es menor o igual a 1 (en valor absoluto), se considera que "cuadro" y se muestra el monto.
     * En otro caso, se indica "No Cuadro".
     */
    public String getEstado() {
        if (Math.abs(discrepancia) <= 1) {
            return "Cuadro por " + discrepancia;
        } else {
            return "No Cuadro";
        }
    }
}
