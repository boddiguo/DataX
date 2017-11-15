package com.alibaba.datax.core;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.alibaba.datax.common.statistics.VMInfo;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.scaffold.base.CaseInitializer;
import com.alibaba.datax.core.util.ConfigParser;
import com.alibaba.datax.core.util.ConfigurationValidate;
import com.alibaba.datax.core.util.SecretUtil;
import com.alibaba.datax.core.util.container.CoreConstant;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by guoyubo on 2017/10/27.
 */
public class EngineDemo extends CaseInitializer {


  @Test
  public void testEntry() {


  }

  @Test
  public void testDirectoRun() {

    final String jobContent = "{\n"
        + "  \"job\" : {\n"
        + "    \"content\" : [\n"
        + "      {\n"
        + "        \"reader\" : {\n"
        + "          \"name\" : \"mysqlreader\",\n"
        + "          \"parameter\" : {\n"
        + "            \"username\" : \"zhao\",\n"
        + "            \"connection\" : [\n"
        + "              {\n"
        + "                \"jdbcUrl\" : [\n"
        + "                  \"jdbc:mysql:\\/\\/10.18.19.43:3307\\/drmain\"\n"
        + "                ],\n"
        + "                \"splitPk\" : \"id\",\n"
        + "                \"querySql\" : [\n"
        + "                  \"select * from dr_doc where id<100\"\n"
        + "                ]\n"
        + "              }\n"
        + "            ],\n"
        + "            \"password\" : \"aVJ542rGqPtk4ol\"\n"
        + "          }\n"
        + "        },\n"
        + "        \"writer\" : {\n"
        + "          \"name\" : \"mysqlwriter\",\n"
        + "          \"parameter\" : {\n"
        + "            \"password\" : \"123456\",\n"
        + "            \"username\" : \"root\",\n"
        + "            \"writeMode\" : \"replace\",\n"
        + "            \"column\" : [\n"
        + "              \"id\",\n"
        + "              \"category\",\n"
        + "              \"type\",\n"
        + "              \"segment\",\n"
        + "              \"content_id\",\n"
        + "              \"version\",\n"
        + "              \"created\"\n"
        + "            ],\n"
        + "            \"connection\" : [\n"
        + "              {\n"
        + "                \"jdbcUrl\" : \"jdbc:mysql:\\/\\/127.0.0.1:3306\\/canal_test\",\n"
        + "                \"table\" : [\n"
        + "                  \"dr_doc\"\n"
        + "                ]\n"
        + "              }\n"
        + "            ],\n"
        + "            \"preSql\" : [\n"
        + "              \"\"\n"
        + "            ]\n"
        + "          }\n"
        + "        }\n"
        + "      }\n"
        + "    ],\n"
        + "    \"setting\" : {\n"
        + "      \"speed\" : {\n"
        + "        \"channel\" : 10\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "}";

    Configuration configuration = parse(jobContent);

    final long jobId = 0;
    configuration.set(CoreConstant.DATAX_CORE_CONTAINER_JOB_ID, jobId);

    //打印vmInfo
    VMInfo vmInfo = VMInfo.getVmInfo();
    System.out.println(vmInfo);

    ConfigurationValidate.doValidate(configuration);
    Engine engine = new Engine();
    Engine.setRuntimeMode("standalone");
    engine.start(configuration);

  }

  /**
   * 指定Job配置路径，ConfigParser会解析Job、Plugin、Core全部信息，并以Configuration返回
   */
  public static Configuration parse(final String jobContent) {
    Configuration configuration = Configuration.from(jobContent);
    configuration = SecretUtil.decryptSecretKey(configuration);

    configuration.merge( Configuration.from(new File(CoreConstant.DATAX_CONF_PATH)), false);
    // todo config优化，只捕获需要的plugin
    String readerPluginName = configuration.getString(
        CoreConstant.DATAX_JOB_CONTENT_READER_NAME);
    String writerPluginName = configuration.getString(
        CoreConstant.DATAX_JOB_CONTENT_WRITER_NAME);

    String preHandlerName = configuration.getString(
        CoreConstant.DATAX_JOB_PREHANDLER_PLUGINNAME);

    String postHandlerName = configuration.getString(
        CoreConstant.DATAX_JOB_POSTHANDLER_PLUGINNAME);

    Set<String> pluginList = new HashSet<String>();
    pluginList.add(readerPluginName);
    pluginList.add(writerPluginName);

    if(StringUtils.isNotEmpty(preHandlerName)) {
      pluginList.add(preHandlerName);
    }
    if(StringUtils.isNotEmpty(postHandlerName)) {
      pluginList.add(postHandlerName);
    }
    try {
      configuration.merge(ConfigParser.parsePluginConfig(new ArrayList<String>(pluginList)), false);
    }catch (Exception e){
      //吞掉异常，保持log干净。这里message足够。
      System.out.println(String.format("插件[%s,%s]加载失败，1s后重试... Exception:%s ", readerPluginName, writerPluginName, e.getMessage()));
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e1) {
        //
      }
      configuration.merge(ConfigParser.parsePluginConfig(new ArrayList<String>(pluginList)), false);
    }

    return configuration;
  }


}
