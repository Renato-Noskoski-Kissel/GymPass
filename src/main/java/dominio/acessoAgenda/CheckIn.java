package dominio.acessoAgenda;

import dominio.RegraDeNegocioException;
import dominio.cadastroRede.Aluno;
import dominio.cadastroRede.Estabelecimento;
import jakarta.persistence.*;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * UC03 - registro de que um aluno chegou a um estabelecimento.
 *
 * Criado por Adesao.registrarCheckIn depois de todas as validações. Nasce
 * pendente, com um código que o aluno apresenta na recepção.
 *
 * UC07 - a recepção valida ou recusa o check-in. O código vale até o fim do
 * dia em que o check-in foi feito; depois disso o check-in expira (UC03 7a).
 *
 * As coordenadas de origem ficam registradas, mas não são expostas à empresa
 * contratante (RNF3).
 */
@Entity
@Table(name = "check_ins")
public class CheckIn {

    /** Sem 0/O, 1/I/L: o código é lido em voz alta ou digitado na recepção. */
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 6;
    private static final SecureRandom SORTEIO = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "latitude_origem", nullable = false)
    private double latitudeOrigem;

    @Column(name = "longitude_origem", nullable = false)
    private double longitudeOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_validacao", nullable = false, length = 20)
    private SituacaoCheckIn situacaoValidacao;

    @Column(name = "codigo", nullable = false, length = 6)
    private String codigo;

    @Column(name = "motivo_recusa", length = 255)
    private String motivoRecusa;

    /** EAGER: Aluno é abstrata; carregar já a subclasse real evita proxy. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "aluno_cpf", nullable = false)
    private Aluno aluno;

    /** EAGER: Estabelecimento é abstrata; carregar já a subclasse real evita proxy. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estabelecimento_cnpj", nullable = false)
    private Estabelecimento estabelecimento;

    /** Exigido pela JPA. */
    protected CheckIn() { }

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

    /** UC03 passo 7 - prazo de validade do comprovante: meia-noite do dia do check-in. */
    public LocalDateTime prazoValidacao() {
        return dataHora.toLocalDate().plusDays(1).atStartOfDay();
    }

    /** UC03 7a - o check-in ainda pendente passou do prazo neste momento? */
    public boolean expirou(LocalDateTime momento) {
        return situacaoValidacao == SituacaoCheckIn.PENDENTE
            && !momento.isBefore(prazoValidacao());
    }

    /** UC03 7a - passa à situação expirada. Só um check-in pendente e vencido expira. */
    public void expirar() {
        if (!expirou(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Este check-in ainda está dentro do prazo.");
        }
        this.situacaoValidacao = SituacaoCheckIn.EXPIRADO;
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

    /**
     * Validar e recusar só valem para check-in pendente e dentro do prazo.
     * Um pendente vencido expira aqui mesmo e a operação é rejeitada (UC07 3b)
     * com CheckInExpiradoException, para que a expiração fique gravada.
     */
    private void exigirPendente() {
        if (expirou(LocalDateTime.now())) {
            expirar();
        }
        switch (situacaoValidacao) {
            case PENDENTE -> { }
            case VALIDADO -> throw new RegraDeNegocioException("Este check-in já foi validado.");
            case RECUSADO -> throw new RegraDeNegocioException("Este check-in já foi recusado.");
            case EXPIRADO -> throw new CheckInExpiradoException();
        }
    }

    public Integer getId() { return id; }
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
