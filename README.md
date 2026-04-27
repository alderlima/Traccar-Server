# Traccar Android Local Server

Este projeto é um aplicativo Android completo capaz de rodar um servidor Traccar localmente, utilizando um JRE (Java Runtime Environment) 17 embutido.

## 🚀 Funcionalidades

*   **Java Nativo (DalvikVM):** O app utiliza o motor Java interno do Android para rodar o Traccar. Isso elimina o erro de "Permission Denied" e reduz o tamanho do APK para o mínimo possível (~2MB).
*   **Traccar Server:** Roda a versão completa do servidor Traccar.
*   **Banco de Dados H2:** Configurado para usar H2 localmente em `./data/database`.
*   **Foreground Service:** O servidor continua rodando em background com uma notificação persistente.
*   **Logs em Tempo Real:** Visualize a saída do console do Traccar diretamente no app.
*   **Painel Web Integrado:** Acesse a interface web do Traccar via WebView no endereço `http://127.0.0.1:8082`.

## 📁 Estrutura de Arquivos em Runtime

O app organiza os arquivos no diretório interno:
`/data/data/com.example.traccarserver/files/server/`
*   `java/`: JRE 17 para ARM64.
*   `traccar/`: Binários, configurações e dados do Traccar.

## 🛠️ Como Buildar

1.  **Opcional:** Coloque o `java17.tar.gz` (JRE ARM64) na pasta `app/src/main/assets/` se quiser embutir o Java. Se não colocar, o app oferecerá o download automático.
2.  Coloque o `traccar.zip` (arquivos do Traccar) na pasta `app/src/main/assets/`.
3.  Abra o projeto no Android Studio.
4.  Sincronize o Gradle e execute o build.

### Novo Sistema de Java Nativo
O aplicativo não precisa mais baixar um binário Java externo. Ele agora converte o ambiente de execução para usar o `dalvikvm` nativo do Android.
- **Vantagem:** Zero problemas de permissão.
- **Vantagem:** Performance otimizada para o hardware do celular.
- **Vantagem:** APK extremamente pequeno.

## ⚙️ Configuração do Banco de Dados

O arquivo `traccar.xml` incluído configura automaticamente o H2:
```xml
<entry key='database.driver'>org.h2.Driver</entry>
<entry key='database.url'>jdbc:h2:file:./data/database</entry>
<entry key='database.user'>sa</entry>
```

## 🔐 Permissões

O app utiliza as seguintes permissões:
*   `INTERNET`: Para acesso ao painel web local.
*   `FOREGROUND_SERVICE`: Para manter o servidor rodando.
*   `WAKE_LOCK`: Para evitar que a CPU entre em repouso profundo.
*   `POST_NOTIFICATIONS`: Para exibir a notificação do serviço (Android 13+).
