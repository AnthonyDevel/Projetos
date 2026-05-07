package New_MercadoSystem.views;

import New_MercadoSystem.models.Produto;
import New_MercadoSystem.utils.DataManager;
import New_MercadoSystem.utils.QuantidadeDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

import java.util.Optional;

public class EstoqueView extends BorderPane {

    private TableView<Produto> tabelaEstoque;
    private ObservableList<Produto> produtosList;
    private FilteredList<Produto> filteredData;
    private TextField txtBusca;

    private Button btnLimpar;
    private Button btnAtualizar;
    private Button btnRelatorio;
    private Button btnNovoProduto;
    private Button btnEditarProduto;
    private Button btnExcluirProduto;
    private Button btnImportar;
    private Button btnEnviarCaixa;

    private CaixaView caixaView;

    public EstoqueView() {
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #f5f6fa;");

        produtosList = DataManager.getInstance().getProdutos();
        filteredData = new FilteredList<>(produtosList, p -> true);

        configurarLayout();
        configurarEventos();
    }

    public void setCaixaView(CaixaView caixaView) {
        this.caixaView = caixaView;
    }

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

        ImageView logoView = new ImageView();
        try {
            Image logo = new Image(getClass().getResourceAsStream("/logo.png"));
            logoView.setImage(logo);
            logoView.setFitHeight(40);
            logoView.setFitWidth(40);
            tituloBox.getChildren().add(logoView);
        } catch (Exception e) {
            Label placeholder = new Label("📦");
            placeholder.setFont(Font.font(30));
            tituloBox.getChildren().add(placeholder);
        }

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

        btnEnviarCaixa.setOnAction(e -> enviarSelecionadosParaCaixa());

        linhaBotoes.getChildren().addAll(
                btnNovoProduto, btnEditarProduto, btnExcluirProduto,
                btnEnviarCaixa, btnAtualizar, btnRelatorio, btnImportar
        );

        painel.getChildren().addAll(tituloBox, linhaBusca, linhaBotoes);
        return painel;
    }

    private Button criarBotao(String texto, String cor) {
        Button btn = new Button(texto);
        btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 15;", cor
        ));
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
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0) {
                    setGraphic(null);
                } else {
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
            @Override
            protected void updateItem(Double preco, boolean empty) {
                super.updateItem(preco, empty);
                setText(empty || preco == null ? null : String.format("R$ %.2f", preco));
            }
        });
        colPreco.setPrefWidth(100);

        TableColumn<Produto, Integer> colQuantidade = new TableColumn<>("Qtd");
        colQuantidade.setCellValueFactory(new PropertyValueFactory<>("quantidadeEstoque"));
        colQuantidade.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer qtd, boolean empty) {
                super.updateItem(qtd, empty);
                if (empty || qtd == null) {
                    setText(null);
                } else {
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
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        colAcoes.setPrefWidth(100);

        tabela.getColumns().addAll(colSelecionar, colCodigo, colNome, colCategoria, colPreco,
                colQuantidade, colStatus, colFornecedor, colAcoes);

        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tabela.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Produto p = tabela.getSelectionModel().getSelectedItem();
                if (p != null) editarProduto(p);
            }
        });

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
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

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
                    return new Produto(
                            txtCodigo.getText(), txtNome.getText(),
                            Double.parseDouble(txtPreco.getText()),
                            Integer.parseInt(txtQtd.getText()),
                            txtCat.getText(), txtForn.getText()
                    );
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
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

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