package com.airline.model;

import java.util.Objects;

public class Passageiro {
    private int id; // Gerado automaticamente
    private String nome;
    private String email;
    private String telefone;

    // Construtor (ID será definido pelo serviço)
    public Passageiro(int id, String nome, String email, String telefone) {
        if (nome == null || nome.trim().isEmpty()) throw new IllegalArgumentException("Nome não pode ser vazio.");
        // Validações simples de email/telefone podem ser adicionadas
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }

    // Getters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }

    // Setters
    public void setNome(String nome) { this.nome = nome; }
    public void setEmail(String email) { this.email = email; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    @Override
    public String toString() {
        return String.format("ID: %d, Nome: %s, Email: %s", id, nome, email);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passageiro that = (Passageiro) o;
        return id == that.id; // ID é a chave única
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}