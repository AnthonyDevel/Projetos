package System_Mercado;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PainelCaixa extends JPanel {

    private JTable tabelaVenda;
    private DefaultTableModel modeloTabela;
    private JLabel lblTotal;

    public PainelCaixa() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Painel Esquerdo (Registro de Produtos) ---
        JPanel painelRegistro = new JPanel(new BorderLayout(5, 5));
        painelRegistro.setBorder(new TitledBorder("Caixa Aberto"));

        // Área para inserir código do produto
        JPanel painelCodigo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelCodigo.add(new JLabel("Código do Produto:"));
        JTextField txtCodigoProduto = new JTextField(20);
        painelCodigo.add(txtCodigoProduto);
        JButton btnAdicionar = new JButton("Adicionar Item");
        painelCodigo.add(btnAdicionar);
        painelRegistro.add(painelCodigo, BorderLayout.NORTH);

        // Tabela com os itens da venda
        String[] colunas = {"Item", "Produto", "Qtd.", "Preço Unit.", "Subtotal"};
        modeloTabela = new DefaultTableModel(colunas, 0);
        tabelaVenda = new JTable(modeloTabela);
        tabelaVenda.setRowHeight(25);
        painelRegistro.add(new JScrollPane(tabelaVenda), BorderLayout.CENTER);

        // --- Painel Direito (Total e Finalização) ---
        JPanel painelTotal = new JPanel();
        painelTotal.setLayout(new BoxLayout(painelTotal, BoxLayout.Y_AXIS));
        painelTotal.setBorder(new EmptyBorder(20, 20, 20, 20));
        painelTotal.setPreferredSize(new Dimension(300, 0));

        // Label do total
        lblTotal = new JLabel("R$ 0,00");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 48));
        lblTotal.setForeground(new Color(0, 102, 0));
        lblTotal.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tituloTotal = new JLabel("TOTAL DA VENDA");
        tituloTotal.setFont(new Font("Arial", Font.BOLD, 24));
        tituloTotal.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Botões
        JButton btnFinalizar = new JButton("Finalizar Venda (F1)");
        btnFinalizar.setFont(new Font("Arial", Font.BOLD, 18));
        btnFinalizar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnFinalizar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JButton btnCancelar = new JButton("Cancelar Venda (F2)");
        btnCancelar.setFont(new Font("Arial", Font.PLAIN, 16));
        btnCancelar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCancelar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        painelTotal.add(tituloTotal);
        painelTotal.add(Box.createRigidArea(new Dimension(0, 20)));
        painelTotal.add(lblTotal);
        painelTotal.add(Box.createVerticalGlue()); // Espaço flexível
        painelTotal.add(btnFinalizar);
        painelTotal.add(Box.createRigidArea(new Dimension(0, 10)));
        painelTotal.add(btnCancelar);

        add(painelRegistro, BorderLayout.CENTER);
        add(painelTotal, BorderLayout.EAST);
    }
}
