// Copyright 2013 Dolphin Emulator Project / 2014 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#pragma once

#ifdef ANDROID
#include <string>

namespace AndroidStorage {
std::string URIEncode(const std::string& value);
bool endsWith(std::string const& fullString, std::string const& ending);
std::string GetParentPath(const std::string& path);
std::string GetFilename(const std::string path);
} // namespace AndroidStorage
#endif
