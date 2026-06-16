package com.unionpay.uas.demo.gfile;

import com.unionpay.uas.sdk.DemoUtil;
import com.unionpay.uas.sdk.SDKConfig;
import com.unionpay.uas.sdk.SDKConstants;
import com.unionpay.uas.sdk.UasService;
import com.unionpay.uas.sdk.gm.GmSDKConfig;
import com.unionpay.uas.sdk.gm.GmUasService;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.Inflater;

/**
 * 统一接入网关的接入示例demo:
 * 1. 根据平台规范组装请求报文头
 * 2. 根据实际业务组装请求报文体
 * 3. 若有敏感信息则对敏感明文做加密
 * 4. 根据平台规范对请求数据进行签名
 * 5. 向统一接入网关发起POST请求
 * 6. 对应答报文进行验签
 * 7. 若有敏感信息则对敏感密文做解密
 * <p>
 * 签名验签方式有国际和国密两种方式 推荐使用国密算法
 * <p><b>声明</b>：以下代码只是为了方便商户测试而提供的样例代码，机构可以根据自己需要，按照技术文档编写。该代码仅供参考，不提供编码，性能，规范性等方面的保障</p>
 * 本demo代码需要jdk版本为1.8.0_161及以上版本
 *
 * @author sdwang
 */
public class DemoSend {

    private static final Logger logger = Logger.getLogger(DemoSend.class);


    public static void handleRsp(String respBodyStr) {

        Map<String, Object> rspBody;
        String formSet = "{\"settleInfo";
        String formCod = "{\"code";
        String form = "{\"";

        //过滤功能，定位到字符串正确解析位置，防止乱码，之后文件格式修改需要同步修改
        int indexOfFormS = respBodyStr.indexOf(formSet);
        int indexOfFormC = respBodyStr.indexOf(formCod);
        int indexOfForm = respBodyStr.indexOf(form);
        int index = 0;

        if (-1 == indexOfFormS) {
            if (-1 == indexOfFormC) {
                logger.warn("正常字符串模式匹配失败\n");
            } else {
                index = indexOfFormC;
            }
        } else {
            index = indexOfFormS;
        }
        //进行兜底定位
        if ((-1 == indexOfFormS) && (-1 == indexOfFormC)) {
            if (-1 == indexOfForm) {
                logger.error("解析特征串失败\n");
                return;
            } else {
                index = indexOfForm;
            }
        }

        //开始解析内容
        respBodyStr = respBodyStr.substring(index);
        logger.info("从银联获得HTTP应答报文为：" + respBodyStr + "\n");
        rspBody = DemoUtil.unwarpJson(new JSONObject(respBodyStr));

        if (rspBody.containsKey("code")) {

            String code = (String) rspBody.get(SDKConstants.param_code);
            logger.info("银联返回的应答码：" + code);

            if ("0000000000".equals(code)) {
                //如果存在文件名和文件内容
                if (rspBody.containsKey("fileName") && rspBody.containsKey("fileContent")) {
                    String fileNameStr = (String) rspBody.get(SDKConstants.param_fileName);
                    logger.info("文件名：" + fileNameStr);

                    String fileContentStr = (String) rspBody.get(SDKConstants.param_fileContent);

                    if (fileContentStr == null) {
                        logger.error("无文件内容");
                        return;
                    }

                    logger.info("文件内长度：" + fileContentStr.length());

                    byte[] fileOutPut = new byte[100 * 1024 * 1024];
                    int fileOutLen = 0;
                    OutputStream outFileStream = null;
                    // 还原成原始文件,先base64解码，再解压，最后还原成原始文件
                    try {
                        byte[] fileByte = Base64.decodeBase64((fileContentStr).getBytes("UTF-8"));

                        Inflater decompresser = new Inflater();
                        decompresser.setInput(fileByte, 0, fileByte.length);
                        fileOutLen = decompresser.inflate(fileOutPut);
                        decompresser.end();

                        outFileStream = new FileOutputStream(fileNameStr);
                        if (fileOutLen <= 0) {
                            logger.error("文件内长度异常");
                            return;
                        }

                        //只写读取出来的文件内容长度，写入文件逻辑请根据预估文件大小，评估性能，灵活改变写入形式
                        outFileStream.write(fileOutPut, 0, fileOutLen);
                        //outFileStream.close();
                    } catch (Exception e) {
                        logger.error("文件解析失败: " + e);
                    }finally {
                        try {
                            if(outFileStream != null) {
                                outFileStream.close();
                            }
                        } catch (Exception e) {
                            logger.error("文件关闭失败: " + e);
                        }
                    }
                }
            }
        } else {
            logger.error("无银联应答码");
        }
    }


    public static void gm() {

        GmSDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();


        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "upntAcp.trade.file");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "7.0.0");                // 版本号 根据实际业务填写
        // reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "123456789012345");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则
        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();

        bodyMap.put("acqInsCode", "00010045");                           //C-收单机构代码
        bodyMap.put("version", "7.0.0");                                   //版本号-固定7.0.0
        bodyMap.put("accessType", "0");                                  //M-接入类型 0：商户直连接入/1：收单机构接入/2：平台类商户接入
        bodyMap.put("txnTime", "20140522121212");                        //M-交易发送时间 格式:yyyyMMddHHmmss
        bodyMap.put("fileType", "67");                                   //M-文件类型，参考规范
        bodyMap.put("reqReserved", "");                        //O-请求方保留域

        bodyMap.put("merId", "012310048990000");                    //C-商户代码，参考规范

        // settleInfo 清算信息
        Map<String, Object> settleInfo = new HashMap<>();
        settleInfo.put("settleDate", "1021");                            //5M清算日期 格式:MMdd
        settleInfo.put("settleNo", "03");                      //7C清算场次信息
        String settleInfostr = JSONObject.wrap(settleInfo).toString(); // 待加密报文串
        System.out.println(settleInfostr);
        bodyMap.put("settleInfo", settleInfo);


        reqData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());

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

        handleRsp(respBodyStr);

    }


    public static void rsa() {
        SDKConfig.loadPropertiesFromSrc();

        Map<String, String> reqData = new TreeMap<>();


        // 组装HTTP请求报文头
        reqData.put(SDKConstants.param_bizMethod, "upntAcp.trade.file");       // 交易类型 根据实际业务填写
        reqData.put(SDKConstants.param_version, "7.0.0");                // 版本号 根据实际业务填写
        // reqData.put(SDKConstants.param_signMethod, "SM2");               // 签名方法
        reqData.put(SDKConstants.param_signMethod, "RSA2");               // 签名方法
        reqData.put(SDKConstants.param_appId, "00010000");        // 发送方系统索引号 根据实际业务填写
        reqData.put(SDKConstants.param_appType, "02");                   // 01机构、02商户
        reqData.put(SDKConstants.param_reqId, DemoUtil.getReqId());      // 发送方流水号，可以自行定制规则


        // 组装HTTP请求报文体 根据实际业务填写
        Map<String, Object> bodyMap = new HashMap<>();

        bodyMap.put("acqInsCode", "00010045");                           //C-收单机构代码
        bodyMap.put("version", "7.0.0");                                  // 版本号-固定7.0.0
        bodyMap.put("accessType", "0");                                  //M-接入类型 0：商户直连接入/1：收单机构接入/2：平台类商户接入
        bodyMap.put("txnTime", "20140522121212");                        //M-订单发送时间 格式:yyyyMMddHHmmss
        bodyMap.put("fileType", "67");                                   //M-文件类型，参考规范
        bodyMap.put("reqReserved", "");                        //O-请求方保留域

        bodyMap.put("merId", "012310048990000");                                   //C-商户代码，参考规范

        // settleInfo 清算信息
        Map<String, Object> settleInfo = new HashMap<>();
        settleInfo.put("settleDate", "1021");                            //清算日期 格式:MMdd
        settleInfo.put("settleNo", "03");                      //清算场次信息
        String settleInfostr = JSONObject.wrap(settleInfo).toString(); // 待加密报文串
        System.out.println(settleInfostr);
        bodyMap.put("settleInfo", settleInfo);


        reqData.put(SDKConstants.param_body, JSONObject.wrap(bodyMap).toString());

        Map<String, String> reqMap = UasService.sign(reqData);

        // 发送请求报文并接受同步应答
        // 这里调用签名方法sign之后 调用post之前不能对map中的键值对做任何修改 如果修改会导致验签不通过
        Map<String, Object> rspMap = UasService.post(reqMap, SDKConfig.getConfig().getTransUrl());

        if (rspMap == null || rspMap.size() == 0) {
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

        if (!UasService.validate(verifyMap)) {
            logger.info("验证银联签名失败\n");
            return;
        } else {
            logger.info("验证银联签名成功\n");
        }

        handleRsp(respBodyStr);

    }


    public static void main(String[] args) {
        // 使用国密方法
        gm();

        // 使用国际方法
        //rsa();
    }


}
