package com.misterworld.server.travelapplication.service;

import com.misterworld.server.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

/** Additional signed, expiring quote binding. Contains hashes, no raw participant information. */
@Component
public class BookingQuoteSigner {
    private final byte[] key;
    public BookingQuoteSigner(@Value("${app.jwt-secret}") String secret) { key=secret.getBytes(StandardCharsets.UTF_8); }
    public String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception e) { throw new IllegalStateException("Cannot hash quote"); }
    }
    private byte[] sign(String value) {
        try { var mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key,"HmacSHA256"));
            return mac.doFinal(("booking-quote:"+value).getBytes(StandardCharsets.UTF_8)); }
        catch(Exception e) { throw new IllegalStateException("Cannot sign quote"); }
    }
    public String issue(Long memberId,String requestHash,String selectionHash,Instant expiresAt) {
        String payload=memberId+":"+requestHash+":"+selectionHash+":"+expiresAt.getEpochSecond();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))+"."+
            Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload));
    }
    public void verify(String token,Long memberId,String requestHash,String selectionHash,Instant now) {
        try {
            var parts=token.split("\\.");
            if(parts.length!=2) throw new IllegalArgumentException();
            String payload=new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8);
            if(!MessageDigest.isEqual(sign(payload),Base64.getUrlDecoder().decode(parts[1]))) throw new IllegalArgumentException();
            var fields=payload.split(":");
            if(fields.length!=4 || !fields[0].equals(memberId.toString()) || !fields[1].equals(requestHash)) throw new IllegalArgumentException();
            if(!Instant.ofEpochSecond(Long.parseLong(fields[3])).isAfter(now) || !fields[2].equals(selectionHash))
                throw new BusinessException(409,"QUOTE_CHANGED","구성 또는 금액을 다시 확인하세요.");
        } catch(BusinessException e) { throw e; }
        catch(Exception e) { throw new BusinessException(400,"INVALID_QUOTE","유효한 견적 확인이 필요합니다."); }
    }
}
