package com.sync.sc.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
public class NavicatPassword {
    private static final String AES_KEY = "libcckeylibcckey";
    private static final String AES_IV = "libcciv libcciv ";
    private static final String BLOW_KEY = "3DC5CA39";
    private static final String BLOW_IV = "FFFFFFFFFFFFFFFF";
    public static String encrypt(String plaintext, int version) throws Exception {
        switch (version) {
            case 11:                return encryptEleven(plaintext);
            case 12:                return encryptTwelve(plaintext);
            default:                throw new IllegalArgumentException("Unsupported version");
        }
    }
    public static String decrypt(String ciphertext, int version) throws Exception {
        switch (version) {
            case 11:                return decryptEleven(ciphertext);
            case 12:                return decryptTwelve(ciphertext);
            default:                throw new IllegalArgumentException("Unsupported version");

        }
    }
    private static String encryptEleven(String plaintext){
        try {
            byte[] inData = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] outData = Encrypt(inData);
            return HexFormat.of().formatHex(outData).toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    private static String encryptTwelve(String plaintext) throws Exception {
        try {
            SecretKeySpec _AesKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec _AesIV = new IvParameterSpec(AES_IV.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, _AesKey, _AesIV);
            byte[] ret = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(ret).toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    private static String decryptEleven(String ciphertext){
        try {
            byte[] inData = HexFormat.of().parseHex(ciphertext);
            byte[] outData = Decrypt(inData);
            return new String(outData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    private static String decryptTwelve(String ciphertext) throws Exception {
        try {
            SecretKeySpec _AesKey = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec _AesIV = new IvParameterSpec(AES_IV.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, _AesKey, _AesIV);
            byte[] ret = cipher.doFinal(HexFormat.of().parseHex(ciphertext));
            return new String(ret, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    private static void xorBytes(byte[] a, byte[] b) {
        for (int i = 0; i < a.length; i++) {
            int aVal = a[i] & 0xff; // convert byte to integer
            int bVal = b[i] & 0xff;
            a[i] = (byte) (aVal ^ bVal); // xor aVal and bVal and typecast to byte
        }
    }

    private static void xorBytes(byte[] a, byte[] b, int l) {
        for (int i = 0; i < l; i++) {
            int aVal = a[i] & 0xff; // convert byte to integer
            int bVal = b[i] & 0xff;
            a[i] = (byte) (aVal ^ bVal); // xor aVal and bVal and typecast to byte
        }
    }
    private static byte[] Decrypt(byte[] inData) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            byte[] userkey_data = BLOW_KEY.getBytes(StandardCharsets.UTF_8);
            sha1.update(userkey_data, 0, userkey_data.length);
            SecretKeySpec  _Key = new SecretKeySpec(sha1.digest(), "Blowfish");
            Cipher _Encryptor = Cipher.getInstance("Blowfish/ECB/NoPadding");
            _Encryptor.init(Cipher.ENCRYPT_MODE, _Key);
            Cipher _Decryptor = Cipher.getInstance("Blowfish/ECB/NoPadding");
            _Decryptor.init(Cipher.DECRYPT_MODE, _Key);
            byte[] initVec = HexFormat.of().parseHex(BLOW_IV);
            byte[] _IV = _Encryptor.doFinal(initVec);
            byte[] CV = Arrays.copyOf(_IV, _IV.length);
            byte[] ret = new byte[inData.length];

            int blocks_len = inData.length / 8;
            int left_len = inData.length % 8;

            for (int i = 0; i < blocks_len; i++) {
                byte[] temp = Arrays.copyOfRange(inData, i * 8, (i * 8) + 8);

                temp = _Decryptor.doFinal(temp);
                xorBytes(temp, CV);
                System.arraycopy(temp, 0, ret, i * 8, 8);
                for (int j = 0; j < CV.length; j++) {
                    CV[j] = (byte) (CV[j] ^ inData[i * 8 + j]);
                }
            }

            if (left_len != 0) {
                CV = _Encryptor.doFinal(CV);
                byte[] temp = Arrays.copyOfRange(inData, blocks_len * 8, (blocks_len * 8) + left_len);

                xorBytes(temp, CV, left_len);
                for (int j = 0; j < temp.length; j++) {
                    ret[blocks_len * 8 + j] = temp[j];
                }
            }

            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private static byte[] Encrypt(byte[] inData) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            byte[] userkey_data = BLOW_KEY.getBytes(StandardCharsets.UTF_8);
            sha1.update(userkey_data, 0, userkey_data.length);
            SecretKeySpec  _Key = new SecretKeySpec(sha1.digest(), "Blowfish");
            Cipher _Encryptor = Cipher.getInstance("Blowfish/ECB/NoPadding");
            _Encryptor.init(Cipher.ENCRYPT_MODE, _Key);
            byte[] initVec = HexFormat.of().parseHex(BLOW_IV);
            byte[] _IV = _Encryptor.doFinal(initVec);
            byte[] CV = Arrays.copyOf(_IV, _IV.length);
            byte[] ret = new byte[inData.length];
            int blocks_len = inData.length / 8;
            int left_len = inData.length % 8;

            for (int i = 0; i < blocks_len; i++) {
                byte[] temp = Arrays.copyOfRange(inData, i * 8, (i * 8) + 8);

                xorBytes(temp, CV);
                temp = _Encryptor.doFinal(temp);
                xorBytes(CV, temp);

                System.arraycopy(temp, 0, ret, i * 8, 8);
            }

            if (left_len != 0) {
                CV = _Encryptor.doFinal(CV);
                byte[] temp = Arrays.copyOfRange(inData, blocks_len * 8, (blocks_len * 8) + left_len);
                xorBytes(temp, CV, left_len);
                System.arraycopy(temp, 0, ret, blocks_len * 8, temp.length);
            }

            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
