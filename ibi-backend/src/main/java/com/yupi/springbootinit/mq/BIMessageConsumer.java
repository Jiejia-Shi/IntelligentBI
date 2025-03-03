package com.yupi.springbootinit.mq;

import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.BIConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.GptManager;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.vo.GptResultResponse;
import com.yupi.springbootinit.service.ChartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
public class BIMessageConsumer {
    @Resource
    private ChartService chartService;

    @Resource
    private GptManager gptManager;

    @RabbitListener(queues = {BIConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("receive message: " + message);
        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "message is null");
        }

        long chartId = Long.parseLong(message);

        // update its status to running
        Chart updateChart = chartService.getById(chartId);
        if (updateChart == null) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "chart is null");
        }
        updateChart.setStatus("running");
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            handleChartUpdateError(chartId, "failed to update chart status to running");
            return;
        }

        // use GptManager to invoke gpt api and get the reply
        String gptResult = gptManager.doChat(buildUserInput(updateChart));

        // get gpt response class based on gpt result
        GptResultResponse gptResultResponse = getGptResultResponse(gptResult);

        // update its status to running
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("succeed");
        updateChartResult.setGenChart(gptResultResponse.getGenChart());
        updateChartResult.setGenResult(gptResultResponse.getGenResult());
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            handleChartUpdateError(updateChartResult.getId(), "failed to update chart status to succeed");
        }

        // ack message
        channel.basicAck(deliveryTag, false);
    }

    private void handleChartUpdateError (long chartId, String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus("failed");
        updateChart.setExecMessage(execMessage);
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            log.error("failed to update chart status to failed");
        }
    }

    private String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String analysisData = chart.getChartData();

        // build gpt request
        StringBuilder sb = new StringBuilder();
        sb.append("Assume you are a data analyst, please help me make some data analysis based on my analysis goal and data.").append("\n");
        sb.append("Analysis goal: ").append(goal).append("\n");
        sb.append("Analysis data: ").append(analysisData).append("\n");
        if (StringUtils.isNotBlank(chartType)) {
            sb.append("Chart type: ").append(chartType).append("\n");
        } else {
            sb.append("Chart type: ").append("any chart type").append("\n");
        }

        return sb.toString();
    }

    @NotNull
    private static GptResultResponse getGptResultResponse(String gptResult) {

        String[] gptResults = gptResult.split("\\Q*****\\E");
        if (gptResults.length < 2) {
            throw new RuntimeException("gpt result error: not enough results");
        }
        String genChart = gptResults[0];
        String genResult = gptResults[1];

        // trim genChart and genResult data
        genChart = genChart
                .replaceFirst("^```javascript", "")
                .replaceFirst("^```json", "")
                .replaceFirst("```", "")
                .trim();
        genResult = genResult.trim();

        GptResultResponse gptResultResponse = new GptResultResponse();
        gptResultResponse.setGenChart(genChart);
        gptResultResponse.setGenResult(genResult);
        return gptResultResponse;
    }
}
