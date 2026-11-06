#ifndef BUILD_CONFIG_SIGNATURE_DATA_H
#define BUILD_CONFIG_SIGNATURE_DATA_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

// This only extracts optional signature metadata. It never validates a code signature.
static inline uint32_t signatureRead32(const uint8_t *p, bool bigEndian) {
    if (bigEndian) {
        return (uint32_t)p[0] << 24 | (uint32_t)p[1] << 16 | (uint32_t)p[2] << 8 | p[3];
    }
    return (uint32_t)p[3] << 24 | (uint32_t)p[2] << 16 | (uint32_t)p[1] << 8 | p[0];
}

static inline bool signatureCMSData(const uint8_t *data, size_t size, const uint8_t **cms, size_t *cmsSize) {
    if (size < 12 || signatureRead32(data, true) != 0xfade0cc0) {
        return false;
    }
    size_t length = signatureRead32(data + 4, true);
    size_t count = signatureRead32(data + 8, true);
    if (length < 12 || length > size || count > (length - 12) / 8) {
        return false;
    }
    for (size_t i = 0; i < count; i++) {
        size_t offset = signatureRead32(data + 16 + i * 8, true);
        if (offset < 12 + count * 8 || offset > length || length - offset < 8) {
            return false;
        }
        size_t blobLength = signatureRead32(data + offset + 4, true);
        if (blobLength < 8 || blobLength > length - offset) {
            return false;
        }
        if (signatureRead32(data + offset, true) == 0xfade0b01 && blobLength > 8) {
            *cms = data + offset + 8;
            *cmsSize = blobLength - 8;
            return true;
        }
    }
    return false;
}

static inline bool signatureMachOData(const uint8_t *data, size_t size, const uint8_t **cms, size_t *cmsSize) {
    if (size < 28) {
        return false;
    }
    uint32_t magic = signatureRead32(data, true);
    bool bigEndian = magic == 0xfeedface || magic == 0xfeedfacf;
    bool is64 = magic == 0xfeedfacf || magic == 0xcffaedfe;
    if (!bigEndian && magic != 0xcefaedfe && magic != 0xcffaedfe) {
        return false;
    }
    size_t offset = is64 ? 32 : 28;
    size_t count = signatureRead32(data + 16, bigEndian);
    size_t commandsSize = signatureRead32(data + 20, bigEndian);
    if (offset > size || commandsSize > size - offset || count > commandsSize / 8) {
        return false;
    }
    size_t commandsEnd = offset + commandsSize;
    for (size_t i = 0; i < count; i++) {
        if (commandsEnd - offset < 8) {
            return false;
        }
        size_t commandSize = signatureRead32(data + offset + 4, bigEndian);
        if (commandSize < 8 || commandSize > commandsEnd - offset) {
            return false;
        }
        if (signatureRead32(data + offset, bigEndian) == 0x1d) { // LC_CODE_SIGNATURE
            if (commandSize < 16) {
                return false;
            }
            size_t dataOffset = signatureRead32(data + offset + 8, bigEndian);
            size_t dataSize = signatureRead32(data + offset + 12, bigEndian);
            if (dataOffset < commandsEnd || dataOffset > size || dataSize > size - dataOffset) {
                return false;
            }
            return signatureCMSData(data + dataOffset, dataSize, cms, cmsSize);
        }
        offset += commandSize;
    }
    return false;
}

static inline bool signatureData(const uint8_t *data, size_t size, const uint8_t **cms, size_t *cmsSize) {
    *cms = NULL;
    *cmsSize = 0;
    if (data == NULL || size < 4) {
        return false;
    }
    uint32_t magic = signatureRead32(data, true);
    if (magic != 0xcafebabe && magic != 0xbebafeca) {
        return signatureMachOData(data, size, cms, cmsSize);
    }
    if (size < 8) {
        return false;
    }
    bool bigEndian = magic == 0xcafebabe;
    size_t count = signatureRead32(data + 4, bigEndian);
    if (count > (size - 8) / 20) {
        return false;
    }
    for (size_t i = 0; i < count; i++) {
        size_t entry = 8 + i * 20;
        size_t offset = signatureRead32(data + entry + 8, bigEndian);
        size_t length = signatureRead32(data + entry + 12, bigEndian);
        if (offset < 8 + count * 20 || offset > size || length > size - offset) {
            return false;
        }
        if (signatureMachOData(data + offset, length, cms, cmsSize)) {
            return true;
        }
    }
    return false;
}

#endif
