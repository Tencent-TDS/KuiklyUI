/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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

#ifndef CORE_RENDER_OHOS_APNGPARSER_H
#define CORE_RENDER_OHOS_APNGPARSER_H

#include <array>
#include <cstdint>
#include <cstring>
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>
#include "libohos_render/expand/components/apng/APNGStructs.h"

class DataView {
 public:
    explicit DataView(std::vector<uint8_t> &buffer) : buffer_(buffer) {}

    uint32_t getUint32(std::size_t offset) {
        if (offset + sizeof(uint32_t) > buffer_.size()) {
            throw std::out_of_range("Offset out of range");
        }

        uint32_t value = 0;
        for (std::size_t i = 0; i < sizeof(uint32_t); ++i) {
            value <<= 8;
            value |= buffer_[offset + i];
        }

        return value;
    }

    uint16_t getUint16(std::size_t offset) {
        if (offset + sizeof(uint16_t) > buffer_.size()) {
            throw std::out_of_range("Offset out of range");
        }

        uint16_t value = 0;
        for (std::size_t i = 0; i < sizeof(uint16_t); ++i) {
            value <<= 8;
            value |= buffer_[offset + i];
        }

        return value;
    }

    uint8_t getUint8(std::size_t offset) {
        if (offset >= buffer_.size()) {
            throw std::out_of_range("Offset out of range");
        }

        return buffer_[offset];
    }

    void setUint32(std::size_t offset, uint32_t value) {
        if (offset + sizeof(uint32_t) > buffer_.size()) {
            throw std::out_of_range("Offset out of range");
        }

        for (std::size_t i = 0; i < sizeof(uint32_t); ++i) {
            buffer_[offset + i] = static_cast<uint8_t>((value >> ((sizeof(uint32_t) - 1 - i) * 8)) & 0xFF);
        }
    }

 private:
    std::vector<uint8_t> &buffer_;
};

static std::array<uint32_t, 256> generateTable() {
    std::array<uint32_t, 256> table;

    for (uint32_t i = 0; i < 256; ++i) {
        uint32_t c = i;
        for (uint32_t k = 0; k < 8; ++k) {
            c = (c & 1) ? (0xEDB88320 ^ (c >> 1)) : (c >> 1);
        }
        table[i] = c;
    }

    return table;
}

static uint32_t crc32(const std::vector<uint8_t> &bytes, size_t start = 0, size_t length = std::string::npos) {
    static const auto table = generateTable();

    if (length == std::string::npos) {
        length = bytes.size() - start;
    }

    uint32_t crc = ~0u;
    for (size_t i = start, l = start + length; i < l; i++) {
        crc = (crc >> 8) ^ table[(crc ^ bytes[i]) & 0xFF];
    }
    return ~crc;
}

static std::string readString(const std::vector<uint8_t> &bytes, size_t off, size_t length) {
    std::string result;
    for (size_t i = 0; i < length; ++i) {
        result.push_back(static_cast<char>(bytes[off + i]));
    }
    return result;
}

static std::vector<uint8_t> makeStringArray(const std::string &str) {
    std::vector<uint8_t> result(str.begin(), str.end());
    return result;
}

static std::vector<uint8_t> subBuffer(const std::vector<uint8_t> &bytes, size_t start, size_t length) {
    std::vector<uint8_t> result(bytes.begin() + start, bytes.begin() + start + length);
    return result;
}
static std::vector<uint8_t> makeChunkBytes(const std::string &type, const std::vector<uint8_t> &dataBytes) {
    std::size_t crcLen = type.length() + dataBytes.size();
    std::vector<uint8_t> bytes(crcLen + 8);
    DataView dv(bytes);

    dv.setUint32(0, dataBytes.size());

    for (std::size_t i = 0; i < type.size(); ++i) {
        bytes[i + 4] = type[i];
    }

    std::copy(dataBytes.begin(), dataBytes.end(), bytes.begin() + 8);

    uint32_t crc = crc32(bytes, 4, crcLen);
    dv.setUint32(crcLen + 4, crc);

    return bytes;
}

static std::vector<uint8_t> makeDWordArray(uint32_t x) {
    return std::vector<uint8_t>{static_cast<uint8_t>((x >> 24) & 0xFF), static_cast<uint8_t>((x >> 16) & 0xFF),
                                static_cast<uint8_t>((x >> 8) & 0xFF), static_cast<uint8_t>(x & 0xFF)};
}

static void eachChunk(std::vector<uint8_t> &bytes,
                      std::function<bool(const std::string &, std::vector<uint8_t> &, size_t, size_t)> callback) {
    size_t off = 8;
    std::string type;
    size_t length = 0;
    bool res = false;
    auto dv = DataView(bytes);
    do {
        length = dv.getUint32(off);
        type = readString(bytes, off + 4, 4);
        res = callback(type, bytes, off, length);
        off += (12 + length);
    } while (res != false && type != "IEND" && off < bytes.size());
}

static void parseAPNG(std::vector<uint8_t> &buffer, std::function<void(std::shared_ptr<APNG>)> completion) {
    if (!completion) {
        return;
    }
    static std::array<uint8_t, 8> PNGSignature = {0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    std::vector<uint8_t> &bytes = buffer;
    std::shared_ptr<APNG> apng = std::make_shared<APNG>();

    if (bytes.size() < 8 || !std::equal(bytes.begin(), bytes.begin() + 8, PNGSignature.begin())) {
        apng->isAPNG = false;
        completion(apng);
        return;
    }

    bool isAnimated = false;
    eachChunk(bytes, [&isAnimated](const std::string &type, const std::vector<uint8_t> &, size_t, size_t) -> bool {
        return !(isAnimated = (type == "acTL"));
    });

    if (!isAnimated) {
        apng->isAPNG = false;
        completion(apng);
        return;
    }

    std::vector<std::vector<uint8_t>> preDataParts;
    std::vector<std::vector<uint8_t>> postDataParts;
    std::vector<uint8_t> headerDataBytes;
    std::shared_ptr<Frame> frame = std::make_shared<Frame>();
    int frameNumber = 0;

    eachChunk(bytes, [&](const std::string &type, std::vector<uint8_t> &bytes, size_t off, size_t length) -> bool {
        auto dv = DataView(bytes);
        if (type == "IHDR") {
            headerDataBytes = subBuffer(bytes, off + 8, length);
            apng->width = dv.getUint32(off + 8);
            apng->height = dv.getUint32(off + 12);
        } else if (type == "acTL") {
            apng->numPlays = dv.getUint32(off + 8 + 4);
        } else if (type == "fcTL") {
            {
                apng->frames.push_back(frame);
                frameNumber++;
            }

            frame = std::make_shared<Frame>();
            frame->width = dv.getUint32(off + 8 + 4);
            frame->height = dv.getUint32(off + 8 + 8);
            frame->left = dv.getUint32(off + 8 + 12);
            frame->top = dv.getUint32(off + 8 + 16);
            uint16_t delayN = dv.getUint16(off + 8 + 20);
            uint16_t delayD = dv.getUint16(off + 8 + 22);
            if (delayD == 0) {
                delayD = 100;
            }
            frame->delay = 1000 * delayN / delayD;
            if (frame->delay <= 10) {
                frame->delay = 16;  // 等于最低30帧
            }
            apng->playTime += frame->delay;
            frame->disposeOp = dv.getUint8(off + 8 + 24);
            frame->blendOp = dv.getUint8(off + 8 + 25);
            frame->dataParts.clear();
            if (frameNumber == 0 && frame->disposeOp == 2) {
                frame->disposeOp = 1;
            }
        } else if (type == "fdAT") {
            frame->dataParts.push_back(subBuffer(bytes, off + 8 + 4, static_cast<size_t>(length - 4)));
        } else if (type == "IDAT") {
            frame->dataParts.push_back(subBuffer(bytes, off + 8, length));
        } else if (type == "IEND") {
            postDataParts.push_back(subBuffer(bytes, off, 12 + length));
        } else {
            preDataParts.push_back(subBuffer(bytes, off, 12 + length));
        }
        return true;
    });

    apng->frames.push_back(frame);

    if (apng->frames.empty()) {
        apng->isAPNG = false;
        completion(apng);
        return;
    }
    apng->WillParseFrame();
    int index = 0;
    bool didAddFrameSuccess = false;
    for (auto frame : apng->frames) {
        std::vector<std::vector<uint8_t>> bb;
        // 预先分配内存
        size_t totalSize = preDataParts.size() + frame->dataParts.size() + postDataParts.size() + 2;
        bb.reserve(totalSize);

        bb.emplace_back(PNGSignature.begin(), PNGSignature.end());

        auto hdv = DataView(headerDataBytes);
        hdv.setUint32(0, frame->width);
        hdv.setUint32(4, frame->height);

        bb.emplace_back(makeChunkBytes("IHDR", headerDataBytes));
        bb.insert(bb.end(), preDataParts.begin(), preDataParts.end());
        for (const auto &p : frame->dataParts) {
            bb.emplace_back(makeChunkBytes("IDAT", p));
        }
        bb.insert(bb.end(), postDataParts.begin(), postDataParts.end());
        frame->SetImageBuffer(bb);
        if (apng->DidAddFrame(frame, index) && !didAddFrameSuccess) {
            didAddFrameSuccess = true;
            completion(apng);
        }
        frame->dataParts.clear();
        bb.clear();
        index++;
    }
    apng->DidEndParserFrame();

    if (apng->frames.empty()) {
        apng->isAPNG = false;
    }
    if (!didAddFrameSuccess) {
        completion(apng);
    }
}

#endif  // CORE_RENDER_OHOS_APNGPARSER_H
