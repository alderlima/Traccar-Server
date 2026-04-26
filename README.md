# Traccar Android Local Server

Este projeto é um aplicativo Android completo capaz de rodar um servidor Traccar localmente, utilizando um JRE (Java Runtime Environment) 17 embutido.

## 🚀 Funcionalidades

*   **Java 17 Embutido:** Não depende de Termux ou outras ferramentas externas.
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

1.  Coloque o `java17.tar.gz` (JRE ARM64) e o `traccar.zip` (arquivos do Traccar) na pasta `app/src/main/assets/`.
2.  Abra o projeto no Android Studio.
3.  Sincronize o Gradle e execute o build.

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
