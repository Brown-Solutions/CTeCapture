package org.brownsolutions.data;

import com.aspose.cells.*;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import org.brownsolutions.model.CTeDTO;
import org.brownsolutions.model.CTeResponseDTO;
import org.brownsolutions.service.CTeStatusService;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataProcessor implements Runnable {

    private final String PATH = System.getProperty("user.home") + "\\Downloads\\";
    private final String FILE_PREFIX = "ctes_cancelados";

    private final HashMap<String, CTeResponseDTO> cTesCanceled = new HashMap<>();

    private final javafx.scene.control.Button executeButton;
    private final javafx.scene.control.Button selectButton;
    private final ProgressBar progress;

    private final String certifiedServerPath;
    private final String certifiedPath;
    private final String filePath;
    private String password;

    private Workbook workbook;


    public DataProcessor(String filePath, String certifiedServerPath, String certifiedPath, javafx.scene.control.Button executeButton, javafx.scene.control.Button selectButton, ProgressBar progress) {
        this.filePath = filePath;
        this.certifiedServerPath = certifiedServerPath;
        this.certifiedPath = certifiedPath;
        this.executeButton = executeButton;
        this.selectButton = selectButton;
        this.progress = progress;
    }

    private void process() {
        List<CTeDTO> cTes = new DataReader().execute(filePath);
        Platform.runLater(() -> progress.setProgress(0.0));
        checksAndResult(cTes);
    }

    private void checksAndResult(List<CTeDTO> cTes) {
        double progressPerFile = 1.0 / cTes.size();

        for (CTeDTO cTe : cTes) {
            try {
                CTeStatusService status = new CTeStatusService(certifiedServerPath, certifiedPath, password, cTe.key());
                CTeResponseDTO response = status.consultEndpoint();

                if (response != null) {
                    cTesCanceled.put(cTe.key(), response);
                }

                Platform.runLater(() -> progress.setProgress(progress.getProgress() + progressPerFile));
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        finish();
    }

    private void finish() {
        createExcel();
        writeAndSaveExcel();

        executeButton.setDisable(false);
        selectButton.setDisable(false);
        Platform.runLater(() -> progress.setProgress(0.0));

        cTesCanceled.clear();
    }

    private void createExcel() {
        try {
            workbook = new Workbook();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeAndSaveExcel() {
        try {
            WorksheetCollection worksheets = workbook.getWorksheets();
            Worksheet sheet = worksheets.get(0);
            Cells cells = sheet.getCells();

            cells.get("A1").putValue("Key CT-e");
            cells.get("B1").putValue("Code");
            cells.get("C1").putValue("Description");

            int row = 2;
            for (Map.Entry<String, CTeResponseDTO> cTe : cTesCanceled.entrySet()) {
                cells.get("A" + row).putValue(cTe.getKey());
                cells.get("B" + row).putValue(cTe.getValue().code());
                cells.get("C" + row).putValue(cTe.getValue().description());
                row++;
            }

            workbook.save(PATH + createFileWithNumber(), SaveFormat.XLSX);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createFileWithNumber() {
        String fileNameWithPrefix = FILE_PREFIX + ".xlsx";
        File file = new File(PATH, fileNameWithPrefix);

        if (!file.exists()) {
            return fileNameWithPrefix;
        }

        File[] files = new File(PATH).listFiles((dir, name) -> name.matches("^" + FILE_PREFIX + " \\((\\d+)\\)\\.xlsx$"));
        int maxNumber = 0;

        if (files != null) {
            maxNumber = Arrays.stream(files)
                    .map(f -> {
                        Matcher matcher = Pattern.compile("^" + FILE_PREFIX + " \\((\\d+)\\)\\.xlsx$").matcher(f.getName());
                        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
                    })
                    .max(Integer::compare)
                    .orElse(0);
        }

        return FILE_PREFIX + " (" + (maxNumber + 1) + ").xlsx";
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void run() {
        process();
    }
}
