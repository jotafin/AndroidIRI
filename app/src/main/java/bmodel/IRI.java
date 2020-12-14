package bmodel;

public class IRI {

    private String iri;
    private String endereco;
    private String nome;
    private Long id;

    public IRI(String iri, String endereco, String nome) {
        this.iri = iri;
        this.endereco = endereco;
        this.nome = nome;
    }

    public String getIri() {
        return iri;
    }

    public void setIri(String iri) {
        this.iri = iri;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
