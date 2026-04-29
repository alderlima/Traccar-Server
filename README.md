# Traccar Server Android App

Um aplicativo Android que funciona como servidor para executar o **Traccar Server** em background, com interface intuitiva, logs em tempo real e suporte a Foreground Service.

## 🎯 Características

- **Execução de Traccar em Background**: Roda o Traccar Server como um Foreground Service
- **Seleção de Pasta**: Interface para selecionar a pasta onde está o arquivo `traccar-server.jar`
- **Logs em Tempo Real**: Visualiza os logs do servidor em tempo real na UI
- **Material Design 3**: Interface moderna e responsiva
- **Notificações Persistentes**: Mantém o usuário informado sobre o status do servidor
- **Baixo Consumo de Bateria**: Otimizado para eficiência energética
- **Acesso à Interface Web**: Acesse o Traccar em `http://localhost:8082`

## 📋 Requisitos

### Para o Aplicativo:

- **Android 9 (API 28)** ou superior
- **Java 17** ou superior (para compilação)
- **Gradle 8.2** ou superior
- **Android SDK 35** (compileSdk)

### Para o Traccar Server:

- **Java 11** ou superior (no dispositivo Android)
- **Arquivo traccar-server.jar** (disponível em https://www.traccar.org/download/)
- **Espaço em disco**: Mínimo 100MB (recomendado 500MB+)
- **RAM**: Mínimo 256MB (recomendado 512MB+)

## 🚀 Como Usar

### 1. Preparar o Arquivo Traccar

1. Baixe o **traccar-server.jar** de https://www.traccar.org/download/
2. Copie o arquivo para uma pasta no seu dispositivo Android
3. Certifique-se de que o arquivo está acessível via Storage Access Framework (SAF)

### 2. Usar o Aplicativo

1. **Abra o aplicativo Traccar Server**
2. **Clique em "Selecionar Pasta"** e navegue até a pasta contendo o `traccar-server.jar`
3. **Clique em "Iniciar Servidor"** para começar
4. **Acesse a interface web** em `http://localhost:8082` (em outro dispositivo na mesma rede)
5. **Clique em "Parar Servidor"** para interromper

## 🛠️ Desenvolvimento

### Clonar o Repositório

```bash
git clone https://github.com/seu-usuario/traccar-server-android.git
cd traccar-server-android
```

### Build Local

```bash
# Build Debug
./gradlew assembleDebug

# Build Release
./gradlew assembleRelease
```

### Executar Testes

```bash
./gradlew test
```

## 📦 Estrutura do Projeto

```
traccar-server-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/jarserver/
│   │   │   ├── MainActivity.java
│   │   │   ├── LogViewModel.java
│   │   │   └── service/ServerService.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   └── xml/
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/
├── .github/workflows/
├── build.gradle
├── settings.gradle
└── README.md
```

## 🔧 Configuração

### Permissões Necessárias

O aplicativo requer as seguintes permissões (definidas em `AndroidManifest.xml`):

- `INTERNET` - Para acesso à rede (Traccar web interface)
- `MANAGE_EXTERNAL_STORAGE` - Para acessar arquivos `.jar`
- `FOREGROUND_SERVICE` - Para manter o serviço ativo
- `POST_NOTIFICATIONS` - Para enviar notificações

### Versões de Dependências

| Dependência | Versão |
|-------------|--------|
| Android SDK | 35 |
| Gradle | 8.2 |
| Android Gradle Plugin | 8.2.0 |
| Java | 17 |
| Material Design | 1.12.0 |
| AndroidX Core | 1.13.1 |
| AndroidX Lifecycle | 2.7.0 |

## 🚀 CI/CD - GitHub Actions

O projeto inclui um workflow automático do GitHub Actions que:

1. Faz checkout do código
2. Configura Java 17
3. Executa o build
4. Gera APK Debug e Release
5. Gera AAB (Android App Bundle)

Todos os artefatos estão disponíveis na aba `Actions` do repositório.

## 📝 Licença

Este projeto está licenciado sob a **MIT License** - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 🤝 Contribuindo

Contribuições são bem-vindas! Por favor, leia o arquivo [CONTRIBUTING.md](CONTRIBUTING.md) para detalhes sobre nosso código de conduta e processo de submissão de pull requests.

## 📞 Suporte

Para reportar bugs ou solicitar features, abra uma issue no repositório GitHub.

## 🔗 Links Úteis

- [Traccar Official Website](https://www.traccar.org/)
- [Traccar Documentation](https://www.traccar.org/documentation/)
- [Android Developer Documentation](https://developer.android.com/)
- [Material Design 3](https://m3.material.io/)

---

**Desenvolvido com ❤️ para Android**
