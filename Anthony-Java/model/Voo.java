package com.airline.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Voo {
    private String numeroVoo; // Ex: "AZ123"
    private String origem;
    private String destino;
    private LocalDateTime horarioPartida;
    private LocalDateTime horarioChegada;
    private int capacidadeTotal;
    private int assentosReservados;
    private double preco;

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Construtor
    public Voo(String numeroVoo, String origem, String destino, LocalDateTime horarioPartida, LocalDateTime horarioChegada, int capacidadeTotal, double preco) {
        if (numeroVoo == null || numeroVoo.trim().isEmpty()) throw new IllegalArgumentException("Número do voo não pode ser vazio.");
        if (origem == null || origem.trim().isEmpty()) throw new IllegalArgumentException("Origem não pode ser vazia.");
        if (destino == null || destino.trim().isEmpty()) throw new IllegalArgumentException("Destino não pode ser vazio.");
        if (horarioPartida == null || horarioChegada == null || horarioChegada.isBefore(horarioPartida)) throw new IllegalArgumentException("Horários inválidos.");
        if (capacidadeTotal <= 0) throw new IllegalArgumentException("Capacidade deve ser positiva.");
        if (preco < 0) throw new IllegalArgumentException("Preço não pode ser negativo.");

        this.numeroVoo = numeroVoo.toUpperCase();
        this.origem = origem;
        this.destino = destino;
        this.horarioPartida = horarioPartida;
        this.horarioChegada = horarioChegada;
        this.capacidadeTotal = capacidadeTotal;
        this.preco = preco;
        this.assentosReservados = 0; // Começa vazio
    }

    // Getters
    public String getNumeroVoo() { return numeroVoo; }
    public String getOrigem() { return origem; }
    public String getDestino() { return destino; }
    public LocalDateTime getHorarioPartida() { return horarioPartida; }
    public LocalDateTime getHorarioChegada() { return horarioChegada; }
    public int getCapacidadeTotal() { return capacidadeTotal; }
    public int getAssentosReservados() { return assentosReservados; }
    public double getPreco() { return preco; }
    public int getAssentosDisponiveis() { return capacidadeTotal - assentosReservados; }

    // Setters (com cuidado, alguns podem não ser necessários ou ter regras)
    public void setHorarioPartida(LocalDateTime horarioPartida) { this.horarioPartida = horarioPartida; }
    public void setHorarioChegada(LocalDateTime horarioChegada) { this.horarioChegada = horarioChegada; }
    public void setPreco(double preco) { this.preco = preco; }
    // Capacidade e Número do Voo geralmente não mudam após criado

    // Métodos para reserva
    public boolean reservarAssento() {
        if (getAssentosDisponiveis() > 0) {
            this.assentosReservados++;
            return true;
        }
        return false; // Sem assentos
    }

    public boolean cancelarReservaAssento() {
        if (this.assentosReservados > 0) {
            this.assentosReservados--;
            return true;
        }
        return false; // Não deveria acontecer se a reserva existe
    }

    // Métodos de formatação para exibição
    public String getHorarioPartidaFormatado() {
        return horarioPartida.format(DATE_TIME_FORMATTER);
    }
    public String getHorarioChegadaFormatado() {
        return horarioChegada.format(DATE_TIME_FORMATTER);
    }


    @Override
    public String toString() {
        return String.format("%s: %s -> %s (%s)",
                numeroVoo, origem, destino, getHorarioPartidaFormatado());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Voo voo = (Voo) o;
        return numeroVoo.equals(voo.numeroVoo); // Chave primária é o número do voo
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroVoo);
    }
}