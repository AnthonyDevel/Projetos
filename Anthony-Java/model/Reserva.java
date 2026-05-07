package com.airline.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Reserva {
    private int idReserva; // Gerado automaticamente
    private String numeroVoo; // FK para Voo
    private int idPassageiro; // FK para Passageiro
    private LocalDateTime dataReserva;
    private StatusReserva status;

    public enum StatusReserva { CONFIRMADA, CANCELADA }

    // Construtor
    public Reserva(int idReserva, String numeroVoo, int idPassageiro, LocalDateTime dataReserva) {
        this.idReserva = idReserva;
        this.numeroVoo = numeroVoo;
        this.idPassageiro = idPassageiro;
        this.dataReserva = dataReserva;
        this.status = StatusReserva.CONFIRMADA; // Padrão
    }

    // Getters
    public int getIdReserva() { return idReserva; }
    public String getNumeroVoo() { return numeroVoo; }
    public int getIdPassageiro() { return idPassageiro; }
    public LocalDateTime getDataReserva() { return dataReserva; }
    public StatusReserva getStatus() { return status; }

    // Setters
    public void setStatus(StatusReserva status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Reserva ID: %d, Voo: %s, Passageiro ID: %d, Status: %s",
                idReserva, numeroVoo, idPassageiro, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reserva reserva = (Reserva) o;
        return idReserva == reserva.idReserva;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idReserva);
    }
}