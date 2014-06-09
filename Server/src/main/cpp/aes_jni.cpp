#include "com_voting_crypto_AESCipher.h"
#include "AES.h"

JNIEXPORT jintArray JNICALL Java_com_voting_crypto_AESCipher_keyExpansion
(JNIEnv *env, jobject jobj, jbyteArray key)
{
	jbyte *j_key = env->GetByteArrayElements(key,0);
	unsigned int *r_key = KeyExpansion((unsigned char *)j_key);
	jintArray result = env->NewIntArray(44); //44 is size of AES extendedkey
	env->SetIntArrayRegion (result, 0, 44 , (jint*)r_key);
	env->ReleaseByteArrayElements(key, j_key, JNI_ABORT);
	delete[] r_key;
	return result;
};

JNIEXPORT jbyteArray JNICALL Java_com_voting_crypto_AESCipher_crypt
(JNIEnv *env, jobject jobj, jbyteArray in, jintArray key, jbyteArray s)
{
	jbyte *j_in = env->GetByteArrayElements(in, 0);
	jint *r_key = env->GetIntArrayElements(key,0);
	jbyte *S0 = env->GetByteArrayElements(s,0);
	jint length = env->GetArrayLength(in);
	unsigned char *res = CTR_AES_crypt((unsigned char*)j_in, (unsigned int*)r_key, (unsigned char*)S0, (int)length);
	jbyteArray result = env->NewByteArray(length);
	env->SetByteArrayRegion(result, 0, length, (jbyte*)res);
	env->ReleaseByteArrayElements(in, j_in, JNI_ABORT);
	env->ReleaseByteArrayElements(s, S0, JNI_ABORT);
	env->ReleaseIntArrayElements(key, r_key, JNI_ABORT);
	delete[] res;
	return result;
};
