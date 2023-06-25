package com.veera;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class CSVUtil {
    public List<String> getIds(String eachFilePath) {
        List<String> ids = new ArrayList<>();
        {
            try {
                InputStream is = new FileInputStream(eachFilePath);
                BufferedReader fileReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                CSVParser csvParser = new CSVParser(fileReader, CSVFormat.EXCEL.withDelimiter(',').withFirstRecordAsHeader());
                Iterable<CSVRecord> csvRecords = csvParser.getRecords();
                for(CSVRecord csvRecord : csvRecords) {
                    String id = csvRecord.get(0);
                    //String id = csvRecord.get("Eureka ID");
                    ids.add(id);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return ids;
    }
}