package dominio.cadastroRede;

import dominio.RegraDeNegocioException;

/**
 * CPF e CNPJ são guardados só com os dígitos: "12.345.678/0001-90" e
 * "12345678000190" são o mesmo cadastro. A pontuação fica só na tela.
 * Não confere dígitos verificadores.
 */
final class Documentos {

    private Documentos() { }

    static String apenasDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    /** Aceita com ou sem pontuação; devolve os 11 dígitos. */
    static String cpf(String cpf) {
        String d = apenasDigitos(cpf);
        if (d.length() != 11) {
            throw new RegraDeNegocioException("O CPF precisa ter 11 dígitos: " + cpf);
        }
        return d;
    }

    /** Aceita com ou sem pontuação; devolve os 14 dígitos. */
    static String cnpj(String cnpj) {
        String d = apenasDigitos(cnpj);
        if (d.length() != 14) {
            throw new RegraDeNegocioException("O CNPJ precisa ter 14 dígitos: " + cnpj);
        }
        return d;
    }
}
