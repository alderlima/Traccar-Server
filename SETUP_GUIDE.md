# Guia de Configuração - Traccar Server Android

## 📋 Visão Geral

Este é um aplicativo Android moderno que permite executar o servidor Traccar (ou qualquer servidor JAR) em um dispositivo Android com um ambiente Java 17 embarcado, funcionando de forma similar ao Termux.

## ✨ Principais Melhorias Implementadas

### 1. **Remoção de Código Obsoleto**
- Removida a implementação Java/XML antiga (`com.jarserver`) que era incompleta
- Consolidado o projeto na implementação moderna em Kotlin/Jetpack Compose (`com.example.traccarserver`)

### 2. **Ambiente Java Embarcado (Termux-Style)**
- O aplicativo baixa e instala automaticamente o Java 17 (OpenJDK) aarch64
- Cria um ambiente isolado dentro do aplicativo, sem depender do Java do sistema
- Configura variáveis de ambiente essenciais (`PREFIX`, `HOME`, `JAVA_HOME`, `PATH`, `LD_LIBRARY_PATH`, `TMPDIR`)

### 3. **Interface Moderna com Jetpack Compose**
- UI responsiva e moderna com Material Design 3
- Telas de navegação: Principal, Logs do Terminal, Interface Web
- Status em tempo real do servidor e progresso de download

### 4. **Gerenciamento Robusto de Arquivos (SAF)**
- Suporte completo ao Storage Access Framework (SAF) do Android
- Permissões persistentes para acesso à pasta do Traccar
- Resolução automática de URIs para caminhos reais

### 5. **Logs Detalhados**
- Terminal em tempo real mostrando toda a saída do servidor
- Auto-scroll para o final dos logs
- Até 1000 linhas de histórico mantidas

## 🚀 Como Usar

### Pré-requisitos
- Android 9 (API 28) ou superior
- Mínimo 500 MB de espaço livre (para Java + Traccar)
- Arquivo JAR do Traccar (ex: `traccar-server.jar`)

### Passo 1: Instalar o Aplicativo
1. Compile o projeto com Android Studio
2. Instale o APK no seu dispositivo Android

### Passo 2: Baixar e Instalar o Java 17
1. Abra o aplicativo
2. Clique em "Baixar Java 17 (AArch64)"
3. Aguarde o download e extração (pode levar 5-10 minutos)
4. Você verá "Java 17: Instalado" quando terminar

### Passo 3: Selecionar a Pasta do Traccar
1. Clique em "Selecionar Pasta do Traccar"
2. Navegue até a pasta que contém o arquivo `traccar-server.jar`
3. O aplicativo detectará automaticamente o JAR

### Passo 4: Iniciar o Servidor
1. Clique em "Iniciar"
2. Veja os logs em tempo real na tela
3. Acesse o painel em `http://localhost:8082` (ou a porta configurada)

### Passo 5: Acessar a Interface Web
1. Clique em "Abrir Interface Web"
2. Ou abra seu navegador e acesse `http://127.0.0.1:8082`

## 🔧 Estrutura do Projeto

```
app/src/main/java/com/example/traccarserver/
├── MainActivity.kt                 # UI principal com Jetpack Compose
├── installer/
│   ├── EnvironmentManager.kt       # Gerencia ambiente Termux
│   ├── JavaDownloadWorker.kt       # Download e instalação do Java
│   └── AssetExtractor.kt           # Utilitário de extração
├── server/
│   └── ServerService.kt            # Serviço foreground que executa o JAR
└── ui/
    └── MainViewModel.kt            # ViewModel para gerenciar estado
```

## 📱 Dependências Principais

- **Jetpack Compose**: UI moderna e reativa
- **WorkManager**: Gerenciamento de download do Java em background
- **OkHttp**: Download de arquivos
- **Coroutines**: Operações assíncronas
- **Material Design 3**: Componentes de UI

## ⚙️ Configuração do Traccar

O aplicativo procura automaticamente por:
- `traccar-server.jar`
- `tracker-server.jar`
- `traccar.jar`

Se você tiver um arquivo `conf/traccar.xml` na mesma pasta, ele será usado automaticamente.

## 🛡️ Permissões Utilizadas

- `INTERNET`: Acesso à rede (para o servidor)
- `READ_EXTERNAL_STORAGE`: Leitura de arquivos
- `WRITE_EXTERNAL_STORAGE`: Escrita de arquivos
- `MANAGE_EXTERNAL_STORAGE`: Gerenciamento de armazenamento
- `FOREGROUND_SERVICE`: Serviço em background
- `WAKE_LOCK`: Manter o dispositivo ativo
- `POST_NOTIFICATIONS`: Notificações do sistema

## 🐛 Troubleshooting

### "Java não instalado"
- Clique em "Baixar Java 17" e aguarde a conclusão
- Verifique se há espaço livre suficiente

### "Arquivo .jar não encontrado"
- Certifique-se de que o arquivo JAR está na pasta selecionada
- Verifique se o nome do arquivo é um dos suportados

### Servidor não inicia
- Verifique os logs para mensagens de erro
- Certifique-se de que o arquivo de configuração `conf/traccar.xml` existe
- Verifique se a porta 8082 não está em uso

### Servidor para quando o app é fechado
- Isso é esperado. Para manter o servidor rodando, mantenha o app aberto ou use a notificação persistente

## 📊 Monitoramento

O aplicativo mostra em tempo real:
- Status do Java (Instalado/Pendente)
- Status do JAR Server (Encontrado/Não encontrado)
- Status do Servidor (EM EXECUÇÃO/PARADO)
- Logs completos do terminal

## 🔐 Segurança

- O Java é executado em um ambiente isolado
- Variáveis de ambiente são configuradas de forma segura
- O aplicativo solicita apenas as permissões necessárias

## 📝 Notas Importantes

1. O servidor roda em **background** mesmo quando o app é minimizado
2. Uma **notificação persistente** indica que o servidor está ativo
3. O **WakeLock** garante que o dispositivo não entre em suspensão
4. Os **logs** são mantidos em memória (até 1000 linhas)

## 🚀 Próximas Versões

Melhorias planejadas:
- Suporte a múltiplos servidores JAR
- Agendamento automático de inicialização
- Backup de configurações
- Interface de configuração do Traccar integrada

## 📞 Suporte

Para problemas ou sugestões, verifique os logs do aplicativo e a saída do terminal.

---

**Versão**: 1.0.0  
**Última atualização**: 2024
