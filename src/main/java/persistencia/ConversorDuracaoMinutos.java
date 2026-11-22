package persistencia;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Duration;

/**
 * Aula.duracao é Duration no domínio e INTEGER (minutos) na coluna
 * aulas.duracao_minutos.
 *
 * O domínio só trabalha com minutos inteiros (a tela envia duracaoMin);
 * segundos, se existissem, seriam descartados ao gravar.
 *
 * autoApply: vale para todo atributo Duration persistido, sem que o
 * domínio precise importar nada deste pacote. Constantes static (como
 * Reserva.ANTECEDENCIA_SEM_PENALIDADE) não são persistidas e ficam de fora.
 */
@Converter(autoApply = true)
public class ConversorDuracaoMinutos implements AttributeConverter<Duration, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Duration duracao) {
        return duracao == null ? null : Math.toIntExact(duracao.toMinutes());
    }

    @Override
    public Duration convertToEntityAttribute(Integer minutos) {
        return minutos == null ? null : Duration.ofMinutes(minutos);
    }
}
