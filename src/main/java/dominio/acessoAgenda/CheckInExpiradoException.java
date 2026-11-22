package dominio.acessoAgenda;

import dominio.RegraDeNegocioException;

/**
 * UC07 3b - a recepção tentou validar ou recusar um check-in que já expirou.
 *
 * É a única recusa em que o domínio altera o estado antes de lançar: o
 * check-in pendente e vencido passa a EXPIRADO, e essa mudança deve ficar
 * gravada mesmo com a operação recusada. Uma classe própria permite que só
 * este caso seja confirmado no banco (noRollbackFor em CheckInService); as
 * demais violações de regra continuam desfazendo a transação inteira.
 *
 * Por ser uma RegraDeNegocioException, a tela recebe o mesmo HTTP 400 com a
 * mensagem de sempre.
 */
public class CheckInExpiradoException extends RegraDeNegocioException {

    public CheckInExpiradoException() {
        super("Este check-in expirou. Peça ao aluno para fazer um novo check-in.");
    }
}
