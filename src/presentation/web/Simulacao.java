package presentation.web;
import application.rotinas.CalculadoraService;

public class Simulacao{
    public static void main(String[] args) {
        CalculadoraService service = new CalculadoraService();

        double numero1 = 10.5;
        double numero2 = 5.5;

        // Executa o fluxo
        double resultado = service.executar(numero1, numero2);

        System.out.println("O resultado da soma é: " + resultado);
    }
}