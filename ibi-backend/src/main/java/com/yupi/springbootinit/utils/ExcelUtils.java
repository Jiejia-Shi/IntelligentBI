package com.yupi.springbootinit.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ExcelUtils {

    public static String convertExcelToCsv(MultipartFile multipartFile) {
//        File file = null;
//        try {
//            file = ResourceUtils.getFile("classpath:test_excel.xlsx");
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }

        // read data from Excel
        List<Map<Integer, String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("excel convert error", e);
        }

        // verification
        if (CollUtil.isEmpty(list)) {
            return "";
        }

        // convert to csv
        StringBuilder sb = new StringBuilder();
        for (Map<Integer, String> map : list) {
            List<String> dataList = map.values().stream().filter(Objects::nonNull).collect(Collectors.toList());
            sb.append(StringUtils.join(dataList, ",")).append("\n");
        }

        return sb.toString();
    }

//    public static void main(String[] args) {
//        System.out.println(convertExcelToCsv());
//    }
}
