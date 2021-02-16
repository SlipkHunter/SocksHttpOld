/*
 * Copyright 2017 Hussain Al-Derry
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.slipkprojects.ultrasshservice.util.securepreferences.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.slipkprojects.ultrasshservice.util.securepreferences.model.EncryptionAlgorithm;

/***
 * @author Hussain Al-Derry <hussain.derry@gmail.com>
 */
public final class CipherServiceImpl implements CipherService{

    private final Logger mLogger;
    private final String mEncryptionAlgorithm;
    private final int ivSize;
    private final Cipher mCipher;

    public static CipherService getInstance(EncryptionAlgorithm algorithm){
        switch(algorithm){

            case AES:{
                return new CipherServiceImpl("AES", "GCM", "NoPadding", 12);
            }

            case TripleDES:{
                return new CipherServiceImpl("DESede", "CBC", "PKCS5Padding", 8);
            }

            default:{
                throw new IllegalArgumentException("Unknown Algorithm");
            }

        }
    }

    private CipherServiceImpl(String algorithm, String blockChaining, String paddingType, int ivSize) {
        this.mEncryptionAlgorithm = algorithm;
        this.ivSize = ivSize;
        this.mLogger = Logger.getLogger(CipherService.class.getName());

        try{
            String encryptionMode = String.format("%s/%s/%s", algorithm, blockChaining, paddingType);
            mLogger.info("Encryption-Mode: " + encryptionMode);
            mCipher = Cipher.getInstance(encryptionMode);
        }catch(NoSuchAlgorithmException | NoSuchPaddingException e){
            mLogger.log(Level.SEVERE, "method: constructor()", e);
            throw new IllegalStateException("Unable to initialize cipher, mode might not be supported");
        }
    }

    @Override
    public int getIVSize() {
        return ivSize;
    }

    @Override
    public byte[] encrypt(byte[] key, byte[] iv, byte[] data) {
        synchronized(mCipher){
            try{
                mCipher.init(Cipher.ENCRYPT_MODE, generateSecretKeySpec(key), generateIvParameterSpec(iv));
                return mCipher.doFinal(data);
            }catch(InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                mLogger.log(Level.SEVERE, "method: encrypt()", e);
                throw new IllegalStateException(String.format("%s: %s", e.getClass().getName(), e.getMessage()));
            }
        }
    }

    @Override
    public byte[] decrypt(byte[] key, byte[] iv, byte[] data) {
        synchronized(mCipher){
            try{
                mCipher.init(Cipher.DECRYPT_MODE, generateSecretKeySpec(key), generateIvParameterSpec(iv));
                return mCipher.doFinal(data);
            }catch(InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                mLogger.log(Level.SEVERE, "method: decrypt()", e);
                throw new IllegalStateException(String.format("%s: %s", e.getClass().getName(), e.getMessage()));
            }
        }
    }

    private SecretKey generateSecretKeySpec(byte[] key){
        return new SecretKeySpec(key, mEncryptionAlgorithm);
    }

    private IvParameterSpec generateIvParameterSpec(byte[] iv){
        return new IvParameterSpec(iv);
    }
}
