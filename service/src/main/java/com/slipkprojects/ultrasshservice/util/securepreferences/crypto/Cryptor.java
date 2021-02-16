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

import android.util.Base64;

import java.security.SecureRandom;
import com.slipkprojects.ultrasshservice.util.securepreferences.model.SecurityConfig;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.params.KeyParameter;

/**
 * @author Hussain Al-Derry <hussain.derry@gmail.com>
 * @version 1.0
 * */
public final class Cryptor {

    /* Variables used for parsing the stored Base64 */
    private static final String SPLITTER = "\\.";
    private static final int INDEX_SALT = 0;
    private static final int INDEX_IV = 1;
    private static final int INDEX_CIPHER_TEXT = 2;

    /* Encryption Configurations */
    private final SecurityConfig mSecurityConfig;
    private final byte[] mSalt;
    private final byte[] mPassword;
    private final CipherService mCipherService;

    /**
     * Initializes the Cryptor with the provided {@link SecurityConfig}
     * @param config The security configurations to use
     * */
    public static Cryptor initWithSecurityConfig(SecurityConfig config){
        return new Cryptor(config);
    }

    private Cryptor(SecurityConfig securityConfig){
        this.mSecurityConfig = securityConfig;
        this.mCipherService = CipherServiceImpl.getInstance(mSecurityConfig.getAlgorithm());

        // Generating Session Salt
        mSalt = new byte[mSecurityConfig.getSaltSize()];
        new SecureRandom().nextBytes(mSalt);

        // Generating Session Password
        mPassword = pbkdf2(mSalt);
    }

    /**
     * Encrypts the given data and returns a formatted Base64 for storage.
     *
     * @param data The data to be encrypted.
     * @return Base64 String to be stored.
     * */
    public String encryptToBase64(byte[] data){
        // Generating Random IV
        SecureRandom mRandom = new SecureRandom();
        byte[] iv = new byte[mCipherService.getIVSize()];
        mRandom.nextBytes(iv);

        byte[] encrypted = mCipherService.encrypt(mPassword, iv, data);
        return String.format("%s.%s.%s", toBase64(mSalt), toBase64(iv), toBase64(encrypted));
    }

    /**
     * Decrypts data from a given Base64 String
     *
     * @param encryptedBase64 The Base64 string to be decrypted.
     * @return The data decrypted as byte array.
     * */
    public byte[] decryptFromBase64(String encryptedBase64){
        String[] parts = encryptedBase64.split(SPLITTER);
        if(parts.length != 3){
            throw new IllegalArgumentException("Malformed data string");
        }

        byte[] salt = fromBase64(parts[INDEX_SALT]);
        byte[] iv = fromBase64(parts[INDEX_IV]);
        byte[] cipherText = fromBase64(parts[INDEX_CIPHER_TEXT]);

        return mCipherService.decrypt(pbkdf2(salt), iv, cipherText);
    }

    /**
     * Generates PBKDF2 hash for the configured password using the provided salt
     *
     * @param salt The salt to use.
     * @return The password hash as byte array
     * */
    private byte[] pbkdf2(byte[] salt){
        byte[] passwordBytes = PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(mSecurityConfig.getPassword());
        PKCS5S2ParametersGenerator mGenerator;
        switch (mSecurityConfig.getDigestType()){

            case SHA1:{
                mGenerator = new PKCS5S2ParametersGenerator(new SHA1Digest());
                break;
            }

            case SHA256:{
                mGenerator = new PKCS5S2ParametersGenerator(new SHA256Digest());
                break;
            }

            case SHA512:{
                mGenerator = new PKCS5S2ParametersGenerator(new SHA512Digest());
                break;
            }

            default:{
                throw new IllegalStateException("Unknown Digest!");
            }

        }

        mGenerator.init(passwordBytes, salt, mSecurityConfig.getPBKDF2Iterations());
        return ((KeyParameter) mGenerator.generateDerivedParameters(mSecurityConfig.getKeySize())).getKey();
    }

    private String toBase64(byte[] data){
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    private byte[] fromBase64(String base64){
        return Base64.decode(base64, Base64.NO_WRAP);
    }

}
