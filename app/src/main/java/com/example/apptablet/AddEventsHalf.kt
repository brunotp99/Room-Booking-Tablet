package com.example.apptablet

import android.content.Intent
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.apptablet.databinding.ActivityAddEventsHalfBinding
import com.example.apptablet.databinding.ActivityRegistroDispositivoBinding
import com.example.apptablet.ui.theme.Azul600
import com.example.apptablet.ui.theme.Azul800
import com.example.apptablet.ui.theme.Vermelho
import com.example.apptablet.ui.theme.WeekScheduleTheme
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.tapadoo.alerter.Alerter
import nl.joery.timerangepicker.TimeRangePicker
import org.json.JSONException
import org.json.JSONObject
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*

class AddEventsHalf : AppCompatActivity() {

    data class Reservas(val nreserva: Int, val datareserva: String, val horainicio: String, val horafim: String, val estadoreserva: Int, val nomeuser: String, val estadouser: Int, val nomesala: String)
    var listReservas = ArrayList<Reservas>()

    private lateinit var obterEventos : ArrayList<Event>
    lateinit var pageHandler : Handler
    lateinit var clockHandler : Handler
    lateinit var finishHandler : Handler
    lateinit var session : SessionManager

    private var setInicio = "00:00"
    private var setFim = "00:00"

    private lateinit var binding : ActivityAddEventsHalfBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEventsHalfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        /* Criação do time picker */
        val picker = findViewById<TimeRangePicker>(R.id.picker)
        picker.thumbSize = 32.px
        picker.sliderWidth = 32.px
        picker.sliderColor = android.graphics.Color.rgb(233, 233, 233)
        picker.thumbColor = android.graphics.Color.TRANSPARENT
        picker.thumbIconColor = android.graphics.Color.WHITE
        picker.sliderRangeGradientStart = android.graphics.Color.parseColor("#2d63ed")
        picker.sliderRangeGradientMiddle = null
        picker.sliderRangeGradientEnd = android.graphics.Color.parseColor("#0099ff")
        picker.thumbSizeActiveGrow = 1.0f
        picker.clockFace = TimeRangePicker.ClockFace.SAMSUNG
        picker.clockVisible = false
        picker.hourFormat = TimeRangePicker.HourFormat.FORMAT_12
        picker.startTimeMinutes  = 9 * 60
        picker.endTimeMinutes = 10 * 60 + 30

        updateTimes()
        updateDuration()

        picker.setOnTimeChangeListener(object : TimeRangePicker.OnTimeChangeListener {
            override fun onStartTimeChange(startTime: TimeRangePicker.Time) {
                updateTimes()
            }

            override fun onEndTimeChange(endTime: TimeRangePicker.Time) {
                updateTimes()
            }

            override fun onDurationChange(duration: TimeRangePicker.TimeDuration) {
                updateDuration()
            }
        })

        picker.setOnDragChangeListener(object : TimeRangePicker.OnDragChangeListener {
            override fun onDragStart(thumb: TimeRangePicker.Thumb): Boolean {
                if(thumb != TimeRangePicker.Thumb.BOTH) {
                    animate(thumb, true)
                }
                return true
            }

            override fun onDragStop(thumb: TimeRangePicker.Thumb) {
                if(thumb != TimeRangePicker.Thumb.BOTH) {
                    animate(thumb, false)
                }

                Log.d(
                    "TimeRangePicker",
                    "Start time: " + picker.startTime
                )
                Log.d(
                    "TimeRangePicker",
                    "End time: " + picker.endTime
                )
                Log.d(
                    "TimeRangePicker",
                    "Total duration: " + picker.duration
                )
            }
        })

        /* Codigo AddEvent */
        val textField = findViewById<TextInputLayout>(R.id.textField)
        val items = obterLista()
        val adapter = ArrayAdapter(applicationContext, R.layout.dropdown_item, items)
        (textField.editText as? AutoCompleteTextView)?.setAdapter(adapter)

        binding.diaDropdown.addTextChangedListener {

            if(LocalDate.now().dayOfWeek.toString() == obterIngles(it.toString())){
                picker.startTimeMinutes  = LocalTime.now().hour * 60 + LocalTime.now().minute
                picker.endTimeMinutes = (LocalTime.now().hour + 1) * 60 + LocalTime.now().minute

                picker.sliderRangeGradientStart = android.graphics.Color.parseColor("#2d63ed")
                picker.sliderRangeGradientMiddle = null
                picker.sliderRangeGradientEnd = android.graphics.Color.parseColor("#0099ff")

                updateTimes()
                updateDuration()

            }else{
                picker.startTimeMinutes  = 9 * 60
                picker.endTimeMinutes = 10 * 60 + 30

                picker.sliderRangeGradientStart = android.graphics.Color.parseColor("#2d63ed")
                picker.sliderRangeGradientMiddle = null
                picker.sliderRangeGradientEnd = android.graphics.Color.parseColor("#0099ff")

                updateTimes()
                updateDuration()
            }
            Log.i("Add", it.toString())
            Log.i("Add", LocalDate.now().dayOfWeek.toString())
            Log.i("Add", obterIngles(it.toString()))
        }

        val qrEsconde = findViewById<LinearLayout>(R.id.qrEsconde)
        qrEsconde.visibility = View.GONE

        val btnFinalizar = findViewById<Button>(R.id.btnFinalizar)
        btnFinalizar.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }

        val btnFechar = findViewById<ImageView>(R.id.btnArrow)
        btnFechar.setOnClickListener {
            finish()
        }

        /* Gerar o QRCODE e verificar as opções selecionadas pelo utilizador */
        var btnGerar = findViewById<Button>(R.id.btnGerar)
        btnGerar.setOnClickListener {

            val selectedValue = (textField.editText as AutoCompleteTextView).text

            if( LocalTime.parse(setInicio).isAfter(LocalTime.parse(setFim)) ){
                alertaDanger(this@AddEventsHalf, "Atenção", getString(R.string.erro_intervalo_invalido))
                return@setOnClickListener
            }else if(selectedValue.toString() == ""){
                alertaDanger(this@AddEventsHalf, "Atenção", getString(R.string.erro_dia_vazio))
                return@setOnClickListener
            }else if(LocalTime.parse(setInicio).isBefore(LocalTime.parse(resources.getString(R.string.horario_abertura)))){
                alertaDanger(this@AddEventsHalf, "Atenção", getString(R.string.erro_hora_inicio) + resources.getString(R.string.horario_abertura))
                return@setOnClickListener
            }else if(LocalTime.parse(setFim).isAfter(LocalTime.parse(resources.getString(R.string.horario_fecho)))){
                alertaDanger(this@AddEventsHalf, "Atenção", getString(R.string.erro_hora_fim) +  resources.getString(R.string.horario_fecho))
                return@setOnClickListener
            }

            val usWeek = ThisLocalizedWeek(Locale.FRANCE)
            val datareserva = LocalDate.parse(usWeek.firstDay.toString()).plusDays(obterIndexDia(selectedValue.toString()).toLong())
            validarSobreposicao(setInicio, setFim, datareserva.toString(), session.getSala().toString())

        }

    }

    /* Obter lista para o dropdown dos dias que podem ser agendados esta semana */
    fun obterLista() : ArrayList<String> {
        val day = LocalDate.now().dayOfWeek.name
        val DaysArray = ArrayList<String>()
        if(day == "MONDAY"){
            DaysArray.add("Segunda")
            DaysArray.add("Terça")
            DaysArray.add("Quarta")
            DaysArray.add("Quinta")
            DaysArray.add("Sexta")
        }else if(day == "TUESDAY"){
            DaysArray.add("Terça")
            DaysArray.add("Quarta")
            DaysArray.add("Quinta")
            DaysArray.add("Sexta")
        }else if(day == "WEDNESDAY"){
            DaysArray.add("Quarta")
            DaysArray.add("Quinta")
            DaysArray.add("Sexta")
        }else if(day == "THURSDAY"){
            DaysArray.add("Quinta")
            DaysArray.add("Sexta")
        }else{
            DaysArray.add("Sexta")
        }
        return DaysArray
    }

    /* Converter a lista de dias reservais para inteiro */
    fun obterIndexDia(dia : String) : Int{
        when(dia){
            "Segunda" -> return 0
            "Terça" -> return 1
            "Quarta" -> return 2
            "Quinta" -> return 3
            "Sexta" -> return 4
            else -> return 0
        }
    }

    fun obterIngles(dia : String) : String{
        when(dia){
            "Segunda" -> return "MONDAY"
            "Terça" -> return "TUESDAY"
            "Quarta" -> return "WEDNESDAY"
            "Quinta" -> return "THURSDAY"
            "Sexta" -> return "FRIDAY"
            else -> return ""
        }
    }

    /* Atualizar os horarios no centro do picker */
    private fun updateTimes() {
        val end_time = findViewById<TextView>(R.id.end_time)
        val start_time = findViewById<TextView>(R.id.start_time)
        val picker = findViewById<TimeRangePicker>(R.id.picker)
        end_time.text = picker.endTime.toString()
        start_time.text = picker.startTime.toString()

        if(picker.startTime.toString().count() == 4)
            setInicio = "0" + picker.startTime.toString()
        else setInicio = picker.startTime.toString()

        if(picker.endTime.toString().count() == 4)
            setFim = "0" + picker.endTime.toString()
        else setFim = picker.endTime.toString()

    }

    /* Atualizar mensagem de duração da reserva */
    private fun updateDuration() {
        val picker = findViewById<TimeRangePicker>(R.id.picker)
        val duration = findViewById<TextView>(R.id.duration)
        if(session.getIntervalo() != 0)
            duration.text = "Duração: " + picker.duration  + " + " + session.getIntervalo().toString() + " para limpezas"
        else  duration.text = "Duração: " + picker.duration
    }

    private fun animate(thumb: TimeRangePicker.Thumb, active: Boolean) {
        val bedtime_layout = findViewById<LinearLayout>(R.id.bedtime_layout)
        val wake_layout = findViewById<LinearLayout>(R.id.wake_layout)
        val activeView = if(thumb == TimeRangePicker.Thumb.START) bedtime_layout else wake_layout
        val inactiveView = if(thumb == TimeRangePicker.Thumb.START) wake_layout else bedtime_layout
        val direction = if(thumb == TimeRangePicker.Thumb.START) 1 else -1

        activeView
            .animate()
            .translationY(if(active) (activeView.measuredHeight / 2f)*direction else 0f)
            .setDuration(300)
            .start()
        inactiveView
            .animate()
            .alpha(if(active) 0f else 1f)
            .setDuration(300)
            .start()
    }

    private val Int.px: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    interface VolleyCallBack {
        fun onSuccess()
    }

    /* Obter listagem de reservas para a outra metade do ecra */
    fun getReservas(callback: VolleyCallBack?) {
        val url = appSettings.URL_reservas_sala + session.getSala()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {
            val jsonArray = response.getJSONArray("" + "data")
            listReservas.clear()
            for (i in 0 until jsonArray.length()) {
                val distr = jsonArray.getJSONObject(i)

                val userarray = distr.getJSONObject("utilizadores")
                val salaarray = distr.getJSONObject("sala")

                val getReserva = distr.getInt("nreserva")
                val getData = distr.getString("datareserva")
                val getHoraInicio = distr.getString("horainicio")
                val getHoraFim = distr.getString("horafim")
                val getEstadoReserva = distr.getInt("estadoreserva")
                val getUtilizador = userarray.getString("utilizador")
                val getEstadoUser = userarray.getInt("estadouser")
                val getSala = salaarray.getString("nomesala")

                listReservas.add(
                    Reservas(getReserva, getData, getHoraInicio, getHoraFim, getEstadoReserva, getUtilizador, getEstadoUser, getSala)
                )

            }

            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace() })
        requestQueue.add(request)
    }

    /* Carregar a lista de reservas para o calendario atraves do JetPack Compose */
    private fun addComposeView(){
        val view = findViewById<ComposeView>(R.id.composeCalendar).setContent{
            val pt = Locale("pt", "PT")
            Locale.setDefault(pt);
            WeekScheduleTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Schedule(events = obterEventos)
                }
            }
        }
    }

    /* Sistema de auto-refresh para atualizar a lista de reservas no calendario */
    fun ReloadActivity(){
        pageHandler = Handler(mainLooper)
        pageHandler.postDelayed(object : Runnable {
            override fun run() {
                var spinner = findViewById<LinearProgressIndicator>(R.id.progressBarCalendar);
                spinner.visibility = View.VISIBLE;

                getReservas(object: VolleyCallBack {
                    override fun onSuccess() {
                        spinner.visibility = View.GONE;
                        val addReservas = ArrayList<Event>()
                        for(item in listReservas){
                            var hinicio = ""
                            var hfim = ""
                            if(item.horainicio.length == 4)
                                hinicio = "0" + item.horainicio
                            else hinicio = item.horainicio

                            if(item.horafim.length == 4)
                                hfim = "0" + item.horafim
                            else hfim = item.horafim

                            val Inicio = item.datareserva + "T" + hinicio + ":00"
                            val Fim = item.datareserva + "T" + hfim + ":00"

                            var color = Azul800
                            if(item.estadoreserva == 3) color = Vermelho
                            else if(item.estadoreserva == 2) color = Azul600

                            addReservas.add(Event(item.nomeuser, color, LocalDateTime.parse(Inicio),  LocalDateTime.parse(Fim), item.nomesala))
                        }
                        obterEventos = addReservas
                        addComposeView()

                    }
                })
                pageHandler.postDelayed(this, appSettings.reloadQR)
            }
        }, 10)

    }

    /* Atualizar relogio a cada 10 segundos */
    fun ClockSystem(){
        /* Atualizar horas cada 10 segundos */
        clockHandler = Handler(mainLooper)
        clockHandler.postDelayed(object : Runnable {
            override fun run() {
                val horas = findViewById<TextView>(R.id.textViewHoras)
                val dataHoje = SimpleDateFormat("HH:mm EEEE", Locale("pt", "PT"))
                horas.text = dataHoje.format(Date())
                clockHandler.postDelayed(this, 10000)
            }
        }, 10)
    }

    /* Enviar informações da reserva e verificar disponibilidade */
    fun validarSobreposicao(inicio : String, fim : String, data : String, nsala : String){
        val url = appSettings.URL_validar_sobreposicao
        val params = HashMap<String, String>()
        params["horainicio"] = inicio
        params["horafim"] = fim
        params["datareserva"] = data
        params["nsala"] = nsala
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), { response ->try {
            if(response.getBoolean("success")){

                val qrEsconde = findViewById<LinearLayout>(R.id.qrEsconde)
                try {
                    val barcodeEncoder = BarcodeEncoder()
                    //nsala, datareserva, horainicio, horafim
                    val url = "{\"nsala\": \"$nsala\", \"datareserva\": \"$data\", \"horainicio\": \"$inicio\", \"horafim\": \"$fim\"}"
                    val bitmap = barcodeEncoder.encodeBitmap(url, BarcodeFormat.QR_CODE, 600, 600)
                    val imageViewQrCode =  findViewById<ImageView>(R.id.ImgQRCODE)
                    val setReserva = findViewById<LinearLayout>(R.id.setReserva)
                    setReserva.visibility = View.GONE
                    qrEsconde.visibility = View.VISIBLE
                    imageViewQrCode.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    alertaDanger(this@AddEventsHalf, "Atenção", getString(R.string.erro_gerar_qrcode))
                }
                finishHandler = Handler(mainLooper)
                finishHandler.postDelayed({ finish() }, appSettings.appReload)

            }else{
                alertaDanger(this@AddEventsHalf, "Atenção", getString(R.string.erro_intervalo_invalido))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            alertaDanger(this@AddEventsHalf, "Atenção", getString(R.string.erro_geral))
        }
        }, { error -> error.printStackTrace().toString() })
        requestQueue.add(request)
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
        Log.i("AddEventsHalf", "Pause: Reloader Destruido")
        pageHandler.removeCallbacksAndMessages(null);
        clockHandler.removeCallbacksAndMessages(null);
        if(this::finishHandler.isInitialized)
            finishHandler.removeCallbacksAndMessages(null);
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("AddEventsHalf", "Destroy: Reloader Destruido")
        pageHandler.removeCallbacksAndMessages(null);
        clockHandler.removeCallbacksAndMessages(null);
        if(this::finishHandler.isInitialized)
            finishHandler.removeCallbacksAndMessages(null);

    }

    override fun onResume() {
        super.onResume()
        Log.i("AddEventsHalf", "Resume: Reloader Reiniciado")
        ReloadActivity()
        ClockSystem()
        callBackend()
    }
}