package org.brownsolutions.data;

import com.aspose.cells.Cells;
import com.aspose.cells.Row;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import org.brownsolutions.model.CTeDTO;

import java.util.ArrayList;
import java.util.List;

public class DataReader {

    public List<CTeDTO> execute(String filePath) {
        List<CTeDTO> list = new ArrayList<>();

        try {
            Workbook workbook = new Workbook(filePath);
            Worksheet worksheet = workbook.getWorksheets().get(0);

            List<CTeDTO> data = processWorksheet(worksheet);
            list.addAll(data);

            return list;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<CTeDTO> processWorksheet(Worksheet worksheet) {
        List<CTeDTO> list = new ArrayList<>();

        Cells cells = worksheet.getCells();
        int maxDataRow = cells.getMaxDataRow() + 1;

        for (int i = 1; i < maxDataRow; i++) {
            Row row = cells.checkRow(i);
            if (row != null) {
                CTeDTO CTeDTO = createCTeDTOFromRow(row);
                list.add(CTeDTO);
            }
        }

        return list;
    }

    private CTeDTO createCTeDTOFromRow(Row row) {
        String key = row.get(0).getStringValue();
        return new CTeDTO(key);
    }
}
