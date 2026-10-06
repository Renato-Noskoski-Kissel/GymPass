package dominio.acessoAgenda;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Estabelecimento;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * UC03 - registro de que um aluno chegou a um estabelecimento.
 *
 * Criado por Adesao.registrarCheckIn depois de todas as validações. Nasce
 * pendente, com um código que o aluno apresenta na recepção.
 *
 * UC07 - a recepção valida ou recusa o check-in. O código não expira
 * (expiração ficou fora do escopo).
 *
 * As coordenadas de origem ficam registradas, mas não são expostas à empresa
 * contratante (RNF3).
 */
public class CheckIn {

    /** Sem 0/O, 1/I/L: o código é lido em voz alta ou digitado na recepção. */
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 6;
    private static final SecureRandom SORTEIO = new SecureRandom();

    private final LocalDateTime dataHora;
    private final double latitudeOrigem;
    private final double longitudeOrigem;
    private SituacaoCheckIn situacaoValidacao;
    private final String codigo;
    private String motivoRecusa;
    private final Aluno aluno;
    private final Estabelecimento estabelecimento;

    public CheckIn(Aluno aluno, Estabelecimento estabelecimento,
                   double latitudeOrigem, double longitudeOrigem) {
        this.aluno = aluno;
        this.estabelecimento = estabelecimento;
        this.latitudeOrigem = latitudeOrigem;
        this.longitudeOrigem = longitudeOrigem;
        this.dataHora = LocalDateTime.now();
        this.situacaoValidacao = SituacaoCheckIn.PENDENTE;
        this.codigo = gerarCodigo();
    }

    private static String gerarCodigo() {
        StringBuilder sb = new StringBuilder(TAMANHO_CODIGO);
        for (int i = 0; i < TAMANHO_CODIGO; i++) {
            sb.append(ALFABETO.charAt(SORTEIO.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    /** UC07 extensão 3c - o check-in só pode ser tratado na recepção onde foi feito. */
    public void conferirNaRecepcao(Estabelecimento recepcao) {
        if (recepcao != estabelecimento) {
            throw new RegraDeNegocioException(
                "Este check-in foi feito em " + estabelecimento + ", não aqui.");
        }
    }

    /** UC07 passo 6 - a recepção libera a entrada. */
    public void validar() {
        exigirPendente();
        this.situacaoValidacao = SituacaoCheckIn.VALIDADO;
    }

    /** UC07 extensão 5a - a recepção recusa a entrada, com motivo. */
    public void recusar(String motivo) {
        exigirPendente();
        if (motivo == null || motivo.isBlank()) {
            throw new RegraDeNegocioException("Informe o motivo da recusa.");
        }
        this.motivoRecusa = motivo.trim();
        this.situacaoValidacao = SituacaoCheckIn.RECUSADO;
    }

    private void exigirPendente() {
        if (situacaoValidacao != SituacaoCheckIn.PENDENTE) {
            throw new RegraDeNegocioException(
                "Este check-in já foi " + (situacaoValidacao == SituacaoCheckIn.VALIDADO
                    ? "validado." : "recusado."));
        }
    }

    public LocalDateTime getDataHora() { return dataHora; }
    public double getLatitudeOrigem() { return latitudeOrigem; }
    public double getLongitudeOrigem() { return longitudeOrigem; }
    public SituacaoCheckIn getSituacaoValidacao() { return situacaoValidacao; }
    public String getCodigo() { return codigo; }
    public String getMotivoRecusa() { return motivoRecusa; }
    public Aluno getAluno() { return aluno; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }

    @Override public String toString() {
        return "Check-in " + codigo + " de " + aluno + " em " + estabelecimento
             + " - " + dataHora + " [" + situacaoValidacao + "]";
    }
}
