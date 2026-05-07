package Programs_Corp;

import javax.swing.*;
import java.awt.*;

public class CaixaEletronicoGUI {
    private double saldo = 0.0;

    public CaixaEletronicoGUI() {

        JFrame frame = new JFrame("Caixa Eletrônico");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new GridLayout(5, 1, 10, 10));


        JLabel labelBemVindo = new JLabel("Caixa Eletrônico Havel-Software!", SwingConstants.CENTER);
        labelBemVindo.setFont(new Font("Arial", Font.BOLD, 16));
        frame.add(labelBemVindo);


        JButton btnSaldo = new JButton("Consultar Saldo");
        btnSaldo.addActionListener(e -> consultarSaldo());
        frame.add(btnSaldo);


        JButton btnDepositar = new JButton("Depositar");
        btnDepositar.addActionListener(e -> depositar());
        frame.add(btnDepositar);


        JButton btnSacar = new JButton("Sacar");
        btnSacar.addActionListener(e -> sacar());
        frame.add(btnSacar);


        JButton btnSair = new JButton("Sair");
        btnSair.setBackground(Color.RED);
        btnSair.setForeground(Color.WHITE);
        btnSair.addActionListener(e -> {
            JOptionPane.showMessageDialog(frame, "Obrigado por usar o Caixa Eletrônico!");
            System.exit(0);
        });
        frame.add(btnSair);


        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void consultarSaldo() {
        JOptionPane.showMessageDialog(null, String.format("Seu saldo atual é: R$ %.2f", saldo),
                "Consultar Saldo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void depositar() {
        String input = JOptionPane.showInputDialog(null, "Digite o valor para depósito:",
                "Depositar", JOptionPane.PLAIN_MESSAGE);
        if (input != null) {
            try {
                double valor = Double.parseDouble(input);
                if (valor > 0) {
                    saldo += valor;
                    JOptionPane.showMessageDialog(null, String.format("Depósito de R$ %.2f realizado com sucesso!", valor),
                            "Depósito", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "O valor do depósito deve ser positivo.",
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Por favor, insira um número válido.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void sacar() {
        String input = JOptionPane.showInputDialog(null, "Digite o valor para saque:",
                "Sacar", JOptionPane.PLAIN_MESSAGE);
        if (input != null) {
            try {
                double valor = Double.parseDouble(input);
                if (valor > 0 && valor <= saldo) {
                    saldo -= valor;
                    JOptionPane.showMessageDialog(null, String.format("Saque de R$ %.2f realizado com sucesso!", valor),
                            "Saque", JOptionPane.INFORMATION_MESSAGE);
                } else if (valor > saldo) {
                    JOptionPane.showMessageDialog(null, "Saldo insuficiente para realizar o saque.",
                            "Erro", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "O valor do saque deve ser positivo.",
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Por favor, insira um número válido.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CaixaEletronicoGUI::new);
    }
}