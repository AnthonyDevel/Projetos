package New_MercadoSystem.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;

public class MainView extends BorderPane {

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
        btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 15 20; -fx-cursor: hand; -fx-background-radius: 5;", cor
        ));

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-padding: 15 20; -fx-cursor: hand; -fx-background-radius: 5;"));
        btn.setOnMouseExited(e -> btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-padding: 15 20; -fx-cursor: hand; -fx-background-radius: 5;", cor)));

        return btn;
    }
}