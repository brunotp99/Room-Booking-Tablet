package com.example.apptablet

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.apptablet.ui.theme.Azul600
import com.example.apptablet.ui.theme.Azul800
import com.example.apptablet.ui.theme.Vermelho
import com.example.apptablet.ui.theme.WeekScheduleTheme
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.json.JSONException
import java.text.SimpleDateFormat
import org.threeten.bp.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

class calendarView : AppCompatActivity() {

    data class Reservas(val nreserva: Int, val datareserva: String, val horainicio: String, val horafim: String, val estadoreserva: Int, val nomeuser: String, val estadouser: Int, val nomesala: String)
    var listReservas = ArrayList<Reservas>()

    lateinit var session : SessionManager
    lateinit var pageHandler : Handler
    lateinit var clockHandler : Handler

    private lateinit var obterEventos : ArrayList<Event>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_view)

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Lisbon"))

        val spinner = findViewById<LinearProgressIndicator>(R.id.progressBarCalendar);
        spinner.visibility = View.VISIBLE;

        ReloadActivity()

        val btnArrow = findViewById<ImageView>(R.id.btnArrow)
        btnArrow.setOnClickListener {
            finish()
        }

        val btnaddEvent = findViewById<FloatingActionButton>(R.id.btnAddEvent)
        btnaddEvent.setOnClickListener {
            val i = Intent(this, AddEventsHalf::class.java)
            startActivity(i)

        }

    }

    interface VolleyCallBack {
        fun onSuccess()
    }

    fun getReservas(callback: VolleyCallBack?) {
        if(session.getSala() == 0){
            session.LogoutUser()
            return
        }
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

    private fun addComposeView(){
        val view = findViewById<ComposeView>(R.id.composeCalendar).setContent{
            val pt = Locale("pt", "PT")
            Locale.setDefault(pt);
            WeekScheduleTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Schedule(events = obterEventos)
                }
            }
        }
    }

    fun ReloadActivity(){

        pageHandler = Handler(mainLooper)
        pageHandler.postDelayed(object : Runnable {
            override fun run() {
                val spinner = findViewById<LinearProgressIndicator>(R.id.progressBarCalendar);
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
                pageHandler.postDelayed(this, appSettings.appReload)
            }
        }, 10)

    }

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
        Log.i("calendarView", "Pause: Reloader Destruido")
        pageHandler.removeCallbacksAndMessages(null);
        clockHandler.removeCallbacksAndMessages(null);
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("calendarView", "Destroy: Reloader Destruido")
        pageHandler.removeCallbacksAndMessages(null);
        clockHandler.removeCallbacksAndMessages(null);

    }

    override fun onResume() {
        super.onResume()
        Log.i("calendarView", "Resume: Reloader Reiniciado")
        ReloadActivity()
        ClockSystem()
        callBackend()
    }

}