package com.unionpay.uas.demo.gov;

import com.unionpay.uas.demo.gov.sm4.SM4Utils;
import com.unionpay.uas.demo.gov.sm4.Utils;
import com.unionpay.uas.sdk.gm.GmSDKConfig;
import com.unionpay.uas.sdk.gm.GmUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.BigIntegers;
import org.json.JSONObject;

import java.util.*;

/*
* tips2.0生码demo
*/
public class TipsGenDemo {

    private static final Logger logger = Logger.getLogger(TipsGenDemo.class);

    public static void main(String[] args) {
        // 使用国密算法调用此方法
        GmDemo();

    }

    public static void GmDemo() {

        // 初始化配置文件，读取证书
        GmSDKConfig.loadPropertiesFromSrc();

        //ed加密密钥
        String tipsSm4Key = "dID2i635SSf1T61t";
        //加签私钥
        String tipsPrivateiKey = "520D9542CF31EB36E9F7D6F666D431D02994FD3F6E6048D53A8F1FED00F9F3BA";
        //加签userid
        String uid = "0000000000";
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("bt", "51");//按码规范处理
        reqMap.put("ek", "00");//按码规范处理
        reqMap.put("ck", "44");//地区码
        reqMap.put("ea", "01");//按码规范处理
        reqMap.put("sa", "01");//按码规范处理
        reqMap.put("v", "2.0");//按码规范处理
        reqMap.put("sk", "00");//按码规范处理
        //secObject为ed的明文
        JSONObject secObject = new JSONObject();
        //按照文档中的ed标签进行组装，以下是参考
        secObject.put("rel_inq_key", "1111");
        secObject.put("detail", "ETS");
        secObject.put("buss_code", "S2_5800_0010");
        secObject.put("mer_id", "12344");

        logger.info("tips2.0业务参数" + secObject.toString());
        String base64ed = SM4Utils.encryptData_ECB(secObject.toString(), tipsSm4Key, false);
        logger.info("tips2.0业务参数sm4加密后ed" + base64ed);
        reqMap.put("ed", base64ed);
        // 加签
        String signData = null;
        Map<String, String> respMap = new HashMap<String, String>();
        try {
            logger.info("tips2.0待加签数据" + coverMap2String(reqMap, "signature"));
            logger.info("tips2.0加签tipsPrivateiKey=" + tipsPrivateiKey + "****uid=" + uid);
            // 签名密钥格式转换
            // 进行摘要
            byte[] digest = digest(coverMap2String(reqMap, "signature").getBytes());
            byte[] privateKey = Utils.hexStringToBytes(tipsPrivateiKey);
            byte[] userIdByte = uid.getBytes();
            byte[] sig = GmUtil.signSm3WithSm2(digest, userIdByte, GmUtil.getPrivatekeyFromD(BigIntegers.fromUnsignedByteArray(privateKey)));
            signData = Base64.encodeBase64String(sig);
        } catch (Exception e) {
            logger.error("tips2.0生码失败:数据加签异常");
            return;
        }
        reqMap.put("sn", signData);
        String tips = "tips://" + coverMap2String(reqMap, "");
        logger.info("tips2.0生成:" + tips);


    }

    /**
     * Desc: HashMap2String
     *
     * @param data
     * @param signParam
     * @return {@link String}
     * @author libinjie
     * @date 2024/3/31 19:40
     */
    public static String coverMap2String(Map<String, String> data, String signParam) {
        TreeMap<String, String> tree = new TreeMap<String, String>();
        Iterator<Map.Entry<String, String>> it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> en = it.next();
            if (signParam.equals(en.getKey().trim())) {
                continue;
            }
            tree.put(en.getKey(), en.getValue());
        }
        it = tree.entrySet().iterator();
        StringBuffer sf = new StringBuffer();
        while (it.hasNext()) {
            Map.Entry<String, String> en = it.next();
            sf.append(en.getKey() + "=" + en.getValue() + "&");
        }
        return sf.substring(0, sf.length() - 1);
    }

    public static byte[] digest(byte[] data) {
        SM3Digest sm3 = new SM3Digest();
        byte[] result = null;
        try {
            sm3.update(data, 0, data.length);
            result = new byte[sm3.getDigestSize()];
            sm3.doFinal(result, 0);
        } catch (Exception e) {
            logger.error("Fail: SM3 byte[] to byte[]", e);
        }
        return result;
    }

}
