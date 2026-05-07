package estoque;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;


public class EstoqueApp extends JFrame {



    private final EstoqueService estoqueService;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField txtId;
    private final JTextField txtNome;
    private final JTextField txtDescricao;
    private final JTextField txtPreco;
    private final JTextField txtQuantidade;
    private final JTextField txtBusca; // Campo para busca

    private final JButton btnAdicionar;
    private final JButton btnAtualizar;
    private final JButton btnRemover;
    private final JButton btnLimpar;
    private final JButton btnBuscar; // Botão para iniciar busca
    private final JButton btnLimparBusca;

    public EstoqueApp() {
        estoqueService = new EstoqueService();

        // Configuração da Janela Principal
        setTitle("Sistema de Gerenciamento de Estoque");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centralizar na tela

        // --- Painel Principal ---
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)); // Layout Principal com espaçamento
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); // Margens internas

        // --- Tabela de Produtos ---
        String[] colunas = {"ID", "Nome", "Descrição", "Preço", "Quantidade"};
        tableModel = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Impede edição direta na tabela
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Apenas uma linha pode ser selecionada
        table.setAutoCreateRowSorter(true); // Habilita ordenação clicando no cabeçalho da coluna

        // Adiciona a tabela a um painel com rolagem
        JScrollPane scrollPane = new JScrollPane(table);

        // --- Painel de Formulário (Entrada de Dados) ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Detalhes do Produto"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Espaçamento entre componentes
        gbc.anchor = GridBagConstraints.WEST; // Alinhar à esquerda

        // Labels e TextFields
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtId = new JTextField(5);
        txtId.setEditable(false); // ID não é editável pelo usuário
        formPanel.add(txtId, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; formPanel.add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.gridwidth = 3; // Ocupa mais espaço horizontal
        txtNome = new JTextField(20);
        formPanel.add(txtNome, gbc);
        gbc.gridwidth = 1; // Reset gridwidth

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; formPanel.add(new JLabel("Descrição:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.gridwidth = 3;
        txtDescricao = new JTextField(30);
        formPanel.add(txtDescricao, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; formPanel.add(new JLabel("Preço:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        txtPreco = new JTextField(10);
        formPanel.add(txtPreco, gbc);


        gbc.gridx = 2; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; formPanel.add(new JLabel("Qtd:"), gbc);
        gbc.gridx = 3; gbc.gridy = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        txtQuantidade = new JTextField(5);
        formPanel.add(txtQuantidade, gbc);

        // --- Painel de Botões de Ação ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5)); // Botões centralizados
        btnAdicionar = new JButton("Adicionar");
        btnAtualizar = new JButton("Atualizar");
        btnRemover = new JButton("Remover");
        btnLimpar = new JButton("Limpar Campos");

        buttonPanel.add(btnAdicionar);
        buttonPanel.add(btnAtualizar);
        buttonPanel.add(btnRemover);
        buttonPanel.add(btnLimpar);

        // --- Painel de Busca ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.add(new JLabel("Buscar por Nome:"));
        txtBusca = new JTextField(20);
        btnBuscar = new JButton("Buscar");
        btnLimparBusca = new JButton("Mostrar Todos");
        searchPanel.add(txtBusca);
        searchPanel.add(btnBuscar);
        searchPanel.add(btnLimparBusca);


        // --- Adicionando os painéis ao Painel Principal ---
        mainPanel.add(searchPanel, BorderLayout.NORTH); // Painel de busca no topo
        mainPanel.add(scrollPane, BorderLayout.CENTER); // Tabela no centro

        // Painel inferior para formulário e botões
        JPanel bottomPanel = new JPanel(new BorderLayout(5,5));
        bottomPanel.add(formPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH); // Adiciona o painel inferior ao sul do painel principal

        // Adiciona o painel principal à janela
        add(mainPanel);

        // --- Configuração dos Event Listeners ---
        configurarListeners();

        // --- Carregar Dados Iniciais ---
        atualizarTabela();

        // Torna a janela visível
        setVisible(true);
    }

    private void configurarListeners() {
        // Adicionar Produto
        btnAdicionar.addActionListener(e -> adicionarProduto());

        // Atualizar Produto
        btnAtualizar.addActionListener(e -> atualizarProduto());

        // Remover Produto
        btnRemover.addActionListener(e -> removerProduto());

        // Limpar Campos do Formulário
        btnLimpar.addActionListener(e -> limparCampos());

        // Selecionar linha na tabela para preencher o formulário
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    // Converte o índice da view para o índice do model (importante com ordenação)
                    int modelRow = table.convertRowIndexToModel(selectedRow);
                    preencherFormularioPelaTabela(modelRow);
                }
            }
        });

        // Buscar Produto
        btnBuscar.addActionListener(e -> buscarProdutos());

        // Limpar Busca (Mostrar todos)
        btnLimparBusca.addActionListener(e -> {
            txtBusca.setText(""); // Limpa o campo de busca
            atualizarTabela(); // Recarrega todos os produtos
            // Limpa o filtro da tabela se houver
            TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
            if(sorter != null) {
                sorter.setRowFilter(null);
            }
        });

        // Permitir busca ao pressionar Enter no campo de busca
        txtBusca.addActionListener(e -> buscarProdutos());
    }

    private void atualizarTabela() {
        // Limpa a tabela
        tableModel.setRowCount(0);

        // Obtém os produtos do serviço
        List<Produto> produtos = estoqueService.listarProdutos();

        // Adiciona os produtos à tabela
        for (Produto p : produtos) {
            tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getNome(),
                    p.getDescricao(),
                    String.format("%.2f", p.getPreco()), // Formata o preço
                    p.getQuantidade()
            });
        }
        // Limpa o filtro da tabela se houver
        TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
        if(sorter != null) {
            sorter.setRowFilter(null);
        }
        System.out.println("Tabela atualizada com " + produtos.size() + " produtos."); // Log
    }

    private void preencherFormularioPelaTabela(int modelRow) {
        txtId.setText(tableModel.getValueAt(modelRow, 0).toString());
        txtNome.setText(tableModel.getValueAt(modelRow, 1).toString());
        txtDescricao.setText(tableModel.getValueAt(modelRow, 2).toString());
        // Remover formatação de moeda antes de colocar no campo
        String precoStr = tableModel.getValueAt(modelRow, 3).toString().replace(",", ".");
        txtPreco.setText(precoStr);
        txtQuantidade.setText(tableModel.getValueAt(modelRow, 4).toString());
        System.out.println("Formulário preenchido com dados da linha: " + modelRow); // Log
    }

    private void limparCampos() {
        txtId.setText("");
        txtNome.setText("");
        txtDescricao.setText("");
        txtPreco.setText("");
        txtQuantidade.setText("");
        table.clearSelection(); // Remove a seleção da tabela
        txtNome.requestFocus(); // Coloca o foco no campo nome
        System.out.println("Campos do formulário limpos."); // Log
    }

    private void adicionarProduto() {
        try {
            String nome = txtNome.getText().trim();
            String descricao = txtDescricao.getText().trim();
            double preco = Double.parseDouble(txtPreco.getText().replace(",", "."));
            int quantidade = Integer.parseInt(txtQuantidade.getText());

            estoqueService.adicionarProduto(nome, descricao, preco, quantidade);
            atualizarTabela();
            limparCampos();
            JOptionPane.showMessageDialog(this, "Produto adicionado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Erro: Preço e Quantidade devem ser números válidos.", "Erro de Formato", JOptionPane.ERROR_MESSAGE);
            System.err.println("Erro de formato ao adicionar: " + ex.getMessage()); // Log
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro de Validação", JOptionPane.ERROR_MESSAGE);
            System.err.println("Erro de validação ao adicionar: " + ex.getMessage()); // Log
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ocorreu um erro inesperado ao adicionar o produto.", "Erro Inesperado", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(); // Log detalhado
        }
    }

    private void atualizarProduto() {
        if (txtId.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione um produto na tabela para atualizar.", "Nenhum Produto Selecionado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int id = Integer.parseInt(txtId.getText());
            String nome = txtNome.getText().trim();
            String descricao = txtDescricao.getText().trim();
            double preco = Double.parseDouble(txtPreco.getText().replace(",", "."));
            int quantidade = Integer.parseInt(txtQuantidade.getText());

            boolean atualizado = estoqueService.atualizarProduto(id, nome, descricao, preco, quantidade);

            if (atualizado) {
                atualizarTabela();
                limparCampos();
                JOptionPane.showMessageDialog(this, "Produto atualizado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Erro: Produto com ID " + id + " não encontrado.", "Erro", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Erro: Preço e Quantidade devem ser números válidos.", "Erro de Formato", JOptionPane.ERROR_MESSAGE);
            System.err.println("Erro de formato ao atualizar: " + ex.getMessage()); // Log
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro de Validação", JOptionPane.ERROR_MESSAGE);
            System.err.println("Erro de validação ao atualizar: " + ex.getMessage()); // Log
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ocorreu um erro inesperado ao atualizar o produto.", "Erro Inesperado", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(); // Log detalhado
        }
    }

    private void removerProduto() {
        if (txtId.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione um produto na tabela para remover.", "Nenhum Produto Selecionado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = Integer.parseInt(txtId.getText());
        String nomeProduto = txtNome.getText(); // Para mostrar na mensagem de confirmação

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza que deseja remover o produto '" + nomeProduto + "' (ID: " + id + ")?",
                "Confirmar Remoção",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean removido = estoqueService.removerProduto(id);
            if (removido) {
                atualizarTabela();
                limparCampos();
                JOptionPane.showMessageDialog(this, "Produto removido com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Isso não deveria acontecer se o ID veio da seleção, mas é bom ter
                JOptionPane.showMessageDialog(this, "Erro: Produto com ID " + id + " não encontrado.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void buscarProdutos() {
        String termoBusca = txtBusca.getText().trim();
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();

        if (termoBusca.length() == 0) {
            // Se a busca for vazia, remove qualquer filtro existente
            sorter.setRowFilter(null);
        } else {
            // Cria um filtro que busca o termo (ignorando maiúsculas/minúsculas) na coluna Nome (índice 1)
            // Você pode ajustar para buscar em outras colunas também
            try {
                // (?i) faz a busca ser case-insensitive
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + termoBusca, 1)); // Coluna 1 = Nome
                System.out.println("Aplicando filtro de busca: " + termoBusca); // Log
            } catch (java.util.regex.PatternSyntaxException e) {
                System.err.println("Erro na expressão regular da busca: " + e.getMessage()); // Log
                JOptionPane.showMessageDialog(this, "Termo de busca inválido.", "Erro na Busca", JOptionPane.ERROR_MESSAGE);
                sorter.setRowFilter(null); // Limpa filtro em caso de erro
            }
        }
    }


    //Método Principal para iniciar a aplicação
    public static void main(String[] args) {
        // Garante que a criação da GUI ocorra na Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            // Define um Look and Feel mais moderno, se disponível (Nimbus)
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                // Se Nimbus não estiver disponível, usa o padrão do sistema
                System.err.println("Nimbus Look and Feel não encontrado, usando padrão."); // Log
            }
            new EstoqueApp(); // Cria e exibe a janela
        });
    }
}