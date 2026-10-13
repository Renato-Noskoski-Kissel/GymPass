package dominio.acessoAgenda;

/**
 * PENDENTE ao fazer check-in (UC03); VALIDADO ou RECUSADO na recepção (UC07);
 * EXPIRADO quando não foi validado até o fim do dia (UC03 7a, UC07 3b).
 */
public enum SituacaoCheckIn { PENDENTE, VALIDADO, RECUSADO, EXPIRADO }
