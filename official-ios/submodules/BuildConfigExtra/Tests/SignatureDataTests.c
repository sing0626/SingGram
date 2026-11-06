#include "../Sources/SignatureData.h"
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void write32(uint8_t *p, uint32_t value, bool big) {
    for (int i = 0; i < 4; i++) p[big ? 3 - i : i] = (uint8_t)(value >> (8 * i));
}

static size_t fixture(uint8_t *p, bool big, bool is64) {
    memset(p, 0, 256);
    size_t header = is64 ? 32 : 28;
    write32(p, is64 ? 0xfeedfacf : 0xfeedface, big);
    write32(p + 16, 1, big);
    write32(p + 20, 16, big);
    write32(p + header, 0x1d, big);
    write32(p + header + 4, 16, big);
    write32(p + header + 8, (uint32_t)(header + 16), big);
    write32(p + header + 12, 32, big);
    uint8_t *signature = p + header + 16;
    write32(signature, 0xfade0cc0, true);
    write32(signature + 4, 32, true);
    write32(signature + 8, 1, true);
    write32(signature + 12, 0x10000, true);
    write32(signature + 16, 20, true);
    write32(signature + 20, 0xfade0b01, true);
    write32(signature + 24, 12, true);
    memcpy(signature + 28, "CMS!", 4);
    return header + 48;
}

int main(void) {
    uint8_t data[256], original[256];
    const uint8_t *cms;
    size_t cmsSize;
    assert(!signatureData(NULL, 0, &cms, &cmsSize));
    for (int big = 0; big < 2; big++) {
        for (int is64 = 0; is64 < 2; is64++) {
            size_t size = fixture(data, big, is64);
            assert(signatureData(data, size, &cms, &cmsSize));
            assert(cmsSize == 4 && memcmp(cms, "CMS!", 4) == 0);
            // Every truncated prefix is passed using exactly that allocation size,
            // so ASan detects a read beyond the supplied file, even inside the fixture.
            for (size_t n = 0; n < size; n++) {
                uint8_t *prefix = n ? (uint8_t *)malloc(n) : NULL;
                if (n) memcpy(prefix, data, n);
                assert(!signatureData(prefix, n, &cms, &cmsSize));
                free(prefix);
            }
            memcpy(original, data, sizeof(data));
            // Mutate every header/offset/length byte; rejection or a bounded CMS
            // slice are both valid outcomes, but a parser must never overread.
            for (size_t i = 0; i < size; i++) {
                for (int v = 0; v < 256; v++) {
                    memcpy(data, original, sizeof(data));
                    data[i] = (uint8_t)v;
                    if (signatureData(data, size, &cms, &cmsSize)) {
                        assert(cms >= data && cms <= data + size);
                        assert(cmsSize <= (size_t)(data + size - cms));
                    }
                }
            }
        }
    }
    uint8_t fat[512] = {0};
    size_t size = fixture(data, false, true);
    write32(fat, 0xcafebabe, true);
    write32(fat + 4, 1, true);
    write32(fat + 16, 28, true);
    write32(fat + 20, (uint32_t)size, true);
    memcpy(fat + 28, data, size);
    assert(signatureData(fat, size + 28, &cms, &cmsSize));
    write32(fat + 16, 0xffffffff, true);
    assert(!signatureData(fat, size + 28, &cms, &cmsSize));
    puts("SignatureData: valid 32/64-bit and fat signatures, truncation and mutation checks passed");
    return 0;
}
