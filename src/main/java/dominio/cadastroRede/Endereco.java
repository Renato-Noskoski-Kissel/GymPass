package dominio.cadastroRede;

import dominio.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Endereço de um estabelecimento.
 *
 * Composição com Estabelecimento (1 para 1): o endereço não existe fora do
 * estabelecimento que localiza. As coordenadas são usadas na validação de
 * proximidade do check-in (RN4).
 *
 * Persistência: objeto de valor sem tabela própria; as colunas ficam
 * embutidas em estabelecimentos.
 */
@Embeddable
public class Endereco {

    /** Raio médio da Terra, usado na fórmula de haversine. */
    private static final double RAIO_TERRA_METROS = 6_371_000;

    @Column(name = "logradouro", nullable = false, length = 150)
    private String logradouro;
    @Column(name = "numero", nullable = false, length = 10)
    private String numero;
    @Column(name = "bairro", nullable = false, length = 80)
    private String bairro;
    @Column(name = "cidade", nullable = false, length = 80)
    private String cidade;
    @Column(name = "cep", nullable = false, length = 9)
    private String cep;
    @Column(name = "latitude", nullable = false)
    private double latitude;
    @Column(name = "longitude", nullable = false)
    private double longitude;

    /** Exigido pela JPA. */
    protected Endereco() { }

    public Endereco(String logradouro, String numero, String bairro, String cidade,
                    String cep, double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new RegraDeNegocioException("Latitude inválida: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new RegraDeNegocioException("Longitude inválida: " + longitude);
        }
        this.logradouro = logradouro;
        this.numero = numero;
        this.bairro = bairro;
        this.cidade = cidade;
        this.cep = cep;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Distância em metros entre este endereço e um ponto (lat, lng).
     * Fórmula de haversine: distância sobre a superfície da esfera, não em linha reta.
     */
    public double distanciaAte(double lat, double lng) {
        double dLat = Math.toRadians(lat - latitude);
        double dLng = Math.toRadians(lng - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(lat))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RAIO_TERRA_METROS * Math.asin(Math.sqrt(a));
    }

    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getCep() { return cep; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    @Override public String toString() {
        return logradouro + ", " + numero + " - " + bairro + ", " + cidade;
    }
}
