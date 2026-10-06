# ============================================================
# 下载 SkyWalking Java Agent，解压到 docker/skywalking/agent/
#
# 用法（项目根目录执行，需 PowerShell 5+）：
#   powershell -ExecutionPolicy Bypass -File .\docker\skywalking\download-agent.ps1
#   powershell -ExecutionPolicy Bypass -File .\docker\skywalking\download-agent.ps1 -Version 9.7.0
#
# 说明：
#   1. 本文件必须保存为 UTF-8 with BOM，否则 Windows PowerShell 5.1 会按 GBK 解析中文导致语法错误。
#   2. 解压后的 agent 目录已在 .gitignore 中忽略，不需要提交到仓库。
# ============================================================
param(
    [string]$Version = '9.7.0',
    [string]$Mirror = 'https://mirrors.aliyun.com/apache/skywalking'
)

$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$agentDir = Join-Path $root 'docker\skywalking\agent'
$fileName = "apache-skywalking-java-agent-$Version.tgz"
$url = "$Mirror/java-agent/$Version/$fileName"
$tmpFile = Join-Path $env:TEMP $fileName

Write-Host "==> 下载 SkyWalking Java Agent $Version" -ForegroundColor Cyan
Write-Host "    $url" -ForegroundColor DarkGray
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Invoke-WebRequest -Uri $url -OutFile $tmpFile -UseBasicParsing

Write-Host "==> 解压到 $agentDir" -ForegroundColor Cyan
if (Test-Path $agentDir) {
    Remove-Item $agentDir -Recurse -Force
}
New-Item -ItemType Directory -Path $agentDir -Force | Out-Null

# 压缩包顶层是 skywalking-agent/ 目录，strip-components=1 直接展开到 agent 目录
tar -xzf $tmpFile -C $agentDir --strip-components=1
Remove-Item $tmpFile -Force

$jarPath = Join-Path $agentDir 'skywalking-agent.jar'
if (-not (Test-Path $jarPath)) {
    throw "解压后未找到 $jarPath，请检查下载包结构是否正确"
}

Write-Host ''
Write-Host "完成！Agent 路径：$jarPath" -ForegroundColor Green
Write-Host ''
Write-Host 'IDEA 各服务 VM options 参考（service_name 换成对应服务名）：' -ForegroundColor Yellow
Write-Host "  -javaagent:`"$jarPath`" -Dskywalking.agent.service_name=mall-user -Dskywalking.collector.backend_service=127.0.0.1:11800"
