#import <PKCS/PKCS.h>

#include <openssl/x509.h>
#include <openssl/pkcs7.h>
#include <openssl/pem.h>
#include <openssl/bytestring.h>

static NSString * _Nullable readName(X509_NAME *subject) {
    BIO *subjectBio = BIO_new(BIO_s_mem());
    if (subjectBio == NULL) {
        return nil;
    }
    X509_NAME_print_ex(subjectBio, subject, 0, XN_FLAG_RFC2253);
    char *dataStart = NULL;
    long nameLength = BIO_get_mem_data(subjectBio, &dataStart);
    NSString *result = [[NSString alloc] initWithBytes:dataStart length:nameLength encoding:NSUTF8StringEncoding];
    BIO_free(subjectBio);
    return result;
}

static NSData * _Nullable readPublicKey(EVP_PKEY *subject) {
    BIO *subjectBio = BIO_new(BIO_s_mem());
    if (subjectBio == NULL) {
        return nil;
    }
    PEM_write_bio_PUBKEY(subjectBio, subject);
    char *dataStart = NULL;
    long nameLength = BIO_get_mem_data(subjectBio, &dataStart);
    NSString *result = [[NSString alloc] initWithBytes:dataStart length:nameLength encoding:NSUTF8StringEncoding];
    BIO_free(subjectBio);
    return [result dataUsingEncoding:NSUTF8StringEncoding];
}

@implementation MTPKCS

- (instancetype)initWithIssuerName:(NSString *)issuerName subjectName:(NSString *)subjectName data:(NSData *)data {
    self = [super init];
    if (self != nil) {
        _issuerName = issuerName;
        _subjectName = subjectName;
        _data = data;
    }
    return self;
}

+ (MTPKCS * _Nullable)parse:(const unsigned char *)buffer size:(int)size {
#if TARGET_OS_IOS
#ifdef TELEGRAM_USE_BORINGSSL
    // Mach-O CMS signatures are DER, not PEM. The output stack must be allocated
    // before BoringSSL appends certificates to it.
    if (buffer == NULL || size <= 0) {
        return nil;
    }
    STACK_OF(X509) *certificates = sk_X509_new_null();
    if (certificates == NULL) {
        return nil;
    }
    CBS input;
    CBS_init(&input, buffer, (size_t)size);
    MTPKCS *result = nil;
    if (PKCS7_get_certificates(certificates, &input) && sk_X509_num(certificates) > 0) {
        X509 *certificate = sk_X509_value(certificates, 0);
        EVP_PKEY *publicKey = X509_get_pubkey(certificate);
        if (publicKey != NULL) {
            result = [[MTPKCS alloc] initWithIssuerName:readName(X509_get_issuer_name(certificate))
                subjectName:readName(X509_get_subject_name(certificate)) data:readPublicKey(publicKey)];
            EVP_PKEY_free(publicKey);
        }
    }
    sk_X509_pop_free(certificates, X509_free);
    return result;
#else
    MTPKCS * _Nullable result = nil;
    PKCS7 *pkcs7 = NULL;
    STACK_OF(X509) *signers = NULL;
    
    pkcs7 = d2i_PKCS7(NULL, &buffer, size);
    if (pkcs7 == NULL) {
        return nil;
    }
    
    if (!PKCS7_type_is_signed(pkcs7)) {
        if (pkcs7) {
            PKCS7_free(pkcs7);
        }
        return nil;
    }
    
    signers = PKCS7_get0_signers(pkcs7, NULL, PKCS7_BINARY);
    if (signers == NULL) {
        if (pkcs7) {
            PKCS7_free(pkcs7);
        }
        return nil;
    }
    
    const X509* cert = sk_X509_pop(signers);
    if (cert == NULL) {
        if (signers) {
            sk_X509_free(signers);
        }
        if (pkcs7) {
            PKCS7_free(pkcs7);
        }
        
        return nil;
    }
    
    X509_NAME *issuerName = X509_get_issuer_name(cert);
    X509_NAME *subjectName = X509_get_subject_name(cert);
    
    NSString *issuerNameString = readName(issuerName);
    NSString *subjectNameString = readName(subjectName);
    
    EVP_PKEY *publicKey = X509_get_pubkey(cert);
    NSData *data = readPublicKey(publicKey);
    
    result = [[MTPKCS alloc] initWithIssuerName:issuerNameString subjectName:subjectNameString data:data];
    
    return result;
#endif
#else
    return nil;
#endif
   
}

@end
