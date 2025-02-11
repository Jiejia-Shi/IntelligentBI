package com.yupi.springbootinit.model.vo;

import lombok.Data;

@Data
public class GptResultResponse {

    private String genChart;

    private String genResult;

    private Long chartId;

}
