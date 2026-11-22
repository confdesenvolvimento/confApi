# IP de origem no cadastro de dispositivos

O fluxo do login atual é ConfiançaApp → ConfApi → Manager → ip_solicitacao no Admin.
A ConfApi captura o IP da conexão no POST de cadastro e envia X-Auth-Client-IP
com a credencial técnica existente. O Manager aceita esse cabeçalho apenas de
uma identidade autenticada presente em device.ip.gateway-users (padrão: api.mobile,
a conta usada na configuração atual da ConfApi). A variável DEVICE_IP_GATEWAY_USERS
permite substituir a lista por nomes exatos separados por vírgula.

Para essa conta, cabeçalho ausente, duplicado ou IP inválido recusam a solicitação;
não há fallback silencioso para o IP da ConfApi. Outra conta não pode delegar IP.
Campos ipCadastro enviados pelo aplicativo continuam sendo ignorados.
O valor é salvo nos metadados da solicitação, retornado como ip_solicitacao e,
após a ativação, copiado para dispositivo.ip_cadastro.

## Proxy na entrada da ConfApi

Configure MOBILE_CLIENT_IP_TRUSTED_PROXIES com os IPs/CIDRs exatos dos proxies
entre a internet e a ConfApi. O perfil prod inclui somente 127.0.0.1/32 e ::1/128
por padrão, para um proxy no mesmo host. Não use uma faixa ampla ou 0.0.0.0/0.
Em acesso direto de desenvolvimento, configure a lista vazia.

O resolvedor consulta o request original, antes das alterações do
ForwardedHeaderFilter do Spring, e percorre X-Forwarded-For da direita para a
esquerda, consumindo apenas os saltos confiáveis. Cabeçalhos de um peer não
confiável são ignorados. Um proxy confiável sem uma origem válida é recusado.

Na borda Nginx que recebe diretamente o cliente, por exemplo:

```nginx
proxy_set_header X-Forwarded-For $remote_addr;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header Forwarded "";
```

Se existir um balanceador antes do Nginx, configure primeiro a cadeia de
confiança nele/no Nginx e inclua apenas os proxies reais. Não substitua a
origem pelo endereço de um balanceador sem preservar a cadeia.
Se o container usa RemoteIpValve, ele também deve aceitar apenas proxies
explicitamente autorizados; a aplicação não recupera uma origem já descartada
ou falsificada antes de chegar ao servlet.

## Publicação e validação

Publique as versões corrigidas dos dois serviços em conjunto, pois a conta
api.mobile passa a exigir o IP no cadastro. O app e o Admin não precisam mudar.
Nenhum banco de produção é alterado pelos testes. Registros anteriores são
preservados: o IP original só pode ser recuperado se houver logs históricos.

Depois da publicação, valide com uma nova solicitação no Wi-Fi e outra na rede
móvel, conferindo a origem observada na borda com ip_solicitacao. Um celular com
VPN terá o IP de saída da VPN; NAT pode fazer celulares compartilharem um IP.
Não é o IP privado do Wi-Fi nem uma identificação única do aparelho.

Os testes MobileDeviceIpTest (ConfApi) e DeviceRequestIpTest (Manager) cobrem
IPv4/IPv6, headers forjados, proxies, transporte HTTP, autenticação da delegação,
persistência e o campo que o Admin mostra. Os testes não confirmam por si só
a topologia/configuração do proxy em produção.

Referência: https://docs.spring.io/spring-boot/docs/2.7.9/reference/html/howto.html#howto.webserver.use-behind-a-proxy-server
