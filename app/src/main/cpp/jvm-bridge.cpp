#include <jni.h>
#include <string>
#include <cstring>
#include <dlfcn.h>
#include "jni.h" // Inclui definições JNI

JavaVM *jvm = nullptr;
JNIEnv *env = nullptr;

typedef jint (JNICALL *JNI_CreateJavaVM)(JavaVM **p_vm, void **p_env, void *vm_args);

extern "C" JNIEXPORT void JNICALL
Java_com_forma2_app_service_JavaProcessService_startJvmAndRunJar(
        JNIEnv *env,
        jobject /* this */,
        jstring jarPath,
        jstring workingDir,
        jstring javaHome) {

    const char *jarPathStr = env->GetStringUTFChars(jarPath, nullptr);
    const char *workingDirStr = env->GetStringUTFChars(workingDir, nullptr);
    const char *javaHomeStr = env->GetStringUTFChars(javaHome, nullptr);

    // Carrega a função JNI_CreateJavaVM da libjvm (já carregada)
    void *handle = dlopen(nullptr, RTLD_NOLOAD);
    if (!handle) {
        // Tenta carregar explicitamente
        handle = dlopen("libjvm.so", RTLD_LAZY);
    }
    if (!handle) {
        LOGE("Falha ao carregar libjvm: %s", dlerror());
        return;
    }
    auto create_vm = reinterpret_cast<JNI_CreateJavaVM>(dlsym(handle, "JNI_CreateJavaVM"));
    if (!create_vm) {
        LOGE("Símbolo JNI_CreateJavaVM não encontrado");
        return;
    }

    // Configurar argumentos da VM
    JavaVMInitArgs vm_args;
    JavaVMOption options[4];
    char classpath[1024];
    snprintf(classpath, sizeof(classpath), "-Djava.class.path=%s", jarPathStr);
    options[0].optionString = classpath;
    options[1].optionString = "-Djava.library.path=/system/lib64:/vendor/lib64";
    options[2].optionString = "-Djava.home=" + std::string(javaHomeStr);
    options[3].optionString = "-Xmx256M"; // Limite de memória

    vm_args.version = JNI_VERSION_1_6;
    vm_args.nOptions = 4;
    vm_args.options = options;
    vm_args.ignoreUnrecognized = JNI_TRUE;

    // Criar a JVM no thread atual
    JNIEnv *jniEnv;
    jint res = create_vm(&jvm, (void **)&jniEnv, &vm_args);
    if (res != JNI_OK) {
        LOGE("Falha ao criar JVM, código: %d", res);
        return;
    }

    // Agora procurar a classe principal via manifest? Simplificamos: executa o jar como um todo.
    // Temos que invocar o método main da classe principal definida no MANIFEST.MF.
    // Para isso, precisamos ler o manifest do jar ou usar o URLClassLoader.
    // Alternativa: usar a classe org.eclipse.jdt.internal.jarinjarloader.JarRsrcLoader (se for Spring Boot) etc.
    // Vamos assumir um jar executável com Main-Class no MANIFEST.MF.
    // Usaremos java.util.jar.JarFile para ler o atributo.
    // Mas é mais complexo. Por simplicidade, chamaremos a classe principal fixa "com.example.Main"? Não.
    // Melhor: usar um launcher padrão que leia o MANIFEST e invoque.
    // Utilizaremos a função do Java: sun.launcher.LauncherHelper.
    
    // Buscar a classe sun.launcher.LauncherHelper
    jclass launcherHelper = jniEnv->FindClass("sun/launcher/LauncherHelper");
    if (!launcherHelper) {
        LOGE("LauncherHelper não encontrada");
        return;
    }
    jmethodID checkAndLoadMain = jniEnv->GetStaticMethodID(launcherHelper, "checkAndLoadMain",
        "(ZZLjava/lang/String;)Ljava/lang/Object;");
    // Parâmetros: true, true, nome da classe principal (extraído via manifest)
    // Precisamos extrair o nome da classe principal do MANIFEST.MF do jar
    jclass jarFileClass = jniEnv->FindClass("java/util/jar/JarFile");
    jmethodID jarInit = jniEnv->GetMethodID(jarFileClass, "<init>", "(Ljava/lang/String;)V");
    jobject jarFileObj = jniEnv->NewObject(jarFileClass, jarInit, env->NewStringUTF(jarPathStr));
    jmethodID getManifest = jniEnv->GetMethodID(jarFileClass, "getManifest", "()Ljava/util/jar/Manifest;");
    jobject manifestObj = jniEnv->CallObjectMethod(jarFileObj, getManifest);
    jmethodID getMainAttributes = jniEnv->GetMethodID(manifestObj->klass, "getMainAttributes", "()Ljava/util/jar/Attributes;");
    jobject mainAttribs = jniEnv->CallObjectMethod(manifestObj, getMainAttributes);
    jmethodID getValue = jniEnv->GetMethodID(mainAttribs->klass, "getValue", "(Ljava/lang/String;)Ljava/lang/String;");
    jstring mainClass = (jstring) jniEnv->CallObjectMethod(mainAttribs, getValue, env->NewStringUTF("Main-Class"));

    const char *mainClassStr = jniEnv->GetStringUTFChars(mainClass, nullptr);
    jstring mainClassName = env->NewStringUTF(mainClassStr);

    jniEnv->CallStaticObjectMethod(launcherHelper, checkAndLoadMain, JNI_TRUE, JNI_TRUE, mainClassName);

    env->ReleaseStringUTFChars(jarPath, jarPathStr);
    env->ReleaseStringUTFChars(workingDir, workingDirStr);
    env->ReleaseStringUTFChars(javaHome, javaHomeStr);
}