package com.unionpay.uas.demo.dcp.vo;

/**
 * @author lxjiang
 * @date 2023/5/23
 */
public class TokenInfo {

    private String transLmt ;

    private String transLmtCurrCd ;

    private String totalDayLmt ;

    private String useNo ;

    private String totalMonLmt ;

    private String chnlBit ;

    private String mchtRange ;

    private String mchntCd ;

    private String tokenExpiry ;

    private String cntryCd ;

    public String getTransLmt() {
        return transLmt;
    }

    public void setTransLmt(String transLmt) {
        this.transLmt = transLmt;
    }

    public String getTransLmtCurrCd() {
        return transLmtCurrCd;
    }

    public void setTransLmtCurrCd(String transLmtCurrCd) {
        this.transLmtCurrCd = transLmtCurrCd;
    }

    public String getTotalDayLmt() {
        return totalDayLmt;
    }

    public void setTotalDayLmt(String totalDayLmt) {
        this.totalDayLmt = totalDayLmt;
    }

    public String getUseNo() {
        return useNo;
    }

    public void setUseNo(String useNo) {
        this.useNo = useNo;
    }

    public String getTotalMonLmt() {
        return totalMonLmt;
    }

    public void setTotalMonLmt(String totalMonLmt) {
        this.totalMonLmt = totalMonLmt;
    }

    public String getChnlBit() {
        return chnlBit;
    }

    public void setChnlBit(String chnlBit) {
        this.chnlBit = chnlBit;
    }

    public String getMchtRange() {
        return mchtRange;
    }

    public void setMchtRange(String mchtRange) {
        this.mchtRange = mchtRange;
    }

    public String getMchntCd() {
        return mchntCd;
    }

    public void setMchntCd(String mchntCd) {
        this.mchntCd = mchntCd;
    }

    public String getTokenExpiry() {
        return tokenExpiry;
    }

    public void setTokenExpiry(String tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }

    public String getCntryCd() {
        return cntryCd;
    }

    public void setCntryCd(String cntryCd) {
        this.cntryCd = cntryCd;
    }
}
