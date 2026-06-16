package com.unionpay.uas.demo.gov.sm4;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SM4Utils {
    //public String secretKey = "";
    //private String iv = "";
    //public boolean hexString = false;

    public static Pattern p = Pattern.compile("\\s*|\t|\r|\n");

    private static String CHARSET_UTF_8 = "UTF-8";

    public SM4Utils() {}

    public static String encryptData_ECB(String plainText,String secretKey,boolean hexString) {
        try {
            SM4_Context ctx = new SM4_Context();
            ctx.isPadding = true;
            ctx.mode = SM4.SM4_ENCRYPT;

            byte[] keyBytes;
            if (hexString) {
                keyBytes = Utils.hexStringToBytes(secretKey);
            } else {
                keyBytes = secretKey.getBytes();
            }

            SM4 sm4 = new SM4();
            sm4.sm4_setkey_enc(ctx, keyBytes);
            byte[] encrypted = sm4.sm4_crypt_ecb(ctx, plainText.getBytes(CHARSET_UTF_8));
            String cipherText = new BASE64Encoder().encode(encrypted);
            if (cipherText != null && cipherText.trim().length() > 0) {
                Matcher m = p.matcher(cipherText);
                cipherText = m.replaceAll("");
            }
            return cipherText;
        } catch (Exception e) {
            return null;
        }
    }

    public static String decryptData_ECB(String cipherText,String secretKey,boolean hexString) {
        try {
            SM4_Context ctx = new SM4_Context();
            ctx.isPadding = true;
            ctx.mode = SM4.SM4_DECRYPT;

            byte[] keyBytes;
            if (hexString) {
                keyBytes = Utils.hexStringToBytes(secretKey);
            } else {
                keyBytes = secretKey.getBytes();
            }

            SM4 sm4 = new SM4();
            sm4.sm4_setkey_dec(ctx, keyBytes);
            byte[] decrypted = sm4.sm4_crypt_ecb(ctx, new BASE64Decoder().decodeBuffer(cipherText));
            return new String(decrypted, CHARSET_UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
//        String plainText = "6226090000000048";
//
//        System.out.println("ECB模式加密");
//        String cipherText = encryptData_ECB(plainText,"6gj2XMLTlYJ9kf34",false);
//        System.out.println("密文: " + cipherText + ", length: " + cipherText.length());
//        System.out.println("");

        String plainText = decryptData_ECB("WtAhJtUcH2cuHPwTTefn9ba+5b0eXILHRkNtHSKjnYI=","6gj2XMLTlYJ9kf34",false);
        System.out.println("明文: " + plainText);
        System.out.println("");
    }
}