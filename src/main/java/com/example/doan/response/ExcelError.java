package com.example.doan.response;

import lombok.Data;

@Data
public class ExcelError {

    // Dòng bị lỗi trong file Excel (row number)
    private int row;

    // Tên cột bị lỗi
    private String column;

    // Nội dung lỗi
    private String message;

    public ExcelError(int row, String column, String message) {
        this.row = row;
        this.column = column;
        this.message = message;
    }
}
