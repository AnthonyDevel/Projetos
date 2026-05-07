package System_Mercado;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class PainelGestao extends JPanel {

    public PainelGestao() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titulo = new JLabel("Módulo de Gestão", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 28));
        add(titulo, BorderLayout.NORTH);

        JPanel painelRelatorios = new JPanel(new GridLayout(2, 2, 20, 20));
        painelRelatorios.setBorder(new TitledBorder("Relatórios e Dashboards"));

        // Botões para acessar diferentes relatórios
        JButton btnVendasDia = new JButton("Relatório de Vendas do Dia");
        JButton btnEstoqueBaixo = new JButton("Relatório de Estoque Baixo");
        JButton btnCurvaABC = new JButton("Análise de Curva ABC");
        JButton btnDesempenho = new JButton("Dashboard de Desempenho");

        // Adiciona ícones (opcional, requer imagens no projeto)
        // btnVendasDia.setIcon(new ImageIcon("path/to/icon.png"));

        painelRelatorios.add(btnVendasDia);
        painelRelatorios.add(btnEstoqueBaixo);
        painelRelatorios.add(btnCurvaABC);
        painelRelatorios.add(btnDesempenho);

        add(painelRelatorios, BorderLayout.CENTER);
    }
}
