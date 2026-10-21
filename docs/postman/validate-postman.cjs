const fs=require('node:fs'), vm=require('node:vm'), assert=require('node:assert/strict');
const collection=JSON.parse(fs.readFileSync('docs/postman/confapi-rodoviario.postman_collection.json','utf8'));
const environment=JSON.parse(fs.readFileSync('docs/postman/confapi-rodoviario.local.postman_environment.json','utf8'));
const requests=collection.item.flatMap(f=>f.item);
assert.equal(requests.length,35);
const known=new Set(environment.values.map(v=>v.key));
const sample=Object.fromEntries(environment.values.map(v=>[v.key,v.value||'exemplo']));
Object.assign(sample,{appToken:'app.jwt.test',usuarioToken:'user.jwt.test',loginApp:'teste',senhaApp:'fixture',loginUsuario:'teste',senhaUsuario:'fixture',codgSistema:'9',codgAgencia:'7',origemCodigo:'ROD_1',destinoCodigo:'CIT_15',codgVinculoOrigem:'11',codgVinculoDestino:'22',dataViagem:'2030-01-15',codigoViagem:'travel-1',assento:'01',codigoViacao:'9',cpfPassageiro:'12345678909',sexoPassageiro:'1',emailCompra:'teste@example.com',chaveCompra:'postman-rodoviario-0001',codgReservaRodoviario:'10',codigoCompra:'123456',codigoReservaTemporaria:'booking-1',codigoPassageiro:'321',codgRodoviarioTrecho:'100',chaveRecebimento:'recebimento-0001',codgFormaPagto:'1',valorRecebimento:'22.40',dataRecebimento:'2026-01-01',codgTransacao:'gateway-0001',permitirOperacoesComEfeito:'false',acaoPermitida:''});
const replace=(text,values)=>text.replace(/{{([^{}]+)}}/g,(_,k)=>String(values[k]??''));
Object.assign(sample,{codgCidadeOrigem:'101',codgCidadeDestino:'202'});
const buy=requests.find(x=>x.name.startsWith('14 '));
const original=JSON.parse(replace(buy.request.body.raw,sample));
sample.payloadRecuperacao=JSON.stringify({reservaRodoviario:{sistema:sample.sistema},emissao:{sistema:sample.sistema,email:sample.emailCompra,pagamento:{}},dadosManager:original.dadosManager});
let assertions=0;
function expect(v,message,negative=false){
 const o={}; const check=ok=>{assert.ok(negative?!ok:ok,message);assertions++;};
 ['to','be','have','and'].forEach(k=>Object.defineProperty(o,k,{get:()=>o}));
 Object.defineProperty(o,'not',{get:()=>expect(v,message,!negative)});
 Object.defineProperty(o,'empty',{get:()=>{check(v!=null&&v.length===0);return o;}});
 o.equal=x=>{check(v===x);return o;};o.above=x=>{check(v>x);return o;};o.oneOf=xs=>{check(xs.includes(v));return o;};
 o.a=o.an=t=>{check(t==='array'?Array.isArray(v):t==='object'?v!==null&&typeof v==='object'&&!Array.isArray(v):typeof v===t);return o;};
 o.property=k=>{check(v!=null&&Object.prototype.hasOwnProperty.call(v,k));return o;};return o;
}
const SKIP=Symbol('skip');
function context(item,vars,body={},status=200,overrides={}){
 const values={...vars}; const logs=[], failures=[];
 const env={get:k=>values[k],set:(k,v)=>{values[k]=v;},unset:k=>delete values[k]};
 const pm={environment:env,variables:{get:k=>Object.hasOwn(overrides,k)?overrides[k]:values[k],replaceIn:s=>replace(s,{...values,...overrides})},request:item.request,
  execution:{skipRequest:()=>{throw SKIP;}},response:{code:status,json:()=>body,to:{have:{status:c=>{assert.equal(status,c);assertions++;}}}},
  expect,test:(name,fn)=>{try{fn();}catch(error){failures.push({name,error});}}};
 return {ctx:{pm,console:{warn:m=>logs.push(m),info:m=>logs.push(m),error:m=>logs.push(m)}},values,logs,failures};
}
const source=(item,kind)=>item.event.find(e=>e.listen===kind).script.exec.join('\n');
const actions={15:'emitir',18:'cancelarReserva',19:'cancelarBilhete',29:'adicionarNota',30:'registrarRecebimento'};
let guarded=0,bodies=0,posts=0;
for(const item of requests){
 for(const event of item.event)new vm.Script(event.script.exec.join('\n'),{filename:item.name+'-'+event.listen});
 for(const match of JSON.stringify(item.request).matchAll(/{{([^{}]+)}}/g))assert.ok(known.has(match[1]),'Variavel nao declarada: '+match[1]);
 const number=Number(item.name.slice(0,2)), action=actions[number];
 if(item.request.body&&!item.name.startsWith('34 ')){
   const body=JSON.parse(replace(item.request.body.raw,sample));bodies++;
   if([10,11,12].includes(number)){assert.equal(body.origem,101);assert.equal(body.destino,202);}
 }
 const vars={...sample,permitirOperacoesComEfeito:action?'true':'false',acaoPermitida:action||''};
 if(action){
   const blocked=context(item,sample);let skipped=false;
   try{vm.runInNewContext(source(item,'prerequest'),blocked.ctx);}catch(e){if(e===SKIP)skipped=true;else throw e;}
   assert.ok(skipped,'Operacao nao bloqueada: '+item.name);guarded++;
 }
 const run=context(item,vars);
 vm.runInNewContext(source(item,'prerequest'),run.ctx);assert.deepEqual(run.failures,[]);
 if([14,15].includes(number)){
   const recover=JSON.parse(run.values.payloadRecuperacao);assert.deepEqual(recover.emissao.pagamento,{});
   assert.equal(run.values.codgReservaRodoviario,undefined);assert.equal(recover.dadosManager.chaveIdempotencia,sample.chaveCompra);
   if(number===15)assert.equal(run.values.codigoReservaTemporaria,'booking-1');
 }
 let b={content:[]}, status=200;
 if(number===1)b={token:'novo.jwt.test'};
 else if(number===2)b={chaveToken:'individual.jwt.test',agencia:{codgAgencia:7}};
 else if((number>=3&&number<=6)||number===9||(number>=10&&number<=13))b={sistema:'QueroPassagem',dados:[]};
 else if(number>=14&&number<=19)b={success:true,requerConferencia:false,mensagem:'Ok',reserva:{reserva:{codgReservaRodoviario:10,status:1},trechos:[{codgRodoviarioTrecho:100}]},fornecedor:{dados:{codigoFornecedor:123456,situacao:'paid',trechos:[{passageiros:[{codigoFornecedor:321,bilhete:{situacao:'emitted',numero:'ABC123'}}]}]}}};
 else if(number===21)b={reserva:{codgReservaRodoviario:10},trechos:[],passageiros:[],assentos:[],bilhetes:[],recebimentos:[]};
 else if([22,23].includes(number))b=[];
 else if(number===29){b={};status=201;}
 else if(number===30){b={codgRecebimento:500};status=201;}
 else if(number===31){b={};status=403;}
 else if(number===32){b={};status=401;}
 else if([33,34].includes(number)){b={};status=400;}
 else if(number===35)b={success:false,requerConferencia:true};
 const post=context(item,vars,b,status);vm.runInNewContext(source(item,'test'),post.ctx);assert.deepEqual(post.failures,[]);posts++;
 if(action)assert.equal(post.values.acaoPermitida,'');
}
for(const k of ['appToken','usuarioToken','senhaApp','senhaUsuario','payloadRecuperacao'])assert.equal(environment.values.find(v=>v.key===k).value,'');
const injected=structuredClone(buy);const withCard=structuredClone(original);withCard.emissao.pagamento.numeroCartao='CARD_SECRET_FIXTURE';withCard.emissao.comprador={nome:'BUYER_SECRET_FIXTURE'};injected.request.body.raw=JSON.stringify(withCard);
const safe=context(injected,{...sample,permitirOperacoesComEfeito:'true',acaoPermitida:'reservar'});vm.runInNewContext(source(injected,'prerequest'),safe.ctx);
assert.ok(!safe.values.payloadRecuperacao.includes('CARD_SECRET_FIXTURE'));assert.ok(!safe.values.payloadRecuperacao.includes('BUYER_SECRET_FIXTURE'));
// Regressao: a consulta deve sair do Postman mesmo com tokens ausentes.
const viacoes=requests.find(x=>x.name.startsWith('03 '));
const semToken=context(viacoes,{baseUrl:'http://localhost:8088',prefix:'/v1/rodoviario',sistema:'QueroPassagem',appToken:''});
vm.runInNewContext(source(viacoes,'prerequest'),semToken.ctx);
assert.deepEqual(semToken.failures,[]);assert.ok(semToken.logs.some(x=>x.includes('401/403')));
const tokenEmOutroEscopo=context(viacoes,{...sample,appToken:undefined},{},200,{appToken:'SEGREDO_ESCOPO'});
vm.runInNewContext(source(viacoes,'prerequest'),tokenEmOutroEscopo.ctx);
assert.ok(!tokenEmOutroEscopo.logs.some(x=>x.includes('appToken vazio')));
assert.ok(!tokenEmOutroEscopo.logs.join().includes('SEGREDO_ESCOPO'));
// Sem token individual, a leitura pode retornar 401 do servidor.
const detalhe=requests.find(x=>x.name.startsWith('21 '));
const semIndividual=context(detalhe,{...sample,usuarioToken:''});
vm.runInNewContext(source(detalhe,'prerequest'),semIndividual.ctx);assert.deepEqual(semIndividual.failures,[]);
assert.ok(semIndividual.logs.some(x=>x.includes('X-Usuario-Token')));
// Falhas de configuracao continuam bloqueadas, com motivo no console e no teste.
for(const [item,vars] of [[viacoes,{...sample,baseUrl:''}],[buy,{...sample,appToken:''}],[buy,{...sample,codgVinculoOrigem:''}]]){
 const blocked=context(item,vars);let skipped=false;
 try{vm.runInNewContext(source(item,'prerequest'),blocked.ctx);}catch(e){if(e===SKIP)skipped=true;else throw e;}
 assert.ok(skipped);assert.equal(blocked.failures.length,1);
 assert.ok(blocked.logs.some(x=>x.includes('REQUISICAO NAO ENVIADA')));
}
// Uma implementacao de skipRequest que apenas retorna tambem nao continua a preparar o checkout.
const semInterrupcao=context(buy,{...sample,appToken:''});let skipCalled=false;
semInterrupcao.ctx.pm.execution.skipRequest=()=>{skipCalled=true;};
assert.throws(()=>vm.runInNewContext(source(buy,'prerequest'),semInterrupcao.ctx));
assert.ok(skipCalled);assert.equal(semInterrupcao.values.codgReservaRodoviario,sample.codgReservaRodoviario);
// As demais operacoes continuam aceitando booleano, espacos e caixa na habilitacao.
const emitir=requests.find(x=>x.name.startsWith('15 '));
for(const [flag,acao,sexo] of [[true,'emitir',0],[' true ',' emitir ','0'],['TRUE','EMITIR','1']]){
 const allowed=context(emitir,{...sample,permitirOperacoesComEfeito:flag,acaoPermitida:acao,sexoPassageiro:sexo});
 vm.runInNewContext(source(emitir,'prerequest'),allowed.ctx);assert.deepEqual(allowed.failures,[]);
}
// O aviso identifica somente o controle que esta errado.
for(const [flag,acao,campo,naoCampo] of [['false','emitir','permitirOperacoesComEfeito','acaoPermitida'],['true','','acaoPermitida','permitirOperacoesComEfeito']]){
 const blocked=context(emitir,{...sample,permitirOperacoesComEfeito:flag,acaoPermitida:acao});let skipped=false;
 try{vm.runInNewContext(source(emitir,'prerequest'),blocked.ctx);}catch(e){if(e===SKIP)skipped=true;else throw e;}
 assert.ok(skipped);const reason=blocked.logs.find(x=>x.includes('REQUISICAO NAO ENVIADA'));
 assert.ok(reason.includes(campo+' deve ser'));assert.ok(!reason.includes(naoCampo+' deve ser'));
 assert.ok(reason.includes('nenhuma chamada foi enviada ao backend'));
}
// A requisicao 14 envia sem habilitacao e nao altera a acao usada por outras operacoes.
assert.ok(!source(buy,'prerequest').includes('permitirOperacoesComEfeito'));
assert.ok(!source(buy,'prerequest').includes('acaoPermitida'));
for(const flags of [{permitirOperacoesComEfeito:'false',acaoPermitida:''},{permitirOperacoesComEfeito:undefined,acaoPermitida:undefined},{permitirOperacoesComEfeito:'false',acaoPermitida:'cancelarReserva',sexoPassageiro:0}]){
 const vars={...sample,...flags};const run=context(buy,vars);
 vm.runInNewContext(source(buy,'prerequest'),run.ctx);assert.deepEqual(run.failures,[]);
 assert.equal(JSON.parse(run.values.payloadRecuperacao).dadosManager.chaveIdempotencia,sample.chaveCompra);
 const post=context(buy,vars);vm.runInNewContext(source(buy,'test'),post.ctx);
 assert.equal(post.values.acaoPermitida,vars.acaoPermitida);
}
// A alternativa manual tambem envia a reserva sem depender dos controles.
const manualCollection=JSON.parse(fs.readFileSync('docs/postman/confapi-rodoviario-reserva-manual.postman_collection.json','utf8'));
assert.equal(manualCollection.item.length,1);assert.deepEqual(manualCollection.auth,collection.auth);
const manual=manualCollection.item[0];
assert.equal(manual.request.method,'POST');assert.equal(manual.request.url,'{{baseUrl}}{{prefix}}/reservar');
assert.equal(manual.request.body.raw,buy.request.body.raw);
assert.deepEqual(manual.request.header,buy.request.header);
const manualPre=source(manual,'prerequest');
new vm.Script(manualPre);new vm.Script(source(manual,'test'));
assert.ok(!manualPre.includes('permitirOperacoesComEfeito'));assert.ok(!manualPre.includes('acaoPermitida'));
assert.ok(!manualPre.includes('sendRequest'));
assert.equal(fs.readFileSync('docs/postman/reservar-manual.pre-request.js','utf8').replace(/\r\n/g,'\n'),manualPre+'\n');
let manualCases=0;
for(const flags of [{permitirOperacoesComEfeito:'false',acaoPermitida:''},{permitirOperacoesComEfeito:undefined,acaoPermitida:undefined},{permitirOperacoesComEfeito:'false',acaoPermitida:'cancelarReserva',sexoPassageiro:0}]){
 const run=context(manual,{...sample,...flags});
 vm.runInNewContext(manualPre,run.ctx);assert.deepEqual(run.failures,[]);
 const recover=JSON.parse(run.values.payloadRecuperacao);
 assert.equal(recover.dadosManager.chaveIdempotencia,sample.chaveCompra);assert.deepEqual(recover.emissao.pagamento,{});
 assert.equal(run.values.codgReservaRodoviario,undefined);manualCases++;
}
for(const key of ['appToken','usuarioToken','codgVinculoOrigem']){
 const blocked=context(manual,{...sample,[key]:''});let skipped=false;
 try{vm.runInNewContext(manualPre,blocked.ctx);}catch(e){if(e===SKIP)skipped=true;else throw e;}
 assert.ok(skipped);assert.ok(blocked.logs.some(x=>x.includes(key)));manualCases++;
}
const manualCard=structuredClone(manual);manualCard.request.body.raw=JSON.stringify(withCard);
const manualRun=context(manualCard,sample);vm.runInNewContext(source(manualCard,'prerequest'),manualRun.ctx);
assert.ok(!manualRun.values.payloadRecuperacao.includes('CARD_SECRET_FIXTURE'));
assert.ok(!manualRun.values.payloadRecuperacao.includes('BUYER_SECRET_FIXTURE'));manualCases++;
console.log(JSON.stringify({requests:requests.length,compiledScripts:requests.length*2,validJsonBodies:bodies,intentionallyMalformedBodies:1,guardedMutations:guarded,postResponseFixtures:posts,assertions,diagnosticRegressions:12,manualReservationCases:manualCases,realHttpCalls:0}));
