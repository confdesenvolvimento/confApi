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
            if ("USUARIO_MANAGER".equals(claims.get("tipoToken"))) {
                if (!validarIdentidadeIndividual(token, claims.get("codigoUsuario"))) return false;
            }
            Date dataExpiracao = claims.getExpiration();
            LocalDateTime data = dataExpiracao.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            return !LocalDateTime.now().isAfter(data);
        }catch (Exception e){
            return false;
        }
    }

    protected boolean validarIdentidadeIndividual(String token,Object userId) {
        if (userId == null) return false;
        var verified=com.confApi.authv2.V2.request(com.confApi.authv2.V2.setting("AUTH_V2_MANAGER_URL"),
                "/internal/v2/autenticacao/usuario/autorizar","POST",java.util.Map.of("token",token),
                com.confApi.authv2.V2.token("AUTH_V2_MANAGER_TOKEN_FILE"),null);
        return verified.path("active").asBoolean() && userId.toString().equals(verified.path("userId").asText());
    }

    public String obterLoginUsuario(String token) throws ExpiredJwtException{
        return (String) obterClaims(token).getSubject();
    }
}
