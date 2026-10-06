package dominio.acessoAgenda;

/**
 * CONFIRMADA ao reservar (UC04a); CANCELADA ao cancelar (UC04b).
 * Se o cancelamento foi tardio, a reserva continua CANCELADA e fica
 * marcada como penalizada (atributo de Reserva, não um terceiro estado).
 */
public enum SituacaoReserva { CONFIRMADA, CANCELADA }
