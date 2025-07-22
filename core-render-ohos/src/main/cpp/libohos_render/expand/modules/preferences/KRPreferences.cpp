/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "KRPreferences.h"

#include <fcntl.h>
#include <fstream>
#include <thread>
#include "thirdparty/tinyXml/tinyxml2.h"

namespace kuikly {
namespace util {

std::unordered_map<std::string, std::string> DataPreferences::LoadFileToMap(const std::string &preferencesFullPath) {
    std::unordered_map<std::string, std::string> krMap;
    tinyxml2::XMLDocument doc;
    FILE *fp = fopen(preferencesFullPath.c_str(), "rb");
    if (fp == nullptr) {
        // KLOG_ERROR(TAG) << "Error opening file: " << preferencesFullPath;
        return krMap;
    }
    // 文件加锁
    int fd = fileno(fp);
    struct flock fileLock;
    fileLock.l_type = F_WRLCK;
    fileLock.l_whence = SEEK_SET;
    fileLock.l_start = 0;
    fileLock.l_len = 0;
    fcntl(fd, F_SETLKW, &fileLock);  // F_SETLKW:阻塞模式

    if (doc.LoadFile(fp) == tinyxml2::XML_SUCCESS) {
        // if (doc.LoadFile(fileName.data()) == tinyxml2::XML_SUCCESS) {
        tinyxml2::XMLElement *root = doc.FirstChildElement("preferences");
        if (root) {
            for (tinyxml2::XMLElement *element = root->FirstChildElement("string"); element;
                 element = element->NextSiblingElement("string")) {
                const char *key = element->Attribute("key");
                const char *value = element->GetText();
                if (key && value) {
                    krMap[key] = value;
                }
            }
        } else {
            // KLOG_ERROR(TAG) << "root = preferences don't exist'";
        }
    } else {
        // KLOG_ERROR(TAG) << "tinyxml2 loadFile error'";
    }
    // 文件解锁
    fileLock.l_type = F_UNLCK;
    fcntl(fd, F_SETLKW, &fileLock);
    fclose(fp);
    return krMap;
}
void DataPreferences::CreatePreferencesDirectoryIfNeeded(const std::string &filesDir) {
    if (std::filesystem::exists(filesDir) && std::filesystem::is_directory(filesDir)) {
        // //KLOG_INFO(TAG) << "preferences folder exists in the current directory.";
    } else {
        // KLOG_INFO(TAG) << "The preferences folder does not exist.";
        if (std::filesystem::create_directory(filesDir)) {
            // KLOG_INFO(TAG) << "Successfully created preferences path folder";
        } else {
            // KLOG_ERROR(TAG) << "Failed to create preferences path folder";
        }
    }
}
DataPreferences::DataPreferences(const std::string &filesDir, const std::string &filesName) {
    // this->CreatePreferencesDirectoryIfNeeded(filesDir);     js context传入的路径
    std::filesystem::path preferencesPath = filesDir;
    std::filesystem::path preferencesName = filesName;
    std::filesystem::path fullPath = preferencesPath / preferencesName;
    this->preferencesFullPath_ = fullPath;
    try {
        keyValueMap_ = this->LoadFileToMap(this->preferencesFullPath_);
    } catch (const std::exception &e) {
        // KLOG_ERROR(TAG) << "Failed to load keyValueMap_ via file";
    }
}

std::shared_ptr<util::DataPreferences> DataPreferences::GetInstance(const std::string &filesDir, const std::string &filesName) {
    static std::once_flag flag;
    static std::shared_ptr<util::DataPreferences> preference;
    std::call_once(flag, [&]() { preference = std::make_shared<util::DataPreferences>(filesDir, filesName); });
    return preference;
}

void DataPreferences::SetSync(const std::string &key, const std::string &value) {
    std::unique_lock<std::mutex> lock(this->mtx_);
    this->keyValueMap_[key] = value;
    lock.unlock();
}

std::string DataPreferences::GetSync(const std::string &key, const std::string &defaultValue) {
    std::unique_lock<std::mutex> lock(this->mtx_);
    auto it = this->keyValueMap_.find(key);
    auto value = it != this->keyValueMap_.end() ? it->second : defaultValue;
    lock.unlock();
    return value;
}

void DataPreferences::Flush() {
    std::thread([this]() { this->FlushSync(); }).detach();
}

void DataPreferences::FlushSync() {
    std::filesystem::path preferencesFullPath = this->preferencesFullPath_;
    tinyxml2::XMLDocument doc;
    tinyxml2::XMLDeclaration *decl = doc.NewDeclaration();
    doc.InsertFirstChild(decl);

    tinyxml2::XMLElement *root = doc.NewElement("preferences");
    root->SetAttribute("version", "1.0");
    doc.InsertEndChild(root);
    std::unique_lock<std::mutex> lock(this->mtx_);
    auto copyMap = this->keyValueMap_;
    lock.unlock();
    for (const auto &pair : copyMap) {
        tinyxml2::XMLElement *element = doc.NewElement("string");
        element->SetAttribute("key", pair.first.c_str());
        element->SetText(pair.second.c_str());
        root->InsertEndChild(element);
    }
    FILE *fp = fopen(preferencesFullPath.c_str(), "w");
    if (fp == nullptr) {
        // KLOG_ERROR(TAG) << "Error opening file: " << preferencesFullPath;
        return;
    }
    // 文件加锁
    int fd = fileno(fp);
    struct flock fileLock;
    fileLock.l_type = F_WRLCK;
    fileLock.l_whence = SEEK_SET;
    fileLock.l_start = 0;
    fileLock.l_len = 0;
    fcntl(fd, F_SETLKW, &fileLock);  // F_SETLKW:阻塞模式

    if (doc.SaveFile(fp) == tinyxml2::XML_SUCCESS) {
        // KLOG_INFO(TAG) << "Preferences Flush success";
    } else {
        // KLOG_ERROR(TAG) << "Preferences Flush: tinyxml2 save error";
    }

    // 文件解锁
    fileLock.l_type = F_UNLCK;
    fcntl(fd, F_SETLKW, &fileLock);
    fclose(fp);
}

}  //  namespace util
}  //  namespace kuikly
