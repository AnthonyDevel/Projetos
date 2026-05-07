import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Code extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainView mainView = new MainView();
        Scene scene = new Scene(mainView, 1200, 700);

        primaryStage.setTitle("Sistema de Supermercado - Super Java FX");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // ==================== MODEL CLASSES ====================

    public static class Produto implements Serializable {
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

    public static class ItemVenda {
        private Produto produto;
        private int quantidade;
        private double subtotal;

        public ItemVenda(Produto produto, int quantidade) {
            this.produto = produto;
            this.quantidade = quantidade;
            this.subtotal = produto.getPreco() * quantidade;
        }

        public Produto getProduto() { return produto; }
        public void setProduto(Produto produto) { this.produto = produto; }

        public int getQuantidade() { return quantidade; }
        public void setQuantidade(int quantidade) {
            this.quantidade = quantidade;
            this.subtotal = produto.getPreco() * quantidade;
        }

        public double getSubtotal() { return subtotal; }
    }

    public static class Venda {
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
                for (ItemVenda item : itens) {
                    if (item.getProduto().getCodigo().equals(produto.getCodigo())) {
                        item.setQuantidade(item.getQuantidade() + quantidade);
                        produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - quantidade);
                        calcularTotal();
                        return;
                    }
                }
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

    // ==================== UTILS CLASSES ====================

    public static class DataManager {
        private static DataManager instance;
        private ObservableList<Produto> produtos;

        private DataManager() {
            produtos = FXCollections.observableArrayList();
            inicializarProdutos();
        }

        public static DataManager getInstance() {
            if (instance == null) {
                instance = new DataManager();
            }
            return instance;
        }

        private void inicializarProdutos() {
            produtos.addAll(
                    new Produto("789001", "Arroz Tipo 1 (5kg)", 25.50, 150, "Alimentos", "Fornecedor A"),
                    new Produto("789002", "Feijão Carioca (1kg)", 8.90, 200, "Alimentos", "Fornecedor A"),
                    new Produto("789003", "Óleo de Soja (900ml)", 7.25, 300, "Alimentos", "Fornecedor B"),
                    new Produto("789004", "Leite Integral (1L)", 4.50, 15, "Laticínios", "Fornecedor C"),
                    new Produto("789005", "Refrigerante (2L)", 8.99, 0, "Bebidas", "Fornecedor D"),
                    new Produto("789006", "Café (500g)", 18.90, 45, "Alimentos", "Fornecedor E"),
                    new Produto("789007", "Açúcar (2kg)", 6.50, 120, "Alimentos", "Fornecedor A"),
                    new Produto("789008", "Farinha (1kg)", 5.80, 200, "Alimentos", "Fornecedor B"),
                    new Produto("789009", "Sabão em Pó (1kg)", 12.90, 80, "Limpeza", "Fornecedor F"),
                    new Produto("789010", "Detergente (500ml)", 2.50, 250, "Limpeza", "Fornecedor F"),
                    new Produto("789011", "Macarrão (500g)", 4.20, 180, "Alimentos", "Fornecedor G"),
                    new Produto("789012", "Molho de Tomate (340g)", 3.80, 220, "Alimentos", "Fornecedor G"),
                    new Produto("789013", "Creme Dental (90g)", 5.90, 150, "Higiene", "Fornecedor H"),
                    new Produto("789014", "Shampoo (350ml)", 12.50, 90, "Higiene", "Fornecedor H"),
                    new Produto("789015", "Sabonete (90g)", 2.30, 300, "Higiene", "Fornecedor H")
            );
        }

        public ObservableList<Produto> getProdutos() { return produtos; }

        public Produto buscarProdutoPorCodigo(String codigo) {
            for (Produto p : produtos) {
                if (p.getCodigo().equals(codigo)) return p;
            }
            return null;
        }

        public void adicionarProduto(Produto produto) { produtos.add(produto); }
        public void removerProduto(Produto produto) { produtos.remove(produto); }

        public int getTotalProdutos() { return produtos.size(); }

        public int getTotalItensEstoque() {
            return produtos.stream().mapToInt(Produto::getQuantidadeEstoque).sum();
        }

        public double getValorTotalEstoque() {
            return produtos.stream().mapToDouble(p -> p.getPreco() * p.getQuantidadeEstoque()).sum();
        }

        public long getProdutosEstoqueBaixo() {
            return produtos.stream().filter(p -> p.getQuantidadeEstoque() < 10 && p.getQuantidadeEstoque() > 0).count();
        }

        public long getProdutosSemEstoque() {
            return produtos.stream().filter(p -> p.getQuantidadeEstoque() <= 0).count();
        }
    }

    public static class QuantidadeDialog {
        public static int showDialog(String titulo, String mensagem, int maxQuantidade) {
            final int[] resultado = {0};

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UTILITY);
            dialog.setTitle(titulo);

            VBox vbox = new VBox(15);
            vbox.setPadding(new Insets(20));
            vbox.setAlignment(Pos.CENTER);
            vbox.setStyle("-fx-background-color: white;");

            Label label = new Label(mensagem);
            label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            label.setWrapText(true);
            label.setAlignment(Pos.CENTER);

            Spinner<Integer> spinner = new Spinner<>();
            SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxQuantidade, 1);
            spinner.setValueFactory(valueFactory);
            spinner.setEditable(true);
            spinner.setPrefWidth(150);
            spinner.setStyle("-fx-font-size: 16px;");

            spinner.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == null || newValue.isEmpty()) return;
                try {
                    int valor = Integer.parseInt(newValue);
                    if (valor < 1) spinner.getEditor().setText("1");
                    if (valor > maxQuantidade) spinner.getEditor().setText(String.valueOf(maxQuantidade));
                } catch (NumberFormatException e) {
                    spinner.getEditor().setText(oldValue);
                }
            });

            HBox botoes = new HBox(10);
            botoes.setAlignment(Pos.CENTER);

            Button btnConfirmar = new Button("Confirmar");
            btnConfirmar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 20; -fx-font-size: 14px;");
            Button btnCancelar = new Button("Cancelar");
            btnCancelar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 10 20; -fx-font-size: 14px;");

            btnConfirmar.setOnAction(e -> { resultado[0] = spinner.getValue(); dialog.close(); });
            btnCancelar.setOnAction(e -> dialog.close());
            btnConfirmar.setDefaultButton(true);

            botoes.getChildren().addAll(btnConfirmar, btnCancelar);
            vbox.getChildren().addAll(label, spinner, botoes);

            Scene scene = new Scene(vbox, 350, 250);
            dialog.setScene(scene);
            dialog.showAndWait();

            return resultado[0];
        }
    }

    // ==================== VIEW CLASSES ====================

    public static class CaixaView extends BorderPane {
        private TableView<ItemVenda> tabelaVenda;
        private Label lblTotal;
        private Label lblInfo;
        private TextField txtCodigoProduto;
        private Venda vendaAtual;
        private Button btnAdicionar, btnFinalizar, btnCancelar;

        public CaixaView() {
            setPadding(new Insets(10));
            setStyle("-fx-background-color: #f5f6fa;");
            vendaAtual = new Venda();
            configurarLayout();
            configurarEventos();
        }

        private void configurarLayout() {
            HBox mainContent = new HBox(10);
            mainContent.getChildren().addAll(criarPainelRegistro(), criarPainelTotal());
            HBox.setHgrow(mainContent.getChildren().get(0), Priority.ALWAYS);
            setCenter(mainContent);
        }

        private VBox criarPainelRegistro() {
            VBox painel = new VBox(10);
            painel.setPadding(new Insets(10));
            painel.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

            Label titulo = new Label("CAIXA ABERTO");
            titulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            titulo.setTextFill(Color.valueOf("#2c3e50"));

            HBox areaCodigo = new HBox(10);
            areaCodigo.setAlignment(Pos.CENTER_LEFT);

            Label lblCodigo = new Label("Código do Produto:");
            lblCodigo.setFont(Font.font("Arial", 14));

            txtCodigoProduto = new TextField();
            txtCodigoProduto.setPrefWidth(300);
            txtCodigoProduto.setPromptText("Digite ou leia o código de barras");

            btnAdicionar = new Button("Adicionar Item");
            btnAdicionar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");

            areaCodigo.getChildren().addAll(lblCodigo, txtCodigoProduto, btnAdicionar);

            tabelaVenda = criarTabelaVenda();

            painel.getChildren().addAll(titulo, areaCodigo, tabelaVenda);
            VBox.setVgrow(tabelaVenda, Priority.ALWAYS);

            return painel;
        }

        private TableView<ItemVenda> criarTabelaVenda() {
            TableView<ItemVenda> tabela = new TableView<>();

            TableColumn<ItemVenda, String> colItem = new TableColumn<>("Item");
            colItem.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(tabela.getItems().indexOf(cellData.getValue()) + 1)));
            colItem.setPrefWidth(50);

            TableColumn<ItemVenda, String> colProduto = new TableColumn<>("Produto");
            colProduto.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProduto().getNome()));
            colProduto.setPrefWidth(300);

            TableColumn<ItemVenda, Integer> colQtd = new TableColumn<>("Qtd.");
            colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
            colQtd.setPrefWidth(70);

            TableColumn<ItemVenda, String> colPreco = new TableColumn<>("Preço Unit.");
            colPreco.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("R$ %.2f", cellData.getValue().getProduto().getPreco())));
            colPreco.setPrefWidth(100);

            TableColumn<ItemVenda, String> colSubtotal = new TableColumn<>("Subtotal");
            colSubtotal.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("R$ %.2f", cellData.getValue().getSubtotal())));
            colSubtotal.setPrefWidth(100);

            TableColumn<ItemVenda, Void> colRemover = new TableColumn<>("Ações");
            colRemover.setCellFactory(param -> new TableCell<>() {
                private final Button btnRemover = new Button("Remover");
                {
                    btnRemover.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 3 10;");
                    btnRemover.setOnAction(event -> removerItem(getIndex()));
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btnRemover);
                }
            });
            colRemover.setPrefWidth(80);

            tabela.getColumns().addAll(colItem, colProduto, colQtd, colPreco, colSubtotal, colRemover);
            tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            return tabela;
        }

        private VBox criarPainelTotal() {
            VBox painel = new VBox(20);
            painel.setPrefWidth(350);
            painel.setPadding(new Insets(20));
            painel.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
            painel.setAlignment(Pos.TOP_CENTER);

            Label tituloTotal = new Label("TOTAL DA VENDA");
            tituloTotal.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            tituloTotal.setTextFill(Color.valueOf("#2c3e50"));

            lblTotal = new Label("R$ 0,00");
            lblTotal.setFont(Font.font("Arial", FontWeight.BOLD, 48));
            lblTotal.setTextFill(Color.valueOf("#27ae60"));

            lblInfo = new Label("Itens: 0");
            lblInfo.setFont(Font.font("Arial", 14));
            lblInfo.setTextFill(Color.valueOf("#7f8c8d"));

            btnFinalizar = new Button("Finalizar Venda (F1)");
            btnFinalizar.setPrefWidth(Double.MAX_VALUE);
            btnFinalizar.setPrefHeight(50);
            btnFinalizar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px; -fx-cursor: hand;");

            btnCancelar = new Button("Cancelar Venda (F2)");
            btnCancelar.setPrefWidth(Double.MAX_VALUE);
            btnCancelar.setPrefHeight(40);
            btnCancelar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;");

            VBox.setMargin(btnFinalizar, new Insets(20, 0, 10, 0));
            painel.getChildren().addAll(tituloTotal, lblTotal, lblInfo, btnFinalizar, btnCancelar);
            return painel;
        }

        private void configurarEventos() {
            btnAdicionar.setOnAction(e -> adicionarProduto());
            txtCodigoProduto.setOnAction(e -> adicionarProduto());
            btnFinalizar.setOnAction(e -> finalizarVenda());
            btnCancelar.setOnAction(e -> cancelarVenda());
        }

        private void adicionarProduto() {
            String codigo = txtCodigoProduto.getText().trim();
            if (codigo.isEmpty()) {
                mostrarAlerta("Erro", "Digite um código de produto!", Alert.AlertType.WARNING);
                return;
            }

            Produto produto = DataManager.getInstance().buscarProdutoPorCodigo(codigo);

            if (produto == null) {
                mostrarAlerta("Erro", "Produto não encontrado!\nCódigo: " + codigo, Alert.AlertType.ERROR);
                txtCodigoProduto.clear();
                return;
            }

            if (produto.getQuantidadeEstoque() <= 0) {
                mostrarAlerta("Erro", "Produto sem estoque!\nProduto: " + produto.getNome(), Alert.AlertType.ERROR);
                txtCodigoProduto.clear();
                return;
            }

            int quantidade = QuantidadeDialog.showDialog(
                    "Selecionar Quantidade",
                    "Quantidade de " + produto.getNome() + ":",
                    produto.getQuantidadeEstoque()
            );

            if (quantidade > 0) {
                adicionarProdutoDireto(produto, quantidade);
            }
            txtCodigoProduto.clear();
        }

        public boolean adicionarProdutoDireto(Produto produto, int quantidade) {
            if (produto == null || quantidade <= 0) return false;
            if (produto.getQuantidadeEstoque() < quantidade) {
                mostrarAlerta("Erro", "Estoque insuficiente!", Alert.AlertType.ERROR);
                return false;
            }
            try {
                vendaAtual.adicionarItem(produto, quantidade);
                atualizarTabela();
                atualizarTotal();
                return true;
            } catch (IllegalArgumentException e) {
                mostrarAlerta("Erro", e.getMessage(), Alert.AlertType.ERROR);
                return false;
            }
        }

        private void removerItem(int index) {
            if (index >= 0 && index < vendaAtual.getItens().size()) {
                vendaAtual.removerItem(index);
                atualizarTabela();
                atualizarTotal();
            }
        }

        private void finalizarVenda() {
            if (vendaAtual.getItens().isEmpty()) {
                mostrarAlerta("Aviso", "Nenhum item na venda!", Alert.AlertType.WARNING);
                return;
            }
            vendaAtual.finalizarVenda();
            mostrarAlerta("Venda Finalizada",
                    String.format("Venda finalizada com sucesso!\n\nTotal: R$ %.2f\nItens: %d",
                            vendaAtual.getTotal(), vendaAtual.getItens().size()),
                    Alert.AlertType.INFORMATION);
            vendaAtual = new Venda();
            atualizarTabela();
            atualizarTotal();
        }

        private void cancelarVenda() {
            if (vendaAtual.getItens().isEmpty()) return;
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Cancelar Venda");
            alert.setHeaderText(null);
            alert.setContentText("Deseja realmente cancelar esta venda?");
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    for (ItemVenda item : vendaAtual.getItens()) {
                        item.getProduto().setQuantidadeEstoque(
                                item.getProduto().getQuantidadeEstoque() + item.getQuantidade()
                        );
                    }
                    vendaAtual = new Venda();
                    atualizarTabela();
                    atualizarTotal();
                }
            });
        }

        private void atualizarTabela() {
            tabelaVenda.setItems(FXCollections.observableArrayList(vendaAtual.getItens()));
        }

        private void atualizarTotal() {
            lblTotal.setText(String.format("R$ %.2f", vendaAtual.getTotal()));
            lblInfo.setText("Itens: " + vendaAtual.getItens().size());
        }

        private void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipo) {
            Alert alert = new Alert(tipo);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensagem);
            alert.showAndWait();
        }
    }

    public static class EstoqueView extends BorderPane {
        private TableView<Produto> tabelaEstoque;
        private ObservableList<Produto> produtosList;
        private FilteredList<Produto> filteredData;
        private TextField txtBusca;
        private Button btnLimpar, btnAtualizar, btnRelatorio, btnNovoProduto, btnEditarProduto, btnExcluirProduto, btnImportar, btnEnviarCaixa;
        private CaixaView caixaView;

        public EstoqueView() {
            setPadding(new Insets(10));
            setStyle("-fx-background-color: #f5f6fa;");
            produtosList = DataManager.getInstance().getProdutos();
            filteredData = new FilteredList<>(produtosList, p -> true);
            configurarLayout();
            configurarEventos();
        }

        public void setCaixaView(CaixaView caixaView) { this.caixaView = caixaView; }

        private void configurarLayout() {
            setTop(criarPainelSuperior());
            tabelaEstoque = criarTabelaEstoque();
            tabelaEstoque.setItems(filteredData);
            setCenter(tabelaEstoque);
            setBottom(criarPainelInferior());
        }

        private VBox criarPainelSuperior() {
            VBox painel = new VBox(15);
            painel.setPadding(new Insets(15));
            painel.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

            HBox tituloBox = new HBox(15);
            tituloBox.setAlignment(Pos.CENTER_LEFT);
            Label placeholder = new Label("📦");
            placeholder.setFont(Font.font(30));
            tituloBox.getChildren().add(placeholder);
            Label titulo = new Label("CONTROLE DE ESTOQUE");
            titulo.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            titulo.setTextFill(Color.valueOf("#2c3e50"));
            tituloBox.getChildren().add(titulo);

            HBox linhaBusca = new HBox(10);
            linhaBusca.setAlignment(Pos.CENTER_LEFT);
            Label lblBusca = new Label("Buscar Produto:");
            lblBusca.setFont(Font.font("Arial", 14));
            txtBusca = new TextField();
            txtBusca.setPrefWidth(400);
            txtBusca.setPromptText("Digite código, nome, categoria ou status...");
            btnLimpar = new Button("Limpar");
            btnLimpar.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 20;");
            linhaBusca.getChildren().addAll(lblBusca, txtBusca, btnLimpar);

            HBox linhaBotoes = new HBox(10);
            linhaBotoes.setAlignment(Pos.CENTER_LEFT);
            btnNovoProduto = criarBotao("➕ Novo Produto", "#27ae60");
            btnEditarProduto = criarBotao("✏️ Editar", "#f39c12");
            btnExcluirProduto = criarBotao("🗑️ Excluir", "#e74c3c");
            btnEnviarCaixa = criarBotao("🛒 Enviar para Caixa", "#e67e22");
            btnAtualizar = criarBotao("🔄 Atualizar", "#3498db");
            btnRelatorio = criarBotao("📊 Relatório", "#9b59b6");
            btnImportar = criarBotao("📥 Importar", "#1abc9c");
            linhaBotoes.getChildren().addAll(btnNovoProduto, btnEditarProduto, btnExcluirProduto, btnEnviarCaixa, btnAtualizar, btnRelatorio, btnImportar);

            painel.getChildren().addAll(tituloBox, linhaBusca, linhaBotoes);
            return painel;
        }

        private Button criarBotao(String texto, String cor) {
            Button btn = new Button(texto);
            btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 15;", cor));
            return btn;
        }

        private TableView<Produto> criarTabelaEstoque() {
            TableView<Produto> tabela = new TableView<>();
            tabela.setStyle("-fx-font-size: 13px;");

            TableColumn<Produto, Void> colSelecionar = new TableColumn<>("Sel.");
            colSelecionar.setCellFactory(column -> new TableCell<>() {
                private final CheckBox checkBox = new CheckBox();
                {
                    checkBox.setOnAction(event -> {
                        Produto p = getTableView().getItems().get(getIndex());
                        p.setSelecionado(checkBox.isSelected());
                    });
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0) setGraphic(null);
                    else {
                        Produto p = getTableView().getItems().get(getIndex());
                        checkBox.setSelected(p.isSelecionado());
                        setGraphic(checkBox);
                    }
                }
            });
            colSelecionar.setPrefWidth(50);

            CheckBox checkBoxTodos = new CheckBox();
            checkBoxTodos.setOnAction(e -> {
                for (Produto p : tabela.getItems()) p.setSelecionado(checkBoxTodos.isSelected());
                tabela.refresh();
            });
            HBox headerBox = new HBox(checkBoxTodos);
            headerBox.setAlignment(Pos.CENTER);
            colSelecionar.setGraphic(headerBox);

            TableColumn<Produto, String> colCodigo = new TableColumn<>("Código");
            colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
            colCodigo.setPrefWidth(100);

            TableColumn<Produto, String> colNome = new TableColumn<>("Nome");
            colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
            colNome.setPrefWidth(200);

            TableColumn<Produto, String> colCategoria = new TableColumn<>("Categoria");
            colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
            colCategoria.setPrefWidth(120);

            TableColumn<Produto, Double> colPreco = new TableColumn<>("Preço");
            colPreco.setCellValueFactory(new PropertyValueFactory<>("preco"));
            colPreco.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Double preco, boolean empty) {
                    super.updateItem(preco, empty);
                    setText(empty || preco == null ? null : String.format("R$ %.2f", preco));
                }
            });
            colPreco.setPrefWidth(100);

            TableColumn<Produto, Integer> colQuantidade = new TableColumn<>("Qtd");
            colQuantidade.setCellValueFactory(new PropertyValueFactory<>("quantidadeEstoque"));
            colQuantidade.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Integer qtd, boolean empty) {
                    super.updateItem(qtd, empty);
                    if (empty || qtd == null) setText(null);
                    else {
                        setText(String.valueOf(qtd));
                        if (qtd <= 0) setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        else if (qtd < 10) setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                        else setStyle("-fx-text-fill: green;");
                    }
                }
            });
            colQuantidade.setPrefWidth(80);

            TableColumn<Produto, String> colStatus = new TableColumn<>("Status");
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colStatus.setPrefWidth(100);

            TableColumn<Produto, String> colFornecedor = new TableColumn<>("Fornecedor");
            colFornecedor.setCellValueFactory(new PropertyValueFactory<>("fornecedor"));
            colFornecedor.setPrefWidth(150);

            TableColumn<Produto, Void> colAcoes = new TableColumn<>("Ações");
            colAcoes.setCellFactory(col -> new TableCell<>() {
                private final Button btnEntrada = new Button("+");
                private final Button btnSaida = new Button("-");
                private final HBox box = new HBox(5, btnEntrada, btnSaida);
                {
                    btnEntrada.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                    btnSaida.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    btnEntrada.setOnAction(e -> entradaEstoque(getTableView().getItems().get(getIndex())));
                    btnSaida.setOnAction(e -> saidaEstoque(getTableView().getItems().get(getIndex())));
                    box.setAlignment(Pos.CENTER);
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
            colAcoes.setPrefWidth(100);

            tabela.getColumns().addAll(colSelecionar, colCodigo, colNome, colCategoria, colPreco, colQuantidade, colStatus, colFornecedor, colAcoes);
            tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            return tabela;
        }

        private HBox criarPainelInferior() {
            HBox painel = new HBox(20);
            painel.setPadding(new Insets(15));
            painel.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
            painel.setAlignment(Pos.CENTER);
            atualizarEstatisticas(painel);
            return painel;
        }

        private void atualizarEstatisticas(HBox painel) {
            painel.getChildren().clear();
            painel.getChildren().addAll(
                    criarCard("Total Produtos", String.valueOf(DataManager.getInstance().getTotalProdutos()), "#3498db"),
                    criarCard("Itens Estoque", String.valueOf(DataManager.getInstance().getTotalItensEstoque()), "#2ecc71"),
                    criarCard("Valor Total", String.format("R$ %.2f", DataManager.getInstance().getValorTotalEstoque()), "#f39c12"),
                    criarCard("Estoque Baixo", String.valueOf(DataManager.getInstance().getProdutosEstoqueBaixo()), "#e67e22"),
                    criarCard("Sem Estoque", String.valueOf(DataManager.getInstance().getProdutosSemEstoque()), "#e74c3c")
            );
        }

        private VBox criarCard(String titulo, String valor, String cor) {
            VBox card = new VBox(5);
            card.setPadding(new Insets(10));
            card.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 5; -fx-pref-width: 120; -fx-alignment: center;", cor));
            card.getChildren().addAll(
                    new Label(titulo) {{ setTextFill(Color.WHITE); setFont(Font.font("Arial", FontWeight.NORMAL, 11)); }},
                    new Label(valor) {{ setTextFill(Color.WHITE); setFont(Font.font("Arial", FontWeight.BOLD, 16)); }}
            );
            return card;
        }

        private void configurarEventos() {
            txtBusca.textProperty().addListener((obs, old, n) ->
                    filteredData.setPredicate(p -> n == null || n.isEmpty() ||
                            p.getCodigo().toLowerCase().contains(n.toLowerCase()) ||
                            p.getNome().toLowerCase().contains(n.toLowerCase()) ||
                            p.getCategoria().toLowerCase().contains(n.toLowerCase()) ||
                            p.getStatus().toLowerCase().contains(n.toLowerCase()))
            );
            btnLimpar.setOnAction(e -> txtBusca.clear());
            btnAtualizar.setOnAction(e -> tabelaEstoque.refresh());
            btnRelatorio.setOnAction(e -> gerarRelatorio());
            btnNovoProduto.setOnAction(e -> abrirDialogNovoProduto());
            btnEditarProduto.setOnAction(e -> {
                Produto p = tabelaEstoque.getSelectionModel().getSelectedItem();
                if (p != null) editarProduto(p);
                else mostrarAlerta("Aviso", "Selecione um produto!");
            });
            btnExcluirProduto.setOnAction(e -> excluirSelecionados());
            btnImportar.setOnAction(e -> mostrarAlerta("Importar", "Função em desenvolvimento..."));
            btnEnviarCaixa.setOnAction(e -> enviarSelecionadosParaCaixa());
        }

        private void entradaEstoque(Produto p) {
            int qtd = QuantidadeDialog.showDialog("Entrada", "Adicionar a " + p.getNome(), 1000);
            if (qtd > 0) {
                p.setQuantidadeEstoque(p.getQuantidadeEstoque() + qtd);
                tabelaEstoque.refresh();
                atualizarEstatisticas((HBox) getBottom());
            }
        }

        private void saidaEstoque(Produto p) {
            int qtd = QuantidadeDialog.showDialog("Saída", "Remover de " + p.getNome(), p.getQuantidadeEstoque());
            if (qtd > 0) {
                p.setQuantidadeEstoque(p.getQuantidadeEstoque() - qtd);
                tabelaEstoque.refresh();
                atualizarEstatisticas((HBox) getBottom());
            }
        }

        private void enviarSelecionadosParaCaixa() {
            if (caixaView == null) {
                mostrarAlerta("Erro", "Caixa não disponível!");
                return;
            }
            ObservableList<Produto> selecionados = FXCollections.observableArrayList();
            for (Produto p : produtosList) if (p.isSelecionado()) selecionados.add(p);
            if (selecionados.isEmpty()) {
                mostrarAlerta("Aviso", "Selecione produtos!");
                return;
            }
            int count = 0;
            for (Produto p : selecionados) {
                if (p.getQuantidadeEstoque() <= 0) continue;
                int qtd = QuantidadeDialog.showDialog("Quantidade", p.getNome() + ":", p.getQuantidadeEstoque());
                if (qtd > 0 && caixaView.adicionarProdutoDireto(p, qtd)) count++;
            }
            for (Produto p : produtosList) p.setSelecionado(false);
            tabelaEstoque.refresh();
            if (count > 0) mostrarAlerta("Sucesso", count + " produtos enviados!");
        }

        private void abrirDialogNovoProduto() {
            Dialog<Produto> dialog = new Dialog<>();
            dialog.setTitle("Novo Produto");
            ButtonType save = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
            TextField txtCodigo = new TextField(); txtCodigo.setPromptText("Código");
            TextField txtNome = new TextField(); txtNome.setPromptText("Nome");
            TextField txtPreco = new TextField(); txtPreco.setPromptText("Preço");
            TextField txtQtd = new TextField(); txtQtd.setPromptText("Quantidade");
            TextField txtCat = new TextField(); txtCat.setPromptText("Categoria");
            TextField txtForn = new TextField(); txtForn.setPromptText("Fornecedor");
            grid.addRow(0, new Label("Código:"), txtCodigo);
            grid.addRow(1, new Label("Nome:"), txtNome);
            grid.addRow(2, new Label("Preço:"), txtPreco);
            grid.addRow(3, new Label("Quantidade:"), txtQtd);
            grid.addRow(4, new Label("Categoria:"), txtCat);
            grid.addRow(5, new Label("Fornecedor:"), txtForn);
            dialog.getDialogPane().setContent(grid);
            dialog.setResultConverter(btn -> {
                if (btn == save) {
                    try {
                        return new Produto(txtCodigo.getText(), txtNome.getText(),
                                Double.parseDouble(txtPreco.getText()),
                                Integer.parseInt(txtQtd.getText()),
                                txtCat.getText(), txtForn.getText());
                    } catch (Exception e) {
                        mostrarAlerta("Erro", "Dados inválidos!");
                    }
                }
                return null;
            });
            Optional<Produto> result = dialog.showAndWait();
            result.ifPresent(p -> {
                DataManager.getInstance().adicionarProduto(p);
                tabelaEstoque.refresh();
                atualizarEstatisticas((HBox) getBottom());
            });
        }

        private void editarProduto(Produto p) {
            Dialog<Produto> dialog = new Dialog<>();
            dialog.setTitle("Editar Produto");
            ButtonType save = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
            TextField txtCodigo = new TextField(p.getCodigo());
            TextField txtNome = new TextField(p.getNome());
            TextField txtPreco = new TextField(String.valueOf(p.getPreco()));
            TextField txtQtd = new TextField(String.valueOf(p.getQuantidadeEstoque()));
            TextField txtCat = new TextField(p.getCategoria());
            TextField txtForn = new TextField(p.getFornecedor());
            grid.addRow(0, new Label("Código:"), txtCodigo);
            grid.addRow(1, new Label("Nome:"), txtNome);
            grid.addRow(2, new Label("Preço:"), txtPreco);
            grid.addRow(3, new Label("Quantidade:"), txtQtd);
            grid.addRow(4, new Label("Categoria:"), txtCat);
            grid.addRow(5, new Label("Fornecedor:"), txtForn);
            dialog.getDialogPane().setContent(grid);
            dialog.setResultConverter(btn -> {
                if (btn == save) {
                    p.setCodigo(txtCodigo.getText());
                    p.setNome(txtNome.getText());
                    p.setPreco(Double.parseDouble(txtPreco.getText()));
                    p.setQuantidadeEstoque(Integer.parseInt(txtQtd.getText()));
                    p.setCategoria(txtCat.getText());
                    p.setFornecedor(txtForn.getText());
                    return p;
                }
                return null;
            });
            dialog.showAndWait();
            tabelaEstoque.refresh();
            atualizarEstatisticas((HBox) getBottom());
        }

        private void excluirSelecionados() {
            ObservableList<Produto> selecionados = FXCollections.observableArrayList();
            for (Produto p : produtosList) if (p.isSelecionado()) selecionados.add(p);
            if (selecionados.isEmpty()) {
                mostrarAlerta("Aviso", "Selecione produtos!");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setContentText("Excluir " + selecionados.size() + " produto(s)?");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    produtosList.removeAll(selecionados);
                    tabelaEstoque.refresh();
                    atualizarEstatisticas((HBox) getBottom());
                }
            });
        }

        private void gerarRelatorio() {
            String relatorio = String.format("""
                📊 RELATÓRIO DE ESTOQUE
                =======================
                
                📦 Total de Produtos: %d
                📦 Itens em Estoque: %d
                💰 Valor Total: R$ %.2f
                
                ⚠️ Estoque Baixo: %d
                ❌ Sem Estoque: %d
                """,
                    DataManager.getInstance().getTotalProdutos(),
                    DataManager.getInstance().getTotalItensEstoque(),
                    DataManager.getInstance().getValorTotalEstoque(),
                    DataManager.getInstance().getProdutosEstoqueBaixo(),
                    DataManager.getInstance().getProdutosSemEstoque()
            );
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Relatório");
            alert.setContentText(relatorio);
            alert.showAndWait();
        }

        private void mostrarAlerta(String titulo, String msg) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setContentText(msg);
            alert.showAndWait();
        }
    }

    public static class GestaoView extends BorderPane {
        public GestaoView() {
            setPadding(new Insets(20));
            setStyle("-fx-background-color: #f5f6fa;");

            Label titulo = new Label("MÓDULO DE GESTÃO");
            titulo.setFont(Font.font("Arial", FontWeight.BOLD, 28));
            titulo.setTextFill(Color.valueOf("#2c3e50"));
            titulo.setPadding(new Insets(0, 0, 20, 0));
            titulo.setAlignment(Pos.CENTER);
            setTop(titulo);

            VBox painel = new VBox(20);
            painel.setAlignment(Pos.CENTER);

            Button btnRelatorio = new Button("📊 Ver Relatório de Estoque");
            btnRelatorio.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 15 30; -fx-cursor: hand;");
            btnRelatorio.setOnAction(e -> mostrarRelatorio());

            painel.getChildren().add(btnRelatorio);
            setCenter(painel);
        }

        private void mostrarRelatorio() {
            String relatorio = String.format("""
                📈 DASHBOARD DO SISTEMA
                =======================
                
                Total de Produtos: %d
                Itens em Estoque: %d
                Valor Total em Estoque: R$ %.2f
                
                Produtos com Estoque Baixo: %d
                Produtos sem Estoque: %d
                """,
                    DataManager.getInstance().getTotalProdutos(),
                    DataManager.getInstance().getTotalItensEstoque(),
                    DataManager.getInstance().getValorTotalEstoque(),
                    DataManager.getInstance().getProdutosEstoqueBaixo(),
                    DataManager.getInstance().getProdutosSemEstoque()
            );
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Relatório de Gestão");
            alert.setContentText(relatorio);
            alert.showAndWait();
        }
    }

    public static class ProdutosView extends BorderPane {
        private TextField txtNome, txtCodigo, txtPreco, txtQuantidade;
        private TableView<Produto> tabelaProdutos;
        private ObservableList<Produto> produtosList;
        private Button btnSalvar, btnLimpar, btnEditar, btnExcluir;

        public ProdutosView() {
            setPadding(new Insets(10));
            setStyle("-fx-background-color: #f5f6fa;");
            inicializarDados();
            configurarLayout();
            configurarEventos();
        }

        private void inicializarDados() {
            produtosList = FXCollections.observableArrayList();
            produtosList.addAll(
                    new Produto("789001", "Arroz Tipo 1 (5kg)", 25.50, 150),
                    new Produto("789002", "Feijão Carioca (1kg)", 8.90, 200),
                    new Produto("789003", "Óleo de Soja (900ml)", 7.25, 300)
            );
        }

        private void configurarLayout() {
            SplitPane splitPane = new SplitPane();
            splitPane.setDividerPositions(0.4);
            splitPane.getItems().addAll(criarPainelFormulario(), criarPainelTabela());
            setCenter(splitPane);
        }

        private VBox criarPainelFormulario() {
            VBox painel = new VBox(15);
            painel.setPadding(new Insets(20));
            painel.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

            Label titulo = new Label("CADASTRO DE PRODUTO");
            titulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            titulo.setTextFill(Color.valueOf("#2c3e50"));

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(15); grid.setPadding(new Insets(10, 0, 10, 0));

            grid.add(new Label("Nome:"), 0, 0);
            txtNome = new TextField();
            txtNome.setPrefWidth(300);
            grid.add(txtNome, 1, 0);

            grid.add(new Label("Código de Barras:"), 0, 1);
            txtCodigo = new TextField();
            grid.add(txtCodigo, 1, 1);

            grid.add(new Label("Preço (R$):"), 0, 2);
            txtPreco = new TextField();
            grid.add(txtPreco, 1, 2);

            grid.add(new Label("Quantidade em Estoque:"), 0, 3);
            txtQuantidade = new TextField();
            grid.add(txtQuantidade, 1, 3);

            HBox botoesForm = new HBox(10);
            botoesForm.setAlignment(Pos.CENTER_RIGHT);

            btnSalvar = new Button("Salvar");
            btnSalvar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 20;");
            btnLimpar = new Button("Limpar");
            btnLimpar.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 10 20;");
            botoesForm.getChildren().addAll(btnSalvar, btnLimpar);

            painel.getChildren().addAll(titulo, grid, botoesForm);
            return painel;
        }

        private VBox criarPainelTabela() {
            VBox painel = new VBox(10);
            painel.setPadding(new Insets(20));
            painel.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");

            Label titulo = new Label("PRODUTOS CADASTRADOS");
            titulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            titulo.setTextFill(Color.valueOf("#2c3e50"));

            tabelaProdutos = criarTabelaProdutos();
            tabelaProdutos.setItems(produtosList);

            HBox botoesTabela = new HBox(10);
            botoesTabela.setAlignment(Pos.CENTER_RIGHT);

            btnEditar = new Button("Editar Selecionado");
            btnEditar.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 15;");
            btnExcluir = new Button("Excluir Selecionado");
            btnExcluir.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 15;");
            botoesTabela.getChildren().addAll(btnEditar, btnExcluir);

            painel.getChildren().addAll(titulo, tabelaProdutos, botoesTabela);
            VBox.setVgrow(tabelaProdutos, Priority.ALWAYS);
            return painel;
        }

        private TableView<Produto> criarTabelaProdutos() {
            TableView<Produto> tabela = new TableView<>();
            TableColumn<Produto, String> colCodigo = new TableColumn<>("Código");
            colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
            colCodigo.setPrefWidth(100);
            TableColumn<Produto, String> colNome = new TableColumn<>("Nome");
            colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
            colNome.setPrefWidth(200);
            TableColumn<Produto, Double> colPreco = new TableColumn<>("Preço");
            colPreco.setCellValueFactory(new PropertyValueFactory<>("preco"));
            colPreco.setCellFactory(column -> new TableCell<>() {
                @Override protected void updateItem(Double preco, boolean empty) {
                    super.updateItem(preco, empty);
                    setText(empty || preco == null ? null : String.format("R$ %.2f", preco));
                }
            });
            colPreco.setPrefWidth(100);
            TableColumn<Produto, Integer> colQuantidade = new TableColumn<>("Qtd. Estoque");
            colQuantidade.setCellValueFactory(new PropertyValueFactory<>("quantidadeEstoque"));
            colQuantidade.setPrefWidth(100);
            tabela.getColumns().addAll(colCodigo, colNome, colPreco, colQuantidade);
            tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            return tabela;
        }

        private void configurarEventos() {
            btnSalvar.setOnAction(e -> salvarProduto());
            btnLimpar.setOnAction(e -> limparFormulario());
            btnEditar.setOnAction(e -> editarProduto());
            btnExcluir.setOnAction(e -> excluirProduto());
        }

        private void salvarProduto() {
            if (txtNome.getText() == null || txtNome.getText().trim().isEmpty()) {
                mostrarAlerta("Erro", "O campo Nome é obrigatório!"); txtNome.requestFocus(); return;
            }
            if (txtCodigo.getText() == null || txtCodigo.getText().trim().isEmpty()) {
                mostrarAlerta("Erro", "O campo Código de Barras é obrigatório!"); txtCodigo.requestFocus(); return;
            }
            if (txtPreco.getText() == null || txtPreco.getText().trim().isEmpty()) {
                mostrarAlerta("Erro", "O campo Preço é obrigatório!"); txtPreco.requestFocus(); return;
            }
            if (txtQuantidade.getText() == null || txtQuantidade.getText().trim().isEmpty()) {
                mostrarAlerta("Erro", "O campo Quantidade é obrigatório!"); txtQuantidade.requestFocus(); return;
            }

            try {
                String codigo = txtCodigo.getText().trim();
                String nome = txtNome.getText().trim();
                double preco = Double.parseDouble(txtPreco.getText().trim().replace(",", "."));
                int quantidade = Integer.parseInt(txtQuantidade.getText().trim());

                if (preco <= 0) { mostrarAlerta("Erro", "O preço deve ser maior que zero!"); txtPreco.requestFocus(); return; }
                if (quantidade < 0) { mostrarAlerta("Erro", "A quantidade não pode ser negativa!"); txtQuantidade.requestFocus(); return; }

                for (Produto p : produtosList) {
                    if (p.getCodigo().equals(codigo)) {
                        p.setNome(nome); p.setPreco(preco); p.setQuantidadeEstoque(quantidade);
                        tabelaProdutos.refresh(); limparFormulario();
                        mostrarAlerta("Sucesso", "Produto atualizado com sucesso!"); return;
                    }
                }

                Produto novoProduto = new Produto(codigo, nome, preco, quantidade);
                produtosList.add(novoProduto);
                limparFormulario();
                mostrarAlerta("Sucesso", "Produto cadastrado com sucesso!");
            } catch (NumberFormatException e) {
                mostrarAlerta("Erro", "Preço e quantidade devem ser números válidos!");
            }
        }

        private void editarProduto() {
            Produto selecionado = tabelaProdutos.getSelectionModel().getSelectedItem();
            if (selecionado == null) { mostrarAlerta("Aviso", "Selecione um produto para editar!"); return; }
            txtCodigo.setText(selecionado.getCodigo());
            txtNome.setText(selecionado.getNome());
            txtPreco.setText(String.valueOf(selecionado.getPreco()));
            txtQuantidade.setText(String.valueOf(selecionado.getQuantidadeEstoque()));
            txtCodigo.setEditable(false);
            txtNome.requestFocus();
        }

        private void excluirProduto() {
            Produto selecionado = tabelaProdutos.getSelectionModel().getSelectedItem();
            if (selecionado == null) { mostrarAlerta("Aviso", "Selecione um produto para excluir!"); return; }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmar Exclusão");
            confirm.setHeaderText(null);
            confirm.setContentText("Deseja realmente excluir o produto " + selecionado.getNome() + "?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    produtosList.remove(selecionado);
                    limparFormulario();
                    mostrarAlerta("Sucesso", "Produto excluído com sucesso!");
                }
            });
        }

        private void limparFormulario() {
            txtCodigo.clear(); txtNome.clear(); txtPreco.clear(); txtQuantidade.clear();
            txtCodigo.setEditable(true);
            txtNome.requestFocus();
        }

        private void mostrarAlerta(String titulo, String mensagem) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo); alert.setHeaderText(null); alert.setContentText(mensagem);
            alert.showAndWait();
        }
    }

    public static class MainView extends BorderPane {
        private StackPane contentArea;
        private CaixaView caixaView;
        private GestaoView gestaoView;
        private EstoqueView estoqueView;
        private ProdutosView produtosView;

        public MainView() {
            caixaView = new CaixaView();
            gestaoView = new GestaoView();
            estoqueView = new EstoqueView();
            produtosView = new ProdutosView();
            estoqueView.setCaixaView(caixaView);
            configurarLayout();
        }

        private void configurarLayout() {
            VBox menuLateral = criarMenuLateral();
            menuLateral.setStyle("-fx-background-color: #2d3436;");
            menuLateral.setPrefWidth(200);
            menuLateral.setPadding(new Insets(20, 10, 20, 10));

            contentArea = new StackPane();
            contentArea.setStyle("-fx-background-color: #f5f6fa;");
            contentArea.getChildren().add(caixaView);

            setLeft(menuLateral);
            setCenter(contentArea);
        }

        private VBox criarMenuLateral() {
            VBox menu = new VBox(15);
            menu.setAlignment(Pos.TOP_CENTER);

            Label titulo = new Label("MENU");
            titulo.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            titulo.setTextFill(Color.WHITE);

            Button btnCaixa = criarBotaoMenu("Caixa", "#636e72");
            Button btnGestao = criarBotaoMenu("Gestão", "#636e72");
            Button btnEstoque = criarBotaoMenu("Estoque", "#636e72");
            Button btnProdutos = criarBotaoMenu("Produtos", "#636e72");

            btnCaixa.setOnAction(e -> contentArea.getChildren().set(0, caixaView));
            btnGestao.setOnAction(e -> contentArea.getChildren().set(0, gestaoView));
            btnEstoque.setOnAction(e -> {
                estoqueView.setCaixaView(caixaView);
                contentArea.getChildren().set(0, estoqueView);
            });
            btnProdutos.setOnAction(e -> contentArea.getChildren().set(0, produtosView));

            menu.getChildren().addAll(titulo, btnCaixa, btnGestao, btnEstoque, btnProdutos);

            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.color(0, 0, 0, 0.3));
            menu.setEffect(shadow);

            return menu;
        }

        private Button criarBotaoMenu(String texto, String cor) {
            Button btn = new Button(texto);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setFont(Font.font("Arial", 16));
            btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 15 20; -fx-cursor: hand; -fx-background-radius: 5;", cor));
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-padding: 15 20; -fx-cursor: hand; -fx-background-radius: 5;"));
            btn.setOnMouseExited(e -> btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 15 20; -fx-cursor: hand; -fx-background-radius: 5;", cor)));
            return btn;
        }
    }
}