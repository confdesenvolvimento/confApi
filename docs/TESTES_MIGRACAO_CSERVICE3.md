# Testes locais sem tocar no banco de produção

Projeto: D:\Projetos\cServices3.0. JDK 21 e Maven. Com as dependências disponíveis, executar mvnw.cmd test ou mvn test. Os testes do cService usam apenas H2 em memória e MockMvc, inclusive a aplicação do SQL manual sobre uma fixture sintética do schema antigo. Não usam a configuração privada .local/application.properties.

O pacote está em target/cService3.jar. A aplicação inicia com geração e persistência desabilitadas; GET http://localhost:8087/actuator/health é apenas a verificação de saúde. As APIs exigem HTTPS e credencial técnica válida. Iniciar o serviço não cria schema e não prepara o Manager.

Para testar a integração real, primeiro preparar um banco separado de homologação e as credenciais/certificados de CONFIGURACAO.md. Não apontar o perfil local para o banco de produção usado pelo cServices2. O usuário não autorizou mudanças nesse banco.

Manager, confApi, Hub e Payara foram compilados com JDK 17. Seus testes selecionados usam mocks/H2; os testes ApplicationTests que carregam a configuração completa não foram executados. Logs e seletores exatos estão em .local/replacement. As suítes do App foram executadas com Node 24, não com o Node 18 incompatível informado anteriormente.

Para rodar o App em aparelho é necessária uma development build com o módulo nativo de chave P-256, SDK/NDK e ambiente Expo compatíveis. Expo Go não substitui essa build. Esta etapa validou TypeScript/Jest; não gerou APK/IPA. O SDK instalado contém somente android-33; faltam platform android-36 e Build Tools 36.0.0. A pasta NDK 27.1.12297006 não contém source.properties, indicando instalação incompleta. Depois de preparar o ambiente nativo, testar com EXPO_PUBLIC_CONFAPI_AUTH_V2_URL apontando ao HTTPS de homologação acessível pelo aparelho.
