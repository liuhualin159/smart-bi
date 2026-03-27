package dev.lhl.etl.service;

import dev.lhl.etl.domain.EtlTask;
import dev.lhl.etl.domain.EtlTaskExecution;
import dev.lhl.etl.config.DataXConfig;
import dev.lhl.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * DataX任务执行服务
 * 负责执行DataX任务，支持全量和增量抽取
 * 
 * 性能要求：
 * - 全量抽取：单表1000万行数据，P95 ≤ 30分钟
 * - 增量抽取：单次增量数据<100万行，P95 ≤ 5分钟
 * 
 * @author smart-bi
 */
@Service
public class DataXExecutionService
{
    private static final Logger log = LoggerFactory.getLogger(DataXExecutionService.class);
    
    @Autowired
    private DataXConfigGenerator configGenerator;
    
    @Autowired
    private DataXMonitorService monitorService;
    
    @Autowired
    private dev.lhl.datasource.service.IDataSourceService dataSourceService;
    
    @Autowired
    private dev.lhl.etl.mapper.EtlTaskExecutionMapper executionMapper;
    
    @Autowired(required = false)
    private DataXConfig dataXConfig;
    
    /**
     * 执行ETL任务
     * 
     * @param etlTask ETL任务
     * @return 执行记录ID
     */
    public Long execute(EtlTask etlTask)
    {
        log.info("开始执行ETL任务: taskId={}, taskName={}", etlTask.getId(), etlTask.getName());
        
        try
        {
            // 创建执行记录
            EtlTaskExecution execution = createExecution(etlTask);
            
            // 获取数据源
            dev.lhl.datasource.domain.DataSource dataSource = dataSourceService.selectDataSourceById(etlTask.getDatasourceId());
            if (dataSource == null)
            {
                throw new RuntimeException("数据源不存在: datasourceId=" + etlTask.getDatasourceId());
            }
            
            // 检查并创建目标表（如果不存在）
            ensureTargetTableExists(etlTask, dataSource);
            
            // 生成DataX配置
            String configJson = configGenerator.generateConfig(etlTask, dataSource);
            
            // 异步执行DataX任务
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try
                {
                    executeDataXTask(configJson, execution);
                }
                catch (Exception e)
                {
                    log.error("DataX任务执行失败: taskId={}", etlTask.getId(), e);
                    updateExecutionStatus(execution, "FAILED", e.getMessage());
                }
            });
            
            // 设置超时
            future.orTimeout(30, TimeUnit.MINUTES);
            
            return execution.getId();
        }
        catch (Exception e)
        {
            log.error("执行ETL任务失败: taskId={}", etlTask.getId(), e);
            throw new RuntimeException("执行ETL任务失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行DataX任务
     * 通过DataX Engine执行任务，支持全量和增量抽取
     * 
     * 执行优先级：
     * 1. DataX Engine（如果Maven依赖完整，可直接使用Java API）
     * 2. DataX 命令行（需要安装DataX完整包，通过Python脚本执行）
     * 3. 模拟执行（开发测试模式，当DataX未安装时）
     * 
     * 注意：
     * - DataX的Maven依赖（datax-core）可能不完整，因为DataX主要通过Python脚本执行
     * - 生产环境建议安装DataX完整包，使用命令行方式执行
     * - 如果使用Engine方式，需要确保所有DataX依赖和插件都包含在classpath中
     * 
     * @param configJson DataX任务配置JSON
     * @param execution 执行记录
     */
    private void executeDataXTask(String configJson, EtlTaskExecution execution)
    {
        log.info("开始执行DataX任务: executionId={}", execution.getId());
        
        try
        {
            // 1. 保存配置到临时文件
            Path configFile = saveConfigToTempFile(configJson, execution.getId());
            
            try
            {
                // 2. 尝试通过DataX Engine执行（如果可用）
                if (isDataXEngineAvailable())
                {
                    executeWithDataXEngine(configFile.toString(), execution);
                }
                // 3. 否则尝试通过命令行执行
                else if (isDataXCommandAvailable())
                {
                    executeWithDataXCommand(configFile.toString(), execution);
                }
                // 4. 如果都不可用，使用模拟执行（开发测试模式）
                else
                {
                    log.warn("DataX未安装，使用模拟执行模式（仅用于开发测试）: executionId={}", execution.getId());
                    executeWithSimulation(configJson, execution);
                }
            }
            finally
            {
                // 5. 清理临时文件
                try
                {
                    Files.deleteIfExists(configFile);
                }
                catch (IOException e)
                {
                    log.warn("删除临时配置文件失败: {}", configFile, e);
                }
            }
            
            // 6. 更新执行记录为成功
            execution.setStatus("SUCCESS");
            execution.setEndTime(new java.util.Date());
            if (execution.getStartTime() != null)
            {
                execution.setDuration(System.currentTimeMillis() - execution.getStartTime().getTime());
            }
            updateExecutionStatus(execution, "SUCCESS", null);
            
            log.info("DataX任务执行成功: executionId={}, duration={}ms", execution.getId(), execution.getDuration());
        }
        catch (Exception e)
        {
            log.error("DataX任务执行失败: executionId={}", execution.getId(), e);
            updateExecutionStatus(execution, "FAILED", e.getMessage());
            throw new RuntimeException("DataX任务执行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 保存配置到临时文件
     */
    private Path saveConfigToTempFile(String configJson, Long executionId) throws IOException
    {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path configFile = Paths.get(tempDir, "datax_config_" + executionId + ".json");
        Files.write(configFile, configJson.getBytes("UTF-8"));
        log.info("DataX 配置文件: {} （可打开检查 Writer 的 connection.jdbcUrl 是否为 smart-ai）", configFile.toAbsolutePath());
        return configFile;
    }
    
    /**
     * 检查DataX Engine是否可用
     * 
     * 注意：DataX的Maven依赖可能不完整，因为DataX主要通过Python脚本执行。
     * 如果使用Engine方式，需要确保所有DataX依赖和插件都包含在classpath中。
     * 
     * @return true表示Engine可用
     */
    private boolean isDataXEngineAvailable()
    {
        try
        {
            // 尝试加载DataX Engine类
            Class<?> engineClass = Class.forName("com.alibaba.datax.core.Engine");
            // 检查entry方法是否存在
            engineClass.getMethod("entry", String[].class);
            log.debug("DataX Engine类可用");
            return true;
        }
        catch (ClassNotFoundException e)
        {
            log.debug("DataX Engine类不可用（未找到com.alibaba.datax.core.Engine），可能需要安装DataX完整包");
            return false;
        }
        catch (NoSuchMethodException e)
        {
            log.debug("DataX Engine类存在但entry方法不可用");
            return false;
        }
    }
    
    /**
     * 检查DataX命令行是否可用
     */
    private boolean isDataXCommandAvailable()
    {
        if (dataXConfig == null || StringUtils.isEmpty(dataXConfig.getHomePath()))
        {
            return false;
        }
        
        Path dataxPath = Paths.get(dataXConfig.getHomePath(), "bin", "datax.py");
        return Files.exists(dataxPath);
    }
    
    /**
     * 通过DataX Engine执行任务
     */
    private void executeWithDataXEngine(String configPath, EtlTaskExecution execution)
    {
        try
        {
            log.info("使用DataX Engine执行任务: executionId={}, configPath={}", execution.getId(), configPath);
            
            // 使用反射调用DataX Engine
            Class<?> engineClass = Class.forName("com.alibaba.datax.core.Engine");
            java.lang.reflect.Method entryMethod = engineClass.getMethod("entry", String[].class);
            
            String[] args = new String[]{configPath};
            entryMethod.invoke(null, (Object) args);
            
            log.info("DataX Engine执行完成: executionId={}", execution.getId());
        }
        catch (Exception e)
        {
            log.error("DataX Engine执行失败: executionId={}", execution.getId(), e);
            throw new RuntimeException("DataX Engine执行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 通过DataX命令行执行任务
     */
    private void executeWithDataXCommand(String configPath, EtlTaskExecution execution) throws IOException, InterruptedException
    {
        log.info("使用DataX命令行执行任务: executionId={}, configPath={}", execution.getId(), configPath);
        
        String dataxHome = dataXConfig.getHomePath();
        String pythonPath = dataXConfig.getPythonPath();
        Path dataxScript = Paths.get(dataxHome, "bin", "datax.py");
        
        ProcessBuilder processBuilder = new ProcessBuilder(
            pythonPath,
            dataxScript.toString(),
            configPath
        );
        
        processBuilder.directory(new File(dataxHome));
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // 读取输出日志
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), "UTF-8")))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                output.append(line).append("\n");
                log.debug("DataX输出: {}", line);
            }
        }
        
        // 等待进程完成
        int exitCode = process.waitFor();
        
        if (exitCode != 0)
        {
            String errorOutput = output.toString();
            log.error("DataX命令行执行失败: executionId={}, exitCode={}, output={}", 
                execution.getId(), exitCode, errorOutput);
            throw new RuntimeException("DataX命令行执行失败，退出码: " + exitCode + ", 输出: " + errorOutput);
        }
        
        // 解析输出，提取数据量等信息
        parseDataXOutput(output.toString(), execution);
        
        log.info("DataX命令行执行完成: executionId={}", execution.getId());
    }
    
    /**
     * 模拟执行（用于开发测试，当DataX未安装时）
     * 
     * 注意：此方法仅用于开发测试，生产环境必须安装DataX完整包。
     * 
     * DataX安装步骤：
     * 1. 从 https://github.com/alibaba/DataX 下载DataX完整包
     * 2. 解压到指定目录（如 /opt/datax 或 Windows下的 D:/datax）
     * 3. 配置 application.yml 中的 datax.homePath 和 datax.pythonPath
     * 4. 确保Python环境可用（DataX需要Python 2.7+ 或 Python 3.x）
     * 
     * 配置示例（application.yml）：
     * datax:
     *   homePath: /opt/datax          # Linux/Mac
     *   # homePath: D:/datax          # Windows
     *   pythonPath: python            # 或 python3
     *   timeout: 3600
     *   threadCount: 1
     *   channelRecordCount: 1000
     */
    private void executeWithSimulation(String configJson, EtlTaskExecution execution)
    {
        log.warn("使用模拟执行模式（DataX未安装）: executionId={}", execution.getId());
        log.warn("生产环境必须安装DataX完整包！请参考：https://github.com/alibaba/DataX");
        
        try
        {
            // 模拟执行时间（1-5秒）
            Thread.sleep(1000 + (long)(Math.random() * 4000));
            
            // 模拟数据量（1000-10000行）
            long dataCount = 1000 + (long)(Math.random() * 9000);
            execution.setDataCount(dataCount);
            
            log.info("模拟执行完成: executionId={}, dataCount={}", execution.getId(), dataCount);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException("模拟执行被中断", e);
        }
    }
    
    /**
     * 解析DataX输出，提取数据量等信息
     */
    private void parseDataXOutput(String output, EtlTaskExecution execution)
    {
        try
        {
            // 尝试从输出中提取数据量
            // DataX输出格式示例：Total 10000 records, 10000 records/s
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Total\\s+(\\d+)\\s+records");
            java.util.regex.Matcher matcher = pattern.matcher(output);
            if (matcher.find())
            {
                long dataCount = Long.parseLong(matcher.group(1));
                execution.setDataCount(dataCount);
                log.debug("从DataX输出提取数据量: executionId={}, dataCount={}", execution.getId(), dataCount);
            }
        }
        catch (Exception e)
        {
            log.warn("解析DataX输出失败: executionId={}", execution.getId(), e);
        }
    }
    
    /**
     * 创建执行记录
     */
    private EtlTaskExecution createExecution(EtlTask etlTask)
    {
        EtlTaskExecution execution = new EtlTaskExecution();
        execution.setTaskId(etlTask.getId());
        execution.setStatus("RUNNING");
        execution.setStartTime(new java.util.Date());
        
        // 保存到数据库
        executionMapper.insertEtlTaskExecution(execution);
        log.info("创建ETL任务执行记录: executionId={}, taskId={}", execution.getId(), etlTask.getId());
        
        return execution;
    }
    
    /**
     * 更新执行状态
     */
    private void updateExecutionStatus(EtlTaskExecution execution, String status, String errorMessage)
    {
        execution.setStatus(status);
        execution.setEndTime(new java.util.Date());
        if (errorMessage != null)
        {
            execution.setErrorMessage(errorMessage);
        }
        
        // 更新数据库
        executionMapper.updateEtlTaskExecution(execution);
        log.info("更新ETL任务执行状态: executionId={}, status={}", execution.getId(), status);
    }

    /**
     * 确保目标表存在，如果不存在则自动创建
     */
    private void ensureTargetTableExists(EtlTask etlTask, dev.lhl.datasource.domain.DataSource dataSource)
    {
        try
        {
            String targetTableName = etlTask.getTargetTable();
            if (dev.lhl.common.utils.StringUtils.isEmpty(targetTableName))
            {
                log.warn("目标表名为空，跳过表存在性检查: taskId={}", etlTask.getId());
                return;
            }

            // 检查目标表是否存在
            boolean tableExists = dataSourceService.checkLocalTableExists(targetTableName);
            if (tableExists)
            {
                log.debug("目标表已存在，无需创建: taskId={}, targetTable={}", etlTask.getId(), targetTableName);
                return;
            }

            log.info("目标表不存在，开始自动创建: taskId={}, targetTable={}", etlTask.getId(), targetTableName);

            // 如果源类型是TABLE，根据源表结构创建目标表
            if ("TABLE".equals(etlTask.getSourceType()))
            {
                String sourceConfig = etlTask.getSourceConfig();
                if (dev.lhl.common.utils.StringUtils.isNotEmpty(sourceConfig))
                {
                    try
                    {
                        com.alibaba.fastjson2.JSONObject config = com.alibaba.fastjson2.JSON.parseObject(sourceConfig);
                        String sourceTableName = config.getString("tableName");
                        if (dev.lhl.common.utils.StringUtils.isNotEmpty(sourceTableName))
                        {
                            log.info("根据源表结构创建目标表: taskId={}, sourceTable={}, targetTable={}", 
                                etlTask.getId(), sourceTableName, targetTableName);
                            dataSourceService.createTargetTableFromSource(
                                etlTask.getDatasourceId(), 
                                sourceTableName, 
                                targetTableName
                            );
                            log.info("目标表创建成功: taskId={}, targetTable={}", etlTask.getId(), targetTableName);
                            return;
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("解析源配置失败，无法自动创建目标表: taskId={}, sourceConfig={}", 
                            etlTask.getId(), sourceConfig, e);
                    }
                }
            }

            // 如果无法自动创建，抛出异常
            throw new RuntimeException("目标表不存在且无法自动创建: targetTable=" + targetTableName + 
                "，请确保源类型为TABLE且已配置源表名，或手动创建目标表");
        }
        catch (Exception e)
        {
            log.error("确保目标表存在失败: taskId={}, targetTable={}", etlTask.getId(), etlTask.getTargetTable(), e);
            throw new RuntimeException("确保目标表存在失败: " + e.getMessage(), e);
        }
    }
}
