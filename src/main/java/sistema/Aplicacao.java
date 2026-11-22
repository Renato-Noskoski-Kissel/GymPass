package sistema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Ponto de entrada da aplicação web.
 *
 * scanBasePackages lista as camadas que o Spring deve varrer em busca de
 * componentes. O pacote "dominio" fica de fora: nenhuma classe de domínio é
 * um bean do Spring. As classes de domínio levam só anotações JPA
 * (mapeamento objeto-relacional), sem nenhuma anotação do Spring.
 *
 * EntityScan diz ao JPA onde estão as entidades (dominio) e os conversores
 * de atributo (persistencia). EnableJpaRepositories diz onde estão as
 * interfaces Spring Data. Sem os dois, o padrão seria só o pacote "sistema".
 */
@SpringBootApplication(scanBasePackages = {"presentation", "application", "persistencia"})
@EntityScan(basePackages = {"dominio", "persistencia"})
@EnableJpaRepositories(basePackages = "persistencia.jpa")
public class Aplicacao {
    public static void main(String[] args) {
        SpringApplication.run(Aplicacao.class, args);
    }
}
