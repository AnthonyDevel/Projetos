package New_MercadoSystem.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Venda {
    private List<ItemVenda> itens;
    private LocalDateTime dataHora;
    private double total;
    private boolean finalizada;

    public Venda() {
        this.itens = new ArrayList<>();
        this.dataHora = LocalDateTime.now();
        this.total = 0.0;
        this.finalizada = false;
    }

    public void adicionarItem(Produto produto, int quantidade) {
        if (produto.getQuantidadeEstoque() >= quantidade) {
            // Verificar se produto já existe na venda
            for (ItemVenda item : itens) {
                if (item.getProduto().getCodigo().equals(produto.getCodigo())) {
                    item.setQuantidade(item.getQuantidade() + quantidade);
                    produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - quantidade);
                    calcularTotal();
                    return;
                }
            }

            // Produto não existe na venda, criar novo item
            ItemVenda item = new ItemVenda(produto, quantidade);
            itens.add(item);
            produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - quantidade);
            calcularTotal();
        } else {
            throw new IllegalArgumentException("Estoque insuficiente! Disponível: " + produto.getQuantidadeEstoque());
        }
    }

    public void removerItem(int index) {
        if (index >= 0 && index < itens.size()) {
            ItemVenda item = itens.remove(index);
            item.getProduto().setQuantidadeEstoque(
                    item.getProduto().getQuantidadeEstoque() + item.getQuantidade()
            );
            calcularTotal();
        }
    }

    private void calcularTotal() {
        total = itens.stream().mapToDouble(ItemVenda::getSubtotal).sum();
    }

    public void finalizarVenda() {
        this.finalizada = true;
        this.dataHora = LocalDateTime.now();
    }

    public List<ItemVenda> getItens() { return itens; }
    public LocalDateTime getDataHora() { return dataHora; }
    public double getTotal() { return total; }
    public boolean isFinalizada() { return finalizada; }
}