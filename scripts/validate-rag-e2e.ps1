param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$ApiUrl = "$BaseUrl/api"

function Write-Step {
    param([string]$Message)
    Write-Host "`n[PASSO] $Message" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Fail {
    param([string]$Message)
    Write-Host "[FALHA] $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "  -> $Message" -ForegroundColor Gray
}

# -----------------------------------------------------------------
# 1. Verificar GET /api/health
# -----------------------------------------------------------------
Write-Step "1. Verificando health endpoint..."

try {
    $health = Invoke-RestMethod -Uri "$ApiUrl/health" -Method Get
    if ($health.status -ne "UP") {
        throw "Health retornou status: $($health.status)"
    }
    Write-Success "Health endpoint OK (status=$($health.status))"
} catch {
    Write-Fail "Health endpoint falhou: $_"
    exit 1
}

# -----------------------------------------------------------------
# 2. Criar sessao
# -----------------------------------------------------------------
Write-Step "2. Criando sessao 'Teste Final RAG Violeta'..."

$sessionBody = @{ title = "Teste Final RAG Violeta" } | ConvertTo-Json
try {
    $session = Invoke-RestMethod -Uri "$ApiUrl/sessions" `
        -Method Post `
        -ContentType "application/json; charset=utf-8" `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($sessionBody))
    Write-Success "Sessao criada (id=$($session.id), title='$($session.title)')"
} catch {
    Write-Fail "Falha ao criar sessao: $_"
    exit 1
}

$sessionId = $session.id

# -----------------------------------------------------------------
# 3. Criar arquivo TXT de teste local
# -----------------------------------------------------------------
Write-Step "3. Criando arquivo TXT de teste..."

$testContent = @"
Teste de recuperacao contextual do MindJournal.

O projeto MindJourney foi desenvolvido pelo Grupo 2.
A cor secreta utilizada no teste de recuperacao contextual e violeta.
Esta informacao deve ser recuperada pelo pipeline RAG a partir deste documento.
"@

$tempFile = [System.IO.Path]::GetTempFileName() -replace '\.tmp$', '.txt'
$tempFile = [System.IO.Path]::ChangeExtension(
    [System.IO.Path]::GetTempFileName(),
    '.txt'
)

try {
    $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
    [System.IO.File]::WriteAllText($tempFile, $testContent, $utf8NoBom)
    Write-Success "Arquivo TXT criado em: $tempFile (UTF-8 sem BOM)"
} catch {
    Write-Fail "Falha ao criar arquivo: $_"
    exit 1
}

# -----------------------------------------------------------------
# 4. Upload do arquivo
# -----------------------------------------------------------------
Write-Step "4. Fazendo upload do arquivo..."

try {
    Add-Type -AssemblyName System.Net.Http

    $client = New-Object System.Net.Http.HttpClient
    $content = New-Object System.Net.Http.MultipartFormDataContent

    $fileStream = [System.IO.File]::OpenRead($tempFile)
    $fileContent = New-Object System.Net.Http.StreamContent($fileStream)
    $fileContent.Headers.ContentDisposition = New-Object System.Net.Http.Headers.ContentDispositionHeaderValue("form-data")
    $fileContent.Headers.ContentDisposition.Name = "file"
    $fileContent.Headers.ContentDisposition.FileName = [System.IO.Path]::GetFileName($tempFile)
    $fileContent.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue("text/plain")
    $content.Add($fileContent)

    $sessionIdContent = New-Object System.Net.Http.StringContent($sessionId.ToString())
    $sessionIdContent.Headers.ContentDisposition = New-Object System.Net.Http.Headers.ContentDispositionHeaderValue("form-data")
    $sessionIdContent.Headers.ContentDisposition.Name = "sessionId"
    $content.Add($sessionIdContent)

    $response = $client.PostAsync("$ApiUrl/upload", $content).GetAwaiter().GetResult()
    $responseContent = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $fileStream.Dispose()
    $client.Dispose()

    if (-not $response.IsSuccessStatusCode) {
        throw "HTTP $($response.StatusCode): $responseContent"
    }

    $uploadResponse = $responseContent | ConvertFrom-Json
    Write-Success "Upload realizado (attachmentId=$($uploadResponse.id), documentId=$($uploadResponse.documentId))"
} catch {
    Write-Fail "Falha no upload: $_"
    exit 1
}

$documentId = $uploadResponse.documentId

# -----------------------------------------------------------------
# 5. Polling do status do documento ate INDEXED
# -----------------------------------------------------------------
Write-Step "5. Polling do status do documento (max 60s, intervalo 2s)..."

$maxAttempts = 30
$attempt = 0
$finalStatus = $null

do {
    $attempt++
    Start-Sleep -Seconds 2

    try {
        $docStatus = Invoke-RestMethod -Uri "$ApiUrl/documents/$documentId/status" -Method Get
        $finalStatus = $docStatus.status
        Write-Info "Tentativa $($attempt): status=$finalStatus"
    } catch {
        Write-Info "Tentativa $($attempt): erro na consulta - $_"
    }
} while ($finalStatus -ne "INDEXED" -and $attempt -lt $maxAttempts)

if ($finalStatus -eq "INDEXED") {
    Write-Success "Documento INDEXED (tentativas=$attempt)"
} elseif ($finalStatus -eq "FAILED") {
    Write-Fail "Documento falhou com status FAILED"
    exit 1
} else {
    Write-Fail "Timeout: documento nao atingiu INDEXED apos $maxAttempts tentativas"
    exit 1
}

# -----------------------------------------------------------------
# 6. Enviar mensagem para o chat
# -----------------------------------------------------------------
Write-Step "6. Enviando mensagem para o chat..."

$chatBody = @{
    sessionId = $sessionId
    content   = "Qual e a cor secreta utilizada no teste de recuperacao contextual?"
} | ConvertTo-Json

try {
    $chatResponse = Invoke-RestMethod -Uri "$ApiUrl/chat" `
        -Method Post `
        -ContentType "application/json; charset=utf-8" `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($chatBody))
    Write-Success "Resposta do chat recebida"
} catch {
    Write-Fail "Falha ao enviar mensagem: $_"
    exit 1
}

$assistantContent = $chatResponse.assistantMessage.content
$sources = $chatResponse.sources

# -----------------------------------------------------------------
# 7. Exibir diagnostico completo
# -----------------------------------------------------------------
Write-Step "7. Diagnostico da resposta do chat..."

Write-Host ""
Write-Host "--- MENSAGEM DO ASSISTENTE ---" -ForegroundColor Gray
Write-Host "$assistantContent" -ForegroundColor White
Write-Host ""

Write-Host "--- SOURCES ($($sources.Count)) ---" -ForegroundColor Gray
if ($sources -and $sources.Count -gt 0) {
    foreach ($s in $sources) {
        Write-Host "  DocumentId:       $($s.documentId)" -ForegroundColor White
        Write-Host "  FileName:         $($s.fileName)" -ForegroundColor White
        Write-Host "  ChunkId:          $($s.chunkId)" -ForegroundColor White
        Write-Host "  Content:          $($s.content)" -ForegroundColor White
        Write-Host "  SimilarityScore:  $($s.similarityScore)" -ForegroundColor White
        Write-Host "  ---" -ForegroundColor Gray
    }
} else {
    Write-Host "  (nenhum source encontrado)" -ForegroundColor Yellow
}
Write-Host ""

# -----------------------------------------------------------------
# 8. Validar resposta
# -----------------------------------------------------------------
Write-Step "8. Validando resposta do assistente..."

$containsVioleta = $assistantContent -match '(?i)violeta'
$hasSources = ($sources -and $sources.Count -gt 0)

if (-not $containsVioleta) {
    Write-Fail "Resposta do assistente nao contem a palavra 'violeta'"
}

if (-not $hasSources) {
    Write-Fail "Resposta nao possui sources"
}

# -----------------------------------------------------------------
# 9. Exibir resumo final
# -----------------------------------------------------------------
Write-Step "9. Resumo final"

Write-Host ""
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "       RESUMO DA VALIDACAO RAG" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "Session ID:           $sessionId" -ForegroundColor White
Write-Host "Document ID:          $documentId" -ForegroundColor White
Write-Host "Status final:         $finalStatus" -ForegroundColor White
Write-Host "Resposta assistente:  $assistantContent" -ForegroundColor White
if ($hasSources) {
    $s = $sources[0]
    Write-Host "Source filename:     $($s.fileName)" -ForegroundColor White
    Write-Host "Source chunkId:      $($s.chunkId)" -ForegroundColor White
    Write-Host "Source content:      $($s.content)" -ForegroundColor White
    Write-Host "Source similarity:   $($s.similarityScore)" -ForegroundColor White
}
Write-Host ""

# -----------------------------------------------------------------
# 10. VEREDITO FINAL
# -----------------------------------------------------------------
Write-Step "10. VEREDITO FINAL"

if ($containsVioleta -and $hasSources) {
    Write-Host ""
    Write-Host "  ##############################" -ForegroundColor Green
    Write-Host "  #        TESTE APROVADO       #" -ForegroundColor Green
    Write-Host "  ##############################" -ForegroundColor Green
    Write-Host ""
    exit 0
} else {
    Write-Host ""
    Write-Host "  ##############################" -ForegroundColor Red
    Write-Host "  #       TESTE REPROVADO       #" -ForegroundColor Red
    Write-Host "  ##############################" -ForegroundColor Red
    Write-Host ""
    exit 1
}
