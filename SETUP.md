# Guia de Configuração do Ambiente

Este documento fornece instruções detalhadas para configurar seu ambiente de desenvolvimento para o JAR Server Android App.

## 📋 Pré-requisitos

- **Sistema Operacional**: Windows, macOS ou Linux
- **Java Development Kit (JDK)**: Versão 21 ou superior
- **Android Studio**: Versão 2024.1 ou superior (recomendado)
- **Android SDK**: API 35 (compileSdk)
- **Gradle**: 8.9 ou superior (incluído no projeto)

## 🛠️ Instalação do JDK 21

### Windows

1. Baixe o JDK 21 de [Oracle](https://www.oracle.com/java/technologies/downloads/#java21) ou [OpenJDK](https://jdk.java.net/21/)
2. Execute o instalador
3. Defina a variável de ambiente `JAVA_HOME`:
   - Abra "Variáveis de Ambiente"
   - Clique em "Nova" e adicione:
     - Nome: `JAVA_HOME`
     - Valor: `C:\Program Files\Java\jdk-21` (ajuste conforme sua instalação)
4. Adicione `%JAVA_HOME%\bin` ao `PATH`

### macOS

```bash
# Usando Homebrew
brew install openjdk@21

# Ou defina JAVA_HOME manualmente
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install openjdk-21-jdk

# Verifique a instalação
java -version
```

## 📱 Instalação do Android Studio

1. Baixe do [site oficial](https://developer.android.com/studio)
2. Execute o instalador
3. Durante a instalação, selecione:
   - Android SDK 35
   - Android SDK Build-Tools 35
   - Android Emulator
   - Android SDK Platform-Tools

## 🔧 Configuração do Android SDK

### Via Android Studio

1. Abra Android Studio
2. Vá para `File` → `Settings` (ou `Android Studio` → `Preferences` no macOS)
3. Navegue até `Appearance & Behavior` → `System Settings` → `Android SDK`
4. Clique em `SDK Platforms` e instale:
   - Android 14 (API 34)
   - Android 15 (API 35)
5. Clique em `SDK Tools` e instale:
   - Android SDK Build-Tools 35
   - Android Emulator
   - Android SDK Platform-Tools

### Via Terminal

```bash
# Defina ANDROID_HOME
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Instale os componentes (requer Android Studio ou sdkmanager)
sdkmanager "platforms;android-35"
sdkmanager "build-tools;35.0.0"
```

## 📦 Clonar e Configurar o Projeto

### 1. Clone o Repositório

```bash
git clone https://github.com/seu-usuario/jar-server-android.git
cd jar-server-android
```

### 2. Configure as Variáveis de Ambiente

```bash
# Linux/macOS
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:ANDROID_HOME = "$env:USERPROFILE\Android\Sdk"
```

### 3. Sincronize o Gradle

```bash
# Linux/macOS
./gradlew --version

# Windows
gradlew.bat --version
```

## 🏗️ Build do Projeto

### Debug APK

```bash
# Linux/macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

O APK será gerado em: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK

```bash
# Linux/macOS
./gradlew assembleRelease

# Windows
gradlew.bat assembleRelease
```

## 📱 Instalação em Dispositivo

### Via ADB (Android Debug Bridge)

```bash
# Conecte o dispositivo via USB
# Ative o modo de desenvolvedor no dispositivo

# Instale o APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Ou para atualizar
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Via Android Studio

1. Conecte o dispositivo
2. Clique em `Run` → `Run 'app'`
3. Selecione o dispositivo
4. Clique em `OK`

## 🧪 Teste de Build

Para verificar se tudo está configurado corretamente:

```bash
# Linux/macOS
./gradlew clean build

# Windows
gradlew.bat clean build
```

Se o build for bem-sucedido, você verá:
```
BUILD SUCCESSFUL in XXs
```

## 🔍 Troubleshooting

### "JAVA_HOME is not set"

**Solução**: Defina a variável de ambiente `JAVA_HOME` corretamente:

```bash
# Verifique onde Java está instalado
which java
java -XshowSettings:properties -version

# Defina JAVA_HOME
export JAVA_HOME=/path/to/jdk-21
```

### "Android SDK not found"

**Solução**: Instale o Android SDK via Android Studio ou defina `ANDROID_HOME`:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
```

### "Gradle build failed"

**Solução**: Limpe o cache do Gradle:

```bash
# Linux/macOS
./gradlew clean

# Windows
gradlew.bat clean
```

### "compileSdk 35 not found"

**Solução**: Instale o Android SDK Platform 35:

```bash
sdkmanager "platforms;android-35"
```

## 📚 Recursos Adicionais

- [Documentação do Android Studio](https://developer.android.com/studio/intro)
- [Guia do Gradle](https://gradle.org/guides/)
- [Android Developer Documentation](https://developer.android.com/docs)
- [Java 21 Documentation](https://docs.oracle.com/en/java/javase/21/)

## ✅ Checklist de Configuração

- [ ] JDK 21 instalado e `JAVA_HOME` configurado
- [ ] Android Studio instalado
- [ ] Android SDK 35 instalado
- [ ] Android SDK Build-Tools 35 instalado
- [ ] Repositório clonado
- [ ] `./gradlew build` executado com sucesso
- [ ] APK gerado em `app/build/outputs/apk/debug/`
- [ ] Dispositivo conectado e modo desenvolvedor ativado
- [ ] APK instalado no dispositivo

---

Se encontrar problemas, consulte a seção de Troubleshooting ou abra uma issue no repositório.
