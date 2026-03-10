#include <jni.h>
#include <android/log.h>

#include <atomic>
#include <chrono>
#include <memory>
#include <mutex>
#include <sstream>
#include <string>
#include <thread>
#include <unordered_map>
#include <vector>

#ifndef USE_LLAMA_STUB
#include <llama.h>
#endif

#define LOG_TAG "llama_jni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

struct Engine {
#ifndef USE_LLAMA_STUB
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
#endif
    std::mutex generation_mutex;
    std::mutex cancel_mutex;
    std::unordered_map<std::string, std::shared_ptr<std::atomic_bool>> cancel_flags;
};

std::string to_string(JNIEnv* env, jstring s) {
    if (s == nullptr) return "";
    const char* c = env->GetStringUTFChars(s, nullptr);
    if (c == nullptr) return "";
    std::string out(c);
    env->ReleaseStringUTFChars(s, c);
    return out;
}

std::vector<std::string> to_vector(JNIEnv* env, jobjectArray arr) {
    std::vector<std::string> out;
    if (!arr) return out;
    const auto n = env->GetArrayLength(arr);
    out.reserve(static_cast<size_t>(n));
    for (jsize i = 0; i < n; ++i) {
        auto* item = static_cast<jstring>(env->GetObjectArrayElement(arr, i));
        out.push_back(to_string(env, item));
        env->DeleteLocalRef(item);
    }
    return out;
}

bool has_stop(const std::string& text, const std::vector<std::string>& stops) {
    for (const auto& s : stops) {
        if (!s.empty() && text.find(s) != std::string::npos) return true;
    }
    return false;
}

int emit_token(JNIEnv* env, jobject listener, jmethodID onToken, const std::string& token) {
    jstring jt = env->NewStringUTF(token.c_str());
    env->CallVoidMethod(listener, onToken, jt);
    env->DeleteLocalRef(jt);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return -1;
    }
    return 0;
}

void emit_error(JNIEnv* env, jobject listener, jmethodID onError, const std::string& msg) {
    jstring jm = env->NewStringUTF(msg.c_str());
    env->CallVoidMethod(listener, onError, jm);
    env->DeleteLocalRef(jm);
    if (env->ExceptionCheck()) env->ExceptionClear();
}

int template_stream(
    JNIEnv* env,
    jobject listener,
    jmethodID onToken,
    const std::string& prompt,
    int maxTokens,
    const std::vector<std::string>& stops,
    const std::shared_ptr<std::atomic_bool>& cancelled
) {
    std::ostringstream oss;
#ifdef USE_LLAMA_STUB
    oss << "LLM native stub active. Prompt: " << prompt
        << ". Add third_party/llama.cpp to enable real inference.";
#else
    oss << "Template output for prompt: " << prompt
        << ". Replace template_stream with llama_decode loop.";
#endif

    std::istringstream input(oss.str());
    std::string word;
    std::string generated;
    int n = 0;

    while (input >> word) {
        if (cancelled->load()) return 1;
        if (n >= maxTokens) break;

        std::string token = word + " ";
        generated += token;

        if (emit_token(env, listener, onToken, token) != 0) return -1;
        if (has_stop(generated, stops)) break;

        ++n;
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    return 0;
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_r2h_magican_ai_runtime_NativeLlamaBridge_nativeCreate(
    JNIEnv* env,
    jobject,
    jstring modelPath,
    jint nCtx,
    jint nThreads,
    jint seed
) {
    (void) nCtx;
    (void) nThreads;
    (void) seed;
    (void) modelPath;

    auto* engine = new Engine();

#ifndef USE_LLAMA_STUB
    llama_backend_init();
    std::string path = to_string(env, modelPath);

    llama_model_params modelParams = llama_model_default_params();
    engine->model = llama_model_load_from_file(path.c_str(), modelParams);
    if (!engine->model) {
        LOGE("llama_model_load_from_file failed: %s", path.c_str());
        delete engine;
        return 0L;
    }

    llama_context_params ctxParams = llama_context_default_params();
    ctxParams.n_ctx = static_cast<uint32_t>(nCtx);
    ctxParams.n_threads = static_cast<uint32_t>(nThreads);

    engine->ctx = llama_init_from_model(engine->model, ctxParams);
    if (!engine->ctx) {
        LOGE("llama_init_from_model failed");
        llama_model_free(engine->model);
        delete engine;
        return 0L;
    }
#endif

    return reinterpret_cast<jlong>(engine);
}

extern "C" JNIEXPORT void JNICALL
Java_com_r2h_magican_ai_runtime_NativeLlamaBridge_nativeDestroy(
    JNIEnv*,
    jobject,
    jlong handle
) {
    auto* engine = reinterpret_cast<Engine*>(handle);
    if (!engine) return;

    {
        std::lock_guard<std::mutex> lock(engine->cancel_mutex);
        for (auto& kv : engine->cancel_flags) kv.second->store(true);
        engine->cancel_flags.clear();
    }

#ifndef USE_LLAMA_STUB
    if (engine->ctx) llama_free(engine->ctx);
    if (engine->model) llama_model_free(engine->model);
#endif
    delete engine;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_r2h_magican_ai_runtime_NativeLlamaBridge_nativeRuntimeMode(
    JNIEnv*,
    jobject
) {
#ifdef USE_LLAMA_STUB
    return 1;
#else
    return 0;
#endif
}

extern "C" JNIEXPORT void JNICALL
Java_com_r2h_magican_ai_runtime_NativeLlamaBridge_nativeCancel(
    JNIEnv* env,
    jobject,
    jlong handle,
    jstring requestId
) {
    auto* engine = reinterpret_cast<Engine*>(handle);
    if (!engine) return;

    const std::string id = to_string(env, requestId);
    std::lock_guard<std::mutex> lock(engine->cancel_mutex);
    auto it = engine->cancel_flags.find(id);
    if (it != engine->cancel_flags.end()) it->second->store(true);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_r2h_magican_ai_runtime_NativeLlamaBridge_nativeGenerate(
    JNIEnv* env,
    jobject,
    jlong handle,
    jstring requestId,
    jstring prompt,
    jint maxTokens,
    jfloat,
    jfloat,
    jobjectArray stopTokens,
    jobject listener
) {
    auto* engine = reinterpret_cast<Engine*>(handle);
    if (!engine) return -1;

#ifndef USE_LLAMA_STUB
    if (!engine->ctx || !engine->model) return -1;
#endif

    jclass cls = env->GetObjectClass(listener);
    jmethodID onToken = env->GetMethodID(cls, "onToken", "(Ljava/lang/String;)V");
    jmethodID onError = env->GetMethodID(cls, "onError", "(Ljava/lang/String;)V");
    env->DeleteLocalRef(cls);
    if (!onToken || !onError) return -1;

    const std::string id = to_string(env, requestId);
    const std::string text = to_string(env, prompt);
    const auto stops = to_vector(env, stopTokens);

    auto cancelled = std::make_shared<std::atomic_bool>(false);
    {
        std::lock_guard<std::mutex> lock(engine->cancel_mutex);
        engine->cancel_flags[id] = cancelled;
    }

    int code = 0;
    {
        std::lock_guard<std::mutex> lock(engine->generation_mutex);
        code = template_stream(env, listener, onToken, text, static_cast<int>(maxTokens), stops, cancelled);
    }

    {
        std::lock_guard<std::mutex> lock(engine->cancel_mutex);
        engine->cancel_flags.erase(id);
    }

    if (code < 0) emit_error(env, listener, onError, "native generation failed");
    return code;
}
