package com.slipkprojects.ultrasshservice.util.securepreferences.model;

/**
 * Encryption algorithms supported by the {@link com.github.hussainderry.securepreferences.crypto.CipherService}
 * @author Hussain Al-Derry <hussain.derry@gmail.com>
 * */
public enum EncryptionAlgorithm {

    AES(new int[]{128, 196, 256}), TripleDES(new int[]{128, 192});

    private int[] keySizesInBits;

    EncryptionAlgorithm(int[] keySizes) {
        this.keySizesInBits = keySizes;
    }

    public int[] getKeySizes() {
        return keySizesInBits;
    }
}
