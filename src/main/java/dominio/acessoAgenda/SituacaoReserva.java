package dominio.acessoAgenda;

/**
 * Nesta iteração toda reserva nasce e permanece CONFIRMADA.
 * CANCELADA entra com Cancelar Reserva e UTILIZADA com Validar Check-in na
 * Recepção, ambos na iteração 3.
 */
public enum SituacaoReserva { CONFIRMADA, CANCELADA, UTILIZADA }
