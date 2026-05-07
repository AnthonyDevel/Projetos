package com.airline.ui;

import com.airline.model.Passageiro;
import com.airline.model.Reserva;
import com.airline.model.Voo;
import com.airline.service.CompanhiaAereaService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.List;

public class AirlineApp extends JFrame {

    private final CompanhiaAereaService service;

    // Componentes da Aba Voos
    private DefaultTableModel voosTableModel;
    private JTable voosTable;
    private JTextField txtNumeroVoo, txtOrigemVoo, txtDestinoVoo, txtPartidaVoo, txtChegadaVoo, txtCapacidadeVoo, txtPrecoVoo;
    private JTextField txtBuscaOrigemVoo, txtBuscaDestinoVoo;

    // Componentes da Aba Passageiros
    private DefaultTableModel passageirosTableModel;
    private JTable passageirosTable;
    private JTextField txtIdPassageiro, txtNomePassageiro, txtEmailPassageiro, txtTelefonePassageiro;

    // Componentes da Aba Reservas
    private DefaultTableModel reservasTableModel;
    private JTable reservasTable;
    private JComboBox<Passageiro> comboPassageiroReserva; // Usar ComboBox para selecionar
    private JComboBox<Voo> comboVooReserva;            // Usar ComboBox para selecionar
    private JTextField txtIdReserva; // Para mostrar ID ao selecionar/cancelar


    public AirlineApp() {
        service = new CompanhiaAereaService();

        setTitle("Sistema de Companhia Aérea");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Criação das abas
        tabbedPane.addTab("Voos", createVoosPanel());
        tabbedPane.addTab("Passageiros", createPassageirosPanel());
        tabbedPane.addTab("Reservas", createReservasPanel());

        add(tabbedPane);

        // Carregar dados iniciais nas tabelas e combos
        atualizarTabelaVoos();
        atualizarTabelaPassageiros();
        atualizarTabelaReservas();
        atualizarCombosReserva();

        setVisible(true);
    }

    // --- Criação do Painel de Voos ---
    private JPanel createVoosPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Tabela
        String[] colunasVoos = {"Nº Voo", "Origem", "Destino", "Partida", "Chegada", "Capacidade", "Disp.", "Preço"};
        voosTableModel = new DefaultTableModel(colunasVoos, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        voosTable = new JTable(voosTableModel);
        voosTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        voosTable.setAutoCreateRowSorter(true);
        JScrollPane scrollPaneVoos = new JScrollPane(voosTable);

        // Painel de Busca
        JPanel buscaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buscaPanel.add(new JLabel("Origem:"));
        txtBuscaOrigemVoo = new JTextField(15);
        buscaPanel.add(txtBuscaOrigemVoo);
        buscaPanel.add(new JLabel("Destino:"));
        txtBuscaDestinoVoo = new JTextField(15);
        buscaPanel.add(txtBuscaDestinoVoo);
        JButton btnBuscarVoo = new JButton("Buscar Voos");
        JButton btnMostrarTodosVoos = new JButton("Mostrar Todos");
        buscaPanel.add(btnBuscarVoo);
        buscaPanel.add(btnMostrarTodosVoos);

        // Formulário
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Detalhes do Voo"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Linha 0
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Nº Voo:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; txtNumeroVoo = new JTextField(8); formPanel.add(txtNumeroVoo, gbc);
        gbc.gridx = 2; gbc.gridy = 0; formPanel.add(new JLabel("Origem:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx=0.5; txtOrigemVoo = new JTextField(15); formPanel.add(txtOrigemVoo, gbc);
        gbc.gridx = 4; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx=0; formPanel.add(new JLabel("Destino:"), gbc);
        gbc.gridx = 5; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx=0.5; txtDestinoVoo = new JTextField(15); formPanel.add(txtDestinoVoo, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx=0; // Reset

        // Linha 1
        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Partida (dd/MM/yyyy HH:mm):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; txtPartidaVoo = new JTextField(16); formPanel.add(txtPartidaVoo, gbc);
        gbc.gridx = 3; gbc.gridy = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; formPanel.add(new JLabel("Chegada (dd/MM/yyyy HH:mm):"), gbc);
        gbc.gridx = 4; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; txtChegadaVoo = new JTextField(16); formPanel.add(txtChegadaVoo, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; // Reset

        // Linha 2
        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Capacidade:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; txtCapacidadeVoo = new JTextField(5); formPanel.add(txtCapacidadeVoo, gbc);
        gbc.gridx = 2; gbc.gridy = 2; formPanel.add(new JLabel("Preço:"), gbc);
        gbc.gridx = 3; gbc.gridy = 2; txtPrecoVoo = new JTextField(8); formPanel.add(txtPrecoVoo, gbc);


        // Botões de Ação
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnAdicionarVoo = new JButton("Adicionar Voo");
        JButton btnRemoverVoo = new JButton("Remover Voo"); // Atualizar seria mais complexo (permitir mudar horários, preço?)
        JButton btnLimparVoo = new JButton("Limpar Campos");
        buttonPanel.add(btnAdicionarVoo);
        buttonPanel.add(btnRemoverVoo);
        buttonPanel.add(btnLimparVoo);

        // Adicionar componentes ao painel principal da aba
        panel.add(buscaPanel, BorderLayout.NORTH);
        panel.add(scrollPaneVoos, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(formPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // --- Listeners da Aba Voos ---
        voosTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = voosTable.getSelectedRow();
                if (row != -1) {
                    int modelRow = voosTable.convertRowIndexToModel(row);
                    txtNumeroVoo.setText(voosTableModel.getValueAt(modelRow, 0).toString());
                    txtOrigemVoo.setText(voosTableModel.getValueAt(modelRow, 1).toString());
                    txtDestinoVoo.setText(voosTableModel.getValueAt(modelRow, 2).toString());
                    txtPartidaVoo.setText(voosTableModel.getValueAt(modelRow, 3).toString());
                    txtChegadaVoo.setText(voosTableModel.getValueAt(modelRow, 4).toString());
                    txtCapacidadeVoo.setText(voosTableModel.getValueAt(modelRow, 5).toString());
                    txtPrecoVoo.setText(voosTableModel.getValueAt(modelRow, 7).toString().replace(",","."));
                    txtNumeroVoo.setEditable(false); // Não pode editar número do voo ao selecionar
                }
            }
        });

        btnAdicionarVoo.addActionListener(e -> adicionarVoo());
        btnRemoverVoo.addActionListener(e -> removerVoo());
        btnLimparVoo.addActionListener(e -> limparCamposVoo());
        btnBuscarVoo.addActionListener(e -> buscarVoos());
        btnMostrarTodosVoos.addActionListener(e -> {
            txtBuscaOrigemVoo.setText("");
            txtBuscaDestinoVoo.setText("");
            atualizarTabelaVoos(); // Mostra todos
        });


        return panel;
    }

    // --- Criação do Painel de Passageiros ---
    private JPanel createPassageirosPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Tabela
        String[] colunasPassageiros = {"ID", "Nome", "Email", "Telefone"};
        passageirosTableModel = new DefaultTableModel(colunasPassageiros, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        passageirosTable = new JTable(passageirosTableModel);
        passageirosTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        passageirosTable.setAutoCreateRowSorter(true);
        JScrollPane scrollPanePassageiros = new JScrollPane(passageirosTable);

        // Formulário
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Detalhes do Passageiro"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; txtIdPassageiro = new JTextField(5); txtIdPassageiro.setEditable(false); formPanel.add(txtIdPassageiro, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx=1.0; txtNomePassageiro = new JTextField(30); formPanel.add(txtNomePassageiro, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx=0; // Reset

        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; txtEmailPassageiro = new JTextField(30); formPanel.add(txtEmailPassageiro, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; // Reset

        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Telefone:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; txtTelefonePassageiro = new JTextField(15); formPanel.add(txtTelefonePassageiro, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; // Reset

        // Botões de Ação
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnAdicionarPassageiro = new JButton("Adicionar Passageiro");
        JButton btnAtualizarPassageiro = new JButton("Atualizar Passageiro");
        JButton btnLimparPassageiro = new JButton("Limpar Campos");
        buttonPanel.add(btnAdicionarPassageiro);
        buttonPanel.add(btnAtualizarPassageiro);
        buttonPanel.add(btnLimparPassageiro);

        // Adicionar componentes ao painel principal da aba
        panel.add(scrollPanePassageiros, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(formPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // --- Listeners da Aba Passageiros ---
        passageirosTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = passageirosTable.getSelectedRow();
                if (row != -1) {
                    int modelRow = passageirosTable.convertRowIndexToModel(row);
                    txtIdPassageiro.setText(passageirosTableModel.getValueAt(modelRow, 0).toString());
                    txtNomePassageiro.setText(passageirosTableModel.getValueAt(modelRow, 1).toString());
                    txtEmailPassageiro.setText(passageirosTableModel.getValueAt(modelRow, 2).toString());
                    txtTelefonePassageiro.setText(passageirosTableModel.getValueAt(modelRow, 3).toString());
                }
            }
        });

        btnAdicionarPassageiro.addActionListener(e -> adicionarPassageiro());
        btnAtualizarPassageiro.addActionListener(e -> atualizarPassageiro());
        btnLimparPassageiro.addActionListener(e -> limparCamposPassageiro());


        return panel;
    }


    // --- Criação do Painel de Reservas ---
    private JPanel createReservasPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Tabela
        String[] colunasReservas = {"ID Reserva", "Nº Voo", "ID Passageiro", "Nome Passageiro", "Data Reserva", "Status"};
        reservasTableModel = new DefaultTableModel(colunasReservas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        reservasTable = new JTable(reservasTableModel);
        reservasTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reservasTable.setAutoCreateRowSorter(true);
        JScrollPane scrollPaneReservas = new JScrollPane(reservasTable);

        // Formulário para Nova Reserva / Cancelamento
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Nova Reserva / Cancelamento"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Passageiro:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; comboPassageiroReserva = new JComboBox<>(); formPanel.add(comboPassageiroReserva, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Voo:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; comboVooReserva = new JComboBox<>(); formPanel.add(comboVooReserva, gbc);

        // Campo para mostrar ID da reserva selecionada (apenas para cancelamento)
        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("ID Reserva (Selecionada):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        txtIdReserva = new JTextField(6);
        txtIdReserva.setEditable(false);
        formPanel.add(txtIdReserva, gbc);


        // Botões de Ação
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnCriarReserva = new JButton("Criar Reserva");
        JButton btnCancelarReserva = new JButton("Cancelar Reserva Selecionada");
        buttonPanel.add(btnCriarReserva);
        buttonPanel.add(btnCancelarReserva);

        // Adicionar componentes ao painel principal da aba
        panel.add(scrollPaneReservas, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(formPanel, BorderLayout.NORTH); // Form em cima dos botões
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // --- Listeners da Aba Reservas ---
        reservasTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = reservasTable.getSelectedRow();
                if (row != -1) {
                    int modelRow = reservasTable.convertRowIndexToModel(row);
                    txtIdReserva.setText(reservasTableModel.getValueAt(modelRow, 0).toString());
                    // Poderia pré-selecionar os combos, mas pode ser confuso.
                    // Apenas exibir o ID para cancelamento é mais simples.
                } else {
                    txtIdReserva.setText(""); // Limpa se deselecionar
                }
            }
        });

        btnCriarReserva.addActionListener(e -> criarReserva());
        btnCancelarReserva.addActionListener(e -> cancelarReserva());


        return panel;
    }

    // --- Métodos de Atualização de Tabelas e Combos ---

    private void atualizarTabelaVoos() {
        atualizarTabelaVoos(service.listarTodosVoos());
        atualizarCombosReserva(); // Atualiza combo de voos para reserva
    }

    private void atualizarTabelaVoos(List<Voo> voos) {
        voosTableModel.setRowCount(0); // Limpa tabela
        for (Voo v : voos) {
            voosTableModel.addRow(new Object[]{
                    v.getNumeroVoo(),
                    v.getOrigem(),
                    v.getDestino(),
                    v.getHorarioPartidaFormatado(),
                    v.getHorarioChegadaFormatado(),
                    v.getCapacidadeTotal(),
                    v.getAssentosDisponiveis(),
                    String.format("%.2f", v.getPreco())
            });
        }
        // Limpa filtro se houver
        TableRowSorter<?> sorter = (TableRowSorter<?>) voosTable.getRowSorter();
        if (sorter != null) {
            sorter.setRowFilter(null);
        }
    }

    private void atualizarTabelaPassageiros() {
        passageirosTableModel.setRowCount(0); // Limpa tabela
        List<Passageiro> passageiros = service.listarTodosPassageiros();
        for (Passageiro p : passageiros) {
            passageirosTableModel.addRow(new Object[]{
                    p.getId(),
                    p.getNome(),
                    p.getEmail(),
                    p.getTelefone()
            });
        }
        atualizarCombosReserva(); // Atualiza combo de passageiros para reserva
    }

    private void atualizarTabelaReservas() {
        reservasTableModel.setRowCount(0); // Limpa tabela
        List<Reserva> reservas = service.listarTodasReservas();
        for (Reserva r : reservas) {
            // Buscar nome do passageiro para exibir na tabela
            String nomePassageiro = service.buscarPassageiroPorId(r.getIdPassageiro())
                    .map(Passageiro::getNome)
                    .orElse("ID: " + r.getIdPassageiro()); // Caso não encontre

            reservasTableModel.addRow(new Object[]{
                    r.getIdReserva(),
                    r.getNumeroVoo(),
                    r.getIdPassageiro(),
                    nomePassageiro, // Exibe nome
                    r.getDataReserva().format(Voo.DATE_TIME_FORMATTER),
                    r.getStatus().name() // Exibe CONFIRMADA ou CANCELADA
            });
        }
        txtIdReserva.setText(""); // Limpa campo de ID selecionado
    }

    private void atualizarCombosReserva() {
        // Atualizar ComboBox de Passageiros
        comboPassageiroReserva.removeAllItems();
        List<Passageiro> passageiros = service.listarTodosPassageiros();
        // Adiciona um item nulo ou "Selecione" se desejar
        // comboPassageiroReserva.addItem(null); // Ou uma string
        for (Passageiro p : passageiros) {
            comboPassageiroReserva.addItem(p); // Usa o toString() do Passageiro
        }

        // Atualizar ComboBox de Voos
        comboVooReserva.removeAllItems();
        List<Voo> voos = service.listarTodosVoos().stream()
                .filter(v -> v.getAssentosDisponiveis() > 0) // Mostra apenas voos com assentos
                .collect(Collectors.toList());
        // comboVooReserva.addItem(null); // Ou uma string
        for (Voo v : voos) {
            comboVooReserva.addItem(v); // Usa o toString() do Voo
        }
    }


    // --- Métodos de Ação (Listeners) ---

    // VOOS
    private void adicionarVoo() {
        try {
            String numVoo = txtNumeroVoo.getText().trim().toUpperCase();
            String origem = txtOrigemVoo.getText().trim();
            String destino = txtDestinoVoo.getText().trim();
            LocalDateTime partida = LocalDateTime.parse(txtPartidaVoo.getText().trim(), Voo.DATE_TIME_FORMATTER);
            LocalDateTime chegada = LocalDateTime.parse(txtChegadaVoo.getText().trim(), Voo.DATE_TIME_FORMATTER);
            int capacidade = Integer.parseInt(txtCapacidadeVoo.getText().trim());
            double preco = Double.parseDouble(txtPrecoVoo.getText().trim().replace(",", "."));

            Voo novoVoo = new Voo(numVoo, origem, destino, partida, chegada, capacidade, preco);
            service.adicionarVoo(novoVoo);

            atualizarTabelaVoos();
            limparCamposVoo();
            JOptionPane.showMessageDialog(this, "Voo adicionado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Formato de data/hora inválido. Use dd/MM/yyyy HH:mm", "Erro de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Capacidade deve ser um número inteiro e Preço um número válido.", "Erro de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao adicionar voo: " + ex.getMessage(), "Erro de Validação", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ocorreu um erro inesperado: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void removerVoo() {
        int selectedRow = voosTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um voo na tabela para remover.", "Nenhum Voo Selecionado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = voosTable.convertRowIndexToModel(selectedRow);
        String numeroVoo = voosTableModel.getValueAt(modelRow, 0).toString();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza que deseja remover o voo " + numeroVoo + "?\n(Isso só será possível se não houver reservas ativas)",
                "Confirmar Remoção",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean removido = service.removerVoo(numeroVoo);
                if (removido) {
                    atualizarTabelaVoos();
                    limparCamposVoo();
                    JOptionPane.showMessageDialog(this, "Voo removido com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    // Deveria ter lançado exceção ou retornado false se não achou,
                    // mas a exceção é mais provável pelo check de reservas.
                    JOptionPane.showMessageDialog(this, "Não foi possível remover o voo (verifique se ele existe).", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IllegalStateException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao remover voo: " + ex.getMessage(), "Erro de Regra", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ocorreu um erro inesperado ao remover: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void limparCamposVoo() {
        txtNumeroVoo.setText("");
        txtOrigemVoo.setText("");
        txtDestinoVoo.setText("");
        txtPartidaVoo.setText("");
        txtChegadaVoo.setText("");
        txtCapacidadeVoo.setText("");
        txtPrecoVoo.setText("");
        voosTable.clearSelection();
        txtNumeroVoo.setEditable(true);
        txtNumeroVoo.requestFocus();
    }

    private void buscarVoos() {
        String origem = txtBuscaOrigemVoo.getText().trim();
        String destino = txtBuscaDestinoVoo.getText().trim();

        // Implementação simples de filtro na tabela (pode ser feito no service também)
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) voosTable.getRowSorter();
        if (sorter == null) {
            sorter = new TableRowSorter<>(voosTableModel);
            voosTable.setRowSorter(sorter);
        }

        // Cria filtros para origem (coluna 1) e destino (coluna 2)
        RowFilter<Object, Object> rfOrigem = null;
        RowFilter<Object, Object> rfDestino = null;
        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        if (!origem.isEmpty()) {
            try {
                rfOrigem = RowFilter.regexFilter("(?i)" + origem, 1); // (?i) for case-insensitive
                filters.add(rfOrigem);
            } catch (java.util.regex.PatternSyntaxException e) {
                System.err.println("Regex inválida para origem: " + origem);
            }
        }
        if (!destino.isEmpty()) {
            try {
                rfDestino = RowFilter.regexFilter("(?i)" + destino, 2);
                filters.add(rfDestino);
            } catch (java.util.regex.PatternSyntaxException e) {
                System.err.println("Regex inválida para destino: " + destino);
            }
        }

        // Combina os filtros (todos devem ser verdadeiros)
        if (!filters.isEmpty()) {
            RowFilter<Object, Object> compoundFilter = RowFilter.andFilter(filters);
            sorter.setRowFilter(compoundFilter);
        } else {
            sorter.setRowFilter(null); // Sem filtros
        }
    }


    // PASSAGEIROS
    private void adicionarPassageiro() {
        try {
            String nome = txtNomePassageiro.getText().trim();
            String email = txtEmailPassageiro.getText().trim();
            String telefone = txtTelefonePassageiro.getText().trim();

            // ID é gerado pelo serviço, então passamos 0 ou um valor dummy
            Passageiro p = new Passageiro(0, nome, email, telefone);
            service.adicionarPassageiro(p);

            atualizarTabelaPassageiros();
            limparCamposPassageiro();
            JOptionPane.showMessageDialog(this, "Passageiro adicionado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao adicionar passageiro: " + ex.getMessage(), "Erro de Validação", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ocorreu um erro inesperado: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void atualizarPassageiro() {
        if (txtIdPassageiro.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione um passageiro na tabela para atualizar.", "Nenhum Passageiro Selecionado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int id = Integer.parseInt(txtIdPassageiro.getText());
            String nome = txtNomePassageiro.getText().trim();
            String email = txtEmailPassageiro.getText().trim();
            String telefone = txtTelefonePassageiro.getText().trim();

            if (nome.isEmpty()) throw new IllegalArgumentException("Nome não pode ser vazio.");

            boolean atualizado = service.atualizarPassageiro(id, nome, email, telefone);

            if (atualizado) {
                atualizarTabelaPassageiros();
                // Re-atualizar tabela de reservas caso o nome tenha mudado
                atualizarTabelaReservas();
                limparCamposPassageiro();
                JOptionPane.showMessageDialog(this, "Passageiro atualizado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Passageiro com ID " + id + " não encontrado.", "Erro", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "ID inválido.", "Erro de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao atualizar passageiro: " + ex.getMessage(), "Erro de Validação", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ocorreu um erro inesperado: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void limparCamposPassageiro() {
        txtIdPassageiro.setText("");
        txtNomePassageiro.setText("");
        txtEmailPassageiro.setText("");
        txtTelefonePassageiro.setText("");
        passageirosTable.clearSelection();
        txtNomePassageiro.requestFocus();
    }

    // RESERVAS
    private void criarReserva() {
        Passageiro passageiroSelecionado = (Passageiro) comboPassageiroReserva.getSelectedItem();
        Voo vooSelecionado = (Voo) comboVooReserva.getSelectedItem();

        if (passageiroSelecionado == null || vooSelecionado == null) {
            JOptionPane.showMessageDialog(this, "Selecione um passageiro e um voo disponíveis.", "Seleção Inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Reserva novaReserva = service.criarReserva(vooSelecionado.getNumeroVoo(), passageiroSelecionado.getId());
            atualizarTabelaReservas();
            atualizarTabelaVoos(); // Atualiza assentos disponíveis na tabela de voos
            atualizarCombosReserva(); // Atualiza voos disponíveis no combo
            JOptionPane.showMessageDialog(this, "Reserva criada com sucesso! ID: " + novaReserva.getIdReserva(), "Sucesso", JOptionPane.INFORMATION_MESSAGE);

        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao criar reserva: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ocorreu um erro inesperado ao criar reserva: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void cancelarReserva() {
        if (txtIdReserva.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione uma reserva na tabela para cancelar.", "Nenhuma Reserva Selecionada", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int idReserva = Integer.parseInt(txtIdReserva.getText());

            Optional<Reserva> reservaOpt = service.buscarReservaPorId(idReserva);
            if (!reservaOpt.isPresent() || reservaOpt.get().getStatus() == Reserva.StatusReserva.CANCELADA) {
                JOptionPane.showMessageDialog(this, "Reserva não encontrada ou já está cancelada.", "Erro", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Reserva reserva = reservaOpt.get();
            // Buscar dados para confirmação
            String nomePassageiro = service.buscarPassageiroPorId(reserva.getIdPassageiro())
                    .map(Passageiro::getNome).orElse("ID "+reserva.getIdPassageiro());


            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    String.format("Tem certeza que deseja cancelar a Reserva %d (Voo: %s, Passageiro: %s)?",
                            idReserva, reserva.getNumeroVoo(), nomePassageiro),
                    "Confirmar Cancelamento",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean cancelado = service.cancelarReserva(idReserva);
                if (cancelado) {
                    atualizarTabelaReservas();
                    atualizarTabelaVoos(); // Atualiza assentos disponíveis
                    atualizarCombosReserva(); // Atualiza voos disponíveis
                    JOptionPane.showMessageDialog(this, "Reserva cancelada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Não foi possível cancelar a reserva.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "ID da reserva selecionada é inválido.", "Erro de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ocorreu um erro inesperado ao cancelar reserva: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }


    // --- Método Principal ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Tenta aplicar o Look and Feel Nimbus para uma aparência melhor
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Nimbus L&F não encontrado, usando o padrão.");
            }
            new AirlineApp();
        });
    }
}