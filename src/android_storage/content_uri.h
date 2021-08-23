// Copyright 2013 Dolphin Emulator Project / 2014 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#pragma once

#ifdef ANDROID
#include <string>

namespace AndroidStorage {
class ContentURI {
    private:
        std::string provider;
        std::string root;
    public:
        /**
         * We will always get a content tree uri like this.
         * ex. content://com.android.externalstorage.documents/tree/primary%3Acitra-emu
         * If we get the provider and tree root, we can construct any content.
         * @param path root path for citra user directory.
         * @return bool
         */
        bool SetRoot(const std::string& path);

        /**
         * Get current citra use root path.
         * @return std::string
         */
        std::string GetRoot();

        /**
         * Parse C++ native file path form to content uri.
         * For example.
         * If root path is content://com.android.externalstorage.documents/tree/primary%3Acitra-emu.
         * The C++ native file path of log file will be:
         * content://com.android.externalstorage.documents/tree/primary%3Acitra-emulog/citra_log.txt.
         * And it will be converted into:
         * content://com.android.externalstorage.documents/tree/primary%3Acitra-emu/document/primary%3Acitra-emu%2Flog%2Fcitra_log.txt.
         * @param path C++ native file path
         * @return std::string
         */
        std::string Parse(const std::string& path);

        ContentURI() {}
    };
}
#endif