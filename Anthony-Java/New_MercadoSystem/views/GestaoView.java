package New_MercadoSystem.views;

import New_MercadoSystem.utils.DataManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

public class GestaoView extends BorderPane {

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