package com.confApi.security.jwt;

import com.confApi.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


@Service
public class JwtService {
    @Value("${security.jwt.expiracao}")
    private long expiracao;
    @Value("${security.jwt.chave-assinatura}")
    private String chaveAssinatura;


    public String gerarToken(Usuario usuario) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiracao * 1000);

        return Jwts
                .builder()
                .setSubject(usuario.getLogin())
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, chaveAssinatura.getBytes())
                .compact();
    }

    private Claims obterClaims(String token ) throws ExpiredJwtException {
        return Jwts
                .parser()
                .setSigningKey(chaveAssinatura.getBytes())
                .parseClaimsJws(token)
                .getBody();
    }
    public boolean tokenValido( String token ){
        try{
            Claims claims = obterClaims(token);
            Date dataExpiracao = claims.getExpiration();
            LocalDateTime data = dataExpiracao.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            return !LocalDateTime.now().isAfter(data);
        }catch (Exception e){
            return false;
        }
    }

    public String obterLoginUsuario(String token) throws ExpiredJwtException{
        return (String) obterClaims(token).getSubject();
    }
}
