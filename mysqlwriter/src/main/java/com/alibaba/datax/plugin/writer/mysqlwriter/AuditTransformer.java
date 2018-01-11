package com.alibaba.datax.plugin.writer.mysqlwriter;

import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.fastjson.JSONObject;

/**
 * Created by guoyubo on 2017/11/27.
 */
public class AuditTransformer {
  
  public Record eval(final Record record) {
    JSONObject jsonObject = new JSONObject();
    JSONObject registerJsonObject = new JSONObject();
    registerJsonObject.put("regPhone", record.getColumn(6).getRawData());
    registerJsonObject.put("email", record.getColumn(7).getRawData());
    registerJsonObject.put("referralId", record.getColumn(8).getRawData());
    registerJsonObject.put("referralName", record.getColumn(9).getRawData());
    jsonObject.put("newRegisterParam", registerJsonObject);
    record.removeColumn(6);
    record.removeColumn(6);
    record.removeColumn(6);
    record.removeColumn(6);
    record.addColumn(new StringColumn(jsonObject.toJSONString()));
    return record;
        
  }
  
}
