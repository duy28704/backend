package com.example.doan.service;


import com.example.doan.dto.LaptopRequest;
import com.example.doan.excel.ExcelColumn;
import com.example.doan.response.ExcelError;
import com.example.doan.response.ExcelResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.*;

@Service
public class ExcelProductService {

    private final DataFormatter formatter = new DataFormatter();

    public <T> ExcelResult<T> importExcel(
            MultipartFile file,
            Class<T> clazz
    ) throws Exception {


        List<T> result = new ArrayList<>();

        // Danh sách lỗi trong quá trình import
        List<ExcelError> errors = new ArrayList<>();

        // Set dùng để kiểm tra duplicate trong Excel
        Set<Object> excelUniqueCheck = new HashSet<>();

        // Auto close workbook để tránh leak memory
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            // Lấy sheet đầu tiên
            Sheet sheet = workbook.getSheetAt(0);

            // Map header Excel -> index column
            Map<String, Integer> headerMap = buildHeader(sheet.getRow(0));

            // Duyệt từng dòng dữ liệu (bỏ header = row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                // Nếu dòng null thì bỏ qua
                if (row == null) continue;

                // Tạo object mới (LaptopRequest, Product,...)
                T obj = clazz.getDeclaredConstructor().newInstance();

                // Check lỗi theo từng row
                boolean rowHasError = false;

                // Duyệt từng field trong class (reflection)
                for (Field field : clazz.getDeclaredFields()) {

                    // Chỉ xử lý field có annotation @ExcelColumn
                    if (!field.isAnnotationPresent(ExcelColumn.class)) continue;

                    ExcelColumn col = field.getAnnotation(ExcelColumn.class);

                    // Lấy dữ liệu từ Excel theo tên cột
                    String rawValue = get(row, headerMap, col.value());

                    // ========== REQUIRED VALIDATION ==========
                    if (col.required() && (rawValue == null || rawValue.isBlank())) {
                        errors.add(new ExcelError(
                                i + 1, // +1 vì Excel bắt đầu từ 1
                                col.value(),
                                "Field required"
                        ));
                        rowHasError = true;
                        continue;
                    }

                    // Convert String -> đúng kiểu field (Integer, Double,...)
                    Object converted = convert(field.getType(), rawValue);

                    // ========== UNIQUE CHECK (trong Excel) ==========
                    if (col.unique() && converted != null) {

                        // Nếu đã tồn tại trong Set → duplicate
                        if (!excelUniqueCheck.add(converted)) {
                            errors.add(new ExcelError(
                                    i + 1,
                                    col.value(),
                                    "Duplicate in Excel"
                            ));
                            rowHasError = true;
                        }
                    }

                    // Cho phép set private field
                    field.setAccessible(true);

                    // Gán value vào object
                    field.set(obj, converted);
                }

                // Nếu row không có lỗi → add vào result
                if (!rowHasError) {
                    result.add(obj);
                }
            }
        }

        return new ExcelResult<>(result, errors);
    }



    private Map<String, Integer> buildHeader(Row row) {
        Map<String, Integer> map = new HashMap<>();

        for (Cell cell : row) {
            map.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex());
        }

        return map;
    }

    // ================= VALUE =================

    private String get(Row row, Map<String, Integer> map, String header) {

        Integer idx = map.get(header);
        if (idx == null) return null;

        Cell cell = row.getCell(idx);
        if (cell == null) return null;

        String value = formatter.formatCellValue(cell).trim();

        return value.isEmpty() ? null : value;
    }



    private Object convert(Class<?> type, String value) {

        if (value == null) return null;

        try {

            if (type == String.class) return value;

            if (type == Integer.class)
                return Integer.parseInt(value.replaceAll("[^0-9]", ""));

            if (type == Double.class)
                return Double.parseDouble(value.replaceAll("[^0-9.]", ""));

            if (type == Long.class)
                return Long.parseLong(value.replaceAll("[^0-9]", ""));

        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void setBuilder(LaptopRequest.LaptopRequestBuilder builder, String fieldName, Object value) {

        try {
            Method method = builder.getClass().getMethod(fieldName, value != null ? value.getClass() : String.class);
            method.invoke(builder, value);
        } catch (Exception ignored) {
            // fallback: bỏ qua field nếu mapping lỗi
        }
    }


}
