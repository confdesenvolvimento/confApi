param([ValidateSet('manager','hub','confapi','payara','admin')][string]$Project='payara')
$ErrorActionPreference='Stop'
$issues=[System.Collections.Generic.List[string]]::new()
# Read only: examines this process environment, never connects to an application or database.
# These are the settings consumed by the current Manager-owned authentication implementation.
$common=@('AUTH_V2_ENABLED','AUTH_V2_ISSUER','AUTH_V2_JWKS_JSON')
$required=@{
 manager=@('AUTH_MOBILE_ENABLED','AUTH_MOBILE_PUBLIC_BASE_URL','AUTH_MOBILE_HMAC_KEY_BASE64','AUTH_MOBILE_HMAC_KEY_ID','AUTH_MOBILE_DELIVERY_AES_KEY_BASE64','AUTH_MOBILE_DELIVERY_ENABLED','AUTH_V2_OPERATOR_PRIVATE_JWK_FILE','AUTH_V2_MAIL_URL','AUTH_V2_MAIL_TOKEN_FILE','CSERVICE_TOKEN_URL','CSERVICE_TOKEN_CLIENT_FILE')
 hub=@('AUTH_V2_MANAGER_URL','AUTH_V2_MANAGER_TOKEN_FILE','AUTH_V2_RESOURCE_ORIGIN','AUTH_V2_MOBILE_ROUTES_JSON')
 confapi=@('AUTH_V2_MANAGER_URL','AUTH_V2_MANAGER_TOKEN_FILE','AUTH_V2_RESOURCE_ORIGIN','AUTH_V2_MOBILE_ROUTES_JSON')
 payara=@('AUTH_V2_PRIMARY_URL','AUTH_V2_PORTAL_URL','AUTH_V2_MANAGER_URL','AUTH_V2_MANAGER_TOKEN_FILE')
 admin=@('AUTH_V2_MANAGER_URL','AUTH_V2_MANAGER_TOKEN_FILE')
}
foreach($name in ($common+$required[$Project])){
 $value=[Environment]::GetEnvironmentVariable($name,'Process')
 if([string]::IsNullOrWhiteSpace($value)){$issues.Add($name+': ausente');continue}
 if($name.EndsWith('_ENABLED') -and $value -ine 'true'){$issues.Add($name+': deve estar true para habilitar o fluxo completo')}
 if($name.EndsWith('_URL') -or $name.EndsWith('_ORIGIN')){
  $parsed=$null
  if(-not [Uri]::TryCreate($value,[UriKind]::Absolute,[ref]$parsed) -or $parsed.Scheme -ne 'https' -or -not $parsed.Host -or $parsed.UserInfo -or $parsed.Query -or $parsed.Fragment -or $value.EndsWith('/')){$issues.Add($name+': base HTTPS invalida')}
  elseif($name.EndsWith('_ORIGIN') -and $parsed.AbsolutePath -ne '/'){$issues.Add($name+': origem deve ser sem caminho')}
  elseif($name -eq 'AUTH_MOBILE_PUBLIC_BASE_URL' -and -not $parsed.AbsolutePath.EndsWith('/api/v2/autenticador-mobile')){$issues.Add($name+': deve terminar em /api/v2/autenticador-mobile no confApi')}
 }
 if($name.EndsWith('_TOKEN_FILE') -or $name -eq 'CSERVICE_TOKEN_CLIENT_FILE'){
  if(-not [IO.Path]::IsPathRooted($value) -or -not (Test-Path -LiteralPath $value -PathType Leaf)){$issues.Add($name+': arquivo absoluto inexistente')}
  else{try{$jwt=(Get-Content -LiteralPath $value -Raw).Trim();if($jwt -notmatch '^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$'){$issues.Add($name+': conteudo nao parece JWT sem prefixo')}}catch{$issues.Add($name+': leitura indisponivel')}}
 }
 if($name -eq 'AUTH_V2_JWKS_JSON'){
  try{$jwks=$value|ConvertFrom-Json;$keys=@($jwks.keys);if(-not $keys.Count){throw 'empty'};foreach($key in $keys){if($key.kty -ne 'RSA' -or -not $key.kid -or -not $key.n -or -not $key.e -or $key.d -or $key.p -or $key.q){throw 'invalid'}}}catch{$issues.Add($name+': exige JWKS RSA publico com kid')}
 }
 if($name -eq 'AUTH_V2_OPERATOR_PRIVATE_JWK_FILE'){
  try{if(-not [IO.Path]::IsPathRooted($value) -or -not (Test-Path -LiteralPath $value -PathType Leaf)){throw 'missing'};$key=Get-Content -LiteralPath $value -Raw|ConvertFrom-Json;if($key.kty -ne 'RSA' -or -not $key.kid -or -not $key.n -or -not $key.e -or -not $key.d){throw 'invalid'}}catch{$issues.Add($name+': exige arquivo absoluto com JWK RSA privado; somente no Manager')}
 }
 if($name.EndsWith('_KEY_BASE64')){
  try{$bytes=[Convert]::FromBase64String($value);if(($name -eq 'AUTH_MOBILE_HMAC_KEY_BASE64' -and $bytes.Length -lt 32) -or ($name -eq 'AUTH_MOBILE_DELIVERY_AES_KEY_BASE64' -and $bytes.Length -ne 32)){throw 'size'}}catch{$issues.Add($name+': tamanho ou Base64 invalido')}
 }
 if($name -eq 'AUTH_V2_MOBILE_ROUTES_JSON'){
  try{if(-not $value.TrimStart().StartsWith('[')){throw 'array'};$routes=$value|ConvertFrom-Json;foreach($route in $routes){if(-not $route.method -or -not $route.path -or -not $route.path.StartsWith('/api/')){throw 'route'}}}catch{$issues.Add($name+': lista de method/path invalida')}
 }
}
if($Project -eq 'manager' -and $env:AUTH_MOBILE_HMAC_KEY_BASE64 -and $env:AUTH_MOBILE_DELIVERY_AES_KEY_BASE64){
 try{$hmac=[Convert]::ToBase64String([Convert]::FromBase64String($env:AUTH_MOBILE_HMAC_KEY_BASE64));$aes=[Convert]::ToBase64String([Convert]::FromBase64String($env:AUTH_MOBILE_DELIVERY_AES_KEY_BASE64));if($hmac -eq $aes){$issues.Add('AUTH_MOBILE_HMAC_KEY_BASE64 e AUTH_MOBILE_DELIVERY_AES_KEY_BASE64: devem ser diferentes')}}catch{}
}
[pscustomobject]@{Projeto=$Project;Configuracao=if($issues.Count){'PENDENTE'}else{'ESTRUTURA_OK'};Observacao='Somente variaveis deste processo; nao verifica configuracao da JVM/IDE, assinatura, validade de JWT, TLS, rede, banco, primeiro aprovador ou autorizacao operacional. Lista vazia de rotas bloqueia acesso mobile aos recursos de negocio.'}
$issues|ForEach-Object {Write-Output $_}
if($issues.Count){exit 1}
