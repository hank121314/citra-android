// Copyright 2013 Dolphin Emulator Project / 2014 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#ifdef ANDROID
#include "path_utils.h"
#include <cctype>
#include <iomanip>
#include <sstream>

namespace AndroidStorage {
    std::string URIEncode(const std::string &value) {
        std::ostringstream escaped;
        escaped.fill('0');
        escaped << std::hex;

        for (std::string::const_iterator i = value.begin(), n = value.end(); i != n; ++i) {
            std::string::value_type c = (*i);

            // Keep alphanumeric and other accepted characters intact
            if (isalnum(c) || c == '-' || c == '_' || c == '.' || c == '~') {
                escaped << c;
                continue;
            }

            // Any other characters are percent-encoded
            escaped << std::uppercase;
            escaped << '%' << std::setw(2) << int((unsigned char) c);
            escaped << std::nouppercase;
        }

        return escaped.str();
    }

    bool endsWith(std::string const &fullString, std::string const &ending) {
        if (fullString.length() >= ending.length()) {
            return (0 == fullString.compare(fullString.length() - ending.length(), ending.length(),
                                            ending));
        } else {
            return false;
        }
    }

    std::string GetParentPath(const std::string &path) {
        std::string delimiter = "%2F";
        auto directory = path;
        size_t skip = 0;
        // If given string is endsWith "%2F" skip the last one.
        if (endsWith(directory, delimiter)) {
            skip = delimiter.length() + 1;
        }
        size_t last_position = directory.rfind("%2F", directory.length() - skip);
        // If we cannot find any "%2F" in path, means that we should return tree root uri.
        if (last_position == std::string::npos) {
            size_t tree_position = directory.find("tree");
            if (tree_position == std::string::npos) return directory;
            size_t primary_end = directory.find('/', tree_position + 1);
            if (primary_end == std::string::npos) return directory;
            return directory.substr(0, primary_end);
        }
        directory = directory.substr(0, last_position + delimiter.length());
        return directory;
    }

    std::string GetFilename(const std::string path) {
        std::string delimiter = "%2F";
        auto filename = path;
        // If path endsWith "%2F" consider it as a directory name.
        if (endsWith(filename, delimiter)) {
            filename = filename.substr(0, filename.length() - delimiter.length());
        }
        size_t last_position = filename.rfind(delimiter);
        if (last_position != std::string::npos) {
            auto remaining = last_position + delimiter.length();
            filename = filename.substr(remaining, filename.length() - remaining);
        }
        return filename;
    }
}
#endif