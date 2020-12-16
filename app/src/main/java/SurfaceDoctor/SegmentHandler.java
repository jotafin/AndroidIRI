package SurfaceDoctor;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Environment;
import android.util.JsonReader;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.internal.JsonReaderInternalAccess;
import com.northbridgeanalytics.mysensors.R;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import api.IRIService;
import bmodel.IRI;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SegmentHandler {


    // salvar dados no firebase e recuperar a referencia para a raiz do firebase
    private DatabaseReference referencia = FirebaseDatabase.getInstance().getReference("/segmenthandler");

    // retrofit
    private Retrofit retrofit;

    // Parâmetros de entrada do usuário padrão.
    private boolean units = true;
    private int maxDistance = 100;
    private int maxSpeed = 8000;
    private int minSpeed = 20;

    private Context context;
    private SensorManager sensorManager;

    private String uniqueID = UUID.randomUUID().toString();

    private long accelerometerStartTime = 0;
    private long accelerometerStopTime = 0;
    private List<SurfaceDoctorPoint> surfaceDoctorPoints = new ArrayList<SurfaceDoctorPoint>();

    private float[] gravity = new float[3];
    private float[] magnetometer = new float[3];

    private float alpha = 0.8f;

    private boolean hasLocationPairs = false;
    private Location endPoint;
    private ArrayList<double[]> segmentCoordinates = new ArrayList<>();

    private float totalAccumulatedDistance = 0.0f;

    // Necessário para criar um evento.
    private SurfaceDoctorInterface listener;
    public void setSomeEventListener (SurfaceDoctorInterface listener) {
        this.listener = listener;
    }

    /**
     * Construtores
     *
     * @param context
     */
    public SegmentHandler(Context context, SensorManager sm) {

        this.context = context;
        sensorManager = sm;
    }

    public void savingDataOnFirebase() {
        //Carregando conteudo do geogson
        String jsonContent = "{\"type\": \"FeatureCollection\", \"features\": [{\"type\": \"Feature\", \"geometry\":" +
                "{ \"type\": \"LineString\",\"coordinates\":[[-36.49172275, -8.90747133], [-36.49179834, -8.90752881], " +
                "[-36.49187166, -8.90759071], [-36.49195073, -8.90766026], [-36.49203731, -8.90773655], [-36.49212759, -8.90780258], " +
                "[-36.49221615, -8.90787286], [-36.49231192, -8.90795723], [-36.49239277, -8.90804266], [-36.49248593, -8.90811959], " +
                "[-36.49257855, -8.90820414]]},\"properties\": {\"ID\":626a4a4b-9a4c-4210-b6fe-acf7e24c6d47," +
                "\"DISTANCE\":124.35908508300781,\"IRIphoneX\":0.6622430940111134,\"IRIphoneY\":0.3742534399297271," +
                "\"IRIphoneZ\":0.6920740324454874,\"IRIearthX\":0.0,\"IRIearthY\":0.0,\"IRIearthZ\":0.0}}]}";

        //referencia.keepSynced(true);
        // acessando o nó padrao do firebase
        referencia.child(String.valueOf(R.xml.preferences));
        referencia.push().setValue(jsonContent);
    }


    /**
     * Método para definir as configurações do SurfaceDoctor que foram definidas pelo usuário.
     *
     * @param inputUnits
     * @param inputSegmentDistance
     * @param inputMaxSpeed
     * @param inputMinSpeed
     */
    public void setSurfaceDoctorPreferences(boolean inputUnits, int inputSegmentDistance,
                                            int inputMaxSpeed, int inputMinSpeed) {
        // TODO: Isso precisa ser implementado.
        units = inputUnits;
        maxDistance = inputSegmentDistance;
        maxSpeed = inputMaxSpeed;
        minSpeed = inputMinSpeed;
    }


    /**
     * Método para receber dados do acelerômetro dos sensores.
     *
     * @param sensorEvent
     */
    public void setSurfaceDoctorAccelerometer(SensorEvent sensorEvent) {

        // CONFIGURAÇÃO DO RETROFIT E DO CONVERSOR - usando o retrofit para api rest
        retrofit = new Retrofit.Builder()
                .baseUrl("http://191.252.223.6.xip.io:10000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();


        // Precisam de tempo entre os eventos do acelerômetro para calcular o IRI.
        // TODO: Verify what time this is.
        accelerometerStartTime = accelerometerStopTime;
        accelerometerStopTime = sensorEvent.timestamp;

        // Verifica se temos pares de locais e horários de início e parada. Nesse caso, começamos a coletar objetos SurfaceDoctorPoint.
        // hasLocationPairs é definido como true pelo método setSurfaceDoctorLocation quando o evento de localização foi gravado.
        if ( hasLocationPairs && accelerometerStartTime > 0 ) {

            float[] inputAccelerometer = sensorEvent.values.clone();

            // Filtro Low-pass para isolar a gravidade.
            float[] adjustedGravity = new float[3];

            adjustedGravity[0] = alpha * gravity[0] + (1 - alpha) * inputAccelerometer[0];
            adjustedGravity[1] = alpha * gravity[1] + (1 - alpha) * inputAccelerometer[1];
            adjustedGravity[2] = alpha * gravity[2] + (1 - alpha) * inputAccelerometer[2];

            // Filtro High-pass para remover a gravidade.
            float[] linearAccelerationPhone = new float[3];

            linearAccelerationPhone[0] = inputAccelerometer[0] - adjustedGravity[0];
            linearAccelerationPhone[1] = inputAccelerometer[1] - adjustedGravity[1];
            linearAccelerationPhone[2] = inputAccelerometer[2] - adjustedGravity[2];

            // Converter acelerômetro do sistema de coordenadas do telefone para o sistema de coordenadas da terra.
            float[] linearAccelerationEarth = VectorAlgebra.earthAccelerometer(
                    linearAccelerationPhone, magnetometer,
                    gravity, sensorManager);

            // Para cada medida do acelerômetro, crie um objeto SurfaceDoctorPoint.
            SurfaceDoctorPoint surfaceDoctorPoint = new SurfaceDoctorPoint(
                    uniqueID,
                    linearAccelerationPhone,
                    linearAccelerationEarth,
                    gravity,
                    magnetometer,
                    accelerometerStartTime,
                    accelerometerStopTime);
            surfaceDoctorPoints.add(surfaceDoctorPoint);
        }
    }


    /**
     * Recebe o retorno do sensor de Gravidade do Android
     *
     * @param sensorEvent
     */
    public void setSurfaceDoctorGravity(SensorEvent sensorEvent ) {
        gravity = sensorEvent.values.clone();
    }

    public void setSurfaceDoctorMagnetometer(SensorEvent sensorEvent) { magnetometer = sensorEvent.values.clone(); }


    /** Método para receber dados de localização do sensor GPS.
     *
     * @param inputLocation
     */
    public void setSurfaceDoctorLocation(Location inputLocation) {

        // Se tivermos um par de objetos de localização, vamos percorrer nossa lógica.
        if (endPoint != null) {

            // Vamos primeiro dizer aos nossos sensores do acelerômetro para começar a somar dados.
            hasLocationPairs = true;

            // Agora vamos transformar o ponto atual no ponto antigo e atualizar o ponto atual com o novo ponto.
            // Usaremos esses pares de locais para extrair dados posteriormente.
            Location startPoint = endPoint;
            endPoint = inputLocation;

            // Estamos registrando, vamos processar os dados.
            executeSurfaceDoctor(startPoint, endPoint );

            // TODO: Precisamos criar um evento que permita à MainActivity saber se estamos dentro da velocidade, registro etc. Isso também pode ser tratado no lado da MainActivity.
            double lineBearing = inputLocation.getBearing();

        } else {
            // Este é o nosso primeiro ponto, nossa lógica depende da comparação de dois objetos de localização, então não vamos fazer nada
            // até chegarmos ao segundo local.
            endPoint = inputLocation;
        }
    }


    /**
     * Este é o principal manipulador lógico do SegmentHandler.
     *
     *  Isso é acionado em cada retorno de chamada de localização GPS do Android.
     */
    private void executeSurfaceDoctor(Location locationStart, Location locationEnd) {

        // A distância aproximada em metros.
        float lineDistance = locationStart.distanceTo(locationEnd);

        // A velocidade em m / s
        float lineSpeed = locationEnd.getSpeed();

        // Longitude / Latitude dos pares de Localização.
        double[] coordinatesStart = new double[]{ locationStart.getLongitude(), locationStart.getLatitude() };
        double[] coordinatesLast = new double[]{ locationEnd.getLongitude(), locationEnd.getLatitude() };

        // Estamos dentro da velocidade e não atingimos o final de um segmento, vamos adicionar a distância entre os pares de coordenadas à distância total do segmento.
        if ( isWithinSpeed( lineSpeed ) && !isSegmentEnd() ) {
            // Anexa a distância entre os pontos de coordenadas à distância total.
            totalAccumulatedDistance += lineDistance;
            // Anexa as novas coordenadas ao ArrayList para que possamos criar uma polilinha mais tarde.
            segmentCoordinates.add(coordinatesStart);

            Log.i("IRI", "Distance: " + totalAccumulatedDistance);
        }
        // Estamos acelerando e chegamos ao final de um segmento, vamos finalizar o segmento.
        else if ( isWithinSpeed( lineSpeed ) && isSegmentEnd() ) {
            // Anexa a distância entre os pontos de coordenadas à distância total.
            totalAccumulatedDistance += lineDistance;
            // Anexa as novas coordenadas ao ArrayList para que possamos criar uma polilinha mais tarde.
            segmentCoordinates.add(coordinatesStart);
            // O segmento está pronto, adicione a última coordenada.
            segmentCoordinates.add(coordinatesLast);

            // Este é o fim de um segmento, vamos enviá-lo para finalização.
            finalizeSegment(uniqueID, totalAccumulatedDistance, segmentCoordinates, surfaceDoctorPoints);

            // Vamos redefinir para o próximo segmento.
            // TODO: Isso é seguro aqui?
            resetSegment(false);
        }
        // Ultrapassamos nosso limite de velocidade, precisamos fazer uma redefinição definitiva.
        // TODO: Perderíamos os pares de coordenadas ou os dados do acelerômetro.
        else if ( !isWithinSpeed( lineSpeed )) {
            // A reinicialização completa também limpará seus pares de locais.
            resetSegment(true );
        }
        else {
            Log.i("SEG", "A condition was met that we didn't think about. ");
        }
    }


    /**
     *  Finaliza um segmento de estrada
     *
     *  Executa quando o limite de distância do segmento foi atingido.      *
     */
    private void finalizeSegment(String id ,double distance, ArrayList<double[]> polyline, List<SurfaceDoctorPoint> measurements) {

        // Cria o cabeçalho para a tabela de saída.
        StringBuilder tableString = new StringBuilder("id,AccPhoneX,AccPhoneY,AccPhoneZ,AccEarthX,AccEarthY,AccEarthZ,");
        tableString.append("GravityX,GravityY,GravityZ,");
        tableString.append("MagnetX,MagnetY,MagnetZ,");
        tableString.append("Created,sStart,sStop,Distance");
        tableString.append(System.getProperty("line.separator"));

        double[] totalVerticalDisplacementPhone = new double[3];
        double[] totalVerticalDisplacementEarth = new double[3];

        // Primeiro, obtenha o deslocamento vertical total do segmento no sistema de coordenadas da Terra e do Telefone.
        for (int i = 0; i < measurements.size(); i++ ) {
            int previousIndex = i - 1;

            // Anexa o SurfaceDoctorPoint como uma linha em tableString para que possamos gerar uma tabela posteriormente.
            tableString.append(measurements.get(i).getRowString());
            tableString.append(", " + distance);
            tableString.append(System.getProperty("line.separator"));

            // Deslocamento vertical é igual ao valor absoluto do deslocamento longitudinal atual menos o anterior
            // deslocamento longitudinal.
            if ( previousIndex >= 0 ) {

                totalVerticalDisplacementPhone[0] += Math.abs( measurements.get(i).getVertDissX(false) - measurements.get(previousIndex).getVertDissX(false) );
                totalVerticalDisplacementPhone[1] += Math.abs( measurements.get(i).getVertDissY(false) - measurements.get(previousIndex).getVertDissY(false) );
                totalVerticalDisplacementPhone[2] += Math.abs( measurements.get(i).getVertDissZ(false) - measurements.get(previousIndex).getVertDissZ(false) );

                totalVerticalDisplacementEarth[0] += Math.abs( measurements.get(i).getVertDissX(true) - measurements.get(previousIndex).getVertDissX(true) );
                totalVerticalDisplacementEarth[1] += Math.abs( measurements.get(i).getVertDissY(true) - measurements.get(previousIndex).getVertDissY(true) );
                totalVerticalDisplacementEarth[2] += Math.abs( measurements.get(i).getVertDissZ(true) - measurements.get(previousIndex).getVertDissZ(true) );
            }
        }

        // IRI (mm/m) = (deslocamento vertical total * 1000) / distância do segmento.
        // TODO: É necessário permitir que o usuário produza IRI em mm / m ou m / km.
        double[] totalIRIPhone = new double[3];
        double[] totalIRIEarth = new double[3];

        totalIRIPhone[0] = (totalVerticalDisplacementPhone[0] * 1000) / distance;
        totalIRIPhone[1] = (totalVerticalDisplacementPhone[1] * 1000) / distance;
        totalIRIPhone[2] = (totalVerticalDisplacementPhone[2] * 1000) / distance;

        totalIRIEarth[0] = (totalVerticalDisplacementEarth[0] * 1000) / distance;
        totalIRIEarth[1] = (totalVerticalDisplacementEarth[1] * 1000) / distance;
        totalIRIEarth[2] = (totalVerticalDisplacementEarth[2] * 1000) / distance;

        /////// tentando media - QUALQUER coisa excluir essa parte ate embaixo do string iriMedia
        double irimedia;
        double t1, t2, t3;
        t1 = totalIRIPhone[0];
        t2 = totalIRIPhone[1];
        t3 = totalIRIPhone[2];

        irimedia = (t1 + t2 + t3)/3;
        // COnvertendo a media para string
        String iriMedia = Double.toString(irimedia);
        String test = null;
        ///////////////////

        Log.i("IRI", "Xphone " + totalIRIPhone[0] + " Yphone " + totalIRIPhone[1] + " Zphone " + totalIRIPhone[2] +
                " XEarth " + totalIRIEarth[0] + " YEarth " + totalIRIEarth[1] + " ZEarth " + totalIRIEarth[2]);
        // Vamos passar os dados do segmento para o SurfaceDoctorEvent para que possam ser usados na atividade principal.
        // O SurfaceDoctorEvent será acionado na MainActivity.
        if (listener != null) {
            SurfaceDoctorEvent e = new SurfaceDoctorEvent();
            e.type = "TYPE_SEGMENT_IRI";
            e.x = totalIRIPhone[0];
            e.y = totalIRIPhone[1];
            e.z = totalIRIPhone[2];
            e.distance = distance;
            // TODO: atribua saída à classe SurfaceDoctorEvent.
            listener.onSurfaceDoctorEvent(e);
        }
        saveResults(id, distance, totalIRIPhone, totalIRIEarth, polyline, tableString.toString());

       // String jsonContent =" coordenadas"+ segmentCoordinates.toString() + " id: "+ id + " distancia: "+ distance + " TotalIRIPhone0: " + totalIRIPhone[0] + " TotalIRIPhone1: " + totalIRIPhone[1] + " TotalIRIPhone2: " + totalIRIPhone[2] + " TotalIRIEarth0: "+ totalIRIEarth[0] +" TotalIRIEarth1: "+ totalIRIEarth[1] + " TotalIRIEarth2: "+ totalIRIEarth[2] +" Polypine: " + polyline + " tableString: " + tableString.toString();
           String jsonContent ="IRI TOTAL: " + iriMedia ;
            referencia.push().setValue(jsonContent);

        // Enviando para a API
        salvarIRI(iriMedia, test, id);
        // (iriMedia, id, test) parte adicionada, qlqr coisa so deixar o () vazio, "()"

    }
    // RETROFIT SALVANDO IRI
    private void salvarIRI(String iriMedia, String test, String id ){
        //configura objeto IRI

      IRI iri = new IRI(iriMedia, test, id );
        //recupera o servico e salva a postagem
        IRIService service = retrofit.create(IRIService.class);
        Call<IRI> call = service.salvarIRI(iri);

        call.enqueue(new Callback<IRI>() {
            @Override
            public void onResponse(Call<IRI> call, Response<IRI> response) {
                if( response.isSuccessful()){
                    IRI iriResposta = response.body();

                }
            }

            @Override
            public void onFailure(Call<IRI> call, Throwable t) {

            }
        });


    }

    private void saveResults(String id, double distance, double[] phoneIRI, double[] earthIRI, ArrayList<double[]> polyline, String table) {
        // TODO: em vez de um arquivo por segmento, acrescente vários segmentos a um arquivo.

        // Verifique se temos acesso ao armazenamento externo?
        if ( isExternalStorageWritable() ) {
            // TODO: salva o arquivo como geoJSON ou EsriJSON.

            // Adiciona resultados ao GeoJSON.

            // Cria uma string geoJSON
            // Tenta usar JSONobject e JSONArray, mas não conseguiu se livrar das aspas nas coordenadas. Não tinha
            // acesso à biblioteca GSON devido a permissões de rede.
            StringBuilder str = new StringBuilder("{\"type\": \"FeatureCollection\", \"features\": [");
            str.append("{\"type\": \"Feature\", \"geometry\":{ \"type\": \"LineString\",");
            str.append("\"coordinates\":" + Arrays.deepToString(polyline.toArray()) + "},");
            str.append("\"properties\": {");
            str.append("\"ID\":" + id + ",");
            str.append("\"DISTANCE\":" + distance + ",");
            str.append("\"IRIphoneX\":" + phoneIRI[0] + ",");
            str.append("\"IRIphoneY\":" + phoneIRI[1] + ",");
            str.append("\"IRIphoneZ\":" + phoneIRI[2] + ",");
            str.append("\"IRIearthX\":" + earthIRI[0] + ",");
            str.append("\"IRIearthY\":" + earthIRI[1] + ",");
            str.append("\"IRIearthZ\":" + earthIRI[2] + "}}");
            str.append("]}");

            // Vamos salvar a string geoJSON.
            File file = getPrivateStorageDirectory(context, String.valueOf(accelerometerStartTime) + ".geojson");
            try {
                file.createNewFile();

                OutputStreamWriter fstream = new OutputStreamWriter( new FileOutputStream(file), StandardCharsets.UTF_16);
                fstream.write(str.toString());
                fstream.flush();
                fstream.close();
            } catch (IOException e) {
                Log.e("ERROR", "Failed to create file with error:\n");
            }

            // Vamos salvar a tabela como um arquivo txt com o ".csv"
            byte[] tableBytes = table.getBytes();
            File tableFile = getPrivateStorageDirectory(context, String.valueOf(accelerometerStartTime) + ".csv");
            try {
                FileOutputStream fos = new FileOutputStream(tableFile);
                try {
                    fos.write(tableBytes);
                    fos.close();
                } catch (IOException e) {
                    Log.e("ERROR", "IO exception");
                }
            } catch (FileNotFoundException e) {
                Log.e("ERROR", "File not found");
            }

        } else {
            // TODO: Se não houver acesso ao armazenamento externo, vamos salvar em interno até obtermos acesso.

        }

    }

    private boolean isWithinSpeed( float inputSpeed ) {
        // TODO: É necessário manipular quais unidades o usuário selecionou.
        float mph = inputSpeed * 2.23694f;
        return minSpeed <= mph && mph <= maxSpeed;
    }


    private boolean isSegmentEnd() {
        // TODO: Precisa lidar com as unidades de entrada do usuário.
        return totalAccumulatedDistance >= maxDistance;
    }


    /**
     * Redefine todos os parâmetros.
     *
     * Usado para reiniciar o registro de um segmento de estrada.
     *
     * @param hardReset boolean
     */
    private void resetSegment(boolean hardReset) {

        // Obtem novo ID de segmento
        uniqueID = UUID.randomUUID().toString();

        // Redefine a distância do segmento para zero.
        totalAccumulatedDistance = 0;

        // Limpa todas as medições do acelerômetro no ArrayList.
        surfaceDoctorPoints.clear();

        // Limpa a lista de coordenadas que formam a polilinha.
        segmentCoordinates.clear();

        // As redefinições rígidas são usadas quando um sensor perde a conectividade ou passa um limite.
        if ( hardReset ) {
            hasLocationPairs = false;
            endPoint = null;
            accelerometerStopTime = 0;
        }
    }


    /* Verifica se o armazenamento externo está disponível para leitura e gravação */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /** Cria um arquivo vazio para o qual pode ser gravado.
     *
     * @param context
     * @param fileName
     * @return
     */
    private File getPrivateStorageDirectory(Context context, String fileName) {
        File file = new File(context.getExternalFilesDir("geojson"), fileName);
        return file;
    }
}