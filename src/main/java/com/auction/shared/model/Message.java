package com.auction.shared.model;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    // "type" giống như tiêu đề thư, để Server biết em muốn làm gì (VD: "LOGIN", "REGISTER", "BID")
    private String type;

    // "payload" là ruột bức thư, chứa dữ liệu thật (VD: tài khoản/mật khẩu, hoặc số tiền đấu giá)
    private Object payload;

    public Message(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }
}