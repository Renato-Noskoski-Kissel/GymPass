package application.rotinas;
import domain.financeiro.CalculadoraSoma;

public class CalculadoraService {

    private final CalculadoraSoma calculadoraSoma = new CalculadoraSoma();

    public double executar(double a, double b) {
        return calculadoraSoma.somar(a, b);
    }
}