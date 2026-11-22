package persistencia;

import dominio.planosAdesao.Ativa;
import dominio.planosAdesao.Encerrada;
import dominio.planosAdesao.EstadoAdesao;
import dominio.planosAdesao.SituacaoAdesao;
import dominio.planosAdesao.Suspensa;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.hibernate.annotations.Immutable;

/**
 * Traduz o estado da Adesao (padrão State) para o texto da coluna
 * adesoes.situacao e de volta.
 *
 * Ao gravar, usa o rótulo que o próprio estado informa (situacao()).
 * Ao ler, cria o objeto de estado correspondente. Os estados não têm
 * atributos, então um objeto novo é equivalente ao que foi gravado.
 *
 * autoApply: vale para todo atributo do tipo EstadoAdesao, sem que o
 * domínio precise importar nada deste pacote.
 *
 * @Immutable: o Hibernate não tenta copiar nem comparar o conteúdo do
 * estado. A adesão só muda de estado trocando a referência (mudarPara),
 * e essa troca é o que ele detecta para gerar o UPDATE.
 */
@Converter(autoApply = true)
@Immutable
public class ConversorEstadoAdesao implements AttributeConverter<EstadoAdesao, String> {

    @Override
    public String convertToDatabaseColumn(EstadoAdesao estado) {
        return estado == null ? null : estado.situacao().name();
    }

    @Override
    public EstadoAdesao convertToEntityAttribute(String coluna) {
        if (coluna == null) {
            return null;
        }
        return switch (SituacaoAdesao.valueOf(coluna.trim())) {
            case ATIVA -> new Ativa();
            case SUSPENSA -> new Suspensa();
            case ENCERRADA -> new Encerrada();
        };
    }
}
