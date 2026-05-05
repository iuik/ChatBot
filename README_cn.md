# ChatBot

涓€涓潰鍚?QQ 绉佽亰鍦烘櫙鐨勮交閲?Spring Boot AI 鑱婂ぉ鍚庣銆?
褰撳墠椤圭洰宸茬粡瑕嗙洊锛?- OneBot V11 绉佽亰娑堟伅鎺ュ叆
- DeepSeek 鍏煎鎺ュ彛鐨勫璇濆洖澶?- Redis 鐭湡涓婁笅鏂囦笌鍘婚噸 / 闃熷垪 / 閿?- MySQL 鑱婂ぉ璁板綍鎸佷箙鍖?- Qdrant 闀挎湡璁板繂
- Prompt Profile + TimeContext
- Reminder / ChatPush
- Human-like Response Delivery锛氭棩甯歌亰澶╁垎娈靛彂閫?
椤圭洰鑼冨洿浠嶇劧淇濇寔鏀舵暃锛?- 浠呭鐞嗙鑱婃枃鏈秷鎭?- 浠呭搷搴?`OWNER_QQ`
- 涓嶅仛缇よ亰銆佸浘鐗囥€佽闊炽€佹枃浠躲€佹祻瑙堝櫒宸ュ叿銆丼hell 宸ュ叿鑳藉姏

## 1. 鎶€鏈爤

- Java 17
- Spring Boot 3.3.x
- Maven
- MySQL
- Redis
- Qdrant
- OneBot V11
- DeepSeek Compatible API

## 2. 鐩綍璇存槑

```text
src/main/java/com/chatbot
鈹溾攢 chat          绉佽亰涓绘祦绋嬨€佹秷鎭帓闃熴€佸鐞?鈹溾攢 chatpush      涓诲姩璇濋
鈹溾攢 config        閰嶇疆椤圭粦瀹?鈹溾攢 deepseek      澶фā鍨嬪璇?鈹溾攢 delivery      鍥炲鍒嗗壊涓庡彂閫佽妭濂?鈹溾攢 memory        Redis / Qdrant 璁板繂鐩稿叧
鈹溾攢 onebot        OneBot 鎺ュ叆涓庡彂閫?鈹溾攢 proactive     鎻愰啋涓庝富鍔ㄦ秷鎭?鈹溾攢 prompt        Persona / Prompt / TimeContext
鈹溾攢 repository    MySQL 鎸佷箙鍖?鈹斺攢 security      Owner 鏍￠獙
```

## 3. 杩愯鍓嶅噯澶?
鑷冲皯鍑嗗浠ヤ笅渚濊禆锛?- JDK 17
- Maven 3.9+
- MySQL
- Redis
- OneBot V11 鏈嶅姟绔?- 鍙敤鐨勫吋瀹硅亰澶╂ā鍨?API Key

濡傛灉鍚敤闀挎湡璁板繂锛岃繕闇€瑕侊細
- Qdrant
- Embedding API

璇存槑锛?- 褰撳墠鍐呯疆鑱婂ぉ瀹㈡埛绔厤缃」鍚嶄粛鐒舵槸 `deepseek.*`
- 浣嗗苟涓嶈〃绀哄彧鑳芥帴 DeepSeek锛屽彧瑕佺洰鏍囨湇鍔″吋瀹瑰綋鍓嶈姹?/ 鍝嶅簲鏍煎紡鍗冲彲
- 濡傛灉鐩爣鏈嶅姟鎺ュ彛涓嶅吋瀹癸紝灏遍渶瑕佹敼浠ｇ爜閫傞厤

## 4. 鏍稿績鎺ュ彛

搴旂敤榛樿鐩戝惉锛?- `8090`

OneBot 涓婃姤鍏ュ彛锛?- `POST /onebot/event`

## 5. 鏈湴寮€鍙?
### 5.1 杩愯娴嬭瘯

```bash
mvn -s .mvn/settings.xml test
```

### 5.2 鏈湴鍚姩

```bash
mvn -s .mvn/settings.xml spring-boot:run
```

### 5.3 鎵撳寘杩愯

```bash
mvn -s .mvn/settings.xml clean package
java -jar target/chatbot-0.0.1-SNAPSHOT.jar
```

## 6. 閰嶇疆鏂瑰紡

椤圭洰浣跨敤鐜鍙橀噺娉ㄥ叆閰嶇疆锛屼笉瑕佹妸瀵嗛挜鐩存帴鍐欒繘浠ｇ爜銆?
### 6.1 蹇呭～閰嶇疆

浠ヤ笅閰嶇疆閫氬父搴旇涓哄繀濉細
- `OWNER_QQ`
- `ONEBOT_API_BASE_URL`
- `DEEPSEEK_API_KEY`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`

### 6.2 甯哥敤閰嶇疆

- `SERVER_PORT`
- `ONEBOT_ACCESS_TOKEN`
- `DEEPSEEK_BASE_URL`
- `DEEPSEEK_MODEL`
- `BOT_TIMEZONE`
- `BOT_PROMPT_PROFILE_FILE`
- `BOT_PROMPT_PROFILE_SOURCE_PRIORITY`
- `BOT_PROMPT_RELOAD_FILE_EACH_REQUEST`
- `BOT_PROMPT_INCLUDE_TIME_CONTEXT`

### 6.3 璁板繂涓庡悜閲忔绱㈤厤缃?
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

### 6.4 鍥炲鍒嗘鍙戦€侀厤缃?
- `BOT_DELIVERY_SPLIT_ENABLED`
- `BOT_DELIVERY_MAX_PARTS`
- `BOT_DELIVERY_MAX_PART_CHARS`
- `BOT_DELIVERY_MIN_DELAY_MILLIS`
- `BOT_DELIVERY_MAX_DELAY_MILLIS`
- `BOT_DELIVERY_SPLIT_DAILY_CHAT_ONLY`

## 7. 涓€閿儴缃?
浠撳簱鏍圭洰褰曟彁渚涗袱涓氦浜掑紡涓€閿儴缃茶剼鏈細

- Windows: [deploy-windows.ps1](/C:/workSpaceforIDEA/ChatBot/deploy-windows.ps1)
- Linux: [deploy-linux.sh](/C:/workSpaceforIDEA/ChatBot/deploy-linux.sh)

鑴氭湰鑳藉姏锛?- 浜や簰杈撳叆杩愯鎵€闇€鐜鍙橀噺
- 鎶婇厤缃繚瀛樺埌鏈湴 `.deploy/` 鐩綍
- 鑷姩鎵ц Maven 鎵撳寘
- 鑷姩鍚姩搴旂敤
- 杈撳嚭鏃ュ織鏂囦欢涓?PID 鏂囦欢浣嶇疆

### 7.1 Windows

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy-windows.ps1
```

鍙敓鎴愰厤缃€佷笉鍚姩锛?
```powershell
powershell -ExecutionPolicy Bypass -File .\deploy-windows.ps1 -PrepareOnly
```

### 7.2 Linux

```bash
chmod +x deploy-linux.sh
./deploy-linux.sh
```

鍙敓鎴愰厤缃€佷笉鍚姩锛?
```bash
./deploy-linux.sh --prepare-only
```

## 8. 閮ㄧ讲鑴氭湰浜х墿

鑴氭湰浼氱敓鎴愶細

```text
.deploy/
鈹溾攢 chatbot.env           Linux 鐜鍙橀噺鏂囦欢
鈹溾攢 chatbot-env.ps1       Windows 鐜鍙橀噺鑴氭湰
鈹溾攢 chatbot.pid           杩涚▼ PID
鈹斺攢 logs/
   鈹溾攢 chatbot.out.log
   鈹斺攢 chatbot.err.log
```

璇存槑锛?- `.deploy/` 寤鸿鍔犲叆蹇界暐锛屼笉瑕佹彁浜ょ湡瀹炲瘑閽?- 閲嶆柊鎵ц鑴氭湰浼氳鐩栭厤缃枃浠?- 濡傛灉妫€娴嬪埌鏃ц繘绋嬶紝鑴氭湰浼氬厛鍋滄鏃ц繘绋嬪啀閲嶆柊閮ㄧ讲

## 9. OneBot 琛屼负绾︽潫

浠呭湪浠ヤ笅鏉′欢婊¤冻鏃跺鐞嗘秷鎭細
- `post_type=message`
- `message_type=private`
- 鍙戦€佽€?`user_id == OWNER_QQ`
- 娑堟伅涓虹函鏂囨湰

涓嶄細澶勭悊锛?- 缇ゆ秷鎭?- CQ 鐮侀潪绾枃鏈秷鎭?- 闈?owner 鐢ㄦ埛娑堟伅

## 10. 鏃ュ父鑱婂ぉ鍒嗘鍙戦€?
鏅€氳亰澶╁洖澶嶄細鎸夎鍒欐媶鎴?1 鍒?3 鏉★紝鏇村儚鐪熷疄绉佽亰锛?- 鐭彞
- 杞诲井闂撮殧
- 涓嶆妸鎻愰啋銆佸懡浠ゃ€佹妧鏈唴瀹逛贡鎷?
榛樿涓嶆媶鐨勫満鏅細
- `/memory`
- `/remind`
- `/chatpush`
- `/prompt`
- Reminder 鍒扮偣娑堟伅
- ChatPush
- 浠ｇ爜鍧?/ 鍛戒护 / SQL / JSON 椋庢牸鍥炲

## 11. 甯歌闂

### 11.1 鏀朵笉鍒?OneBot 鍥炶皟

妫€鏌ワ細
- OneBot 鍙嶅悜 HTTP 鏄惁鎸囧悜鏈湇鍔?`POST /onebot/event`
- 闃茬伀澧欎笌绔彛鏄惁鏀捐
- `OWNER_QQ` 鏄惁姝ｇ‘

### 11.2 鑳芥敹鍒版秷鎭絾涓嶅洖澶?
妫€鏌ワ細
- 鏄惁涓虹鑱婃枃鏈?- `ONEBOT_API_BASE_URL` 鏄惁鍙揪
- `ONEBOT_ACCESS_TOKEN` 鏄惁鍖归厤
- 褰撳墠閰嶇疆鐨勮亰澶╂ā鍨嬫帴鍙ｅ拰 API Key 鏄惁鏈夋晥

### 11.3 鍚姩鏃舵姤鏁版嵁搴撴垨 Redis 閿欒

妫€鏌ワ細
- MySQL / Redis 鏈嶅姟鏄惁宸插惎鍔?- 杩炴帴鍦板潃銆佽处鍙枫€佸瘑鐮佹槸鍚︽纭?- 鏈嶅姟鍣ㄧ綉缁滄槸鍚﹀彲杈?
## 12. 瀹夊叏寤鸿

- 涓嶈鎶婄湡瀹?API Key銆佹暟鎹簱瀵嗙爜鎻愪氦鍒颁粨搴?- 涓嶈鎶?`.deploy/` 鐩綍鎻愪氦鍒扮増鏈簱
- 涓嶈鍦ㄦ棩蹇椾腑鎵撳嵃瀹屾暣 Token / Key
- 鐢熶骇鐜寤鸿閫氳繃鐙珛璐﹀彿鍜屾渶灏忔潈闄愭暟鎹簱璐﹀彿杩愯

## 13. 褰撳墠寤鸿鐨勬彁浜ゆ祦绋?
```bash
mvn -s .mvn/settings.xml test
mvn -s .mvn/settings.xml clean package
```

濡傛灉浣犲彧鎯虫渶蹇惎鍔ㄤ竴濂楀彲杩愯鐜锛岀洿鎺ョ敤锛?- `deploy-windows.ps1`
- `deploy-linux.sh`

