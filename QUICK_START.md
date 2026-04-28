# 🚀 Quick Start - JAR Server Android App

## Clone e Execute em 3 Passos

### 1️⃣ Clone o Repositório

```bash
git clone https://github.com/seu-usuario/jar-server-android.git
cd jar-server-android
```

### 2️⃣ Configure o Ambiente (Primeira Vez)

```bash
# Linux/macOS
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=$HOME/Android/Sdk

# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:ANDROID_HOME = "$env:USERPROFILE\Android\Sdk"
```

> **Nota**: Se você ainda não tem JDK 21 ou Android SDK instalados, veja `SETUP.md`

### 3️⃣ Build e Execute

```bash
# Build Debug APK
./gradlew assembleDebug

# Ou build Release APK
./gradlew assembleRelease

# Ou build AAB (Android App Bundle)
./gradlew bundleRelease
```

Os artefatos serão gerados em:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

---

## 🤖 GitHub Actions - Build Automático

O projeto já vem configurado com CI/CD automático!

### Como Usar:

1. **Crie um repositório no GitHub**
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/seu-usuario/jar-server-android.git
   git push -u origin main
   ```

2. **A cada push, o GitHub Actions automaticamente:**
   - ✅ Faz checkout do código
   - ✅ Configura JDK 21
   - ✅ Executa build com Gradle
   - ✅ Gera APK Debug e Release
   - ✅ Gera AAB (Android App Bundle)
   - ✅ Faz upload dos artefatos para download

3. **Baixe os APKs gerados:**
   - Vá para `Actions` no seu repositório GitHub
   - Clique no workflow mais recente
   - Baixe os artefatos em `Artifacts`

---

## 📱 Instalar no Dispositivo

### Via ADB (Android Debug Bridge)

```bash
# Conecte o dispositivo via USB
# Ative o modo de desenvolvedor

adb install app/build/outputs/apk/debug/app-debug.apk
```

### Via Android Studio

1. Conecte o dispositivo
2. Clique em `Run` → `Run 'app'`
3. Selecione o dispositivo
4. Clique em `OK`

---

## 🧪 Verificar Build

Para testar se tudo está funcionando:

```bash
./gradlew clean build
```

Se bem-sucedido, você verá:
```
BUILD SUCCESSFUL in XXs
```

---

## ⚙️ Troubleshooting Rápido

| Problema | Solução |
|----------|---------|
| `JAVA_HOME is not set` | Defina a variável de ambiente (veja `SETUP.md`) |
| `compileSdk 35 not found` | Instale Android SDK Platform 35 |
| `Gradle build failed` | Execute `./gradlew clean` e tente novamente |
| `Permission denied` | Execute `chmod +x gradlew` no Linux/macOS |

---

## 📚 Documentação Completa

- **README.md** - Guia completo e features
- **SETUP.md** - Configuração detalhada do ambiente
- **CONTRIBUTING.md** - Diretrizes para contribuições

---

## 🎯 Próximos Passos

1. ✅ Clone o projeto
2. ✅ Configure o ambiente (SETUP.md)
3. ✅ Execute `./gradlew build`
4. ✅ Customize conforme necessário
5. ✅ Faça push para GitHub
6. ✅ Aproveite o CI/CD automático!

---

**Pronto para começar? Boa sorte! 🚀**
