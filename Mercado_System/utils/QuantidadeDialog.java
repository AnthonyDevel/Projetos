package New_MercadoSystem.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class QuantidadeDialog {

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
        btnConfirmar.setStyle(
                "-fx-background-color: #27ae60;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 20;" +
                        "-fx-font-size: 14px;"
        );

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle(
                "-fx-background-color: #e74c3c;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 20;" +
                        "-fx-font-size: 14px;"
        );

        btnConfirmar.setOnAction(e -> {
            resultado[0] = spinner.getValue();
            dialog.close();
        });

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