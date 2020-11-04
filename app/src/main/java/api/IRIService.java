package api;

import bmodel.IRI;
import retrofit2.Call;
import retrofit2.http.GET;

public interface IRIService {
    @GET("pessoa-controlador/get")
    Call<IRI> recuperarIRI();
}
