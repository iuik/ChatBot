# ChatBot

A lightweight Spring Boot backend for QQ private-chat AI interactions.

The current project includes:
- OneBot V11 private message intake
- DeepSeek-compatible chat replies
- Redis short-term context, deduplication, queue, and locks
- MySQL chat history persistence
- Qdrant long-term memory
- Prompt Profile + TimeContext
- Reminder / ChatPush
- Human-like response delivery for daily chat splitting

The scope is intentionally narrow:
- Private text messages only
- Replies only to `OWNER_QQ`
- No group chat, image, voice, file, browser-tool, or shell-tool features

## 1. Tech Stack

- Java 17
- Spring Boot 3.3.x
- Maven
- MySQL
- Redis
- Qdrant
- OneBot V11
- DeepSeek Compatible API
- Docker / Docker Compose

## 2. Project Structure

```text
src/main/java/com/chatbot
|- chat          Private chat flow, queueing, processing
|- chatpush      Proactive topic pushing
|- config        Configuration binding
|- deepseek      LLM chat integration
|- delivery      Reply splitting and paced delivery
|- memory        Redis / Qdrant memory logic
|- onebot        OneBot ingress and outbound client
|- proactive     Reminders and proactive messages
|- prompt        Persona / Prompt / TimeContext
|- repository    MySQL persistence
\- security      Owner validation
```

## 3. Prerequisites

Required for a typical deployment:
- JDK 17
- Maven 3.9+
- MySQL
- Redis
- A OneBot V11 server
- A compatible chat model API key

Required if long-term memory is enabled:
- Qdrant
- An embedding API

Notes:
- The default built-in chat client is configured under `deepseek.*`
- It is not limited to DeepSeek as long as the target service is compatible with the current request and response format
- If the target provider is not compatible, code changes are required

## 4. Main Endpoint

Default application port:
- `8090`

OneBot webhook endpoint:
- `POST /onebot/event`

## 5. Local Development

### 5.1 Run Tests

```bash
mvn -s .mvn/settings.xml test
```

### 5.2 Run Locally

```bash
mvn -s .mvn/settings.xml spring-boot:run
```

### 5.3 Build and Run Jar

```bash
mvn -s .mvn/settings.xml clean package
java -jar target/chatbot-0.0.1-SNAPSHOT.jar
```

## 6. Configuration

The project is configured through environment variables. Do not hard-code secrets.

### 6.1 Commonly Required Variables

These should usually be treated as required:
- `OWNER_QQ`
- `ONEBOT_API_BASE_URL`
- `DEEPSEEK_API_KEY`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`

### 6.2 Frequently Used Variables

- `SERVER_PORT`
- `ONEBOT_ACCESS_TOKEN`
- `DEEPSEEK_BASE_URL`
- `DEEPSEEK_MODEL`
- `BOT_TIMEZONE`
- `BOT_PROMPT_PROFILE_FILE`
- `BOT_PROMPT_PROFILE_SOURCE_PRIORITY`
- `BOT_PROMPT_RELOAD_FILE_EACH_REQUEST`
- `BOT_PROMPT_INCLUDE_TIME_CONTEXT`

### 6.3 Memory and Vector Retrieval

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

### 6.4 Split Delivery Settings

- `BOT_DELIVERY_SPLIT_ENABLED`
- `BOT_DELIVERY_MAX_PARTS`
- `BOT_DELIVERY_MAX_PART_CHARS`
- `BOT_DELIVERY_MIN_DELAY_MILLIS`
- `BOT_DELIVERY_MAX_DELAY_MILLIS`
- `BOT_DELIVERY_SPLIT_DAILY_CHAT_ONLY`

## 7. One-Click Deployment

Two interactive deployment scripts are provided in the repository root:

- Windows: [deploy-windows.ps1](/C:/workSpaceforIDEA/ChatBot/deploy-windows.ps1)
- Linux: [deploy-linux.sh](/C:/workSpaceforIDEA/ChatBot/deploy-linux.sh)

They will:
- Prompt for runtime environment variables
- Save the configuration under `.deploy/`
- Build the project with Maven
- Start the application
- Output log and PID file locations

### 7.1 Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy-windows.ps1
```

Prepare configuration only:

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy-windows.ps1 -PrepareOnly
```

### 7.2 Linux

```bash
chmod +x deploy-linux.sh
./deploy-linux.sh
```

Prepare configuration only:

```bash
./deploy-linux.sh --prepare-only
```

## 8. Docker Deployment

The repository now includes:
- [Dockerfile](/C:/workSpaceforIDEA/ChatBot/Dockerfile)
- [docker-compose.yml](/C:/workSpaceforIDEA/ChatBot/docker-compose.yml)
- [docker/chatbot.env.example](/C:/workSpaceforIDEA/ChatBot/docker/chatbot.env.example)

### 8.1 Prepare Environment Variables

Copy the example file and fill in real values:

```bash
cp docker/chatbot.env.example docker/chatbot.env
```

Then edit:
- `OWNER_QQ`
- `ONEBOT_API_BASE_URL`
- `DEEPSEEK_API_KEY`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`

If you use long-term memory, also configure:
- `EMBEDDING_*`
- `QDRANT_*`

Update `docker-compose.yml` to use your actual env file:

```yaml
env_file:
  - ./docker/chatbot.env
```

### 8.2 Build and Start

```bash
docker compose up -d --build
```

### 8.3 Stop

```bash
docker compose down
```

### 8.4 Important Notes

- The container exposes port `8090`
- `./prompts` is mounted read-only into `/app/prompts`
- The sample file uses `host.docker.internal` for OneBot, MySQL, Redis, and Qdrant
- On Linux, if `host.docker.internal` is unavailable, replace it with your host IP or service name

## 9. Deployment Artifacts

The one-click scripts generate:

```text
.deploy/
|- chatbot.env           Linux environment file
|- chatbot-env.ps1       Windows environment script
|- chatbot.pid           Process PID
\- logs/
   |- chatbot.out.log
   \- chatbot.err.log
```

Notes:
- `.deploy/` should not be committed with real secrets
- Re-running a script overwrites the saved config
- If an old process is detected, the scripts stop it before redeploying

## 10. OneBot Message Constraints

Messages are processed only when all of the following are true:
- `post_type=message`
- `message_type=private`
- `user_id == OWNER_QQ`
- The message is plain text

The service ignores:
- Group messages
- Non-text CQ messages
- Messages from non-owner users

## 11. Human-like Split Delivery

Daily chat replies can be split into 1 to 3 QQ messages to feel more natural:
- Shorter sentences
- Small delays between parts
- No random splitting of reminders, commands, or technical content

By default, the following are not split:
- `/memory`
- `/remind`
- `/chatpush`
- `/prompt`
- Scheduled reminder messages
- ChatPush messages
- Replies containing code blocks, commands, SQL, or JSON-like content

## 12. Troubleshooting

### 12.1 OneBot callback not reaching the service

Check:
- The OneBot reverse HTTP target points to `POST /onebot/event`
- Firewall and port exposure are correct
- `OWNER_QQ` is correct

### 12.2 Messages arrive but no reply is sent

Check:
- The message is a private plain-text message
- `ONEBOT_API_BASE_URL` is reachable
- `ONEBOT_ACCESS_TOKEN` matches
- The configured chat model endpoint and API key are valid

### 12.3 Startup fails with database or Redis errors

Check:
- MySQL and Redis are running
- Host, port, username, and password are correct
- The network path is reachable

### 12.4 Docker container starts but cannot reach host services

Check:
- `ONEBOT_API_BASE_URL`, MySQL, Redis, and Qdrant hosts are reachable from inside the container
- On Linux, replace `host.docker.internal` if needed
- The target services are listening on non-loopback addresses when required

## 13. Security Notes

- Do not commit real API keys or database passwords
- Do not commit `.deploy/` or real Docker env files
- Do not log full tokens or API keys
- Use separate runtime accounts and least-privilege database credentials in production

## 14. Recommended Verification

```bash
mvn -s .mvn/settings.xml test
mvn -s .mvn/settings.xml clean package
docker compose config
```

If you want the fastest path to a runnable environment, use:
- `deploy-windows.ps1`
- `deploy-linux.sh`
- `docker compose up -d --build`
