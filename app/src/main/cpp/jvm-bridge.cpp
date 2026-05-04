#include <jni.h>
#include <android/log.h>
#include <string>
#include <cstring>
#include <dlfcn.h>

#define LOG_TAG "Forma2JVM"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef jint (*JNI_CreateJavaVM_t)(JavaVM **, void **, void *);

extern "C" JNIEXPORT void JNICALL
Java_com_forma2_app_service_JavaProcessService_startJvmAndRunJar(
        JNIEnv *env, jobject /* this */,
        jstring jarPath, jstring workingDir, jstring javaHome) {

    const char *jarPathStr = env->GetStringUTFChars(jarPath, nullptr);
    const char *workingDirStr = env->GetStringUTFChars(workingDir, nullptr);
    const char *javaHomeStr = env->GetStringUTFChars(javaHome, nullptr);

    // A libjvm.so já foi carregada via System.load() em Java, então podemos obtê-la.
    void *handle = dlopen("libjvm.so", RTLD_NOLOAD);
    if (!handle) {
        LOGE("libjvm.so não está carregada. Erro: %s", dlerror());
        return;
    }

    auto create_vm = reinterpret_cast<JNI_CreateJavaVM_t>(dlsym(handle, "JNI_CreateJavaVM"));
    if (!create_vm) {
        LOGE("Símbolo JNI_CreateJavaVM não encontrado");
        return;
    }

    JavaVM *jvm = nullptr;
    JNIEnv *jniEnv = nullptr;

    // Argumentos para a JVM
    JavaVMInitArgs vmArgs;
    JavaVMOption options[4];

    std::string classpathOpt = "-Djava.class.path=" + std::string(jarPathStr);
    std::string javaHomeOpt = "-Djava.home=" + std::string(javaHomeStr);
    std::string memoryOpt = "-Xmx256M";

    options[0].optionString = const_cast<char *>(classpathOpt.c_str());
    options[1].optionString = const_cast<char *>(memoryOpt.c_str());
    options[2].optionString = const_cast<char *>(javaHomeOpt.c_str());
    options[3].optionString = const_cast<char *>("-Djava.library.path=/system/lib64:/vendor/lib64");

    vmArgs.version = JNI_VERSION_1_6;
    vmArgs.nOptions = 4;
    vmArgs.options = options;
    vmArgs.ignoreUnrecognized = JNI_TRUE;

    jint res = create_vm(&jvm, (void **)&jniEnv, &vmArgs);
    if (res != JNI_OK) {
        LOGE("Falha ao criar JVM, código: %d", res);
        return;
    }

    LOGD("JVM criada com sucesso! Procurando Main-Class no MANIFEST...");

    // Lê o MANIFEST.MF do JAR para obter a classe principal
    jclass jarFileClass = jniEnv->FindClass("java/util/jar/JarFile");
    if (!jarFileClass) {
        LOGE("JarFile class not found");
        return;
    }
    jmethodID jarFileCtor = jniEnv->GetMethodID(jarFileClass, "<init>", "(Ljava/lang/String;)V");
    jobject jarFileObj = jniEnv->NewObject(jarFileClass, jarFileCtor, env->NewStringUTF(jarPathStr));

    jmethodID getManifest = jniEnv->GetMethodID(jarFileClass, "getManifest", "()Ljava/util/jar/Manifest;");
    jobject manifestObj = jniEnv->CallObjectMethod(jarFileObj, getManifest);

    jclass manifestClass = jniEnv->GetObjectClass(manifestObj);
    jmethodID getMainAttributes = jniEnv->GetMethodID(manifestClass, "getMainAttributes", "()Ljava/util/jar/Attributes;");
    jobject attribsObj = jniEnv->CallObjectMethod(manifestObj, getMainAttributes);

    jclass attribsClass = jniEnv->GetObjectClass(attribsObj);
    jmethodID getValue = jniEnv->GetMethodID(attribsClass, "getValue", "(Ljava/lang/String;)Ljava/lang/String;");
    jstring mainClassStr = (jstring) jniEnv->CallObjectMethod(attribsObj, getValue, env->NewStringUTF("Main-Class"));

    if (mainClassStr == nullptr) {
        LOGE("Main-Class não encontrada no MANIFEST.MF");
        return;
    }

    const char *mainClassChars = jniEnv->GetStringUTFChars(mainClassStr, nullptr);
    LOGD("Main-Class: %s", mainClassChars);

    // Invocar sun.launcher.LauncherHelper.checkAndLoadMain
    jclass launcherHelper = jniEnv->FindClass("sun/launcher/LauncherHelper");
    if (!launcherHelper) {
        LOGE("LauncherHelper não encontrada");
        return;
    }
    jmethodID checkAndLoadMain = jniEnv->GetStaticMethodID(launcherHelper, "checkAndLoadMain",
        "(ZZLjava/lang/String;)Ljava/lang/Object;");

    jniEnv->CallStaticObjectMethod(launcherHelper, checkAndLoadMain, JNI_TRUE, JNI_TRUE, mainClassStr);

    // Liberar recursos
    jniEnv->ReleaseStringUTFChars(mainClassStr, mainClassChars);
    env->ReleaseStringUTFChars(jarPath, jarPathStr);
    env->ReleaseStringUTFChars(workingDir, workingDirStr);
    env->ReleaseStringUTFChars(javaHome, javaHomeStr);

    LOGD("JVM encerrada.");
}