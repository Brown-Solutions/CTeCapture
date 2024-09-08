package org.brownsolutions.controller;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.brownsolutions.Main;
import org.brownsolutions.data.DataProcessor;
import org.brownsolutions.utils.AlertPane;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Controller {

    private DataProcessor dataProcessor;

    public PasswordField passwordField;

    public ProgressBar progress;

    public Button executeButton;
    public Button selectButton;

    public void onExecute(ActionEvent actionEvent) {
        if (passwordField.getText().isEmpty()) {
            AlertPane.showAlert("Erro!", "Por favor, insira sua senha para continuar.");
            return;
        }

        dataProcessor.setPassword(passwordField.getText());
        new Thread(dataProcessor).start();
        selectButton.setDisable(true);
        executeButton.setDisable(true);
    }

    public void onSelect(ActionEvent actionEvent) {
        try {
            File file = showFileDialog(actionEvent);
            if (file == null) return;

            InputStream pfxStream = Main.class.getClassLoader().getResourceAsStream("cer_matriz.pfx");
            InputStream crtStream = Main.class.getClassLoader().getResourceAsStream("svrs.crt");

            if (pfxStream == null || crtStream == null) {
                AlertPane.showAlert("Erro!", "Certificado(s) não encontrado(s).");
                return;
            }

            File tempPfxFile = createTempFile(pfxStream, "certified_usaflex", ".pfx");
            File tempCrtFile = createTempFile(crtStream, "certified_svrs", ".crt");

            dataProcessor = new DataProcessor(file.getAbsolutePath(), tempCrtFile.getAbsolutePath(), tempPfxFile.getAbsolutePath(), executeButton, selectButton, progress);
            executeButton.setDisable(false);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private File createTempFile(InputStream inputStream, String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        try (inputStream; FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
        }
        return tempFile;
    }

    private File showFileDialog(Event event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar arquivo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivo Excel", "*.xlsx", "*.xls"));

        Stage stage = getStage(event);

        return fileChooser.showOpenDialog(stage);
    }

    private Stage getStage(Event event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }
}
