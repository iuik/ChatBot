param(
    [switch]$PrepareOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

$DeployDir = Join-Path $ProjectRoot ".deploy"
$LogDir = Join-Path $DeployDir "logs"
$EnvFile = Join-Path $DeployDir "chatbot-env.ps1"
$PidFile = Join-Path $DeployDir "chatbot.pid"

New-Item -ItemType Directory -Force -Path $DeployDir | Out-Null
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Read-ConfigValue {
    param(
        [string]$Name,
        [string]$Prompt,
        [string]$DefaultValue = "",
        [bool]$Required = $false
    )

    while ($true) {
        if ($DefaultValue -ne "") {
            $inputValue = Read-Host "$Prompt [$DefaultValue]"
        } else {
            $inputValue = Read-Host $Prompt
        }

        if ([string]::IsNullOrWhiteSpace($inputValue)) {
            $inputValue = $DefaultValue
        }

        if ($Required -and [string]::IsNullOrWhiteSpace($inputValue)) {
            Write-Host "$Name is required." -ForegroundColor Yellow
            continue
        }

        return $inputValue
    }
}

function Set-EnvVar {
    param(
        [string]$Name,
        [string]$Value
    )

    [Environment]::SetEnvironmentVariable($Name, $Value, "Process")
}

$configItems = @(
    @{ Name = "SERVER_PORT"; Prompt = "HTTP port"; Default = "8090"; Required = $true },
    @{ Name = "OWNER_QQ"; Prompt = "Owner QQ"; Default = ""; Required = $true },
    @{ Name = "ONEBOT_API_BASE_URL"; Prompt = "OneBot API base URL"; Default = "http://127.0.0.1:3000"; Required = $true },
    @{ Name = "ONEBOT_ACCESS_TOKEN"; Prompt = "OneBot access token"; Default = ""; Required = $false },
    @{ Name = "DEEPSEEK_API_KEY"; Prompt = "DeepSeek API key"; Default = ""; Required = $true },
    @{ Name = "DEEPSEEK_BASE_URL"; Prompt = "DeepSeek base URL"; Default = "https://api.deepseek.com"; Required = $true },
    @{ Name = "DEEPSEEK_MODEL"; Prompt = "DeepSeek model"; Default = "deepseek-chat"; Required = $true },
    @{ Name = "SPRING_DATASOURCE_URL"; Prompt = "MySQL JDBC URL"; Default = "jdbc:mysql://127.0.0.1:3306/chatbot?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"; Required = $true },
    @{ Name = "SPRING_DATASOURCE_USERNAME"; Prompt = "MySQL username"; Default = "root"; Required = $true },
    @{ Name = "SPRING_DATASOURCE_PASSWORD"; Prompt = "MySQL password"; Default = ""; Required = $true },
    @{ Name = "SPRING_DATASOURCE_DRIVER_CLASS_NAME"; Prompt = "JDBC driver class"; Default = "com.mysql.cj.jdbc.Driver"; Required = $true },
    @{ Name = "SPRING_DATA_REDIS_HOST"; Prompt = "Redis host"; Default = "127.0.0.1"; Required = $true },
    @{ Name = "SPRING_DATA_REDIS_PORT"; Prompt = "Redis port"; Default = "6379"; Required = $true },
    @{ Name = "SPRING_DATA_REDIS_USERNAME"; Prompt = "Redis username"; Default = ""; Required = $false },
    @{ Name = "SPRING_DATA_REDIS_PASSWORD"; Prompt = "Redis password"; Default = ""; Required = $false },
    @{ Name = "SPRING_DATA_REDIS_DATABASE"; Prompt = "Redis database index"; Default = "0"; Required = $true },
    @{ Name = "SPRING_SQL_INIT_MODE"; Prompt = "Spring SQL init mode"; Default = "always"; Required = $true },
    @{ Name = "BOT_TIMEZONE"; Prompt = "Bot timezone"; Default = "Asia/Shanghai"; Required = $true },
    @{ Name = "BOT_CHAT_TIMEOUT_SECONDS"; Prompt = "Chat timeout seconds"; Default = "20"; Required = $true },
    @{ Name = "BOT_CHAT_MAX_REPLY_TOKENS"; Prompt = "Chat max reply tokens"; Default = "500"; Required = $true },
    @{ Name = "BOT_CHAT_MAX_CONTEXT_MESSAGES"; Prompt = "Chat max context messages"; Default = "20"; Required = $true },
    @{ Name = "BOT_CHAT_QUEUE_DEBOUNCE_MILLIS"; Prompt = "Queue debounce millis"; Default = "1200"; Required = $true },
    @{ Name = "BOT_CHAT_PROCESSING_LOCK_SECONDS"; Prompt = "Queue processing lock seconds"; Default = "120"; Required = $true },
    @{ Name = "BOT_CHAT_QUEUE_MAX_BATCH_SIZE"; Prompt = "Queue max batch size"; Default = "8"; Required = $true },
    @{ Name = "BOT_CHAT_QUEUE_MAX_MERGED_MESSAGE_CHARS"; Prompt = "Queue max merged chars"; Default = "4000"; Required = $true },
    @{ Name = "BOT_MEMORY_AUTO_EXTRACT_ENABLED"; Prompt = "Enable auto memory extract (true/false)"; Default = "false"; Required = $true },
    @{ Name = "BOT_PROACTIVE_ENABLED"; Prompt = "Enable proactive reminders (true/false)"; Default = "true"; Required = $true },
    @{ Name = "BOT_PROACTIVE_SCAN_INTERVAL_MILLIS"; Prompt = "Proactive scan interval millis"; Default = "30000"; Required = $true },
    @{ Name = "BOT_PROACTIVE_QUIET_HOURS_START"; Prompt = "Proactive quiet start"; Default = "23:30"; Required = $true },
    @{ Name = "BOT_PROACTIVE_QUIET_HOURS_END"; Prompt = "Proactive quiet end"; Default = "08:30"; Required = $true },
    @{ Name = "BOT_PROACTIVE_MAX_PER_DAY"; Prompt = "Proactive max per day"; Default = "3"; Required = $true },
    @{ Name = "BOT_PROACTIVE_TIMEZONE"; Prompt = "Proactive timezone"; Default = "Asia/Shanghai"; Required = $true },
    @{ Name = "BOT_PROACTIVE_DEFER_MINUTES_WHEN_QUIET"; Prompt = "Proactive defer minutes when quiet"; Default = "30"; Required = $true },
    @{ Name = "BOT_REMINDER_NL_ENABLED"; Prompt = "Enable natural reminder parsing (true/false)"; Default = "true"; Required = $true },
    @{ Name = "BOT_REMINDER_AI_INTENT_ENABLED"; Prompt = "Enable reminder AI intent (true/false)"; Default = "true"; Required = $true },
    @{ Name = "BOT_REMINDER_AI_REPLY_ENABLED"; Prompt = "Enable reminder AI reply (true/false)"; Default = "true"; Required = $true },
    @{ Name = "BOT_REMINDER_MAX_CANDIDATES"; Prompt = "Reminder max candidates"; Default = "5"; Required = $true },
    @{ Name = "BOT_CHATPUSH_ENABLED"; Prompt = "Enable ChatPush (true/false)"; Default = "false"; Required = $true },
    @{ Name = "BOT_CHATPUSH_SCAN_INTERVAL_MILLIS"; Prompt = "ChatPush scan interval millis"; Default = "600000"; Required = $true },
    @{ Name = "BOT_CHATPUSH_MIN_IDLE_HOURS"; Prompt = "ChatPush min idle hours"; Default = "6"; Required = $true },
    @{ Name = "BOT_CHATPUSH_COOLDOWN_HOURS"; Prompt = "ChatPush cooldown hours"; Default = "6"; Required = $true },
    @{ Name = "BOT_CHATPUSH_MAX_PER_DAY"; Prompt = "ChatPush max per day"; Default = "2"; Required = $true },
    @{ Name = "BOT_CHATPUSH_QUIET_HOURS_START"; Prompt = "ChatPush quiet start"; Default = "23:30"; Required = $true },
    @{ Name = "BOT_CHATPUSH_QUIET_HOURS_END"; Prompt = "ChatPush quiet end"; Default = "08:30"; Required = $true },
    @{ Name = "BOT_CHATPUSH_TIMEZONE"; Prompt = "ChatPush timezone"; Default = "Asia/Shanghai"; Required = $true },
    @{ Name = "BOT_CHATPUSH_MAX_MESSAGE_LENGTH"; Prompt = "ChatPush max message length"; Default = "120"; Required = $true },
    @{ Name = "BOT_CHATPUSH_USE_AI_GENERATOR"; Prompt = "ChatPush use AI generator (true/false)"; Default = "true"; Required = $true },
    @{ Name = "BOT_CHATPUSH_MIN_ALLOWED_IDLE_MINUTES"; Prompt = "ChatPush min allowed idle minutes"; Default = "10"; Required = $true },
    @{ Name = "BOT_CHATPUSH_MIN_ALLOWED_COOLDOWN_MINUTES"; Prompt = "ChatPush min allowed cooldown minutes"; Default = "10"; Required = $true },
    @{ Name = "BOT_CHATPUSH_MAX_ALLOWED_PER_DAY"; Prompt = "ChatPush max allowed per day"; Default = "10"; Required = $true },
    @{ Name = "BOT_PROMPT_PROFILE_FILE"; Prompt = "Prompt profile file path"; Default = ""; Required = $false },
    @{ Name = "BOT_PROMPT_PROFILE_TEXT"; Prompt = "Prompt profile text"; Default = ""; Required = $false },
    @{ Name = "BOT_PROMPT_PROFILE_SOURCE_PRIORITY"; Prompt = "Prompt source priority"; Default = "file-first"; Required = $true },
    @{ Name = "BOT_PROMPT_RELOAD_FILE_EACH_REQUEST"; Prompt = "Reload prompt file each request (true/false)"; Default = "false"; Required = $true },
    @{ Name = "BOT_PROMPT_MAX_PROFILE_CHARS"; Prompt = "Prompt max profile chars"; Default = "8000"; Required = $true },
    @{ Name = "BOT_PROMPT_INCLUDE_TIME_CONTEXT"; Prompt = "Include time context (true/false)"; Default = "true"; Required = $true },
    @{ Name = "BOT_DELIVERY_SPLIT_ENABLED"; Prompt = "Enable response split delivery (true/false)"; Default = "true"; Required = $true },
    @{ Name = "BOT_DELIVERY_MAX_PARTS"; Prompt = "Delivery max parts"; Default = "3"; Required = $true },
    @{ Name = "BOT_DELIVERY_MAX_PART_CHARS"; Prompt = "Delivery max chars per part"; Default = "80"; Required = $true },
    @{ Name = "BOT_DELIVERY_MIN_DELAY_MILLIS"; Prompt = "Delivery min delay millis"; Default = "700"; Required = $true },
    @{ Name = "BOT_DELIVERY_MAX_DELAY_MILLIS"; Prompt = "Delivery max delay millis"; Default = "1800"; Required = $true },
    @{ Name = "BOT_DELIVERY_SPLIT_DAILY_CHAT_ONLY"; Prompt = "Split only daily chat (true/false)"; Default = "true"; Required = $true },
    @{ Name = "EMBEDDING_ENABLED"; Prompt = "Enable embedding (true/false)"; Default = "true"; Required = $true },
    @{ Name = "EMBEDDING_BASE_URL"; Prompt = "Embedding base URL"; Default = ""; Required = $false },
    @{ Name = "EMBEDDING_API_KEY"; Prompt = "Embedding API key"; Default = ""; Required = $false },
    @{ Name = "EMBEDDING_MODEL"; Prompt = "Embedding model"; Default = ""; Required = $false },
    @{ Name = "EMBEDDING_DIMENSION"; Prompt = "Embedding dimension"; Default = "1024"; Required = $true },
    @{ Name = "EMBEDDING_TIMEOUT_SECONDS"; Prompt = "Embedding timeout seconds"; Default = "15"; Required = $true },
    @{ Name = "QDRANT_ENABLED"; Prompt = "Enable Qdrant (true/false)"; Default = "true"; Required = $true },
    @{ Name = "QDRANT_HOST"; Prompt = "Qdrant host"; Default = "127.0.0.1"; Required = $true },
    @{ Name = "QDRANT_PORT"; Prompt = "Qdrant port"; Default = "6334"; Required = $true },
    @{ Name = "QDRANT_COLLECTION"; Prompt = "Qdrant collection"; Default = "qq_bot_memory"; Required = $true },
    @{ Name = "QDRANT_API_KEY"; Prompt = "Qdrant API key"; Default = ""; Required = $false },
    @{ Name = "QDRANT_VECTOR_SIZE"; Prompt = "Qdrant vector size"; Default = "1024"; Required = $true }
)

$envMap = [ordered]@{}
foreach ($item in $configItems) {
    $envMap[$item.Name] = Read-ConfigValue -Name $item.Name -Prompt $item.Prompt -DefaultValue $item.Default -Required $item.Required
}

$envLines = @()
$envLines += "# Generated by deploy-windows.ps1"
$envLines += "# Re-run the script to update values"
foreach ($entry in $envMap.GetEnumerator()) {
    $escaped = $entry.Value.Replace('`', '``').Replace('"', '`"')
    $envLines += ('$env:{0}="{1}"' -f $entry.Key, $escaped)
}
Set-Content -Path $EnvFile -Value $envLines -Encoding UTF8

foreach ($entry in $envMap.GetEnumerator()) {
    Set-EnvVar -Name $entry.Key -Value $entry.Value
}

Write-Host ""
Write-Host "Saved environment file: $EnvFile" -ForegroundColor Green

if ($PrepareOnly) {
    Write-Host "PrepareOnly mode enabled. No build or start performed." -ForegroundColor Yellow
    exit 0
}

if (Test-Path $PidFile) {
    $oldPid = (Get-Content $PidFile -Raw).Trim()
    if ($oldPid) {
        $existing = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
        if ($existing) {
            Write-Host "Stopping old process PID=$oldPid" -ForegroundColor Yellow
            Stop-Process -Id $oldPid -Force
            Start-Sleep -Seconds 1
        }
    }
}

Write-Host "Building project..." -ForegroundColor Cyan
& mvn -s .mvn/settings.xml clean package

$jarFile = Get-ChildItem -Path (Join-Path $ProjectRoot "target") -Filter "*.jar" |
    Where-Object { $_.Name -notlike "*.original" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jarFile) {
    throw "No runnable jar found under target/."
}

$stdoutLog = Join-Path $LogDir "chatbot.out.log"
$stderrLog = Join-Path $LogDir "chatbot.err.log"

Write-Host "Starting application from $($jarFile.FullName)" -ForegroundColor Cyan
$process = Start-Process -FilePath "java" `
    -ArgumentList @("-jar", $jarFile.FullName) `
    -WorkingDirectory $ProjectRoot `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -PassThru

Set-Content -Path $PidFile -Value $process.Id -Encoding ASCII

Write-Host ""
Write-Host "Deployment completed." -ForegroundColor Green
Write-Host "PID: $($process.Id)"
Write-Host "Out log: $stdoutLog"
Write-Host "Err log: $stderrLog"
Write-Host "Env file: $EnvFile"
