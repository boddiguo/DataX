# DataX KafkaWriter


---


## 1 快速介绍

KafkaWriter 插件实现了写入数据到kafka



## 2 实现原理

KafkaWriter 通过 DataX 框架获取 Reader 生成的协议数据，根据你配置的 `writeMode` 生成


## 3 功能说明

### 3.1 配置样例

* 这里使用一份从内存产生到 Kafka 导入的数据。

```json
{
    "job" : {
        "content" : [
            {
                "reader" : {
                    "name" : "oraclereader",
                    "parameter" : {
                        "username" : "",
                        "connection" : [
                            {
                                "jdbcUrl" : [
                                ],
                                "splitPk" : "id",
                                "querySql" : [
                                ]
                            }
                        ],
                        "password" : ""
                    }
                },
                "writer" : {
                    "name" : "kafkawriter",
                    "parameter" : {
                        "column" : [
                            "AUD_USER",
                            "AUD_CLIENT_IP",
                            "AUD_SERVER_IP",
                            "AUD_ACTION",
                            "AUD_APPLICATION",
                            "AUD_DATE"
                        ],
                        "topic": "datax.actor.sync",
                        "toTableName": "actor",
                        "writeMode": "insert",
                        "connection": [
                            {
                                "serverAddress" : "localhost:9092"
                            }
                        ]

                    }
                }
            }
        ],
        "setting" : {
            "speed" : {
                "channel" : 10
            }
        }
    }
}
```


### 3.2 参数说明

* **serverAddress**

	* 描述：kafka连接信息
	
 	* 必选：是 <br />

	* 默认值：无 <br />

* **topic**

	* 描述：发送到kafka的topic <br />

	* 必选：是 <br />

	* 默认值：无 <br />


* **column**

	* 描述：目的表需要写入数据的字段,字段之间用英文逗号分隔。例如: "column": ["id","name","age"]。

			**column配置项必须指定，不能留空！**

	* 必选：是 <br />

	* 默认值：否 <br />

