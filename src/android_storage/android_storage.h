// Copyright 2013 Dolphin Emulator Project / 2014 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#pragma once

#ifdef ANDROID
#include <string>
#include <vector>
#include <jni.h>

#define ANDROID_STORAGE_FUNCTIONS(V)                                                               \
    V(CreateFile, bool, (const std::string& directory, const std::string& filename), create_file,  \
      "createFile", "(Ljava/lang/String;Ljava/lang/String;)Z")                                     \
    V(CreateDir, bool, (const std::string& directory, const std::string& filename), create_dir,    \
      "createDir", "(Ljava/lang/String;Ljava/lang/String;)Z")                                      \
    V(OpenContentUri, int, (const std::string& filepath, AndroidOpenMode openmode),                \
      open_content_uri, "openContentUri", "(Ljava/lang/String;Ljava/lang/String;)I")               \
    V(GetFilesName, std::vector<std::string>, (const std::string& filepath), get_files_name,       \
      "getFilesName", "(Ljava/lang/String;)[Ljava/lang/String;")                                   \
    V(CopyFile, bool,                                                                              \
      (const std::string& source, const std::string& destination_path,                             \
       const std::string& destination_filename),                                                   \
      copy_file, "copyFile", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z")          \
    V(RenameFile, bool, (const std::string& source, const std::string& filename), rename_file,     \
      "renameFile", "(Ljava/lang/String;Ljava/lang/String;)Z")

#define ANDROID_SINGLE_PATH_DETERMINE_FUNCTIONS(V)                                                 \
    V(IsDirectory, bool, is_directory, CallStaticBooleanMethod, "isDirectory",                     \
      "(Ljava/lang/String;)Z")                                                                     \
    V(FileExists, bool, file_exists, CallStaticBooleanMethod, "fileExists",                        \
      "(Ljava/lang/String;)Z")                                                                     \
    V(GetSize, std::uint64_t, get_size, CallStaticLongMethod, "getSize", "(Ljava/lang/String;)J")  \
    V(DeleteDocument, bool, delete_document, CallStaticBooleanMethod, "deleteDocument",            \
      "(Ljava/lang/String;)Z")

namespace AndroidStorage {
static JavaVM* g_jvm = nullptr;
static jclass native_library = nullptr;

#define FR(FunctionName, ReturnValue, JMethodID, Caller, JMethodName, Signature) F(JMethodID)
#define FS(FunctionName, ReturnValue, Parameters, JMethodID, JMethodName, Signature) F(JMethodID)
#define F(JMethodID) static jmethodID JMethodID = nullptr;
ANDROID_SINGLE_PATH_DETERMINE_FUNCTIONS(FR)
ANDROID_STORAGE_FUNCTIONS(FS)
#undef F
#undef FS
#undef FR

// Reference:
// https://developer.android.com/reference/android/os/ParcelFileDescriptor#parseMode(java.lang.String)
enum class AndroidOpenMode {
    READ = 0,                // "r"
    WRITE = 1,               // "w"
    READ_WRITE = 2,          // "rw"
    WRITE_APPEND = 3,        // "wa"
    WRITE_TRUNCATE = 4,      // "wt
    READ_WRITE_APPEND = 5,   // "rwa"
    READ_WRITE_TRUNCATE = 6, // "rwt"
    NEVER = 7,
};

AndroidOpenMode ParseOpenmode(const std::string& openmode);

void RegisterCallbacks(JNIEnv* env, jclass clazz);

void UnRegisterCallbacks();

#define FS(FunctionName, ReturnValue, Parameters, JMethodID, JMethodName, Signature)               \
    F(FunctionName, Parameters, ReturnValue)
#define F(FunctionName, Parameters, ReturnValue) ReturnValue FunctionName Parameters;
ANDROID_STORAGE_FUNCTIONS(FS)
#undef F
#undef FS

#define FR(FunctionName, ReturnValue, JMethodID, Caller, JMethodName, Signature)                   \
    F(FunctionName, ReturnValue)
#define F(FunctionName, ReturnValue) ReturnValue FunctionName(const std::string& filepath);
ANDROID_SINGLE_PATH_DETERMINE_FUNCTIONS(FR)
#undef F
#undef FR
#endif
}
