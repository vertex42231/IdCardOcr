package com.example.IdCardOcr.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

//身份证识别结果数据模型
public class IdentifyResult implements Serializable {
    private int errorcode;
    private String errormsg;
    //身份证信息字段（使用SerializedName映射JSON字段名）
    @SerializedName("Name")
    private String name;
    @SerializedName("Sex")
    private String sex;
    @SerializedName("Nation")
    private String nation;
    @SerializedName("Birth")
    private String birth;
    @SerializedName("Address")
    private String address;
    @SerializedName("IdNum")
    private String id;

    //Getter和Setter方法
    public int getErrorcode() { return errorcode; }
    public void setErrorcode(int errorcode) { this.errorcode = errorcode; }
    public String getErrormsg() { return errormsg; }
    public void setErrormsg(String errormsg) { this.errormsg = errormsg; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public String getNation() { return nation; }
    public void setNation(String nation) { this.nation = nation; }
    public String getBirth() { return birth; }
    public void setBirth(String birth) { this.birth = birth; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
