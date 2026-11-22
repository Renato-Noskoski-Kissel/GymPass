package application;

/**
 * O registro pedido (cnpj, cpf, id) não existe. A apresentação responde 404.
 *
 * Não é RegraDeNegocioException: não é uma regra do domínio que foi violada,
 * é a requisição que aponta para algo inexistente.
 */
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String mensagem) { super(mensagem); }

    /** Devolve o valor, ou lança esta exceção se ele for nulo. */
    public static <T> T exigir(T valor, String mensagem) {
        if (valor == null) {
            throw new NaoEncontradoException(mensagem);
        }
        return valor;
    }
}
