package br.com.agricultura.planejamentofinanceiro.security;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Implementacao RFC 6238 (TOTP com HMAC-SHA1, janela de 30s e 6 digitos)
 * compativel com Google Authenticator.
 */
@Service
public class TotpService {

    private static final int INTERVALO_SEGUNDOS = 30;
    private static final int DIGITOS = 6;
    private static final int JANELA_TOLERANCIA = 1;
    private static final String BASE32_ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public boolean validarCodigo(String segredoBase32, String codigoInformado) {
        if (segredoBase32 == null || segredoBase32.isBlank() || codigoInformado == null) {
            return false;
        }
        String codigoLimpo = codigoInformado.trim().replaceAll("\\s+", "");
        if (!codigoLimpo.matches("\\d{6}")) {
            return false;
        }

        long janelaAtual = Instant.now().getEpochSecond() / INTERVALO_SEGUNDOS;
        byte[] chave = decodificarBase32(segredoBase32);

        for (int offset = -JANELA_TOLERANCIA; offset <= JANELA_TOLERANCIA; offset++) {
            String esperado = gerarCodigoParaJanela(chave, janelaAtual + offset);
            if (esperado.equals(codigoLimpo)) {
                return true;
            }
        }
        return false;
    }

    public String gerarCodigoAtual(String segredoBase32) {
        long janelaAtual = Instant.now().getEpochSecond() / INTERVALO_SEGUNDOS;
        byte[] chave = decodificarBase32(segredoBase32);
        return gerarCodigoParaJanela(chave, janelaAtual);
    }

    private String gerarCodigoParaJanela(byte[] chave, long janela) {
        try {
            byte[] dados = ByteBuffer.allocate(8).putLong(janela).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(chave, "HmacSHA1"));
            byte[] hash = mac.doFinal(dados);

            int offset = hash[hash.length - 1] & 0x0F;
            int binario = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binario % (int) Math.pow(10, DIGITOS);
            return String.format("%0" + DIGITOS + "d", otp);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Erro ao calcular TOTP", ex);
        }
    }

    private byte[] decodificarBase32(String base32) {
        String normalizado = base32.trim().replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        int buffer = 0;
        int bitsNoBuffer = 0;
        byte[] resultado = new byte[(normalizado.length() * 5) / 8];
        int indice = 0;

        for (int i = 0; i < normalizado.length(); i++) {
            char c = normalizado.charAt(i);
            int val = BASE32_ALFABETO.indexOf(c);
            if (val < 0) {
                throw new IllegalArgumentException("Segredo Base32 inválido.");
            }
            buffer = (buffer << 5) | val;
            bitsNoBuffer += 5;
            if (bitsNoBuffer >= 8) {
                resultado[indice++] = (byte) ((buffer >> (bitsNoBuffer - 8)) & 0xFF);
                bitsNoBuffer -= 8;
            }
        }
        return resultado;
    }
}
