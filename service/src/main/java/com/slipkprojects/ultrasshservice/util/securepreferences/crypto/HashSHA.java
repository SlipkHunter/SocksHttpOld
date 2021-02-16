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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Hussain Al-Derry <hussain.derry@gmail.com>
 * @version 0.1
 * */
public class HashSHA {

    /**
     * Hashes the given data using SHA-256
     * @param data the byte data to be hashed
     * @return hashed byte array containing the hash
     * */
    public static byte[] hashUsingSHA256(byte[] data){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        }catch(NoSuchAlgorithmException e){
            throw new IllegalStateException("Unable to hash!");
        }
    }

    /**
     * Hashes the given data using SHA-512
     * @param data the byte data to be hashed
     * @return hashed byte array containing the hash
     * */
    public static byte[] hashUsingSHA512(byte[] data){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            return digest.digest(data);
        }catch(NoSuchAlgorithmException e){
            throw new IllegalStateException("Unable to hash!");
        }
    }
}
