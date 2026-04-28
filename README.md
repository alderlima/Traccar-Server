# JAR Server Android App

Um aplicativo Android em Java que atua como servidor para executar arquivos `.jar` em background, com suporte a Foreground Service para garantir execução contínua e baixo consumo de bateria.

## 🎯 Características

- **Execução de JAR em Background**: Roda arquivos `.jar` como um Foreground Service
- **Seleção de Pasta**: Interface para selecionar a pasta onde estão os arquivos `.jar`
- **Logs em Tempo Real**: Visualiza os logs do servidor em tempo real na UI
- **Material Design 3**: Interface moderna e responsiva
- **Otimização de Bateria**: Implementação eficiente para minimizar consumo de energia
- **Build Automático**: GitHub Actions configurado para build automático do APK

## 📋 Requisitos

- **Android 9 (API 28)** ou superior
- **Java 21** (para compilação)
- **Gradle 8.4.0** ou superior
- **Android SDK 35** (compileSdk)

## 🚀 Como Usar

### 1. Clonar o Repositório

```bash
git clone https://github.com/seu-usuario/jar-server-android.git
cd jar-server-android
```

### 2. Build Local

#### No Android Studio

1. Abra o projeto no Android Studio
2. Aguarde a sincronização do Gradle
3. Clique em `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
4. O APK será gerado em `app/build/outputs/apk/debug/`

#### Via Terminal

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requer keystore configurado)
./gradlew assembleRelease

# Build AAB (Android App Bundle)
./gradlew bundleRelease
```

### 3. Instalar no Dispositivo

```bash
# Via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Ou via Android Studio: Run → Run 'app'
```

### 4. Usar o Aplicativo

1. **Selecionar Pasta**: Clique em "Selecionar Pasta" e escolha o diretório onde estão seus arquivos `.jar`
2. **Iniciar Servidor**: Clique em "Iniciar Servidor" para começar a executar o JAR
3. **Monitorar Logs**: Visualize os logs do servidor em tempo real
4. **Parar Servidor**: Clique em "Parar Servidor" para encerrar a execução

## 🔧 Estrutura do Projeto

```
jar-server-android/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/jarserver/
│   │       │   ├── MainActivity.java          # Activity principal
│   │       │   ├── LogViewModel.java          # ViewModel para logs
│   │       │   └── service/
│   │       │       └── ServerService.java     # Foreground Service
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml      # Layout principal
│   │       │   ├── values/
│   │       │   │   ├── strings.xml            # Strings da app
│   │       │   │   ├── colors.xml             # Paleta de cores
│   │       │   │   └── themes.xml             # Temas Material Design 3
│   │       │   └── drawable/
│   │       └── AndroidManifest.xml
│   ├── build.gradle                           # Configuração do módulo app
│   └── proguard-rules.pro
├── .github/
│   └── workflows/
│       └── android-ci.yml                     # GitHub Actions workflow
├── build.gradle                               # Configuração root
├── settings.gradle
├── gradle.properties
├── README.md
└── .gitignore
```

## 📱 Componentes Principais

### MainActivity.java
- Interface principal do aplicativo
- Gerencia seleção de pasta via Storage Access Framework (SAF)
- Controla inicialização e parada do servidor
- Exibe logs em tempo real

### ServerService.java
- Foreground Service que executa o arquivo `.jar`
- Mantém o servidor rodando mesmo quando o app é minimizado
- Captura e transmite logs do processo Java
- Implementa `START_STICKY` para reinicialização automática

### LogViewModel.java
- ViewModel para comunicação reativa entre Service e UI
- Utiliza LiveData para observação de mudanças
- Gerencia estado do servidor e logs

## 🔐 Permissões

O aplicativo requer as seguintes permissões:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## 🔄 GitHub Actions

O projeto inclui um workflow automático que:

1. **Faz checkout** do código
2. **Configura Java 21** (JDK)
3. **Executa Gradle build**
4. **Gera APK debug e release**
5. **Faz upload dos artefatos**

Cada push ou pull request acionará o workflow automaticamente.

### Arquivo: `.github/workflows/android-ci.yml`

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Make gradlew executable
        run: chmod +x ./gradlew
      - name: Build with Gradle
        run: ./gradlew build
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-debug.apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

## 📊 Dependências

- **AndroidX Core**: `androidx.appcompat:appcompat:1.7.0`
- **Material Design 3**: `com.google.android.material:material:1.12.0`
- **Lifecycle**: `androidx.lifecycle:lifecycle-viewmodel:2.8.0`
- **Storage Access Framework**: `androidx.documentfile:documentfile:1.0.1`
- **Coroutines**: `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0`

## ⚙️ Configuração Avançada

### Aumentar Memória do Gradle

Editar `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=1024m
```

### Configurar Keystore para Release

```bash
# Gerar keystore
keytool -genkey -v -keystore release.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias release

# Configurar em build.gradle
signingConfigs {
    release {
        storeFile file("release.keystore")
        storePassword "sua_senha"
        keyAlias "release"
        keyPassword "sua_senha"
    }
}
```

## 🐛 Troubleshooting

### "Arquivo .jar não encontrado"
- Verifique se o arquivo `.jar` está na pasta selecionada
- Confirme as permissões de acesso à pasta

### "Servidor não inicia"
- Verifique os logs do aplicativo
- Certifique-se de que a JVM está disponível no dispositivo
- Aumente a memória alocada no arquivo `.jar` se necessário

### "Permissão negada"
- Conceda as permissões necessárias nas configurações do dispositivo
- Para Android 6+, as permissões são solicitadas em runtime

## 📝 Licença

Este projeto está sob a licença MIT. Veja o arquivo LICENSE para detalhes.

## 🤝 Contribuindo

Contribuições são bem-vindas! Por favor:

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📞 Suporte

Para reportar bugs ou sugerir melhorias, abra uma issue no repositório.

---

**Desenvolvido em 2026** | Versão 1.0.0
