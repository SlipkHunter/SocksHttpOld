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
package com.slipkprojects.ultrasshservice.util.securepreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.slipkprojects.ultrasshservice.util.securepreferences.crypto.Cryptor;
import com.slipkprojects.ultrasshservice.util.securepreferences.model.SecurityConfig;
import com.slipkprojects.ultrasshservice.util.securepreferences.crypto.HashSHA;

/**
 * @author Hussain Al-Derry <hussain.derry@gmail.com>
 * @version 1.0
 * */
public final class SecurePreferences implements SharedPreferences{

    private static final String CHARSET = "UTF-8";
    private final Cryptor mCryptor;
    private SharedPreferences mProxyPreferences;

    /**
     * Creates an instance of the preferences using the provided security configurations.
     * @param context The context to be used to create the instance
     * @param filename The preferences filename
     * @param securityConfig The security configurations to use
     * @return The SecurePreferences instance
     * */
    public static SecurePreferences getInstance(Context context, String filename, SecurityConfig securityConfig){
        if(context == null || filename == null || securityConfig == null){
            throw new IllegalArgumentException("Params cannot be null!");
        }
        return new SecurePreferences(context.getApplicationContext(), filename, securityConfig);
    }

    private SecurePreferences(Context context, String fileName, SecurityConfig securityConfig) {
        this.mCryptor = Cryptor.initWithSecurityConfig(securityConfig);
        this.mProxyPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    private String generateKeyHash(String key){
        try{
            byte[] mBytes = HashSHA.hashUsingSHA256(key.getBytes(CHARSET));
            return Base64.encodeToString(mBytes, Base64.NO_WRAP);
        }catch(UnsupportedEncodingException e){
            throw new IllegalStateException(e.getMessage());
        }
    }

    private String encryptToBase64(String data){
        try{
            return mCryptor.encryptToBase64(data.getBytes(CHARSET));
        }catch(UnsupportedEncodingException e){
            throw new IllegalStateException(e.getMessage());
        }
    }

    private String decryptFromBase64(String base64Data){
        try{
            byte[] decrypted = mCryptor.decryptFromBase64(base64Data);
            return new String(decrypted, CHARSET);
        }catch(UnsupportedEncodingException e){
            throw new IllegalStateException(e.getMessage());
        }
    }

    @Override
    public Map<String, ?> getAll() {
        throw new UnsupportedOperationException("Operation Not Supported!");
    }

    @Override
    public String getString(String key, String defValue){
        final String encryptedData = mProxyPreferences.getString(generateKeyHash(key), null);
        return encryptedData != null ? decryptFromBase64(encryptedData) : defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defSet) {
        Set<String> encryptedSet = mProxyPreferences.getStringSet(generateKeyHash(key), null);

        if(encryptedSet != null){
            Set<String> plainSet = new HashSet<>();
            for(String temp : encryptedSet) {
                plainSet.add(decryptFromBase64(temp));
            }
            return plainSet;
        }else{
            return defSet;
        }
    }

    @Override
    public int getInt(String key, int defValue) {
        final String data = getString(key, null);
        return data != null ? Integer.parseInt(data) : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        final String data = getString(key, null);
        return data != null ? Long.parseLong(data) : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        final String data = getString(key, null);
        return data != null ? Float.parseFloat(data) : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        final String data = getString(key, null);
        return data != null ? Boolean.parseBoolean(data) : defValue;
    }

    @Override
    public boolean contains(String key) {
        return mProxyPreferences.contains(generateKeyHash(key));
    }

    @Override
    public SecurePreferences.Editor edit() {
        return new Editor();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        mProxyPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        mProxyPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    public final class Editor implements SharedPreferences.Editor{

        protected SharedPreferences.Editor mProxyEditor;

        public Editor(){
            this.mProxyEditor = SecurePreferences.this.mProxyPreferences.edit();
        }

        @Override
        public SecurePreferences.Editor putString(String key, String data){
            String hashedKey = generateKeyHash(key);
            String encryptedData = encryptToBase64(data);
            mProxyEditor.putString(hashedKey, encryptedData);
            return this;
        }

        @Override
        public SecurePreferences.Editor putStringSet(String key, Set<String> set) {
            String hashedKey = generateKeyHash(key);
            Set<String> encryptedSet = new HashSet<>();

            for(String temp : set) {
                encryptedSet.add(encryptToBase64(temp));
            }

            mProxyEditor.putStringSet(hashedKey, encryptedSet);
            return this;
        }

        @Override
        public SecurePreferences.Editor putInt(String key, int data) {
            String hashedKey = generateKeyHash(key);
            String encryptedData = encryptToBase64(Integer.toString(data));
            mProxyEditor.putString(hashedKey, encryptedData);
            return this;
        }

        @Override
        public SecurePreferences.Editor putLong(String key, long data){
            String hashedKey = generateKeyHash(key);
            String encryptedData = encryptToBase64(Long.toString(data));
            mProxyEditor.putString(hashedKey, encryptedData);
            return this;
        }

        @Override
        public SecurePreferences.Editor putFloat(String key, float data) {
            String hashedKey = generateKeyHash(key);
            String encryptedData = encryptToBase64(Float.toString(data));
            mProxyEditor.putString(hashedKey, encryptedData);
            return this;
        }

        @Override
        public SecurePreferences.Editor putBoolean(String key, boolean data) {
            String hashedKey = generateKeyHash(key);
            String encryptedData = encryptToBase64(Boolean.toString(data));
            mProxyEditor.putString(hashedKey, encryptedData);
            return this;
        }

        @Override
        public SecurePreferences.Editor remove(String key) {
            mProxyEditor.remove(generateKeyHash(key));
            return this;
        }

        @Override
        public SecurePreferences.Editor clear() {
            mProxyEditor.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return mProxyEditor.commit();
        }

        @Override
        public void apply() {
            mProxyEditor.apply();
        }

    }

}
