package sistema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação web.
 *
 * scanBasePackages lista as camadas que o Spring deve varrer. O pacote
 * "dominio" fica de fora de propósito: as classes de domínio são Java puro,
 * sem nenhuma anotação de framework.
 */
@SpringBootApplication(scanBasePackages = {"presentation", "application", "persistencia"})
public class Aplicacao {
    public static void main(String[] args) {
        SpringApplication.run(Aplicacao.class, args);
    }
}
