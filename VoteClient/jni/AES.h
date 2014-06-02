#ifndef AES_H_
#define AES_H_
#define SIZE 4 //size of row (or column) in bytes
#define BLOCK 16 //size of block in bytes; numb of blocks

unsigned int* KeyExpansion (const unsigned char*);
unsigned char* encrypt_AES (const unsigned char*, const unsigned int*);
unsigned char* CTR_AES_crypt (const unsigned char*, const unsigned int*, unsigned char*, const int);

unsigned int RotByte(unsigned int);
void Counter (unsigned char*);
#endif /* AES_H_ */
