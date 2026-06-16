package com.unionpay.uas.demo.cbs.util;

import com.unionpay.uas.sdk.DemoUtil;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;

/**
 * cbs 需要的工具方法
 */
public class CbsUtils {

    public static String getCbsTxnNo(String merId){
        return merId + DemoUtil.getRandomString(35 - merId.length());
    }

    // 先压缩，再base64
    public static byte[] deflateEncode(byte[] raw) throws Exception {
        if (raw == null || raw.length == 0) {
            throw new IllegalArgumentException("Ecode and deflate error with null raw byte[]");
        }
        byte[] tmpByte = deflater(raw);
        return base64encode(tmpByte);
    }

    public static byte[] base64encode(byte[] raw) {
        return Base64.encodeBase64(raw);
    }
    public static byte[] deflater(byte[] inputByte)  {
        int compressedDataLength = 0;
        Deflater compresser = new Deflater();
        compresser.setInput(inputByte);
        compresser.finish();
        ByteArrayOutputStream o = new ByteArrayOutputStream(inputByte.length);
        byte[] result = new byte[1024];
        try {
            while (!compresser.finished()) {
                compressedDataLength = compresser.deflate(result);
                o.write(result, 0, compressedDataLength);
            }
        } catch (Exception e) {
            throw new RuntimeException("DEFLATER");
        }
        finally {
            if (o != null) {
                try {
                    o.close();
                } catch(Exception e){}
            }
        }
        compresser.end();
        return o.toByteArray();
    }

}
