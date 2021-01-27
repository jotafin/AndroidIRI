package bmodel;


public class IRI {
    private String iri;
    private double lati;
    private double longi;
    private double latf;
    private double longf;
    private String nome;
    private String id;



    /**
     * @param iri
     * @param lati
     * @param longi
     * @param latf
     * @param longf
     * @param id
     */
    public IRI(String iri, double lati, double longi, double latf, double longf, String id) {
        super();
        this.iri = iri;
        this.lati = lati;
        this.longi = longi;
        this.latf = latf;
        this.longf = longf;
        this.id = id;
    }

    /**
     * @return the iri
     */
    public String getIri() {
        return iri;
    }
    /**
     * @param iri the iri to set
     */
    public void setIri(String iri) {
        this.iri = iri;
    }

    /**
     * @return the lati
     */
    public double getLati() {
        return lati;
    }
    /**
     * @param lati the lati to set
     */
    public void setLati(double lati) {
        this.lati = lati;
    }
    /**
     * @return the longi
     */
    public double getLongi() {
        return longi;
    }
    /**
     * @param longi the longi to set
     */
    public void setLongi(double longi) {
        this.longi = longi;
    }
    /**
     * @return the latf
     */
    public double getLatf() {
        return latf;
    }
    /**
     * @param latf the latf to set
     */
    public void setLatf(double latf) {
        this.latf = latf;
    }
    /**
     * @return the longf
     */
    public double getLongf() {
        return longf;
    }
    /**
     * @param longf the longf to set
     */
    public void setLongf(double longf) {
        this.longf = longf;
    }
    /**
     * @return the nome
     */
    public String getNome() {
        return nome;
    }
    /**
     * @param nome the nome to set
     */
    public void setNome(String nome) {
        this.nome = nome;
    }
    /**
     * @return the id
     */
    public String getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }


}



