package com.airline.service;

import com.airline.model.Passageiro;
import com.airline.model.Reserva;
import com.airline.model.Voo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CompanhiaAereaService {

    private final List<Voo> voos;
    private final List<Passageiro> passageiros;
    private final List<Reserva> reservas;
    private final AtomicInteger proximoIdPassageiro = new AtomicInteger(1);
    private final AtomicInteger proximoIdReserva = new AtomicInteger(1);

    public CompanhiaAereaService() {
        voos = new ArrayList<>();
        passageiros = new ArrayList<>();
        reservas = new ArrayList<>();
        // Adicionar dados de exemplo (opcional)
        adicionarDadosIniciais();
    }

    private void adicionarDadosIniciais() {
        try {
            adicionarVoo(new Voo("AZ100", "São Paulo", "Rio de Janeiro", LocalDateTime.now().plusDays(1).withHour(10).withMinute(0), LocalDateTime.now().plusDays(1).withHour(11).withMinute(0), 150, 350.0));
            adicionarVoo(new Voo("GA550", "Curitiba", "Salvador", LocalDateTime.now().plusDays(2).withHour(14).withMinute(30), LocalDateTime.now().plusDays(2).withHour(17).withMinute(0), 120, 850.0));
            adicionarVoo(new Voo("TA987", "São Paulo", "Buenos Aires", LocalDateTime.now().plusDays(3).withHour(8).withMinute(0), LocalDateTime.now().plusDays(3).withHour(11).withMinute(0), 180, 1200.0));

            adicionarPassageiro(new Passageiro(0, "Ana Silva", "ana.silva@email.com", "11999998888"));
            adicionarPassageiro(new Passageiro(0, "Bruno Costa", "bruno@email.com", "21988887777"));
        } catch (Exception e) {
            System.err.println("Erro ao adicionar dados iniciais: " + e.getMessage());
        }
    }

    // --- Operações de Voo ---
    public void adicionarVoo(Voo voo) {
        if (buscarVooPorNumero(voo.getNumeroVoo()).isPresent()) {
            throw new IllegalArgumentException("Voo com número " + voo.getNumeroVoo() + " já existe.");
        }
        voos.add(voo);
        System.out.println("Voo adicionado: " + voo);
    }

    public Optional<Voo> buscarVooPorNumero(String numeroVoo) {
        return voos.stream().filter(v -> v.getNumeroVoo().equalsIgnoreCase(numeroVoo)).findFirst();
    }

    public List<Voo> listarTodosVoos() {
        return new ArrayList<>(voos); // Retorna cópia
    }

    public List<Voo> buscarVoos(String origem, String destino) {
        return voos.stream()
                .filter(v -> (origem == null || origem.isEmpty() || v.getOrigem().equalsIgnoreCase(origem)))
                .filter(v -> (destino == null || destino.isEmpty() || v.getDestino().equalsIgnoreCase(destino)))
                .collect(Collectors.toList());
    }

    public boolean removerVoo(String numeroVoo) {
        // Idealmente, verificar se há reservas ativas antes de remover
        long reservasAtivas = reservas.stream()
                .filter(r -> r.getNumeroVoo().equalsIgnoreCase(numeroVoo) && r.getStatus() == Reserva.StatusReserva.CONFIRMADA)
                .count();
        if (reservasAtivas > 0) {
            throw new IllegalStateException("Não é possível remover o voo " + numeroVoo + " pois existem reservas ativas.");
        }
        boolean removido = voos.removeIf(v -> v.getNumeroVoo().equalsIgnoreCase(numeroVoo));
        if(removido) System.out.println("Voo removido: " + numeroVoo);
        return removido;
    }


    // --- Operações de Passageiro ---
    public Passageiro adicionarPassageiro(Passageiro passageiro) {
        // Verifica se já existe por email (ou outra regra de negócio)
        if (passageiros.stream().anyMatch(p -> p.getEmail().equalsIgnoreCase(passageiro.getEmail()))) {
            throw new IllegalArgumentException("Passageiro com email " + passageiro.getEmail() + " já cadastrado.");
        }
        Passageiro novoPassageiro = new Passageiro(
                proximoIdPassageiro.getAndIncrement(),
                passageiro.getNome(),
                passageiro.getEmail(),
                passageiro.getTelefone());
        passageiros.add(novoPassageiro);
        System.out.println("Passageiro adicionado: " + novoPassageiro);
        return novoPassageiro;
    }

    public Optional<Passageiro> buscarPassageiroPorId(int id) {
        return passageiros.stream().filter(p -> p.getId() == id).findFirst();
    }

    public Optional<Passageiro> buscarPassageiroPorEmail(String email) {
        return passageiros.stream().filter(p -> p.getEmail().equalsIgnoreCase(email)).findFirst();
    }

    public List<Passageiro> listarTodosPassageiros() {
        return new ArrayList<>(passageiros); // Retorna cópia
    }

    public boolean atualizarPassageiro(int id, String nome, String email, String telefone) {
        Optional<Passageiro> passageiroOpt = buscarPassageiroPorId(id);
        if(passageiroOpt.isPresent()) {
            // Verifica se o novo email já está em uso por OUTRO passageiro
            Optional<Passageiro> outroComEmail = buscarPassageiroPorEmail(email);
            if(outroComEmail.isPresent() && outroComEmail.get().getId() != id) {
                throw new IllegalArgumentException("O email " + email + " já está sendo usado por outro passageiro.");
            }

            Passageiro p = passageiroOpt.get();
            p.setNome(nome);
            p.setEmail(email);
            p.setTelefone(telefone);
            System.out.println("Passageiro atualizado: " + p);
            return true;
        }
        return false;
    }


    // --- Operações de Reserva ---
    public Reserva criarReserva(String numeroVoo, int idPassageiro) {
        Optional<Voo> vooOpt = buscarVooPorNumero(numeroVoo);
        if (!vooOpt.isPresent()) {
            throw new IllegalArgumentException("Voo " + numeroVoo + " não encontrado.");
        }
        Optional<Passageiro> passageiroOpt = buscarPassageiroPorId(idPassageiro);
        if (!passageiroOpt.isPresent()) {
            throw new IllegalArgumentException("Passageiro com ID " + idPassageiro + " não encontrado.");
        }

        Voo voo = vooOpt.get();

        // Verifica se o passageiro já tem reserva neste voo
        boolean jaReservado = reservas.stream()
                .anyMatch(r -> r.getNumeroVoo().equalsIgnoreCase(numeroVoo) &&
                        r.getIdPassageiro() == idPassageiro &&
                        r.getStatus() == Reserva.StatusReserva.CONFIRMADA);
        if (jaReservado) {
            throw new IllegalStateException("Passageiro já possui reserva confirmada para este voo.");
        }


        if (!voo.reservarAssento()) {
            throw new IllegalStateException("Voo " + numeroVoo + " está lotado.");
        }

        Reserva novaReserva = new Reserva(
                proximoIdReserva.getAndIncrement(),
                voo.getNumeroVoo(),
                passageiroOpt.get().getId(),
                LocalDateTime.now()
        );
        reservas.add(novaReserva);
        System.out.println("Reserva criada: " + novaReserva);
        return novaReserva;
    }

    public Optional<Reserva> buscarReservaPorId(int idReserva) {
        return reservas.stream().filter(r -> r.getIdReserva() == idReserva).findFirst();
    }

    public List<Reserva> listarTodasReservas() {
        return new ArrayList<>(reservas); // Retorna cópia
    }

    public List<Reserva> listarReservasPorPassageiro(int idPassageiro) {
        return reservas.stream()
                .filter(r -> r.getIdPassageiro() == idPassageiro)
                .collect(Collectors.toList());
    }

    public List<Reserva> listarReservasPorVoo(String numeroVoo) {
        return reservas.stream()
                .filter(r -> r.getNumeroVoo().equalsIgnoreCase(numeroVoo))
                .collect(Collectors.toList());
    }

    public boolean cancelarReserva(int idReserva) {
        Optional<Reserva> reservaOpt = buscarReservaPorId(idReserva);
        if (!reservaOpt.isPresent()) {
            System.err.println("Tentativa de cancelar reserva inexistente: ID " + idReserva);
            return false; // Reserva não encontrada
        }

        Reserva reserva = reservaOpt.get();
        if (reserva.getStatus() == Reserva.StatusReserva.CANCELADA) {
            System.out.println("Reserva ID " + idReserva + " já estava cancelada.");
            return false; // Já cancelada
        }

        Optional<Voo> vooOpt = buscarVooPorNumero(reserva.getNumeroVoo());
        if (vooOpt.isPresent()) {
            // Libera o assento no voo
            vooOpt.get().cancelarReservaAssento();
            reserva.setStatus(Reserva.StatusReserva.CANCELADA);
            System.out.println("Reserva cancelada: " + reserva);
            return true;
        } else {
            // Voo associado não existe mais? Logar erro, mas cancelar a reserva mesmo assim.
            System.err.println("Voo " + reserva.getNumeroVoo() + " associado à reserva " + idReserva + " não encontrado ao cancelar.");
            reserva.setStatus(Reserva.StatusReserva.CANCELADA);
            return true; // Considera cancelada mesmo sem achar o voo
        }
    }
}