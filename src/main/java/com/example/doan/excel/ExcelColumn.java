package com.example.doan.excel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {

    // Tên cột trong Excel (header)
    String value();

    // Bắt buộc phải có dữ liệu hay không
    boolean required() default false;

    // Có kiểm tra trùng trong file Excel hay không
    boolean unique() default false;
}
