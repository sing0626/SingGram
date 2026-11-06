#import <BuildConfigExtra/BuildConfigExtra.h>

#include "SignatureData.h"
#include <limits.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>

#import <CommonCrypto/CommonCrypto.h>
#import <CommonCrypto/CommonDigest.h>

#import <PKCS/PKCS.h>

static NSData *sha1(NSData *data) {
    uint8_t digest[20];
    CC_SHA1(data.bytes, (CC_LONG)data.length, digest);
    
    return [[NSData alloc] initWithBytes:digest length:20];
}

static MTPKCS * _Nullable checkSignature(const char *filename) {
    char *buffer = NULL;
    
    int fd = open(filename, O_RDONLY);
    
    if (fd == -1) {
        return nil;
    }
    
    struct stat st;
    if (fstat(fd, &st) != 0 || st.st_size <= 0 || (uintmax_t)st.st_size > SIZE_MAX) {
        close(fd);
        return nil;
    }
    
    buffer = mmap(NULL, (size_t)st.st_size, PROT_READ, MAP_FILE|MAP_PRIVATE, fd, 0);
    
    if (buffer == MAP_FAILED) {
        if (fd != -1) {
            close(fd);
        }
        return nil;
    }
    
    const uint8_t *cms = NULL;
    size_t cmsSize = 0;
    MTPKCS *result = nil;
    if (signatureData((const uint8_t *)buffer, (size_t)st.st_size, &cms, &cmsSize) && cmsSize <= INT_MAX) {
        result = [MTPKCS parse:cms size:(int)cmsSize];
    }
    if (buffer) {
        munmap(buffer, (size_t)st.st_size);
    }
    if (fd != -1) {
        close(fd);
    }
    
    return result;
}

@implementation BuildConfigExtra

+ (NSDictionary * _Nonnull)signatureDict {
    NSMutableDictionary *dataDict = [[NSMutableDictionary alloc] init];
    MTPKCS *signature = checkSignature([[[NSBundle mainBundle] executablePath] UTF8String]);
    if (signature.issuerName != nil) {
        dataDict[@"issuerName"] = signature.issuerName;
    }
    if (signature.subjectName != nil) {
        dataDict[@"name"] = signature.subjectName;
    }
    if (signature.data != nil) {
        dataDict[@"data"] = [sha1(signature.data) base64EncodedStringWithOptions:0];
        dataDict[@"data1"] = [signature.data base64EncodedStringWithOptions:0];
    }
    return dataDict;
}

@end
