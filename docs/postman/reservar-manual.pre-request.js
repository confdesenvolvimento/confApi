// Envio individual pelo botao Send: esta operacao executa booking e checkout.
const required=["baseUrl", "prefix", "appToken", "usuarioToken", "sistema", "codigoViagem", "assento", "nomePassageiro", "sobrenomePassageiro", "cpfPassageiro", "dataNascimento", "sexoPassageiro", "emailCompra", "chaveCompra", "codgAgencia", "codgSistema", "codgVinculoOrigem", "codgVinculoDestino"];
function bloquear(motivo){
 console.error("[ConfApi] REQUISICAO NAO ENVIADA: "+motivo);
 pm.test("Configuracao para enviar a requisicao",()=>{throw new Error(motivo);});
 pm.execution.skipRequest();
 throw new Error(motivo);
}
const missing=required.filter(k=>!String(pm.variables.get(k)??"").trim());
if(missing.length)bloquear("Preencha as variaveis: "+missing.join(", "));
console.info("[ConfApi] Preparando POST /reservar - envio manual");
if(!String(pm.variables.get("appToken")||"").trim())console.warn("[ConfApi] appToken vazio. A chamada sera enviada e o ConfApi deve responder 401/403; preencha Authorization > Bearer Token.");
if(!String(pm.variables.get("usuarioToken")||"").trim())console.warn("[ConfApi] usuarioToken vazio. O acesso ao Manager exige X-Usuario-Token.");
try{JSON.parse(pm.variables.replaceIn(pm.request.body.raw));}catch(e){bloquear("Body invalido: confira variaveis numericas e campos preenchidos.");}
const original=JSON.parse(pm.variables.replaceIn(pm.request.body.raw));
const recovery={emissao:{sistema:original.emissao.sistema||"QueroPassagem",email:original.emissao.email,pagamento:{}},dadosManager:original.dadosManager};
if(original.reservaRodoviario)recovery.reservaRodoviario={sistema:original.reservaRodoviario.sistema||"QueroPassagem"};else recovery.emissao.codigoReserva=original.emissao.codigoReserva;
pm.environment.set("payloadRecuperacao",JSON.stringify(recovery));
["codgReservaRodoviario","codigoCompra","codigoPassageiro","codgRodoviarioTrecho"].forEach(k=>pm.environment.unset(k));
if(original.reservaRodoviario)pm.environment.unset("codigoReservaTemporaria");
