package New_MercadoSystem.views;

import New_MercadoSystem.models.Produto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

public class ProdutosView extends BorderPane {

    private TextField txtNome, txtCodigo, txtPreco, txtQuantidade;
    private TableView<Produto> tabelaProdutos;
    private ObservableList<Produto> produtosList;

    // Botões como variáveis de instância
    private Button btnSalvar;
    private Button btnLimpar;
    private Button btnEditar;
    private Button btnExcluir;

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
        // SplitPane para dividir formulário e tabela
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.4);

        // Painel do Formulário
        VBox painelFormulario = criarPainelFormulario();

        // Painel da Tabela
        VBox painelTabela = criarPainelTabela();

        splitPane.getItems().addAll(painelFormulario, painelTabela);

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
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(10, 0, 10, 0));

        // Campos do formulário
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

        // Botões do formulário
        HBox botoesForm = new HBox(10);
        botoesForm.setAlignment(Pos.CENTER_RIGHT);

        btnSalvar = new Button("Salvar");
        btnSalvar.setStyle(
                "-fx-background-color: #27ae60;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 20;"
        );

        btnLimpar = new Button("Limpar");
        btnLimpar.setStyle(
                "-fx-background-color: #7f8c8d;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 20;"
        );

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

        // Tabela
        tabelaProdutos = criarTabelaProdutos();
        tabelaProdutos.setItems(produtosList);

        // Botões da tabela
        HBox botoesTabela = new HBox(10);
        botoesTabela.setAlignment(Pos.CENTER_RIGHT);

        btnEditar = new Button("Editar Selecionado");
        btnEditar.setStyle(
                "-fx-background-color: #f39c12;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 15;"
        );

        btnExcluir = new Button("Excluir Selecionado");
        btnExcluir.setStyle(
                "-fx-background-color: #e74c3c;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 15;"
        );

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
            @Override
            protected void updateItem(Double preco, boolean empty) {
                super.updateItem(preco, empty);
                if (empty || preco == null) {
                    setText(null);
                } else {
                    setText(String.format("R$ %.2f", preco));
                }
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
        // Agora podemos referenciar os botões diretamente pelas variáveis de instância
        btnSalvar.setOnAction(e -> salvarProduto());
        btnLimpar.setOnAction(e -> limparFormulario());
        btnEditar.setOnAction(e -> editarProduto());
        btnExcluir.setOnAction(e -> excluirProduto());

        // Atalho de teclado para salvar (Ctrl+Enter)
        setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode().toString().equals("ENTER")) {
                salvarProduto();
            }
        });
    }

    private void salvarProduto() {
        // Validação dos campos
        if (txtNome.getText() == null || txtNome.getText().trim().isEmpty()) {
            mostrarAlerta("Erro", "O campo Nome é obrigatório!");
            txtNome.requestFocus();
            return;
        }

        if (txtCodigo.getText() == null || txtCodigo.getText().trim().isEmpty()) {
            mostrarAlerta("Erro", "O campo Código de Barras é obrigatório!");
            txtCodigo.requestFocus();
            return;
        }

        if (txtPreco.getText() == null || txtPreco.getText().trim().isEmpty()) {
            mostrarAlerta("Erro", "O campo Preço é obrigatório!");
            txtPreco.requestFocus();
            return;
        }

        if (txtQuantidade.getText() == null || txtQuantidade.getText().trim().isEmpty()) {
            mostrarAlerta("Erro", "O campo Quantidade é obrigatório!");
            txtQuantidade.requestFocus();
            return;
        }

        try {
            String codigo = txtCodigo.getText().trim();
            String nome = txtNome.getText().trim();
            double preco = Double.parseDouble(txtPreco.getText().trim().replace(",", "."));
            int quantidade = Integer.parseInt(txtQuantidade.getText().trim());

            // Validar valores positivos
            if (preco <= 0) {
                mostrarAlerta("Erro", "O preço deve ser maior que zero!");
                txtPreco.requestFocus();
                return;
            }

            if (quantidade < 0) {
                mostrarAlerta("Erro", "A quantidade não pode ser negativa!");
                txtQuantidade.requestFocus();
                return;
            }

            // Verificar se produto já existe
            for (Produto p : produtosList) {
                if (p.getCodigo().equals(codigo)) {
                    // Atualizar existente
                    p.setNome(nome);
                    p.setPreco(preco);
                    p.setQuantidadeEstoque(quantidade);
                    tabelaProdutos.refresh();
                    limparFormulario();
                    mostrarAlerta("Sucesso", "Produto atualizado com sucesso!");
                    return;
                }
            }

            // Criar novo produto
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
        if (selecionado == null) {
            mostrarAlerta("Aviso", "Selecione um produto para editar!");
            return;
        }

        txtCodigo.setText(selecionado.getCodigo());
        txtNome.setText(selecionado.getNome());
        txtPreco.setText(String.valueOf(selecionado.getPreco()));
        txtQuantidade.setText(String.valueOf(selecionado.getQuantidadeEstoque()));

        txtCodigo.setEditable(false);

        // Focar no campo nome para facilitar a edição
        txtNome.requestFocus();
    }

    private void excluirProduto() {
        Produto selecionado = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarAlerta("Aviso", "Selecione um produto para excluir!");
            return;
        }

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
        txtCodigo.clear();
        txtNome.clear();
        txtPreco.clear();
        txtQuantidade.clear();
        txtCodigo.setEditable(true);
        txtNome.requestFocus(); // Focar no nome para novo cadastro
    }

    private void mostrarAlerta(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    // Métodos getters para acessar os componentes (se necessário)
    public TableView<Produto> getTabelaProdutos() {
        return tabelaProdutos;
    }

    public ObservableList<Produto> getProdutosList() {
        return produtosList;
    }
}