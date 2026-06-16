package com.unionpay.uas.demo.dcp;

import com.unionpay.uas.demo.dcp.vo.DigtCredential;
import com.unionpay.uas.sdk.DemoUtil;
import com.unionpay.uas.sdk.SDKConfig;
import com.unionpay.uas.sdk.SDKConstants;
import com.unionpay.uas.sdk.UasService;
import com.unionpay.uas.sdk.gm.GmSDKConfig;
import com.unionpay.uas.sdk.gm.GmUasService;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author lxjiang
 * @date 2023/5/8
 */
public class DemoWalletCredentialCancel {

    private static final Logger logger = Logger.getLogger(DemoWalletCredentialCancel.class);

    public static void gm(){

        GmSDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "dcp.wallet.cancelCrdl.apply");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        // reqData.put(SDKConstants.param_signMethod, "SM2-CERT");               // 签名方法
        reqData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        JSONObject body = new JSONObject();
        body.put("cardWalletId", "00088880001");
        body.put("transTm", "2023-05-05 09:34:54");
        body.put("transNo", "444300002023050509345412089001");
        body.put("credentialSeqId", "DC00000000000000032194FF0724497E20000034");

        DigtCredential digtCredential = new DigtCredential();
        digtCredential.setStaticCryptData("07094B28B77EE4CA152B7C4D10A7305EBA881755F3792F32BBFB6A2D0C1848CA248DDEDBBDB5077FD1A8669FFB5A6D62B630246AD738A6CEA4385E6BA3500F0A");
        digtCredential.setCredentialVersion("0000");
        digtCredential.setAtc("0");
        digtCredential.setAcctFaceId("TYHSGFJSHF1234657890TYHSGFJSHF1234657890");
        digtCredential.setPriAcctNoMask("622841*********0925");
        digtCredential.setCarrierId("SU72AIDJ21394281347KHFABFVAI72KJDSAFHKADSFGO1");
        digtCredential.setLocInfoData("0");
        JSONObject digtCredentialJson = new JSONObject(digtCredential);
        String digtCredentialStr = digtCredentialJson.toString();
        body.put("digtCredential", new JSONObject(digtCredentialStr));

        body.put("bankInsIdCd", "07000000011");

        JSONObject subBody = new JSONObject();
        subBody.put("reqReserved-1","reqReserved-1");
        body.put("reqReserved", subBody);



        reqData.put(SDKConstants.param_body, body.toString());

        Map<String, String> reqMap = GmUasService.sign(reqData);

        // 发送请求报文并接受同步应答
        // 这里调用签名方法sign之后 调用post之前不能对map中的键值对做任何修改 如果修改会导致验签不通过
        Map<String, Object> rspMap = UasService.post(reqMap, GmSDKConfig.getConfig().getTransUrl());

        if (rspMap == null || rspMap.isEmpty()) {
            logger.info("POST请求服务端失败");
            return;
        }

        Map<String, String> respHeader = (Map<String, String>) rspMap.get(SDKConstants.param_header);
        String respBodyStr = (String) rspMap.get(SDKConstants.param_body);

        // 组装验签报文
        // 注：银联网关返回的应答头字段是驼峰式的 但在某些特殊场景(如HTTP 2.0)下头字段会转为小写; 此处为不区分大小写 在解析应答头时将所有字段名统一转换为小写;
        // 注：调用请求方在使用其他HTTP工具库时需要自行注意兼容头字段大小写问题
        Map<String, String> verifyMap = new HashMap<>();
        verifyMap.put(SDKConstants.param_signId, respHeader.get("signid"));
        verifyMap.put(SDKConstants.param_sign, respHeader.get("sign"));
        verifyMap.put(SDKConstants.param_bizMethod, respHeader.get("bizmethod"));
        verifyMap.put(SDKConstants.param_version, respHeader.get("version"));
        verifyMap.put(SDKConstants.param_signMethod, respHeader.get("signmethod"));
        verifyMap.put(SDKConstants.param_appId, respHeader.get("appid"));
        verifyMap.put(SDKConstants.param_reqId, respHeader.get("reqid"));
        verifyMap.put(SDKConstants.param_signPubKeyCert, respHeader.get("signpubkeycert"));
        verifyMap.put(SDKConstants.param_body, respBodyStr);

        if (!GmUasService.validate(verifyMap)) {
            logger.info("验证银联签名失败\n");
            return;
        } else {
            logger.info("验证银联签名成功\n");
        }

        Map<String, Object> rspBody;
        try {
            logger.info("从银联获得HTTP应答报文为：" + respBodyStr + "\n");
            rspBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
            if (rspBody.containsKey("encryptData")) {
                Map<String, Object> encryptData = (Map<String, Object>) rspBody.get(SDKConstants.param_encryptData);
                String decKey = (String) encryptData.get(SDKConstants.param_key);
                String decData = (String) encryptData.get(SDKConstants.param_data);
                String certId = (String) encryptData.get(SDKConstants.param_certId);
                try {
                    String decodeStr = GmUasService.gmDec(decData, decKey, certId);
                    logger.info("解银联加密后的敏感信息的报文：" + decodeStr);
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        } catch (JSONException e) {
            logger.info("应答体解json失败。");
        }
    }

    public static void rsa(){
        SDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();

        // 组装http请求报文头
        reqData.put(SDKConstants.param_bizMethod, "dcp.wallet.cancelCrdl.apply");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "1.0.0");                // 版本号 根据实际业务填写
        reqData.put(SDKConstants.param_signMethod, "RSA2");              // 签名方法
        // reqData.put(SDKConstants.param_signMethod, "RSA2-CERT");              // 签名方法
        reqData.put(SDKConstants.param_appId, "00010000");               // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则

        // 组装HTTP请求报文体 根据实际业务填写
        JSONObject body = new JSONObject();
        body.put("cardWalletId", "00088880001");
        body.put("transTm", "2023-05-05 09:34:54");
        body.put("transNo", "444300002023050509345412089001");
        body.put("credentialSeqId", "DC00000000000000032194FF0724497E20000034");

        DigtCredential digtCredential = new DigtCredential();
        digtCredential.setStaticCryptData("07094B28B77EE4CA152B7C4D10A7305EBA881755F3792F32BBFB6A2D0C1848CA248DDEDBBDB5077FD1A8669FFB5A6D62B630246AD738A6CEA4385E6BA3500F0A");
        digtCredential.setCredentialVersion("0000");
        digtCredential.setAtc("0");
        digtCredential.setAcctFaceId("TYHSGFJSHF1234657890TYHSGFJSHF1234657890");
        digtCredential.setPriAcctNoMask("622841*********0925");
        digtCredential.setCarrierId("SU72AIDJ21394281347KHFABFVAI72KJDSAFHKADSFGO1");
        digtCredential.setLocInfoData("0");
        JSONObject digtCredentialJson = new JSONObject(digtCredential);
        String digtCredentialStr = digtCredentialJson.toString();
        body.put("digtCredential", new JSONObject(digtCredentialStr));

        body.put("bankInsIdCd", "07000000011");

        JSONObject subBody = new JSONObject();
        subBody.put("reqReserved-1","reqReserved-1");
        body.put("reqReserved", subBody);

        reqData.put(SDKConstants.param_body, body.toString());

        Map<String, String> reqMap = UasService.sign(reqData);

        // 发送请求报文并接受同步应答
        // 这里调用签名方法sign之后 调用post之前不能对map中的键值对做任何修改 如果修改会导致验签不通过
        Map<String, Object> rspMap = UasService.post(reqMap, SDKConfig.getConfig().getTransUrl());

        if(rspMap == null || rspMap.size() == 0) {
            logger.info("POST请求服务端失败");
            return;
        }

        Map<String, String> respHeader = (Map<String, String>)rspMap.get(SDKConstants.param_header);
        String respBodyStr = (String)rspMap.get(SDKConstants.param_body);

        // 组装验签报文
        // 注：银联网关返回的应答头字段是驼峰式的 但在某些特殊场景(如HTTP 2.0)下头字段会转为小写; 此处为不区分大小写 在解析应答头时将所有字段名统一转换为小写;
        // 注：调用请求方在使用其他HTTP工具库时需要自行注意兼容头字段大小写问题
        Map<String, String> verifyMap = new HashMap<>();
        verifyMap.put(SDKConstants.param_signId, respHeader.get("signid"));
        verifyMap.put(SDKConstants.param_sign, respHeader.get("sign"));
        verifyMap.put(SDKConstants.param_bizMethod, respHeader.get("bizmethod"));
        verifyMap.put(SDKConstants.param_version, respHeader.get("version"));
        verifyMap.put(SDKConstants.param_signMethod, respHeader.get("signmethod"));
        verifyMap.put(SDKConstants.param_appId, respHeader.get("appid"));
        verifyMap.put(SDKConstants.param_reqId, respHeader.get("reqid"));
        verifyMap.put(SDKConstants.param_signPubKeyCert, respHeader.get("signpubkeycert"));
        verifyMap.put(SDKConstants.param_body, respBodyStr);

        if(!UasService.validate(verifyMap)) {
            logger.info("验证银联签名失败\n");
            return;
        }else {
            logger.info("验证银联签名成功\n");
        }

        Map<String, Object> rspBody;
        try {
            logger.info("从银联获得HTTP应答报文为：" + respBodyStr + "\n");
            rspBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));
            if (rspBody.containsKey("encryptData")) {
                Map<String, Object> encryptData = (Map<String, Object>) rspBody.get(SDKConstants.param_encryptData);
                String decKey = (String) encryptData.get(SDKConstants.param_key);
                String decData = (String) encryptData.get(SDKConstants.param_data);
                String certId = (String) encryptData.get(SDKConstants.param_certId);
                try {
                    String decodeStr = UasService.rsaDec(decData, decKey, certId);
                    logger.info("解银联加密后的敏感信息的报文：" + decodeStr);
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        } catch (JSONException e) {
            logger.info("应答体解json失败。");
        }
    }

    public static void main(String[] args) {
        // 使用国密方法
        gm();

        // 使用国际方法
        //rsa();
    }

}
