package dominio;

/** Lançada quando uma regra de negócio do domínio é violada. */
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) { super(mensagem); }
}
