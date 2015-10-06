package com.example.usuario.gps_app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MainActivity extends ActionBarActivity {

        Button botonObtener;
        Button botonPedir;
        TextView labelHora;
        EditText textBoxObtener;
        EditText textBoxTelefono;
        String num_predet;
        GoogleMap mapa;//mapa con el que se trabajará



    @Override
        protected void onCreate(Bundle savedInstanceState) { //lo que hace la aplicación al iniciar
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main); //carga la view en el activity
            botonObtener=(Button)findViewById(R.id.btn_posicion); //establecemos las referencias de los elementos de la view en una variable para gestionar
            botonPedir=(Button)findViewById(R.id.btn_peticion); //botonPedir.setEnabled(false);
            textBoxObtener=(EditText)findViewById(R.id.editText2);
            labelHora=(TextView)findViewById(R.id.LabelHora);
            textBoxTelefono=(EditText)findViewById(R.id.editText);
            num_predet="615934713"; //número predeterminado que aparecerá al iniciar
            textBoxObtener.setText(num_predet);//se establece en la caja de texto un número predeterminado
            textBoxTelefono.setText(num_predet);
            mapa = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            mapa.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();

            //noinspection SimplifiableIfStatement
            if (id == R.id.action_settings) {
                return true;
            }

            return super.onOptionsItemSelected(item);
        }

        /**
         * Método que obtiene las coordenadas a partir del mensaje de
         * la propia bandeja del teléfono.
         * Se debe de introducir un número válido en el text box que lo acompaña
         * */
        public void onClickObtener(View view){
            String mensaje=obtenerMensaje(textBoxObtener.getText().toString());//guardamos el mensaje en una variable
            if(textBoxObtener.getText().length()<9){//comprueba si lo que hay en la caja de texto tiene menos caracteres que un número telefónico
                Toast.makeText(getApplicationContext(),
                        "Debe introducir un número de teléfono",
                        Toast.LENGTH_LONG).show();
                textBoxObtener.setBackgroundColor(Color.rgb(255,150,150));//indica que está mal lo que ahi se escribio
            }else {
                if (textBoxObtener.getText().length() >= 10) {//si el número introducido es mayor que un número de teléfono
                    Toast.makeText(getApplicationContext(),
                            "El número debe de ser válido (menor de 9 dígitos)" + mensaje.length(),
                            Toast.LENGTH_LONG).show();
                    textBoxObtener.setBackgroundColor(Color.rgb(255,150,150));
                } else {//si el número de teléfono es correcto
                    textBoxObtener.setBackgroundColor(getResources().getColor(android.R.color.transparent));//quitamos el color erróneo del campo de texto
                    try {//inicia la captura de excepciones
                        String hora = mensaje.substring(0, 9); //obtenemos la hora del mensaje
                        String lon = mensaje.substring((mensaje.lastIndexOf("Lon:") + 4), (mensaje.lastIndexOf("Lon:") + 19));//+2 si extendida
                        String lat = mensaje.substring((mensaje.lastIndexOf("Lat:") + 4), (mensaje.lastIndexOf("Lat:") + 18));//+2 //Obtenemos latitud y longitud del mensaje
                        String lon_orientacion = lon.substring(lon.length() - 1);//Obtenemos el carácter que indica cual es la horientacion W/E-S/N
                        String lat_orientacion = lat.substring(lat.length() - 1);

                        lon=lon.replaceAll("[^0-9]", ""); //limpiamos las Strings correspondiente a las coordenadas de cualquier caracter que no sea un dígito
                        lat=lat.replaceAll("[^0-9]", "");

                        int lonGrados=Integer.parseInt(lon.substring(0,2)); //se obtienen los grados, es decir, los dos primeros dígitos de esa string de dígitos
                        float lonMinutos=Float.parseFloat(lon.substring(2))/1000000;//se obtienen los minutos, y como previamente al purgar de caracteres no numéricos la String quitamos el punto decimal, se lo incorporamos dividiendo
                        int latGrados=Integer.parseInt(lat.substring(0,2));
                        float latMinutos=Float.parseFloat(lat.substring(2))/1000000;

                        double longitud=conversionGradosDecimal(lonGrados,lonMinutos);//Obtenemos las coordenadas reales en decimal a través de un método de conversión
                        double latitud=conversionGradosDecimal(latGrados,latMinutos);

                       if (lon_orientacion.equalsIgnoreCase("W")) {
                            longitud = longitud * (-1);//adaptamos el símbolo de las coordenadas numéricas dependiendo de la orientación
                        }
                        if (lat_orientacion.equalsIgnoreCase("S")) {
                            latitud = latitud * (-1);
                        }

                        obtenerPosicion(latitud, longitud, hora); //llamamos al método de obtener posición

                        this.labelHora.setText("Hora de la localización: " + hora + "\n Coordenadas: Lat " + conversionDecimalGrados(latitud) + "Lon " + conversionDecimalGrados(longitud));//actualizamos la label con la hora y las coordenadas  lat 43.176224 lon-8.203902  diferencia de lat 0.117502 y long 0.136005
                }catch(StringIndexOutOfBoundsException e){//Si el número no tiene mensajes o el último mensaje guardado no es del formato establecido, captura el error.
                        Toast.makeText(getApplicationContext(),
                                "Ese número no tiene un mensaje de coordenadas",
                                Toast.LENGTH_LONG).show();
                    }
                    }
                }

        }


    /**
     * Método que calcula a partir de grados y minutos su valor decimal para
     * Google Maps
     * */
    public double conversionGradosDecimal( double grados, double minutos )
    {
        return grados + (minutos/60);
    }

    public String conversionDecimalGrados(double coordenadas){
        int segundos = (int)Math.round(coordenadas * 3600);
        int grados = segundos / 3600;
        segundos = Math.abs(segundos % 3600);
        int minutos = segundos / 60;
        segundos %= 60;

        return grados+"º "+minutos+"' "+segundos+"\"";
    }


        /**
         * Enviamos un mensaje para que el dispositivo GPS nos envie un mensaje con
         * coordenadas. Escribimos el número al que queremos enviar
         * */
        public void onClickPedir(View view){
            if(textBoxTelefono.getText().length()<9){//como en el anterior
                Toast.makeText(getApplicationContext(),
                        "Debe introducir un número de teléfono",
                        Toast.LENGTH_LONG).show();
                textBoxTelefono.setBackgroundColor(Color.rgb(255,150,150));
            }else{
               if(textBoxTelefono.getText().length()>=10){
                    Toast.makeText(getApplicationContext(),
                            "El número debe de ser válido (menor de 9 dígitos)",
                            Toast.LENGTH_LONG).show();
                    textBoxTelefono.setBackgroundColor(Color.rgb(255,150,150));
                }else {

                    sendSMS(textBoxTelefono.getText().toString());//llamamos al método para enviar mensajes y le pasamos el número

                    textBoxObtener.setText(textBoxTelefono.getText().toString());//actualiamos las cajas para que tengan el mismo número
                    textBoxTelefono.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                }
            }


        }


        /**
         * Envía un mensaje a un número que pasamos como parámetro
         * */
        protected void sendSMS(String numero) {
            String toPhoneNumber = "+34"+numero;//número adaptado y ubicado en españa
            String smsMessage ="OP";//mensaje a enviar
            try {
                SmsManager smsManager = SmsManager.getDefault();//crea un objeto SmsManager con una instancia predeterminada
                smsManager.sendTextMessage(toPhoneNumber, null, smsMessage, null, null);//se envía un mensaje a un numero dado
                Toast.makeText(getApplicationContext(), "SMS enviado.",
                        Toast.LENGTH_LONG).show();
            } catch (Exception e) {//si captura alguna excepción el mensaje no se envía
                Toast.makeText(getApplicationContext(),
                        "Envío de SMS fallido.",
                        Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }

        /**
         * Obtiene el último mensaje en la bandeja de entrada del móvil
         * para un número que le pasemos como parámetro
         * */
        protected String obtenerMensaje(String numero){
            ContentResolver contentResolver = getContentResolver();//Instanciamos un objeto que gestiona los contenidos asociados al contexto de la aplicación
            String[] phoneNumber=new String[]{"+34"+numero};//El número de teléfono que queremos (array de string porque pueden ser varios)
            Cursor cursor1 = contentResolver.query(Uri.parse("content://sms/inbox"), new String[] { "_id", "thread_id", "address", "person", "date","body", "type" }, "address=?", phoneNumber, null);
            //Un cursor que recoge cada uno de los mensajes que el content resolver nos pilla de la carpeta dada para el número dado

            int indexBody = cursor1.getColumnIndex("body");
            //En el cursor los valores se almacenan tal que asi: {([identificador]=valor),([identficador2],valor2)}.
            //Esto lo que hace es pillar el valor asociado a la columna que se llama "body" donde irá el cuerpo del mensaje
            if (indexBody < 0 || !cursor1.moveToFirst())//si no hay nada
            {
                return "Nada";
            }
            else{
                return cursor1.getString(indexBody);//pilla el primer body que encuente. El del primer mensaje en la bandeja de entrada, es decir el último por fecha

            }
        }


    /**
     * Método que a partir de dos valores equivalentes a una medida de longitud y latitud
     * situa el mapa en una posición y añade un marcador
     * */
    public void obtenerPosicion(double latitud, double longitud, String hora){

        LatLng posicion = new LatLng(latitud, longitud);//Objeto de Google maps que equivale a la latitud y longitud (+-gg.mmssss)
        CameraPosition posicionCamara = new CameraPosition.Builder()//Objeto de cámara
                .target(posicion)   //Centramos el mapa en la posicion
                .zoom(19)         //Establecemos el zoom en 19
                .bearing(45)      //Establecemos la orientación con el noreste arriba
                .tilt(10)         //Bajamos el punto de vista de la cámara 10 grados
                .build();

        CameraUpdate camara =
                CameraUpdateFactory.newCameraPosition(posicionCamara);//actualizamos la cámara para que haga el movimiento
        mapa.addMarker(new MarkerOptions()//añadimos un nuevo marcador al mapa en la posición que le dimos.
                .position(posicion)
                .title("posicion")
                .snippet(hora));
        mapa.animateCamera(camara);//animamos la cámara con todos los datos anteriores

    }

}


