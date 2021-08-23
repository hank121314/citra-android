// Copyright 2013 Dolphin Emulator Project / 2014 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#ifdef ANDROID
#include <vector>
#include "content_uri.h"
#include "path_utils.h"

namespace AndroidStorage {
    std::string ContentURI::GetRoot() {
        if (root.empty() || provider.empty()) {
            return "";
        }
        char builder[PATH_MAX];
        sprintf(builder, "content://%s/tree/%s", provider.c_str(), root.c_str());
        return std::string(builder);
    }

    bool ContentURI::SetRoot(const std::string& path) {
        const char* prefix = "content://";
        if (path.rfind(prefix, 0) == 0) {
            auto components = path.substr(strlen(prefix));
            std::vector<std::string> parts;
            size_t position = 0;
            char delimiter = '/';
            while ((position = components.find(delimiter)) != std::string::npos) {
                parts.emplace_back(components.substr(0, position));
                components.erase(0, position + 1);
            }
            parts.emplace_back(components);
            provider = parts[0];
            if (parts[1] == "tree") {
                root = parts[2];
            }
            return true;
        }
        return false;
    }

    std::string ContentURI::Parse(const std::string& path) {
        if (root.empty() || provider.empty()) {
            return path;
        }
        char builder[PATH_MAX];
        sprintf(builder, "content://%s/tree/%s", provider.c_str(), root.c_str());
        std::string directory(builder);
        // Check whether this path is from citra root directory. If not return it directly.
        if (path.rfind(directory, 0) == 0) {
            auto document_provider_path = "/" "document" "/" + root;
            auto appended = path;
            appended.erase(0, directory.length());
            // If append is empty means we are in citra root directory.
            if (appended.empty()) {
                return directory;
            }
            // If append is startsWith document provider path, the URI is already parsed.
            if (appended.rfind(document_provider_path, 0) == 0) return path;
            return directory + document_provider_path + URIEncode("/" + appended);
        }
        return path;
    }
}
#endif