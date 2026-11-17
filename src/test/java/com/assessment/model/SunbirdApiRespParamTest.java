package com.assessment.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SunbirdApiRespParamTest {

    @Test
    void noArgConstructorShouldLeaveFieldsNull() {
        SunbirdApiRespParam param = new SunbirdApiRespParam();
        assertNull(param.getResmsgid());
        assertNull(param.getMsgid());
    }

    @Test
    void idConstructorShouldSetResmsgidAndMsgid() {
        SunbirdApiRespParam param = new SunbirdApiRespParam("req-1");
        assertEquals("req-1", param.getResmsgid());
        assertEquals("req-1", param.getMsgid());
    }

    @Test
    void shouldGetAndSetAllFields() {
        SunbirdApiRespParam param = new SunbirdApiRespParam();
        param.setResmsgid("rid");
        param.setMsgid("mid");
        param.setErr("err");
        param.setStatus("FAILED");
        param.setErrmsg("error message");

        assertEquals("rid", param.getResmsgid());
        assertEquals("mid", param.getMsgid());
        assertEquals("err", param.getErr());
        assertEquals("FAILED", param.getStatus());
        assertEquals("error message", param.getErrmsg());
    }
}
