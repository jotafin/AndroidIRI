package api;

import java.util.List;

import bmodel.IRI;
import bmodel.ListaIRI;
import okhttp3.MediaType;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface IRIService {
    @GET("/get")
    Call<IRI> recuperarIRI();

    @GET("/endereco/{endereco}")
    Call<List<ListaIRI>> recuperarListaIRI();

    @POST("/post")
    Call<IRI>salvarIRI(@Body IRI iri);

    @FormUrlEncoded
    @POST("/post")
    Call<IRI>salvarIRI(
            @Field("iri") String iri,
            @Field("endereco") String endereco,
            @Field("nome") String nome
    );

}
