package com.example.apptablet

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.tapadoo.alerter.Alerter
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var session : SessionManager
    lateinit var pageHandler : Handler
    lateinit var someHandler : Handler
    lateinit var codeHandler : Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        val btnCalendar = findViewById<ImageView>(R.id.btnCalendar)
        btnCalendar.setOnClickListener {
            someHandler.removeCallbacksAndMessages(null);
            val openCalendar = Intent(applicationContext, calendarView::class.java)
            startActivity(openCalendar)
        }

        /* Evento do botão das opções, abrir dialog para verificar PIN e aceder a pagina das opções */
        val btnSettings = findViewById<ImageView>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.dialog_pinview, null)
            val digitsPin = dialogView.findViewById<EditText>(R.id.digitsPin)

            dialog.setView(dialogView)
            dialog.setCancelable(true)
            val customDialog = dialog.create()
            customDialog.show()

            digitsPin.setOnKeyListener(object : View.OnKeyListener {
                override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                validarPin(object: VolleyCallBack {
                                    override fun onSuccess() {
                                        pageHandler.removeCallbacksAndMessages(null);
                                        someHandler.removeCallbacksAndMessages(null);
                                        codeHandler.removeCallbacksAndMessages(null);
                                        customDialog.dismiss()
                                        val i = Intent(applicationContext, Settings::class.java)
                                        startActivity(i)
                                    }

                                    override fun onFailure() {
                                        digitsPin.error = "Dados de acesso inválidos!"
                                    }

                                }, digitsPin.text.toString())
                                return true
                            }
                            else -> {}
                        }
                    }
                    return false
                }
            })
        }

        /* Possibilidade de atualizar o ecra puxando para baixo do lado esquerdo */
        val swipe : SwipeRefreshLayout = findViewById(R.id.swipeRefresh)
        swipe.setOnRefreshListener {
            currentState()
            minutosReserva()
            isAvaiable()
            updateHeader()
            alertaSuccess(this@MainActivity, "Sucesso", getString(R.string.sucesso_atualizar))
        }

    }

    interface VolleyCallBack {
        fun onSuccess()
        fun onFailure()
    }

    private fun validarPin(callback: VolleyCallBack?, pin: String) {

        val url = appSettings.URL_validar_pin + session.getTablet()
        val params = HashMap<String, String>()
        params["pin"] = pin
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), { response ->try {

            val success = response.getBoolean("success")
            if(success) callback?.onSuccess()
            else callback?.onFailure();

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure() })
        requestQueue.add(request)
    }

    /* Gerir funções de autorefresh */
    fun loopFunctions(){
        pageHandler = Handler(mainLooper)
        pageHandler.postDelayed(object : Runnable {
            override fun run() {
                currentState()
                pageHandler.postDelayed(this, appSettings.appReload)
            }
        }, 10)
    }

    fun reloadQR(){
        codeHandler = Handler(mainLooper)
        codeHandler.postDelayed(object : Runnable {
            override fun run() {
                minutosReserva()
                codeHandler.postDelayed(this, appSettings.reloadQR)
            }
        }, 10)
    }

    /* Verificar se uma sala esta disponivel ou ocupada */
    fun currentState(){
        val requestQueue = Volley.newRequestQueue(this)
        val postURL = appSettings.URL_estado_sala + session.getSala()
        val strRequest: StringRequest = object : StringRequest(
            Method.GET, postURL,
            Response.Listener { response ->
                if(response.toBoolean()){
                    pageHandler.removeCallbacksAndMessages(null);
                    var i = Intent(this, ocupadoActivity::class.java)
                    startActivity(i)
                }
            },
            Response.ErrorListener { error ->
                alertaDanger(this@MainActivity, "Atenção", getString(R.string.erro_geral))
                Log.i("MainActivity", error.toString())
            }) {
        }
        requestQueue.add(strRequest)
    }

    /* Obter minutos até a proxima reserva para gerar QRCODE para reserva rapida */
    fun minutosReserva(){
        val url = appSettings.URL_minutos_disponiveis + session.getSala()
        Log.i("Main", url)
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {
            if(response.getBoolean("status")){
                Log.i("Main", response.toString())
                var limit = 0
                if(response.has("limit") && !response.isNull("limit"))
                    limit = response.getInt("limit")

                /* Gerar QRCODES */
                val infoAviso = findViewById<TextView>(R.id.infoAviso)
                val titleQRCODE = findViewById<TextView>(R.id.titleQRCODE)
                val card = findViewById<MaterialCardView>(R.id.card)
                val reservaRapida = findViewById<LinearLayout>(R.id.reservaRapida)
                val msg = findViewById<TextView>(R.id.textView)
                reservaRapida.visibility = View.GONE
                msg.visibility = View.GONE
                titleQRCODE.visibility = View.VISIBLE
                infoAviso.visibility = View.VISIBLE
                card.visibility = View.VISIBLE

                val hours = limit / 60; //since both are ints, you get an int
                val minutes = limit % 60;

                when (hours) {
                    0 -> infoAviso.text = HtmlCompat.fromHtml("A sala será reservada por <strong>$minutes</strong> minutos", HtmlCompat.FROM_HTML_MODE_LEGACY)
                    1 -> infoAviso.text = HtmlCompat.fromHtml("A sala será reservada por <strong>1</strong> hora e <strong>$minutes</strong> minutos", HtmlCompat.FROM_HTML_MODE_LEGACY)
                    2 -> infoAviso.text = HtmlCompat.fromHtml("A sala será reservada por <strong>2</strong> horas", HtmlCompat.FROM_HTML_MODE_LEGACY)
                    else -> infoAviso.text = HtmlCompat.fromHtml("A sala será reservada por <strong>$hours</strong> horas e <strong>$minutes</strong> minutos", HtmlCompat.FROM_HTML_MODE_LEGACY)
                }

                try {
                    val nsala = session.getSala()
                    val currentData = LocalDate.now()
                    val currentTime = LocalTime.now()
                    val formatterData = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val formatterTime = DateTimeFormatter.ofPattern("HH:mm")
                    val formattedData = currentData.format(formatterData).toString()
                    val formattedTime = currentTime.format(formatterTime).toString()
                    val fim = currentTime.plusMinutes(limit.toLong())
                    val formattedFim = fim.format(formatterTime).toString()

                    val url = "{\"nsala\": \"$nsala\", \"datareserva\": \"$formattedData\", \"horainicio\": \"$formattedTime\", \"horafim\": \"$formattedFim\"}"
                    val barcodeEncoder = BarcodeEncoder()
                    val bitmap = barcodeEncoder.encodeBitmap(url, BarcodeFormat.QR_CODE, 700, 700)
                    val imageViewQrCode =  findViewById<ImageView>(R.id.ImgQRCODE)
                    imageViewQrCode.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    alertaDanger(this@MainActivity, "Atenção", getString(R.string.erro_gerar_qrcode))
                }
            }else{
                val infoAviso = findViewById<TextView>(R.id.infoAviso)
                val titleQRCODE = findViewById<TextView>(R.id.titleQRCODE)
                val card = findViewById<MaterialCardView>(R.id.card)
                titleQRCODE.visibility = View.GONE
                infoAviso.visibility = View.GONE
                card.visibility = View.GONE
                val reservaRapida = findViewById<LinearLayout>(R.id.reservaRapida)
                reservaRapida.visibility = View.VISIBLE
                val msg = findViewById<TextView>(R.id.textView)

                val motivo = response.getString("motivo")
                if(motivo == "estabelecimento") msg.text = getString(R.string.nao_reseravel_estabelecimento)
                else msg.text = getString(R.string.nao_reservavel_tempo)

                msg.visibility = View.VISIBLE
            }
            val swipe : SwipeRefreshLayout = findViewById(R.id.swipeRefresh)
            swipe.isRefreshing = false
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace().toString() })
        requestQueue.add(request)
    }

    fun ClockSystem(){
        /* Atualizar horas cada 10 segundos */
        val dataHora = findViewById<TextView>(R.id.dataHora)
        someHandler = Handler(mainLooper)
        someHandler.postDelayed(object : Runnable {
            override fun run() {
                TimeZone.setDefault(TimeZone.getTimeZone("Europe/Lisbon"))
                val sdf = SimpleDateFormat("HH:mm EEEE, dd MMMM yyyy", Locale("pt", "PT"))
                val date = sdf.format(Date())

                val words = date.toString().split(" ").toMutableList()
                var output = ""
                for(word in words){
                    output += word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + " "
                }
                output = output.trim()
                dataHora.text = output
                someHandler.postDelayed(this, 10000)
            }
        }, 10)
    }

    fun isAvaiable() {

        val url = appSettings.URL_get_sala + session.getSala()

        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

            if(response.getBoolean("success")){
                val sala = response.getJSONObject("data")
                val estado = sala.getInt("estadosala")

                if(estado == 0){
                    pageHandler.removeCallbacksAndMessages(null);
                    someHandler.removeCallbacksAndMessages(null);
                    codeHandler.removeCallbacksAndMessages(null);
                    val i = Intent(this, redirActivities::class.java)
                    startActivity(i)
                }

            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error ->
            Log.i("Main", error.toString())
        })
        requestQueue.add(request)
    }

    fun updateHeader(){
        try{
            val salas: HashMap<String, String> = session.getSalaDetails()
            val nomesala = salas[SessionManager.KEY_NOMESALA]
            val lugares = salas[SessionManager.KEY_LUGARES]

            val topBarSala = findViewById<TextView>(R.id.textViewSala)
            val topBarLugares = findViewById<TextView>(R.id.textViewLugares)
            topBarSala.text = nomesala.toString()
            topBarLugares.text = lugares.toString() + " Lugares"

        }catch (e:Exception){
            Log.e("Session", "Não foi possivel obter os dados na sessao")
        }
    }

    /* Verificar se a backend esta online, caso contrario mostrar aviso de erro */
    fun callBackend(){
        val url = appSettings.callBackend
        val requestQueue = Volley.newRequestQueue(applicationContext)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error ->
            val i = Intent(applicationContext, redirActivities::class.java)
            startActivity(i)
        })
        requestQueue.add(request)
    }

    override fun onPause() {
        super.onPause()
        Log.i("MainActivity", "Disp-Pause: Reloader Destruido")
        pageHandler.removeCallbacksAndMessages(null);
        someHandler.removeCallbacksAndMessages(null);
        codeHandler.removeCallbacksAndMessages(null);
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("MainActivity", "Disp-Destroy: Reloader Destruido")
        pageHandler.removeCallbacksAndMessages(null);
        someHandler.removeCallbacksAndMessages(null);
        codeHandler.removeCallbacksAndMessages(null);

    }

    override fun onResume() {
        super.onResume()
        Log.i("MainActivity", "Disp-Resume: Reloader Reiniciado")
        ClockSystem()
        loopFunctions()
        reloadQR()

        updateHeader()
        callBackend()

    }

}