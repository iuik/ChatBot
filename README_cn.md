# ChatBot

一个面向 QQ 私聊 AI 交互的轻量级 Spring Boot 后端。

当前项目已包含：
- OneBot V11 私聊消息接入
- 兼容 DeepSeek 风格的聊天回复
- Redis 短期上下文、去重、队列和锁
- MySQL 聊天记录持久化
- Qdrant 长期记忆
- Prompt Profile + TimeContext
- Reminder / ChatPush
- 面向日常聊天的拟人化分段发送

项目范围刻意保持收敛：
- 仅处理私聊文本消息
- 仅回复 `OWNER_QQ`
- 不支持群聊、图片、语音、文件、浏览器工具或 Shell 工具能力

## 1. 技术栈

- Java 17
- Spring Boot 3.3.x
- Maven
- MySQL
- Redis
- Qdrant
- OneBot V11
- DeepSeek Compatible API
- Docker / Docker Compose

## 2. 项目结构

```text
src/main/java/com/chatbot
|- chat          私聊主流程、排队与处理
|- chatpush      主动话题推送
|- config        配置绑定
|- deepseek      大模型对话集成
|- delivery      回复分段与节奏发送
|- memory        Redis / Qdrant 记忆逻辑
|- onebot        OneBot 接入与发送客户端
|- proactive     提醒与主动消息
|- prompt        Persona / Prompt / TimeContext
|- repository    MySQL 持久化
\- security      Owner 校验
```

## 3. 运行前置条件

常规部署通常需要：
- JDK 17
- Maven 3.9+
- MySQL
- Redis
- OneBot V11 服务端
- 一个兼容的聊天模型 API Key

若启用长期记忆，还需要：
- Qdrant
- 一个 embedding API

说明：
- 当前内置聊天客户端默认使用 `deepseek.*` 这一组配置
- 但并不只限于 DeepSeek，只要目标服务兼容当前请求和响应格式即可
- 如果目标服务不兼容，则需要改代码适配

## 4. 主要接口

默认应用端口：
- `8090`

OneBot webhook 接口：
- `POST /onebot/event`

## 5. 本地开发

### 5.1 运行测试

```bash
mvn -s .mvn/settings.xml test
```

### 5.2 本地启动

```bash
mvn -s .mvn/settings.xml spring-boot:run
```

### 5.3 打包并运行 Jar

```bash
mvn -s .mvn/settings.xml clean package
java -jar target/chatbot-0.0.1-SNAPSHOT.jar
```

## 6. 配置方式

项目通过环境变量配置。不要把真实密钥硬编码到代码里。

### 6.1 常用必填变量

通常建议视为必填：
- `OWNER_QQ`
- `ONEBOT_API_BASE_URL`
- 如果要校验入站 OneBot webhook，则配置 `ONEBOT_CALLBACK_TOKEN`
- `DEEPSEEK_API_KEY`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`

### 6.2 常用变量

- `SERVER_PORT`
- `ONEBOT_ACCESS_TOKEN`
- `ONEBOT_CALLBACK_TOKEN`
- `DEEPSEEK_BASE_URL`
- `DEEPSEEK_MODEL`
- `BOT_TIMEZONE`
- `BOT_PROMPT_PROFILE_FILE`
- `BOT_PROMPT_PROFILE_SOURCE_PRIORITY`
- `BOT_PROMPT_RELOAD_FILE_EACH_REQUEST`
- `BOT_PROMPT_INCLUDE_TIME_CONTEXT`

### 6.3 记忆与向量检索

- `EMBEDDING_ENABLED`
- `EMBEDDING_BASE_URL`
- `EMBEDDING_API_KEY`
- `EMBEDDING_MODEL`
- `EMBEDDING_DIMENSION`
- `QDRANT_ENABLED`
- `QDRANT_HOST`
- `QDRANT_PORT`
- `QDRANT_COLLECTION`
- `QDRANT_API_KEY`
- `QDRANT_VECTOR_SIZE`

### 6.4 分段发送配置

- `BOT_DELIVERY_SPLIT_ENABLED`
- `BOT_DELIVERY_MAX_PARTS`
- `BOT_DELIVERY_MAX_PART_CHARS`
- `BOT_DELIVERY_MIN_DELAY_MILLIS`
- `BOT_DELIVERY_MAX_DELAY_MILLIS`
- `BOT_DELIVERY_SPLIT_DAILY_CHAT_ONLY`

## 7. 一键部署

仓库根目录提供了两份交互式部署脚本：

- Windows: [deploy-windows.ps1](/C:/workSpaceforIDEA/ChatBot/deploy-windows.ps1)
- Linux: [deploy-linux.sh](/C:/workSpaceforIDEA/ChatBot/deploy-linux.sh)

它们会：
- 询问运行所需环境变量
- 将配置保存到 `.deploy/`
- 使用 Maven 构建项目
- 启动应用
- 输出日志和 PID 文件位置

### 7.1 Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy-windows.ps1
```

只生成配置：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy-windows.ps1 -PrepareOnly
```

### 7.2 Linux

```bash
chmod +x deploy-linux.sh
./deploy-linux.sh
```

只生成配置：

```bash
./deploy-linux.sh --prepare-only
```

## 8. Docker 部署

仓库现已包含：
- [Dockerfile](/C:/workSpaceforIDEA/ChatBot/Dockerfile)
- [docker-compose.yml](/C:/workSpaceforIDEA/ChatBot/docker-compose.yml)
- [docker/chatbot.env.example](/C:/workSpaceforIDEA/ChatBot/docker/chatbot.env.example)

### 8.1 准备环境变量

复制示例文件并填入真实值：

```bash
cp docker/chatbot.env.example docker/chatbot.env
```

然后至少修改：
- `OWNER_QQ`
- `ONEBOT_API_BASE_URL`
- 如果要校验入站 webhook，则配置 `ONEBOT_CALLBACK_TOKEN`
- `DEEPSEEK_API_KEY`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`

如果启用长期记忆，还需要配置：
- `EMBEDDING_*`
- `QDRANT_*`

再把 `docker-compose.yml` 里的环境文件改成你实际使用的文件：

```yaml
env_file:
  - ./docker/chatbot.env
```

### 8.2 构建并启动

```bash
docker compose up -d --build
```

### 8.3 停止

```bash
docker compose down
```

### 8.4 重要说明

- 容器对外暴露端口 `8090`
- `./prompts` 会以只读方式挂载到 `/app/prompts`
- 示例文件默认使用 `host.docker.internal` 访问 OneBot、MySQL、Redis 和 Qdrant
- 如果你在 Linux 上运行且该地址不可用，请改成宿主机 IP 或容器服务名

## 9. 部署产物

一键部署脚本会生成：

```text
.deploy/
|- chatbot.env           Linux 环境变量文件
|- chatbot-env.ps1       Windows 环境变量脚本
|- chatbot.pid           进程 PID
\- logs/
   |- chatbot.out.log
   \- chatbot.err.log
```

说明：
- `.deploy/` 不应提交真实密钥
- 重复执行脚本会覆盖已保存配置
- 若检测到旧进程，脚本会先停止旧进程再重新部署

## 10. OneBot 消息限制

只有在以下条件全部满足时，消息才会被处理：
- `post_type=message`
- `message_type=private`
- `user_id == OWNER_QQ`
- 消息是纯文本

服务会忽略：
- 群消息
- 非文本 CQ 消息
- 非 owner 用户消息

## 11. 拟人化分段发送

日常聊天回复可以拆成 1 到 3 条 QQ 消息，让私聊体验更自然：
- 句子更短
- 分段之间有轻微停顿
- 不会随意拆分提醒、命令或技术内容

默认不会拆分的内容：
- `/memory`
- `/remind`
- `/chatpush`
- `/prompt`
- 定时提醒消息
- ChatPush 消息
- 含代码块、命令、SQL 或 JSON 风格内容的回复

## 12. 常见问题排查

### 12.1 OneBot 回调没有到达服务

检查：
- OneBot 反向 HTTP 目标是否指向 `POST /onebot/event`
- 防火墙和端口暴露是否正确
- `OWNER_QQ` 是否填写正确

### 12.2 收到消息但没有发出回复

检查：
- 消息是否为私聊纯文本
- `ONEBOT_API_BASE_URL` 是否可访问
- `ONEBOT_ACCESS_TOKEN` 是否匹配
- 配置的聊天模型接口和 API Key 是否有效

### 12.2 OneBot 回调被 401 拒绝

检查：
- `ONEBOT_CALLBACK_TOKEN` 是否与 OneBot 反向 HTTP 回调发送的 token 一致
- 回调请求是否使用 `Authorization: Bearer <token>`，或直接在 `Authorization` 请求头里发送原始 token

### 12.3 启动时报数据库或 Redis 错误

检查：
- MySQL 和 Redis 是否运行中
- 主机、端口、用户名、密码是否正确
- 网络连通性是否正常

### 12.4 Docker 容器启动后无法访问宿主机服务

检查：
- `ONEBOT_API_BASE_URL`、MySQL、Redis、Qdrant 地址是否能从容器内访问
- Linux 下若 `host.docker.internal` 不可用，请改成宿主机 IP 或服务名
- 目标服务在需要时应监听非回环地址

## 13. 安全说明

- 不要提交真实 API Key 或数据库密码
- 不要提交 `.deploy/` 或真实 Docker 环境变量文件
- 不要在日志中输出完整 token 或 API Key
- 生产环境请使用独立运行账号与最小权限数据库凭据

## 14. 建议验证步骤

```bash
mvn -s .mvn/settings.xml test
mvn -s .mvn/settings.xml clean package
docker compose config
```

如果你想最快搭好可运行环境，可以直接使用：
- `deploy-windows.ps1`
- `deploy-linux.sh`
- `docker compose up -d --build`
