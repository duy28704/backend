package com.example.doan.response;

import lombok.Data;

import java.util.List;
@Data
public class ExcelResult<T> {
    private List<T> data;
    private List<ExcelError> errors;
    private boolean hasError;

    public ExcelResult(List<T> data, List<ExcelError> errors) {
        this.data = data;
        this.errors = errors;
        this.hasError = !errors.isEmpty();
    }
}
