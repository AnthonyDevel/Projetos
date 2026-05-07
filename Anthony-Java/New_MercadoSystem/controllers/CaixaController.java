package New_MercadoSystem.controllers;

import New_MercadoSystem.models.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;

import java.util.HashMap;
import java.util.Map;

public class CaixaController {

    private Map<String, Produto> produtosCadastrados;
    private Venda vendaAtual;
    private TableView<ItemVenda> tabelaVenda;
    private Label lblTotal;

    public CaixaController(TableView<ItemVenda> tabelaVenda, Label lblTotal) {
        this.tabelaVenda = tabelaVenda;
        this.lblTotal = lblTotal;
        this.vendaAtual = new Venda();
        this.produtosCadastrados = new HashMap<>();

        inicializarProdutos();
    }

    private void inicializarProdutos() {
        // Produtos de exemplo
        produtosCadastrados.put("789001", new Produto("789001", "Arroz Tipo 1 (5kg)", 25.50, 150));
        produtosCadastrados.put("789002", new Produto("789002", "Feijão Carioca (1kg)", 8.90, 200));
        produtosCadastrados.put("789003", new Produto("789003", "Óleo de Soja (900ml)", 7.25, 300));
        produtosCadastrados.put("789004", new Produto("789004", "Leite Integral (1L)", 4.50, 15));
        produtosCadastrados.put("789005", new Produto("789005", "Refrigerante (2L)", 8.99, 0));
        produtosCadastrados.put("789006", new Produto("789006", "Café (500g)", 18.90, 45));
        produtosCadastrados.put("789007", new Produto("789007", "Açúcar (2kg)", 6.50, 120));
        produtosCadastrados.put("789008", new Produto("789008", "Farinha (1kg)", 5.80, 200));
    }

    public boolean adicionarItem(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            mostrarAlerta("Erro", "Digite um código de produto!", Alert.AlertType.WARNING);
            return false;
        }

        Produto produto = produtosCadastrados.get(codigo.trim());
        if (produto == null) {
            mostrarAlerta("Erro", "Produto não encontrado!\nCódigo: " + codigo, Alert.AlertType.ERROR);
            return false;
        }

        if (produto.getQuantidadeEstoque() <= 0) {
            mostrarAlerta("Erro", "Produto sem estoque!\nProduto: " + produto.getNome(), Alert.AlertType.ERROR);
            return false;
        }

        try {
            vendaAtual.adicionarItem(produto, 1);
            atualizarTabela();
            atualizarTotal();
            return true;
        } catch (IllegalArgumentException e) {
            mostrarAlerta("Erro", e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }
    }

    public void removerItem(int index) {
        if (index >= 0 && index < vendaAtual.getItens().size()) {
            vendaAtual.removerItem(index);
            atualizarTabela();
            atualizarTotal();
        }
    }

    public boolean finalizarVenda() {
        if (vendaAtual.getItens().isEmpty()) {
            mostrarAlerta("Aviso", "Nenhum item na venda!", Alert.AlertType.WARNING);
            return false;
        }

        vendaAtual.finalizarVenda();

        mostrarAlerta("Venda Finalizada",
                String.format("Venda finalizada com sucesso!\n\nTotal: R$ %.2f\nItens: %d\nData: %s",
                        vendaAtual.getTotal(),
                        vendaAtual.getItens().size(),
                        vendaAtual.getDataHora().toString().replace("T", " ")),
                Alert.AlertType.INFORMATION);

        // Criar nova venda
        vendaAtual = new Venda();
        atualizarTabela();
        atualizarTotal();

        return true;
    }

    public boolean cancelarVenda() {
        if (vendaAtual.getItens().isEmpty()) {
            return false;
        }

        // Devolver estoque
        for (ItemVenda item : vendaAtual.getItens()) {
            item.getProduto().setQuantidadeEstoque(
                    item.getProduto().getQuantidadeEstoque() + item.getQuantidade()
            );
        }

        vendaAtual = new Venda();
        atualizarTabela();
        atualizarTotal();

        return true;
    }

    public void adicionarQuantidade(int index, int quantidade) {
        if (index >= 0 && index < vendaAtual.getItens().size()) {
            ItemVenda item = vendaAtual.getItens().get(index);
            Produto produto = item.getProduto();

            if (produto.getQuantidadeEstoque() >= quantidade) {
                int novaQuantidade = item.getQuantidade() + quantidade;
                item.setQuantidade(novaQuantidade);
                produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - quantidade);
                atualizarTabela();
                atualizarTotal();
            } else {
                mostrarAlerta("Erro", "Estoque insuficiente!", Alert.AlertType.ERROR);
            }
        }
    }

    public void diminuirQuantidade(int index, int quantidade) {
        if (index >= 0 && index < vendaAtual.getItens().size()) {
            ItemVenda item = vendaAtual.getItens().get(index);
            int novaQuantidade = item.getQuantidade() - quantidade;

            if (novaQuantidade > 0) {
                item.setQuantidade(novaQuantidade);
                item.getProduto().setQuantidadeEstoque(item.getProduto().getQuantidadeEstoque() + quantidade);
            } else {
                removerItem(index);
            }
            atualizarTabela();
            atualizarTotal();
        }
    }

    public Produto buscarProduto(String codigo) {
        return produtosCadastrados.get(codigo);
    }

    public double getTotalVenda() {
        return vendaAtual.getTotal();
    }

    public int getQuantidadeItens() {
        return vendaAtual.getItens().size();
    }

    public Venda getVendaAtual() {
        return vendaAtual;
    }

    public ObservableList<ItemVenda> getItensVenda() {
        return FXCollections.observableArrayList(vendaAtual.getItens());
    }

    private void atualizarTabela() {
        tabelaVenda.setItems(FXCollections.observableArrayList(vendaAtual.getItens()));
    }

    private void atualizarTotal() {
        lblTotal.setText(String.format("R$ %.2f", vendaAtual.getTotal()));
    }

    private void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}