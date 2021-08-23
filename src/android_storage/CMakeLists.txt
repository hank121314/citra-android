if(ANDROID)
    add_library(android_storage STATIC
            android_storage.h
            android_storage.cpp
            content_uri.h
            content_uri.cpp
            path_utils.h
            path_utils.cpp)
endif()