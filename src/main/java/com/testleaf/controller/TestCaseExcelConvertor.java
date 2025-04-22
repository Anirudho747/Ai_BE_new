package com.testleaf.controller;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class TestCaseExcelConvertor {

    @PostMapping("/downloadTestCases")
    public ResponseEntity<byte[]> downloadTestCases(@RequestBody TestCaseDetailsResponse testCaseResponse) {
        try {
            if (testCaseResponse == null || testCaseResponse.getTestCases() == null) {
                return ResponseEntity.badRequest().body("Invalid test case data".getBytes());
            }

            // Generate Excel File
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Test Cases");

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"TC_No", "Test_Case_Description", "Pre-requisites", "Test_Steps", "Actual_Result", "Expected_Result", "Status"};
            for (int i = 0; i < columns.length; i++) {
                headerRow.createCell(i).setCellValue(columns[i]);
            }

            // Populate Data Rows
            List<TestCase> testCases = testCaseResponse.getTestCases();
            int rowNum = 1;
            for (TestCase testCase : testCases) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(testCase.getTcNo() != null ? testCase.getTcNo() : "");
                row.createCell(1).setCellValue(testCase.getTestCaseDescription() != null ? testCase.getTestCaseDescription() : "");
                row.createCell(2).setCellValue(testCase.getPreRequisites() != null ? testCase.getPreRequisites() : "");
                row.createCell(3).setCellValue(testCase.getTestSteps() != null ? testCase.getTestSteps() : "");
                //Uncomment if you need actual results
                //row.createCell(4).setCellValue(testCase.getActualResult() != null ? testCase.getActualResult() : "");
                row.createCell(4).setCellValue("");
                row.createCell(5).setCellValue(testCase.getExpectedResult() != null ? testCase.getExpectedResult() : "");
                row.createCell(6).setCellValue(testCase.getStatus() != null ? testCase.getStatus() : "");
            }

            workbook.write(out);
            workbook.close();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=testcases.xlsx");
            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating Excel file".getBytes());
        }
    }
}


// Model Classes
class TestCaseDetailsResponse {
    @JsonProperty("testCases")
    private List<TestCase> testCases;

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCase> testCases) {
        this.testCases = testCases;
    }
}

class TestCase {
    @JsonProperty("TC_No")
    private String tcNo;
    @JsonProperty("Test_Case_Description")
    private String testCaseDescription;
    @JsonProperty("Pre-requisites")
    private String preRequisites;
    @JsonProperty("Test_Steps")
    private String testSteps;
    @JsonProperty("Actual_Result")
    private String actualResult;
    @JsonProperty("Expected_Result")
    private String expectedResult;
    @JsonProperty("Status")
    private String status;

    // Getters and Setters
    public String getTcNo() {
        return tcNo;
    }
    public void setTcNo(String tcNo) {
        this.tcNo = tcNo;
    }
    public String getTestCaseDescription() {
        return testCaseDescription;
    }
    public void setTestCaseDescription(String testCaseDescription) {
        this.testCaseDescription = testCaseDescription;
    }
    public String getPreRequisites() {
        return preRequisites;
    }
    public void setPreRequisites(String preRequisites) {
        this.preRequisites = preRequisites;
    }
    public String getTestSteps() {
        return testSteps;
    }
    public void setTestSteps(String testSteps) {
        this.testSteps = testSteps;
    }
    public String getActualResult() {
        return actualResult;
    }
    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }
    public String getExpectedResult() {
        return expectedResult;
    }
    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
