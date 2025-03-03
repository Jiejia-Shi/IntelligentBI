package com.yupi.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.GptManager;
import com.yupi.springbootinit.manager.RedisLimiterManager;
import com.yupi.springbootinit.model.dto.chart.*;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.enums.FileUploadBizEnum;
import com.yupi.springbootinit.model.vo.GptResultResponse;
import com.yupi.springbootinit.mq.BIMessageProducer;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.ExcelUtils;
import com.yupi.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.redisson.RedissonRateLimiter;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private GptManager gptManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BIMessageProducer biMessageProducer;


    /**
     * add
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * delete
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // if exist
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);

        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * update
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // if exist
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * get by id
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * get chart list
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    // @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest).orderByDesc("id"));
        return ResultUtils.success(chartPage);
    }

    /**
     * get chart vo list
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<Chart>> listChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // limit
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * edit
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // if exist
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);

        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);

        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


    /**
     * gen chart (sync)
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<GptResultResponse> genChartByAI(@RequestPart("file") MultipartFile multipartFile,
                                                        GenChartByAIRequest genChartByAIRequest, HttpServletRequest request) {
        String name = genChartByAIRequest.getName();
        String goal = genChartByAIRequest.getGoal();
        String chartType = genChartByAIRequest.getChartType();

        // verification
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "Analysis goal is empty");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "Name is too long");

        // verify files
        long fileSize = multipartFile.getSize();
        String fileName = multipartFile.getOriginalFilename();
        // size should <= 1MB
        long maxSize = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > maxSize, ErrorCode.PARAMS_ERROR, "We only support files less than 1MB");
        // verify file type
        String suffix = FileUtil.getSuffix(fileName);
        List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "invalid file type");

        // rate limit
        User loginUser = userService.getLoginUser(request);
        String rateLimitKey = "genChartByAI_" + loginUser.getId();
        redisLimiterManager.doRateLimit(rateLimitKey, 1);

        // convert excel to csv
        String analysisData = ExcelUtils.convertExcelToCsv(multipartFile);

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

        // use GptManager to invoke gpt api and get the reply
        String gptResult = gptManager.doChat(sb.toString());

        // get gpt response class based on gpt result
        GptResultResponse gptResultResponse = getGptResultResponse(gptResult);

        // save chart data to database
        Chart chart = saveChartToDatabase(genChartByAIRequest, gptResultResponse, analysisData, loginUser);
        gptResultResponse.setChartId(chart.getId());
        return ResultUtils.success(gptResultResponse);
    }

    /**
     * gen chart async
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<GptResultResponse> genChartByAIAsync(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAIRequest genChartByAIRequest, HttpServletRequest request) {
        String name = genChartByAIRequest.getName();
        String goal = genChartByAIRequest.getGoal();
        String chartType = genChartByAIRequest.getChartType();

        // verification
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "Analysis goal is empty");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "Name is too long");

        // verify files
        long fileSize = multipartFile.getSize();
        String fileName = multipartFile.getOriginalFilename();
        // size should <= 1MB
        long maxSize = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > maxSize, ErrorCode.PARAMS_ERROR, "We only support files less than 1MB");
        // verify file type
        String suffix = FileUtil.getSuffix(fileName);
        List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "invalid file type");

        // rate limit
        User loginUser = userService.getLoginUser(request);
        String rateLimitKey = "genChartByAI_" + loginUser.getId();
        redisLimiterManager.doRateLimit(rateLimitKey, 1);

        // convert excel to csv
        String analysisData = ExcelUtils.convertExcelToCsv(multipartFile);

        // save chart data to database
        Chart chart = saveChartToDatabaseAsync(genChartByAIRequest, analysisData, loginUser);
        // gptResultResponse.setChartId(chart.getId());

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

        // run gpt request async
        CompletableFuture.runAsync(() -> {
            // update its status to running
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus("running");
            boolean b = chartService.updateById(updateChart);
            if (!b) {
                handleChartUpdateError(chart.getId(), "failed to update chart status to running");
                return;
            }

            // use GptManager to invoke gpt api and get the reply
            String gptResult = gptManager.doChat(sb.toString());

            // get gpt response class based on gpt result
            GptResultResponse gptResultResponse = getGptResultResponse(gptResult);

            // update its status to running
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setStatus("succeed");
            updateChartResult.setGenChart(gptResultResponse.getGenChart());
            updateChartResult.setGenResult(gptResultResponse.getGenResult());
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                handleChartUpdateError(chart.getId(), "failed to update chart status to succeed");
            }
        }, threadPoolExecutor);

        GptResultResponse gptResultResponse = new GptResultResponse();
        gptResultResponse.setChartId(chart.getId());
        return ResultUtils.success(gptResultResponse);
    }

    /**
     * gen chart async (mq)
     * @param multipartFile
     * @param genChartByAIRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<GptResultResponse> genChartByAIAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                             GenChartByAIRequest genChartByAIRequest, HttpServletRequest request) {
        String name = genChartByAIRequest.getName();
        String goal = genChartByAIRequest.getGoal();
        String chartType = genChartByAIRequest.getChartType();

        // verification
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "Analysis goal is empty");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "Name is too long");

        // verify files
        long fileSize = multipartFile.getSize();
        String fileName = multipartFile.getOriginalFilename();
        // size should <= 1MB
        long maxSize = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > maxSize, ErrorCode.PARAMS_ERROR, "We only support files less than 1MB");
        // verify file type
        String suffix = FileUtil.getSuffix(fileName);
        List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "invalid file type");

        // rate limit
        User loginUser = userService.getLoginUser(request);
        String rateLimitKey = "genChartByAI_" + loginUser.getId();
        redisLimiterManager.doRateLimit(rateLimitKey, 1);

        // convert excel to csv
        String analysisData = ExcelUtils.convertExcelToCsv(multipartFile);

        // save chart data to database
        Chart chart = saveChartToDatabaseAsync(genChartByAIRequest, analysisData, loginUser);
        // gptResultResponse.setChartId(chart.getId());

        long newChartId = chart.getId();

        // send message
        biMessageProducer.sendMessage(String.valueOf(newChartId));

        GptResultResponse gptResultResponse = new GptResultResponse();
        gptResultResponse.setChartId(newChartId);
        return ResultUtils.success(gptResultResponse);
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

    private Chart saveChartToDatabaseAsync(GenChartByAIRequest genChartByAIRequest, String analysisData, User loginUser) {
        Chart chart = new Chart();

        chart.setName(genChartByAIRequest.getName());
        chart.setGoal(genChartByAIRequest.getGoal());
        chart.setChartData(analysisData);
        chart.setChartType(genChartByAIRequest.getChartType());
        chart.setUserId(loginUser.getId());
        chart.setStatus("wait");

        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "error: fail to save chart");

        return chart;
    }

    private Chart saveChartToDatabase(GenChartByAIRequest genChartByAIRequest, GptResultResponse gptResultResponse, String analysisData, User loginUser) {
        Chart chart = new Chart();

        chart.setName(genChartByAIRequest.getName());
        chart.setGoal(genChartByAIRequest.getGoal());
        chart.setChartData(analysisData);
        chart.setChartType(genChartByAIRequest.getChartType());
        chart.setGenChart(gptResultResponse.getGenChart());
        chart.setGenResult(gptResultResponse.getGenResult());
        chart.setUserId(loginUser.getId());
        chart.setStatus("succeed");

        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "error: fail to save chart");

        return chart;
    }

}
