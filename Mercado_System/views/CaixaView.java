package New_MercadoSystem.views;

import New_MercadoSystem.models.ItemVenda;
import New_MercadoSystem.models.Produto;
import New_MercadoSystem.models.Venda;
import New_MercadoSystem.utils.DataManager;
import New_MercadoSystem.utils.QuantidadeDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

public class CaixaView extends BorderPane {

    private TableView<ItemVenda> tabelaVenda;
    private Label lblTotal;
    private Label lblInfo;
    private TextField txtCodigoProduto;
    private Venda vendaAtual;

    private Button btnAdicionar;
    private Button btnFinalizar;
    private Button btnCancelar;

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
        btnAdicionar.setStyle(
                "-fx-background-color: #27ae60;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 15;"
        );

        areaCodigo.getChildren().addAll(lblCodigo, txtCodigoProduto, btnAdicionar);

        tabelaVenda = criarTabelaVenda();

        painel.getChildren().addAll(titulo, areaCodigo, tabelaVenda);
        VBox.setVgrow(tabelaVenda, Priority.ALWAYS);

        return painel;
    }

    private TableView<ItemVenda> criarTabelaVenda() {
        TableView<ItemVenda> tabela = new TableView<>();

        TableColumn<ItemVenda, String> colItem = new TableColumn<>("Item");
        colItem.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(tabela.getItems().indexOf(cellData.getValue()) + 1)));
        colItem.setPrefWidth(50);

        TableColumn<ItemVenda, String> colProduto = new TableColumn<>("Produto");
        colProduto.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getProduto().getNome()));
        colProduto.setPrefWidth(300);

        TableColumn<ItemVenda, Integer> colQtd = new TableColumn<>("Qtd.");
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colQtd.setPrefWidth(70);

        TableColumn<ItemVenda, String> colPreco = new TableColumn<>("Preço Unit.");
        colPreco.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("R$ %.2f", cellData.getValue().getProduto().getPreco())));
        colPreco.setPrefWidth(100);

        TableColumn<ItemVenda, String> colSubtotal = new TableColumn<>("Subtotal");
        colSubtotal.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("R$ %.2f", cellData.getValue().getSubtotal())));
        colSubtotal.setPrefWidth(100);

        TableColumn<ItemVenda, Void> colRemover = new TableColumn<>("Ações");
        colRemover.setCellFactory(param -> new TableCell<>() {
            private final Button btnRemover = new Button("Remover");
            {
                btnRemover.setStyle(
                        "-fx-background-color: #e74c3c;" +
                                "-fx-text-fill: white;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 3 10;"
                );
                btnRemover.setOnAction(event -> removerItem(getIndex()));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
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
        btnFinalizar.setStyle(
                "-fx-background-color: #27ae60;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;"
        );

        btnCancelar = new Button("Cancelar Venda (F2)");
        btnCancelar.setPrefWidth(Double.MAX_VALUE);
        btnCancelar.setPrefHeight(40);
        btnCancelar.setStyle(
                "-fx-background-color: #e74c3c;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-cursor: hand;"
        );

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

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Venda Finalizada");
        alert.setHeaderText(null);
        alert.setContentText(String.format(
                "Venda finalizada com sucesso!\n\nTotal: R$ %.2f\nItens: %d",
                vendaAtual.getTotal(), vendaAtual.getItens().size()
        ));
        alert.showAndWait();

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