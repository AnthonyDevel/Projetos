package New_MercadoSystem.models;

import java.io.Serializable;

public class Produto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String codigo;
    private String nome;
    private double preco;
    private int quantidadeEstoque;
    private String status;
    private String categoria;
    private String fornecedor;
    private boolean selecionado;

    public Produto(String codigo, String nome, double preco, int quantidadeEstoque) {
        this.codigo = codigo;
        this.nome = nome;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
        this.categoria = "Geral";
        this.fornecedor = "Não especificado";
        this.selecionado = false;
        atualizarStatus();
    }

    public Produto(String codigo, String nome, double preco, int quantidadeEstoque, String categoria, String fornecedor) {
        this.codigo = codigo;
        this.nome = nome;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
        this.categoria = categoria;
        this.fornecedor = fornecedor;
        this.selecionado = false;
        atualizarStatus();
    }

    private void atualizarStatus() {
        if (quantidadeEstoque <= 0) {
            status = "Sem Estoque";
        } else if (quantidadeEstoque < 10) {
            status = "Baixo";
        } else {
            status = "OK";
        }
    }

    // Getters e Setters
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }

    public int getQuantidadeEstoque() { return quantidadeEstoque; }
    public void setQuantidadeEstoque(int quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
        atualizarStatus();
    }

    public String getStatus() { return status; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getFornecedor() { return fornecedor; }
    public void setFornecedor(String fornecedor) { this.fornecedor = fornecedor; }

    public boolean isSelecionado() { return selecionado; }
    public void setSelecionado(boolean selecionado) { this.selecionado = selecionado; }

    @Override
    public String toString() {
        return nome + " - R$ " + String.format("%.2f", preco) + " (" + quantidadeEstoque + " uni)";
    }
}