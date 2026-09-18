#include <android/log.h>
#include <dlfcn.h>
#include <errno.h>
#include <link.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

#include "native_api.h"

extern "C" void pig_dart_return_true();
extern "C" void pig_dart_return_7();
extern "C" void pig_dart_return_12();
extern "C" void pig_dart_return_8();
extern "C" void pig_probe_back_process();
extern "C" void pig_probe_maml_animation();
extern "C" void pig_native_probe_hit(int id);
extern "C" void* g_pig_back_process_backup;
extern "C" void* g_pig_maml_animation_backup;

namespace {

constexpr char kLogTag[] = "PigLauncherNative";
constexpr char kLauncherProcess[] = "com.miui.home";
constexpr char kSpawnerPath[] = "/system_ext/bin/hyos_spawner";
constexpr char kRustLibrary[] = "libapp_launcher.so";
constexpr char kDartLibrary[] = "libapp.so";
constexpr size_t kMaxProtectedPages = 16;

constexpr uintptr_t kDartFolderBlurSupported = 0x8fab98;
constexpr uintptr_t kDartUseDefaultFolderIcon = 0x8fac18;
constexpr uintptr_t kDartBlurSupported = 0x8fad54;
constexpr uintptr_t kDartGrid4ItemsMaxCount = 0x182f4d4;
constexpr uintptr_t kDartGrid9ItemsMaxCount = 0x182f4dc;
constexpr uintptr_t kDartGrid9LargeIconNum = 0x182f530;

constexpr uintptr_t kRustPreStartupSupport = 0x6cd298;
constexpr uintptr_t kRustBackProcess = 0x700c68;
constexpr uintptr_t kRustMamlAnimation = 0x712ce8;

constexpr uint8_t kSigFolderBlurSupported[] = {
        0xfd, 0x79, 0xbf, 0xa9, 0xfd, 0x03, 0x0f, 0xaa,
        0xef, 0x41, 0x00, 0xd1, 0xe2, 0x03, 0x01, 0xaa};
constexpr uint8_t kSigUseDefaultFolderIcon[] = {
        0xfd, 0x79, 0xbf, 0xa9, 0xfd, 0x03, 0x0f, 0xaa,
        0x23, 0x30, 0x4f, 0xb8, 0x63, 0x80, 0x1c, 0x8b};
constexpr uint8_t kSigBlurSupported[] = {
        0xfd, 0x79, 0xbf, 0xa9, 0xfd, 0x03, 0x0f, 0xaa,
        0xef, 0x21, 0x00, 0xd1, 0x40, 0x3f, 0x40, 0xf9};
constexpr uint8_t kSigGrid4ItemsMaxCount[] = {
        0xe0, 0x00, 0x80, 0xd2, 0xc0, 0x03, 0x5f, 0xd6};
constexpr uint8_t kSigGrid9ItemsMaxCount[] = {
        0x80, 0x01, 0x80, 0xd2, 0xc0, 0x03, 0x5f, 0xd6};
constexpr uint8_t kSigGrid9LargeIconNum[] = {
        0x00, 0x01, 0x80, 0xd2, 0xc0, 0x03, 0x5f, 0xd6};
constexpr uint8_t kSigPreStartupSupport[] = {
        0x68, 0x42, 0x00, 0x90, 0x08, 0xe1, 0x0c, 0x91,
        0x08, 0xfd, 0xdf, 0x88, 0xc8, 0x00, 0x00, 0x35};
constexpr uint8_t kSigBackProcess[] = {
        0xff, 0x83, 0x04, 0xd1, 0xeb, 0x2b, 0x0a, 0x6d,
        0xe9, 0x23, 0x0b, 0x6d, 0xfd, 0x7b, 0x0c, 0xa9};
constexpr uint8_t kSigMamlAnimation[] = {
        0xff, 0x83, 0x04, 0xd1, 0xfd, 0x7b, 0x0d, 0xa9,
        0xfc, 0x73, 0x00, 0xf9, 0xf8, 0x5f, 0x0f, 0xa9};

HookFunType g_hook = nullptr;
void* g_folder_blur_backup = nullptr;
void* g_default_folder_backup = nullptr;
void* g_blur_backup = nullptr;
void* g_grid4_max_backup = nullptr;
void* g_grid9_max_backup = nullptr;
void* g_grid9_large_backup = nullptr;
void* g_prestartup_backup = nullptr;
using MadviseFn = int (*)(void*, size_t, int);
MadviseFn g_madvise_backup = nullptr;
volatile uint32_t g_dart_state = 0;
volatile uint32_t g_rust_state = 0;
volatile uint32_t g_guard_state = 0;
volatile uint32_t g_probe_counts[6]{};
uintptr_t g_protected_pages[kMaxProtectedPages]{};
volatile size_t g_protected_page_count = 0;

struct ImageSearch {
    const char* basename;
    uintptr_t base;
    size_t matches;
};

const char* BaseName(const char* path) {
    if (path == nullptr) return nullptr;
    const char* slash = strrchr(path, '/');
    return slash == nullptr ? path : slash + 1;
}

int FindImageCallback(dl_phdr_info* info, size_t, void* opaque) {
    auto* search = static_cast<ImageSearch*>(opaque);
    if (info == nullptr || info->dlpi_name == nullptr) return 0;
    const char* name = BaseName(info->dlpi_name);
    if (name == nullptr || strcmp(name, search->basename) != 0) return 0;
    ++search->matches;
    if (search->matches == 1) {
        search->base = static_cast<uintptr_t>(info->dlpi_addr);
    }
    return 0;
}

uintptr_t FindImageBase(const char* basename) {
    ImageSearch search{basename, 0, 0};
    dl_iterate_phdr(FindImageCallback, &search);
    return search.matches == 1 ? search.base : 0;
}

bool ReadSmallFile(const char* path, char* output, size_t capacity) {
    if (capacity < 2) return false;
    FILE* file = fopen(path, "re");
    if (file == nullptr) return false;
    const size_t length = fread(output, 1, capacity - 1, file);
    fclose(file);
    if (length == 0 || length >= capacity) return false;
    output[length] = '\0';
    return true;
}

bool IsLauncherHyosProcess() {
    char executable[128]{};
    const ssize_t length = readlink("/proc/self/exe", executable, sizeof(executable) - 1);
    if (length <= 0 || static_cast<size_t>(length) >= sizeof(executable)) return false;
    executable[length] = '\0';
    char process[64]{};
    return strcmp(executable, kSpawnerPath) == 0 &&
            ReadSmallFile("/proc/self/cmdline", process, sizeof(process)) &&
            strcmp(process, kLauncherProcess) == 0;
}

void Log(int priority, const char* message) {
    __android_log_write(priority, kLogTag, message);
}

bool Match(uintptr_t address, const uint8_t* signature, size_t size) {
    return address != 0 && signature != nullptr && memcmp(reinterpret_cast<const void*>(address), signature, size) == 0;
}

size_t PageSize() {
    static size_t value = 0;
    if (value == 0) {
        const long queried = sysconf(_SC_PAGESIZE);
        value = queried > 0 ? static_cast<size_t>(queried) : 4096;
    }
    return value;
}

uintptr_t PageStart(uintptr_t address) {
    return address & ~(static_cast<uintptr_t>(PageSize()) - 1);
}

void AddProtectedPage(uintptr_t address) {
    const uintptr_t page = PageStart(address);
    size_t count = __atomic_load_n(&g_protected_page_count, __ATOMIC_ACQUIRE);
    for (size_t i = 0; i < count; ++i) {
        if (g_protected_pages[i] == page) return;
    }
    if (count >= kMaxProtectedPages) return;
    g_protected_pages[count] = page;
    __atomic_store_n(&g_protected_page_count, count + 1, __ATOMIC_RELEASE);
}

int GuardedMadvise(void* address, size_t length, int advice) {
    MadviseFn original = __atomic_load_n(&g_madvise_backup, __ATOMIC_ACQUIRE);
    if (original == nullptr) {
        errno = ENOSYS;
        return -1;
    }
    if (advice != MADV_DONTNEED || length == 0) return original(address, length, advice);
    const uintptr_t begin = reinterpret_cast<uintptr_t>(address);
    if (begin % PageSize() != 0 || length > UINTPTR_MAX - begin) return original(address, length, advice);
    const uintptr_t end = begin + length;
    uintptr_t cursor = begin;
    bool skipped = false;
    while (cursor < end) {
        uintptr_t protected_page = end;
        const size_t count = __atomic_load_n(&g_protected_page_count, __ATOMIC_ACQUIRE);
        for (size_t i = 0; i < count; ++i) {
            const uintptr_t page = g_protected_pages[i];
            if (page >= cursor && page < end && page < protected_page) protected_page = page;
        }
        if (protected_page == end) break;
        if (protected_page > cursor && original(reinterpret_cast<void*>(cursor), protected_page - cursor, advice) != 0) return -1;
        cursor = protected_page + PageSize();
        if (cursor > end) cursor = end;
        skipped = true;
    }
    if (!skipped) return original(address, length, advice);
    return cursor < end ? original(reinterpret_cast<void*>(cursor), end - cursor, advice) : 0;
}

void InstallGuard() {
    uint32_t expected = 0;
    if (!__atomic_compare_exchange_n(&g_guard_state, &expected, 1, false, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) return;
    void* backup = nullptr;
    const int result = g_hook(reinterpret_cast<void*>(madvise), reinterpret_cast<void*>(GuardedMadvise), &backup);
    if (result == 0 && backup != nullptr) {
        __atomic_store_n(&g_madvise_backup, reinterpret_cast<MadviseFn>(backup), __ATOMIC_RELEASE);
        __atomic_store_n(&g_guard_state, 2, __ATOMIC_RELEASE);
        Log(ANDROID_LOG_INFO, "HYOS hook-page guard installed");
    } else {
        __atomic_store_n(&g_guard_state, 3, __ATOMIC_RELEASE);
        Log(ANDROID_LOG_WARN, "HYOS hook-page guard unavailable");
    }
}

bool InstallHook(uintptr_t target, const uint8_t* signature, size_t signature_size, void* replacement, void** backup) {
    if (!Match(target, signature, signature_size)) return false;
    AddProtectedPage(target);
    return g_hook(reinterpret_cast<void*>(target), replacement, backup) == 0 && *backup != nullptr;
}

extern "C" bool pig_force_prestartup_support() {
    uint32_t count = __atomic_add_fetch(&g_probe_counts[5], 1, __ATOMIC_RELAXED);
    if (count <= 3) Log(ANDROID_LOG_INFO, "feature5 pre-startup support forced on");
    return true;
}

void InstallDartHooks(uintptr_t base) {
    uint32_t expected = 0;
    if (!__atomic_compare_exchange_n(&g_dart_state, &expected, 1, false, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) return;
    bool ok = true;
    ok &= InstallHook(base + kDartFolderBlurSupported, kSigFolderBlurSupported, sizeof(kSigFolderBlurSupported), reinterpret_cast<void*>(pig_dart_return_true), &g_folder_blur_backup);
    ok &= InstallHook(base + kDartUseDefaultFolderIcon, kSigUseDefaultFolderIcon, sizeof(kSigUseDefaultFolderIcon), reinterpret_cast<void*>(pig_dart_return_true), &g_default_folder_backup);
    ok &= InstallHook(base + kDartBlurSupported, kSigBlurSupported, sizeof(kSigBlurSupported), reinterpret_cast<void*>(pig_dart_return_true), &g_blur_backup);
    ok &= InstallHook(base + kDartGrid4ItemsMaxCount, kSigGrid4ItemsMaxCount, sizeof(kSigGrid4ItemsMaxCount), reinterpret_cast<void*>(pig_dart_return_7), &g_grid4_max_backup);
    ok &= InstallHook(base + kDartGrid9ItemsMaxCount, kSigGrid9ItemsMaxCount, sizeof(kSigGrid9ItemsMaxCount), reinterpret_cast<void*>(pig_dart_return_12), &g_grid9_max_backup);
    ok &= InstallHook(base + kDartGrid9LargeIconNum, kSigGrid9LargeIconNum, sizeof(kSigGrid9LargeIconNum), reinterpret_cast<void*>(pig_dart_return_8), &g_grid9_large_backup);
    __atomic_store_n(&g_dart_state, ok ? 2u : 3u, __ATOMIC_RELEASE);
    Log(ok ? ANDROID_LOG_INFO : ANDROID_LOG_WARN, ok ? "Dart AOT hooks installed for features 1-3" : "Dart AOT signature mismatch; features 1-3 skipped");
}

void InstallRustHooks(uintptr_t base) {
    uint32_t expected = 0;
    if (!__atomic_compare_exchange_n(&g_rust_state, &expected, 1, false, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) return;
    bool ok = true;
    ok &= InstallHook(base + kRustPreStartupSupport, kSigPreStartupSupport, sizeof(kSigPreStartupSupport), reinterpret_cast<void*>(pig_force_prestartup_support), &g_prestartup_backup);
    ok &= InstallHook(base + kRustBackProcess, kSigBackProcess, sizeof(kSigBackProcess), reinterpret_cast<void*>(pig_probe_back_process), &g_pig_back_process_backup);
    ok &= InstallHook(base + kRustMamlAnimation, kSigMamlAnimation, sizeof(kSigMamlAnimation), reinterpret_cast<void*>(pig_probe_maml_animation), &g_pig_maml_animation_backup);
    __atomic_store_n(&g_rust_state, ok ? 2u : 3u, __ATOMIC_RELEASE);
    Log(ok ? ANDROID_LOG_INFO : ANDROID_LOG_WARN, ok ? "Rust hooks installed for features 4-5" : "Rust signature mismatch; features 4-5 skipped");
}

void TryInstallExisting() {
    const uintptr_t rust = FindImageBase(kRustLibrary);
    if (rust != 0) InstallRustHooks(rust);
    const uintptr_t dart = FindImageBase(kDartLibrary);
    if (dart != 0) InstallDartHooks(dart);
}

void OnLibraryLoaded(const char* name, void*) {
    if (name == nullptr || !IsLauncherHyosProcess()) return;
    const char* base_name = BaseName(name);
    if (base_name == nullptr) return;
    if (strcmp(base_name, kRustLibrary) == 0) {
        const uintptr_t base = FindImageBase(kRustLibrary);
        if (base != 0) InstallRustHooks(base);
    } else if (strcmp(base_name, kDartLibrary) == 0) {
        const uintptr_t base = FindImageBase(kDartLibrary);
        if (base != 0) InstallDartHooks(base);
    }
}

}

extern "C" {
void* g_pig_back_process_backup = nullptr;
void* g_pig_maml_animation_backup = nullptr;
}

extern "C" void pig_native_probe_hit(int id) {
    if (id < 0 || id >= 6) return;
    const uint32_t count = __atomic_add_fetch(&g_probe_counts[id], 1, __ATOMIC_RELAXED);
    if (count <= 4 || (count & (count - 1)) == 0) {
        if (id == 4) {
            Log(ANDROID_LOG_INFO, "feature4 native back process hit");
        } else if (id == 5) {
            Log(ANDROID_LOG_INFO, "feature5 MAML transition hit");
        }
    }
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries* entries) {
    if (entries == nullptr || entries->hook_func == nullptr || entries->unhook_func == nullptr || !IsLauncherHyosProcess()) return nullptr;
    g_hook = entries->hook_func;
    Log(ANDROID_LOG_INFO, "native entry initialized in MiuiHome HYOS child");
    InstallGuard();
    TryInstallExisting();
    return OnLibraryLoaded;
}
