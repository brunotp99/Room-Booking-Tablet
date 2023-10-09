package com.example.apptablet

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import org.threeten.bp.LocalTime
import java.util.*
import kotlin.collections.ArrayList

class ocupadoActivity : AppCompatActivity() {

    lateinit var session : SessionManager

    data class Reservas(val nreserva: Int, val datareserva: String, val horainicio: String, val horafim: String, val estadoreserva: Int, val nomeuser: String, val estadouser: Int, val nomesala: String)
    var listReservas = java.util.ArrayList<Reservas>()

    lateinit var pageHandler : Handler
    lateinit var someHandler : Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocupado)

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Lisbon"))

        val listDate = findViewById<TextView>(R.id.headerList)
        val dataHoje = SimpleDateFormat("dd MMMM yyyy", Locale("pt", "PT"))
        listDate.text = "Hoje - " + dataHoje.format(Date())

        val btnCalendar = findViewById<ImageView>(R.id.btnCalendar)
        btnCalendar.setOnClickListener {
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
                                        customDialog.dismiss()
                                        val i = Intent(applicationContext, Settings::class.java)
                                        startActivity(i)
                                    }

                                    override fun onFailure(error : String) {
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
            obterReservas()
            isAvaiable()
            updateHeader()
            alertaSuccess(this@ocupadoActivity, "Sucesso", getString(R.string.sucesso_atualizar))
        }

    }

    interface VolleyCallBack {
        fun onSuccess()
        fun onFailure(error : String)
    }

    private fun validarPin(callback: VolleyCallBack?, pin: String) {

        val url = appSettings.URL_validar_pin + session.getTablet()
        val params = HashMap<String, String>()
        params["pin"] = pin
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject(params as Map<String, String>), { response ->try {

            val success = response.getBoolean("success")
            if(success) callback?.onSuccess()
            else callback?.onFailure("Pin errado");

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.toString()) })
        requestQueue.add(request)
    }

    /* Obter lista de reservas por sala na backend */
    fun getReservas(callback: VolleyCallBack?) {
        val url = appSettings.URL_reservas_sala_hoje + session.getSala()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, {
                response ->try {
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

                listReservas.add(Reservas(getReserva, getData, getHoraInicio, getHoraFim, getEstadoReserva, getUtilizador, getEstadoUser, getSala))

            }
            val swipe : SwipeRefreshLayout = findViewById(R.id.swipeRefresh)
            swipe.isRefreshing = false
            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace() })
        requestQueue.add(request)
    }

    /* Carregar os dados para a listview */
    fun obterReservas(){
        var spinner = findViewById<LinearProgressIndicator>(R.id.progressBar);
        spinner.visibility = View.VISIBLE;

        getReservas(object: VolleyCallBack {
            override fun onSuccess() {
                spinner.visibility = View.GONE;
                val listView = findViewById<ListView>(R.id.main_listview)
                listView.adapter = MyCustomAdapter(applicationContext, listReservas)
            }

            override fun onFailure(error: String) {
                Log.i("Ocupado", error)
            }
        })
    }

    /* Gerir funções de autorefresh */
    fun loopFunctions(){
        pageHandler = Handler(mainLooper)
        pageHandler.postDelayed(object : Runnable {
            override fun run() {
                currentState()
                obterReservas()
                pageHandler.postDelayed(this, appSettings.appReload)
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
                if(!response.toBoolean()){
                    pageHandler.removeCallbacksAndMessages(null);
                    val i = Intent(applicationContext, MainActivity::class.java)
                    startActivity(i)
                }
            },
            Response.ErrorListener { error ->
                alertaDanger(this@ocupadoActivity, "Sucesso", getString(R.string.erro_atualizar_reservas))
            }) {
        }
        requestQueue.add(strRequest)
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

    /* Criação de uma listview customizada */
    private class MyCustomAdapter(context: Context, val getReservas : ArrayList<Reservas>): BaseAdapter() {
        private val mContext: Context
        init {
            mContext = context
        }
        override fun getCount(): Int {
            return getReservas.size
        }
        override fun getItemId(posicao: Int): Long {
            return posicao.toLong()
        }
        override fun getItem(posicao: Int): Any {
            return "Item"
        }

        override fun getView(posicao: Int, convertView: View?, viewGroup: ViewGroup?): View {

            val layoutInflater = LayoutInflater.from(mContext)
            val rowMain = layoutInflater.inflate(R.layout.row_main, viewGroup, false)

            val setHorario = rowMain.findViewById<TextView>(R.id.setHorario)
            setHorario.text = getReservas[posicao].horainicio + " - " + getReservas[posicao].horafim

            val setTitulo = rowMain.findViewById<TextView>(R.id.setTitulo)
            setTitulo.text = getReservas[posicao].nomesala

            val setUsuario = rowMain.findViewById<TextView>(R.id.setUsuario)
            setUsuario.text = getReservas[posicao].nomeuser

            this.notifyDataSetChanged();
            return rowMain
        }
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
        Log.i("Teste", "Ocupado-Pause: Reloader Destruido")
        pageHandler.removeCallbacksAndMessages(null);
        someHandler.removeCallbacksAndMessages(null);
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("Teste", "Ocupado-Destroy: Reloader Destruido")
        pageHandler.removeCallbacksAndMessages(null);
        someHandler.removeCallbacksAndMessages(null);

    }

    override fun onResume() {
        super.onResume()
        Log.i("Teste", "Ocupado-Resume: Reloader Reiniciado")
        ClockSystem()
        loopFunctions()
        updateHeader()
        callBackend()
    }

}