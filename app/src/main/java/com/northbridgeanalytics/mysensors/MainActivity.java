package com.northbridgeanalytics.mysensors;

// Fontes
// https://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
// https://google-developer-training.gitbooks.io/android-developer-advanced-course-practicals/content/unit-1-expand-the-user-experience/lesson-3-sensors/3-2-p-working-with-sensor-based-orientation/3-2-p-working-with-sensor-based-orientation.html
// https://stackoverflow.com/questions/5464847/transforming-accelerometers-data-from-devices-coordinates-to-real-world-coordi
// https://stackoverflow.com/questions/23701546/android-get-accelerometers-on-earth-coordinate-system
// https://stackoverflow.com/questions/11578636/acceleration-from-devices-coordinate-system-into-absolute-coordinate-system

import SurfaceDoctor.SegmentHandler;
import SurfaceDoctor.SurfaceDoctorEvent;
import SurfaceDoctor.SurfaceDoctorInterface;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;

import androidx.fragment.app.FragmentManager;

import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.sax.ElementListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;

import api.IRIService;
import bmodel.IRI;
import bmodel.ListaIRI;
import myAlerts.AlertDialogGPS;
import SurfaceDoctor.VectorAlgebra;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.osmdroid.api.IMapController;
import org.osmdroid.api.IMapView;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.*;
import org.osmdroid.bonuspack.utils.PolylineEncoder;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.HEREWeGoTileSource;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.LogRecord;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static java.security.AccessController.getContext;

// TODO: https://blog.fossasia.org/comparing-different-graph-view-libraries-and-integrating-them-in-pslab-android-application/


public class MainActivity extends AppCompatActivity
        implements SensorEventListener, LocationListener, SurfaceDoctorInterface {

    // salvar dados no firebase e recuperar a referencia para a raiz do firebase
    private DatabaseReference referencia = FirebaseDatabase.getInstance().getReference();

    // autenticando usuario
    //private FirebaseAuth usuario = FirebaseAuth.getInstance();

    // Código de retorno de chamada para permissões de GPS.
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private FragmentManager fm = getSupportFragmentManager();

    private float[] adjustedGravity = new float[3];
    private float[] linear_acceleration = new float[3];


    private MapView osm;
    private MapController mc;
    // retrofit
    private Retrofit retrofit;
    private List<ListaIRI> listaIri = new ArrayList<>();

    // Valores muito pequenos para o acelerômetro (nos três eixos) devem ser interpretados como 0. Este valor é a quantidade de desvio diferente de zero aceitável.
    private static final float VALUE_DRIFT = 0.05f;

    // TextViews para exibir os valores atuais do sensor.
    private TextView TextSensorPhoneAccX;
    private TextView TextSensorPhoneAccY;
    private TextView TextSensorPhoneAccZ;

    private TextView TextSensorEarthAccX;
    private TextView TextSensorEarthAccY;
    private TextView TextSensorEarthAccZ;

    private TextView TextSensorPhoneAzimuth;
    private TextView TextSensorPhonePitch;
    private TextView TextSensorPhoneRoll;

    // Instância do gerenciador de sensores do sistema.
    private SensorManager SensorManager;
    private LocationManager locationManager;
    private SegmentHandler segmentHandler;

    // Sensores de acelerômetro e magnetômetro, conforme recuperados do gerenciador de sensores.
    private Sensor SensorAccelerometer;
    private Sensor SensorMagnetometer;
    private Sensor SensorGravity;

    // Variáveis para manter os valores atuais do sensor.
    private float[] AccelerometerData = new float[3];
    private float[] MagnetometerData = new float[3];
    private float[] GravityData = new float[3];

    // Variáveis para manter os valores atuais da localização.
    private double currentLatitude;
    private double currentLongitude;

    // Botão para alternar o registro do GPS.
    private Button toggleRecordingButton;
    private boolean isToggleRecordingButtonClicked = false;

    //  Clique para alternar o registro do GPS.
    private View.OnClickListener toggleRecordingListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // salvarIRI();
            recuperarIRI();
            // TODO: O usuário precisa de feedback visual do estado atual do botão.
            // TODO: Os booleanos precisam ser movidos para o final da função. Eles podem ser um retorno da função?
            if (!isToggleRecordingButtonClicked) {
                toggleRecordingClickedOn();
            } else {
                toggleRecordingClickedOff();
            }
        }
    };


    //******************************************************************************************************************
    //                                            Ciclo de Vida da Atividade
    //******************************************************************************************************************

    private File getPrivateStorageDirectory(String fileName) {
        File file = new File(fileName);
        return file;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        //retrofit
        retrofit = new Retrofit.Builder()
                .baseUrl("http://191.252.223.6.xip.io:10000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /*
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

        */
        // Cadastro de usuario
        // usuario.createUserWithEmailAndPassword();
/*
        // O código verifica se o aplicativo tem permissões para acessar a internet e escrever no armazenamento externo, se o usuário ainda não tiver cedido as permissões será aberta uma janela de diálogo padrão do Android para solicitar as permissões.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                String[] permissoes = {Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissoes, 1);
            }
        }
*/
        osm = (MapView) findViewById(R.id.mapView);
        osm.setTileSource(TileSourceFactory.OpenTopo);
        osm.setBuiltInZoomControls(true);
        osm.setMultiTouchControls(true);
        osm.setTilesScaledToDpi(true);

        GeoPoint pontoInicial = new GeoPoint(-8.9068, -36.4913);
        mc = (MapController) osm.getController();
        mc.setZoom(15);
        mc.setCenter(pontoInicial);

        // Adicionando o OpenStreetMap
        //Pega o mapa adicionada no arquivo activity_main.xml
        //MapView mapa = findViewById(R.id.mapView);
        //Fonte de imagens
        //mapa.setTileSource(TileSourceFactory.OpenTopo);
        //mapa.setTilesScaledToDpi(true);
        // mapa.setMultiTouchControls(true);

        //Cria um ponto de referência com base na latitude e longitude
        // GeoPoint pontoInicial = new GeoPoint(-8.9068, -36.4913);
        //IMapController mapController = osm.getController();
        //Faz zoom no mapa
        //mapController.setZoom(15);
        //Centraliza o mapa no ponto de referência
        //mapController.setCenter(pontoInicial);

        //Cria um marcador no mapa, o ponto inicial
        Marker startMarker = new Marker(osm);
        startMarker.setPosition(pontoInicial);
        startMarker.setTitle("Ponto Inicial");
        //Posição do ícone
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        osm.getOverlays().add(startMarker);

        // locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //quando mudar a localização, ele vai atualizar
        // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0, this);
/*
      osm = (MapView) findViewById(R.id.mapView);

        osm = (MapView) findViewById(R.id.mapView);
        osm.setTileSource(TileSourceFactory.MAPNIK);
        osm.setBuiltInZoomControls(true);
        osm.setMultiTouchControls(true);

        mc = (MapController) osm.getController();
        mc.setZoom(12);


        GeoPoint center = new GeoPoint(0,0);
        mc.animateTo(center);
        addMarker(center);
            */
        //RETROFIT
        recuperarListasIRIRetrofit();

        // Bloqueia a orientação para retrato
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Obtem a visualização do botão de gravação inicial.
        toggleRecordingButton = findViewById(R.id.toggleRecording);

        // Define quando clicar para a visualização do botão iniciar a gravação.
        toggleRecordingButton.setOnClickListener(toggleRecordingListener);

        // Obtem os TextViews que mostrarão os valores do sensor.
        TextSensorPhoneAccX = (TextView) findViewById(R.id.phone_acc_x);
        TextSensorPhoneAccY = (TextView) findViewById(R.id.phone_acc_y);
        TextSensorPhoneAccZ = (TextView) findViewById(R.id.phone_acc_z);
        TextSensorEarthAccX = (TextView) findViewById(R.id.earth_acc_x);
        TextSensorEarthAccY = (TextView) findViewById(R.id.earth_acc_y);
        TextSensorEarthAccZ = (TextView) findViewById(R.id.earth_acc_z);
        TextSensorPhoneAzimuth = (TextView) findViewById(R.id.phone_azimuth);
        TextSensorPhonePitch = (TextView) findViewById(R.id.phone_pitch);
        TextSensorPhoneRoll = (TextView) findViewById(R.id.phone_roll);


        // Obtem sensores acelerômetro e magnetômetro do gerenciador de sensores.
        // O método getDefaultSensor () retornará nulo se o sensor não estiver disponível no dispositivo.
        SensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);
        SensorAccelerometer = SensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        SensorMagnetometer = SensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD);
        SensorGravity = SensorManager.getDefaultSensor(
                Sensor.TYPE_GRAVITY);

        // Obtem o Gerenciador de localização.
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        Log.i("Activity", "OnCreate has fired");
    }

    // Adicionando um marcador no openStreetMap
    /*public void addMarker(GeoPoint center){
        Marker marker = new Marker(osm);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        osm.getOverlays().clear();
        osm.getOverlays().add(marker);
        osm.invalidate();
    }
*/
    // RETROFIT SALVANDO IRI
    /*
    private void salvarIRI(){
        //configura objeto IRI
       // IRI iri = new IRI("0.0012312", "testeAPPANDROID", "testando" );
        //recupera o servico e salva a postagem
        IRIService service = retrofit.create(IRIService.class);
       // Call<IRI> call = service.salvarIRI(iri);

         // call.enqueue(new Callback<IRI>() {
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


    }*/
                                        // RETROFIT
    public void recuperarIRI(){
        IRIService iriService = retrofit.create(IRIService.class);
        Call<IRI> call = iriService.recuperarIRI();

        //criar a requisição
        call.enqueue(new Callback<IRI>() {
            @Override
            public void onResponse(Call<IRI> call, Response<IRI> response) {
                if( response.isSuccessful() ){
                    IRI iri = response.body();
                }
            }

            @Override
            public void onFailure(Call<IRI> call, Throwable t) {

            }
        });
    }

    private void recuperarListasIRIRetrofit(){
        IRIService service = retrofit.create(IRIService.class);
        Call<List<ListaIRI>> call = service.recuperarListaIRI();

        call.enqueue(new Callback<List<ListaIRI>>() {
            @Override
            public void onResponse(Call<List<ListaIRI>> call, Response<List<ListaIRI>> response) {
                if( response.isSuccessful()){
                    listaIri = response.body();

                    for (int i=0; i<listaIri.size(); i++){
                        ListaIRI listaIRI = listaIri.get( i );
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ListaIRI>> call, Throwable t) {

            }
        });
    }
    /**
     * Os ouvintes dos sensores são registrados neste retorno de chamada para que
     * eles podem não ser registrados em onStop ().
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Os ouvintes dos sensores são registrados neste retorno de chamada e
        // pode ser não registrado em onStop ().
        //
        // Verifica se os sensores estão disponíveis antes de registrar os ouvintes.
        // Ambos os ouvintes são registrados com uma quantidade "normal" de atraso
        // (SENSOR_DELAY_NORMAL).
        // TODO: Precisa de uma caixa de diálogo informando que os sensores não estão disponíveis.
        if (SensorAccelerometer != null) {
            SensorManager.registerListener(this, SensorAccelerometer,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (SensorMagnetometer != null) {
            SensorManager.registerListener(this, SensorMagnetometer,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (SensorManager != null) {
            SensorManager.registerListener(this, SensorGravity,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        Log.i("Activity", "OnStart has fired");
    }

    @Override
    protected void onResume() {
        super.onResume();


        Log.i("Activity", "onResume has fired");

    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.i("Activity", "onPause has fired");
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Cancela o registro de todos os ouvintes do sensor nesse retorno de chamada para que eles não continuem usando os recursos quando o aplicativo for parado.
        SensorManager.unregisterListener(this);

        Log.i("Activity", "OnStop has fired");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i("Activity", "onDestroy has fired");
    }


    //******************************************************************************************************************
    //                                           PERMISSÕES E CONFIGURAÇÕES
    //******************************************************************************************************************

    // Para de registrar quando o usuário desligar o GPS.
    private void toggleRecordingClickedOff() {
        // Desativa as atualizações do LocationListener.
        locationManager.removeUpdates(this);
        isToggleRecordingButtonClicked = false;

        // O usuário não deseja mais gravar o IRI, então vamos excluí-lo.
        segmentHandler = null;

    }


    // O usuário ativou o registro GPS.
    private void toggleRecordingClickedOn() {

        // Verifica se temos permissão para usar o GPS e solicite-o, se não o fizermos.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Não temos permissões, é melhor perguntar.
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            // MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION é uma constante inteira que usaremos para procurar o
            // resultado dessa solicitação no retorno de chamada onRequestPermissionsResult ().

        } else {
            // Já temos permissão, então vamos ativar o GPS.
            System.out.println("===> ATIVANDO O GPS");
            enableGPS();
        }
    }


    /**
     * Retorno de chamada do Android para uma resposta às solicitações de permissão.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // Se a solicitação for cancelada, as matrizes de resultados estarão vazias.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Pede permissão ao usuário e ele diz que sim.
                    enableGPS();

                } else {
                    // ele disse que não.

                    // Como nada resultou do pressionamento do botão, vamos torná-lo para falso.
                    isToggleRecordingButtonClicked = false;

                    // TODO: Algo precisa acontecer se eles negarem permissões.
                    // Permissão negada! Desative a funcionalidade que depende dessa permissão.
                }
                return;
            }
 /*       super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // Se a solicitação de permissão foi cancelada o array vem vazio.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permissão cedida, recria a activity para carregar o mapa, só será executado uma vez
                    this.recreate();

                }
*/
        }
    }


    // Depois de obter permissão, ativa o GPS.
    private void enableGPS() {
        System.out.println("INICIANDO GPSSSS!");
        // Verifica se o usuário tem o GPS ativado.
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            // Temos permissão, mas o GPS não está ativado. Pergunte ao usuário se ele gostaria de acessar as configurações de localização.
            AlertDialogGPS gpsSettings = new AlertDialogGPS();
            gpsSettings.show(fm, "Alert Dialog");

            // O GPS não foi ativado com o pressionar do botão, por isso, verifique se ele ainda é falso.
            isToggleRecordingButtonClicked = false;

        } else {

            // Tem permissão e o GPS está ativado, vamos começar a registrar.
            // Registra o ouvinte no Gerenciador de localização para receber atualizações de localização apenas do GPS.
            // O segundo parâmetro controla o intervalo de tempo mínimo entre as notificações e o terceiro é a alteração mínima na
            // distância entre notificações - definir ambos como zero quando solicita notificações de localização com a maior frequência possível.
            System.out.println("TENTANDO OBTER PERMISSAO");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            System.out.println("PERMISSÃO CONCEDIDA");
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0, this);

            // Iniciado com sucesso o registro do GPS, define o botão como clicado.
            isToggleRecordingButtonClicked = true;

            // Estamos prontos para iniciar o registro, vamos criar um novo objeto SegmentHandler.
            segmentHandler = new SegmentHandler(this, SensorManager);
            segmentHandler.setSomeEventListener(this);
            System.out.println("TENTANDO SALVAR DADOS EXTERNAMENTE");
            //segmentHandler.savingDataOnFirebase();
        }
    }


    //******************************************************************************************************************
    //                                                INÍCIO  DA BARRA DO APLICATIVO
    //******************************************************************************************************************

    /**
     * Adiciona entradas à barra de ação.
     * Adiciona todas as entradas, como configurações, ao menu suspenso da barra de ação.
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_toolbar_menu, menu);
        return true;
    }

    /**
     * Retorno de chamada de itens da barra de aplicativos
     *
     * Esse método é chamado quando o usuário seleciona um dos itens da barra de aplicativos e passa um objeto de Item de Menu para indicar
     * qual item foi clicado. O ID retornado de MenutItem.getItemId () corresponde ao ID que você declarou para a barra de aplicativos
     * em item res/menu/ <- menu.xml->
     *
     * @param item MenuItem retorna o objeto para indicar qual item foi clicado. Usa MenuItem.getItemId () para obter valor.
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // O usuário escolhe o item Configurações, mostra a interface do usuário das configurações do aplicativo.

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.preferenceFragment, new SettingsFragment())
                        .addToBackStack(null)
                        .commit();

                return true;

            default:
                // Se chegar aqui, a ação do usuário não foi reconhecida.
                // Invoca a superclasse para lidar com isso.
                return super.onOptionsItemSelected(item);
        }
    }



    // TODO: O que é isso? É assim que você obtém as preferências que estavam nas configurações de algum modo.
    private void getLoggingSettings() {

        SharedPreferences settings = getDefaultSharedPreferences(this);

        // Obtenha configurações sobre o tipo de arquivo.
        boolean isEsriJASON = settings.getBoolean("preference_filename_json", false);
        String loggingFilePrefix = settings.getString("preference_filename_prefix", "IRICalculado");

        // Obtem as configurações sobre as variáveis de log.
        boolean loggingUnits = settings.getBoolean("preference_logging_units", true);
        int maxLoggingDistance = Integer.parseInt(
                settings.getString("preference_logging_distance", "1000"));
        int maxLoggingSpeed = Integer.parseInt(
                settings.getString("preference_logging_max_Speed", "80"));
        int minLoggingSpeed = Integer.parseInt(
                settings.getString("preference_logging_min_speed", "20"));

    }

    //******************************************************************************************************************
    //                                            INÍCIA O RETORNO DOS SENSORES
    //******************************************************************************************************************

    //*********************************************   Acelerômetro  ***************************************************

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();

        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:

                // TODO: Os dados devem ser removidos desse método o mais rápido possível.
                AccelerometerData = sensorEvent.values.clone();

                float alpha = 0.8f;

                // Filtro low-pass para isolar a gravidade.
                adjustedGravity[0] = alpha * GravityData[0] + (1 - alpha) *  AccelerometerData[0];
                adjustedGravity[1] = alpha * GravityData[1] + (1 - alpha) *  AccelerometerData[1];
                adjustedGravity[2] = alpha * GravityData[2] + (1 - alpha) *  AccelerometerData[2];

                // Filtro High-pass para remover a gravidade.
                linear_acceleration[0] =  AccelerometerData[0] - adjustedGravity[0];
                linear_acceleration[1] =  AccelerometerData[1] - adjustedGravity[1];
                linear_acceleration[2] =  AccelerometerData[2] - adjustedGravity[2];


                // O objeto segmentHandler é criado no método enableGPS () quando o usuário pressiona o botão de  log de início
                // Se o objeto segmentHandler existir, significa que temos permissões de localização, o GPS é
                // ativado e precisamos passar o acelerômetro SensorEvent para o SegmentHandler.
                if (segmentHandler != null) {
                    segmentHandler.setSurfaceDoctorAccelerometer(sensorEvent);
                }
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                MagnetometerData = sensorEvent.values.clone();

                if ( segmentHandler != null ) {
                    segmentHandler.setSurfaceDoctorMagnetometer(sensorEvent);
                }
                break;
            case Sensor.TYPE_GRAVITY:
                GravityData = sensorEvent.values.clone();

                if ( segmentHandler != null ) {
                    segmentHandler.setSurfaceDoctorGravity(sensorEvent);
                }

                break;
            default:
                return;
        }


        // Obtem os valores do acelerômetro do telefone no sistema de coordenadas da Terra.
        //
        // X = Leste / Oeste
        // Y = Norte / Sul
        // Z = Para cima / Para baixo
        float[] earthAcc = VectorAlgebra.earthAccelerometer(
                linear_acceleration, MagnetometerData,
                GravityData, SensorManager);

        // TODO: Também precisamos de dados do acelerômetro no sistema de coordenadas do usuário, onde y está sempre em frente.

        // Obtem a orientação do telefone - em radianos.
        float[] phoneOrientationValuesRadians = VectorAlgebra.phoneOrientation(
                AccelerometerData, MagnetometerData, SensorManager);

        // A orientação do telefone é dada em radianos, vamos converter isso em graus.
        double[] phoneOrientationValuesDegrees = VectorAlgebra.radiansToDegrees(phoneOrientationValuesRadians);


        // Exibe os dados do acelerômetro do telefone na View.
        TextSensorPhoneAccX.setText(getResources().getString(
                R.string.value_format, linear_acceleration[0]));
        TextSensorPhoneAccY.setText(getResources().getString(
                R.string.value_format, linear_acceleration[1]));
        TextSensorPhoneAccZ.setText(getResources().getString(
                R.string.value_format, linear_acceleration[2]));

        // Exibe os dados do acelerômetro do telefone no sistema de coordenadas da Terra.
        TextSensorEarthAccX.setText(getResources().getString(
                R.string.value_format, earthAcc[0]));
        TextSensorEarthAccY.setText(getResources().getString(
                R.string.value_format, earthAcc[1]));
        TextSensorEarthAccZ.setText(getResources().getString(
                R.string.value_format, earthAcc[2]));

        // Exibe os dados de orientação do telefone na View.
        TextSensorPhoneAzimuth.setText(getResources().getString(
                R.string.value_format, phoneOrientationValuesDegrees[0]));
        TextSensorPhonePitch.setText(getResources().getString(
                R.string.value_format, phoneOrientationValuesDegrees[1]));
        TextSensorPhoneRoll.setText(getResources().getString(
                R.string.value_format, phoneOrientationValuesDegrees[2]));

    }


    /**
     * Retorno do Android para alteração da precisão do acelerômetro.
     * <p>
     * Deve ser implementado para satisfazer a interface SensorEventListener;
     * não utilizado neste aplicativo.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {


    }


    //*********************************************  Localização   *******************************************************

    /**
     * Retorno do Android para localização GPS
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        GeoPoint center = new GeoPoint(location.getLatitude(), location.getLongitude());

        MapView mapa = findViewById(R.id.mapView);
        Marker atualizado = new Marker(mapa);
        atualizado.setPosition(center);
        atualizado.setTitle("Atual");
        //Posição do ícone
        atualizado.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapa.getOverlays().add(atualizado);

        // O objeto segmentHandler é criado no método enableGPS () quando o usuário pressiona o log de início
        // Se o objeto segmentHandler existir, significa que temos permissões de localização, o GPS é
        // ativado e precisamos passar a localização do GPS para o SegmentHandler.
        if (segmentHandler != null) {
            segmentHandler.setSurfaceDoctorLocation(location);
        }
    }


    // Chamado quando o status do provedor é alterado. Esse método é chamado quando um provedor não consegue buscar um local
    // ou se o provedor ficou disponível recentemente após um período de indisponibilidade.
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i("Location",  "onSatusChanged fired");
    }


    // Chamado quando o provedor é ativado pelo usuário
    @Override
    public void onProviderEnabled(String provider) {
        // TODO: remove a mensagem de que o aplicativo não funcionará com o GPS desativado.
        Log.i("Location", "onProviderEnabled fired");
    }


    // Chamado quando o fornecedor é desativado pelo usuário. Se requestLocationUpdates for chamado em um já desativado
    // provedor, esse método é chamado imediatamente.
    @Override
    public void onProviderDisabled(String provider) {
        // TODO: adiciona a mensagem de que o aplicativo não funcionará com o GPS desativado e solicite a ativação.

        // Temos permissão, mas o GPS está desativado. Permite solicitar ao usuário para ativá-lo.
        enableGPS();

    }


    //******************************************************************************************************************
    //                                            Surface Doctor
    //******************************************************************************************************************


    /**
     * Evento de SegmentHandler
     *
     * @param surfaceDoctorEvent
     */
    @Override
    public void onSurfaceDoctorEvent(SurfaceDoctorEvent surfaceDoctorEvent) {
        String surfaceDoctorEventType = surfaceDoctorEvent.getType();

        switch (surfaceDoctorEventType) {
            case "TYPE_SEGMENT_IRI":
                TextView x = findViewById(R.id.last_IRI_x);
                TextView y = findViewById(R.id.last_IRI_y);
                TextView z = findViewById(R.id.last_IRI_z);

                x.setText(Double.toString(surfaceDoctorEvent.x));
                y.setText(Double.toString(surfaceDoctorEvent.y));
                z.setText(Double.toString(surfaceDoctorEvent.z));
        }
    }

    //Pegar rotas
    public void getRoute(View view){
        EditText etOrigin = findViewById(R.id.origin);
        EditText etDestination = findViewById(R.id.destination);
        final String o = etOrigin.getText().toString();
        final String d = etDestination.getText().toString();

        new Thread(){
            public void run(){
                GeoPoint start = getLocation(o);
                GeoPoint end = getLocation(d);

                if(start != null && end != null){
                    drawRoute(start, end);
                }else{
                    Toast.makeText(MainActivity.this, "FAIL!", Toast.LENGTH_SHORT);
                }
            }
        }.start();
    }

    //ROtas
    public GeoPoint getLocation(String location){
        Geocoder gn = new Geocoder(MainActivity.this);
        GeoPoint gp = null;
        List<Address> al = new ArrayList<Address>();

        try{
            al = gn.getFromLocationName(location, 1);

            if(al != null && al.size() > 0){
                Log.i("Script", "Rua: "+ al.get(0).getThoroughfare());
                // nome de cidade
                Log.i("Script", "Cidade: "+ al.get(0).getSubAdminArea());
                // nome do Estado
                Log.i("Script", "Estado: "+ al.get(0).getAdminArea());
                // nome do país
                Log.i("Script", "País: "+ al.get(0).getCountryName());
                gp = new GeoPoint(al.get(0).getLatitude(), al.get(0).getLongitude());
            }
        }catch (IOException e ){e.printStackTrace();}

        return (gp);
    }
    //sugestaõ de rota no mapa
    public void drawRoute(GeoPoint start, GeoPoint end){
        RoadManager roadManager = new OSRMRoadManager(this);
        ArrayList<GeoPoint> point = new ArrayList<GeoPoint>();
        point.add(start);
        point.add(end);
        Road road = roadManager.getRoad(point);
        final Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                osm.getOverlays().add(roadOverlay);
            }
        });
    }
}