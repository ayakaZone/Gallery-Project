package com.yupi.yupicture.interfaces.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserExchangeVIPCodeRequest implements Serializable {

    private static final long serialVersionUID = -3189051263355535625L;
    /**
     * vip兑换码
     */
    private String vipCode;

}
