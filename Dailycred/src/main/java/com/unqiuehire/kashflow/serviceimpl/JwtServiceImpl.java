//package com.unqiuehire.kashflow.serviceimpl;
//
//import com.unqiuehire.kashflow.service.JwtService;
//import io.jsonwebtoken.*;
//import org.springframework.stereotype.Service;
//
//import java.util.Date;
//
//@Service
//public class JwtServiceImpl implements JwtService {
//
//    private final String SECRET_KEY = "mysecretkey";
//
//    @Override
//    public String generateToken(String phone, String role) {
//        return Jwts.builder()
//                .setSubject(phone)
//                .claim("role", role)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
//                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
//                .compact();
//    }
//
//    @Override
//    public String extractPhone(String token) {
//        return getClaims(token).getSubject();
//    }
//
//    @Override
//    public boolean validateToken(String token, String phone) {
//        return extractPhone(token).equals(phone) && !isExpired(token);
//    }
//
//    private boolean isExpired(String token) {
//        return getClaims(token).getExpiration().before(new Date());
//    }
//
//    private Claims getClaims(String token) {
//        return Jwts.parser()
//                .setSigningKey(SECRET_KEY)
//                .parseClaimsJws(token)
//                .getBody();
//    }
//}