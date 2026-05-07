package System_Mercado;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PainelProdutos extends JPanel {

    private JTextField txtNome, txtCodigo, txtPreco, txtQuantidade;
    private JTable tabelaProdutos;
    private DefaultTableModel modeloTabela;

    public PainelProdutos() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        // --- Painel do Formulário (à esquerda) ---
        JPanel painelFormulario = new JPanel(new GridBagLayout());
        painelFormulario.setBorder(new TitledBorder("Cadastro de Produto"));
        painelFormulario.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Labels e Campos de Texto
        gbc.gridx = 0; gbc.gridy = 0; painelFormulario.add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1; txtNome = new JTextField(20); painelFormulario.add(txtNome, gbc);

        gbc.gridx = 0; gbc.gridy = 1; painelFormulario.add(new JLabel("Código de Barras:"), gbc);
        gbc.gridx = 1; txtCodigo = new JTextField(20); painelFormulario.add(txtCodigo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; painelFormulario.add(new JLabel("Preço (R$):"), gbc);
        gbc.gridx = 1; txtPreco = new JTextField(20); painelFormulario.add(txtPreco, gbc);

        gbc.gridx = 0; gbc.gridy = 3; painelFormulario.add(new JLabel("Quantidade em Estoque:"), gbc);
        gbc.gridx = 1; txtQuantidade = new JTextField(20); painelFormulario.add(txtQuantidade, gbc);

        // Painel de Botões do formulário
        JPanel painelBotoesForm = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        painelBotoesForm.setBackground(Color.WHITE);
        JButton btnSalvar = new JButton("Salvar");
        JButton btnLimpar = new JButton("Limpar");
        painelBotoesForm.add(btnSalvar);
        painelBotoesForm.add(btnLimpar);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        painelFormulario.add(painelBotoesForm, gbc);

        // --- Painel da Tabela (à direita) ---
        JPanel painelTabela = new JPanel(new BorderLayout());
        painelTabela.setBorder(new TitledBorder("Produtos Cadastrados"));
        painelTabela.setBackground(Color.WHITE);

        // Criação da tabela
        String[] colunas = {"Código", "Nome", "Preço", "Qtd. Estoque"};
        modeloTabela = new DefaultTableModel(colunas, 0);
        tabelaProdutos = new JTable(modeloTabela);
        tabelaProdutos.setFillsViewportHeight(true);
        tabelaProdutos.setRowHeight(25);
        tabelaProdutos.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        // Exemplo de dados (remover em um sistema real)
        modeloTabela.addRow(new Object[]{"789001", "Arroz Tipo 1 (5kg)", 25.50, 150});
        modeloTabela.addRow(new Object[]{"789002", "Feijão Carioca (1kg)", 8.90, 200});
        modeloTabela.addRow(new Object[]{"789003", "Óleo de Soja (900ml)", 7.25, 300});

        JScrollPane scrollPane = new JScrollPane(tabelaProdutos);
        painelTabela.add(scrollPane, BorderLayout.CENTER);

        // Painel de botões da tabela
        JPanel painelBotoesTabela = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        painelBotoesTabela.setBackground(Color.WHITE);
        JButton btnEditar = new JButton("Editar Selecionado");
        JButton btnExcluir = new JButton("Excluir Selecionado");
        painelBotoesTabela.add(btnEditar);
        painelBotoesTabela.add(btnExcluir);
        painelTabela.add(painelBotoesTabela, BorderLayout.SOUTH);

        // --- Adiciona os painéis ao painel principal usando JSplitPane ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelFormulario, painelTabela);
        splitPane.setDividerLocation(450); // Define a posição inicial do divisor
        add(splitPane, BorderLayout.CENTER);
    }
}