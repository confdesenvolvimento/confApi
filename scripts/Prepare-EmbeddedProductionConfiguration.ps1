param(
    [Parameter(Mandatory=$true)][string]$MobileConfigPath,
    [Parameter(Mandatory=$true)][string]$CserviceConfigPath,
    [string]$CserviceProjectPath='D:\Projetos\cServices3.0'
)
$ErrorActionPreference='Stop'
$projectPath=Split-Path $PSScriptRoot -Parent
$privateMobile=Get-Content -LiteralPath $MobileConfigPath -Raw | ConvertFrom-Json
foreach($name in @('technicalLogin','technicalPassword','cserviceLogin','cservicePassword','cserviceTokenResponseKey')) {
    if([string]::IsNullOrWhiteSpace($privateMobile.$name)) { throw "Configuracao privada incompleta: $name" }
}
$productionServices=[ordered]@{
    manager='https://intranet.confianca.tur.br/confiancamanger4/'
    hub='https://intranet.confianca.tur.br/SandBox4/'
    confapi='https://intranet.confianca.tur.br/confApi4/'
    cache='https://intranet.confianca.tur.br/CacheHotelManger/'
    cservice='https://codtk2.confianca.tur.br/'
    email='https://intranet.confianca.tur.br/email/mail/'
}
$mobile=[ordered]@{
    enabled=$true
    development=$false
    technicalLogin=$privateMobile.technicalLogin
    technicalPassword=$privateMobile.technicalPassword
    cserviceLogin=$privateMobile.cserviceLogin
    cservicePassword=$privateMobile.cservicePassword
    cserviceTokenResponseKey=$privateMobile.cserviceTokenResponseKey
    services=$productionServices
    productionServices=$productionServices
    allowedProductionHosts=@('intranet.confianca.tur.br','codtk2.confianca.tur.br','codtk.confianca.tur.br')
}
$mobileTarget=Join-Path $projectPath 'src\main\resources\confapi-mobile.json'
[IO.File]::WriteAllText($mobileTarget,($mobile|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))

# Preserve the existing private datasource and cryptographic material, removing
# local-only switches and any external config imports from the production resource.
$cserviceLines=[IO.File]::ReadAllLines($CserviceConfigPath)
$cserviceLines=@($cserviceLines | Where-Object {
    $_ -notmatch '^\s*(app\.dev(?:\.allowed-origins)?|cservices\.token\.debug-log-enabled|server\.port|spring\.config\.[^=\s]+|spring\.profiles\.[^=\s]+)\s*[:=]'
})
$cserviceLines+=@('','app.dev=false','cservices.token.debug-log-enabled=false','server.port=8087')
foreach($key in @('tokens.storage.url','tokens.storage.username','tokens.storage.password','cservices.token.hash-secret')) {
    if(-not ($cserviceLines -match ('^'+[regex]::Escape($key)+'\s*[:=]\s*\S'))) {
        throw "Configuracao privada incompleta: $key"
    }
}
$cserviceTarget=Join-Path $CserviceProjectPath 'src\main\resources\application-bundled-prod.properties'
[IO.File]::WriteAllLines($cserviceTarget,$cserviceLines,[Text.UTF8Encoding]::new($false))
Write-Output 'Configuracoes privadas de producao incorporadas aos recursos locais; valores nao exibidos.'
