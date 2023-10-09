package com.example.apptablet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class desativadoActivity : AppCompatActivity() {

    lateinit var session : SessionManager
    lateinit var clockHandler : Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_desativado)

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        val waitResponse = findViewById<LinearProgressIndicator>(R.id.waitResponse)
        val textViewMsg = findViewById<TextView>(R.id.textViewMsg)
        waitResponse.visibility = View.VISIBLE
        textViewMsg.visibility = View.GONE
        Handler(Looper.getMainLooper()).postDelayed({ getSalas() }, 1000)
        /* Obter dados do SessionManager */
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
                                        clockHandler.removeCallbacksAndMessages(null);
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

        /* Possibilidade de atualizar o ecra puxando para baixo */
        val swipe : SwipeRefreshLayout = findViewById(R.id.swipeRefresh)
        swipe.setOnRefreshListener {
            isAvaiable()
            updateHeader()
            alertaSuccess(this@desativadoActivity, "Sucesso", getString(R.string.sucesso_atualizar))
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

    /* Obter lista de salas da backend */
    fun getSalas() {
        val url = appSettings.URL_get_sala + session.getSala()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

            val distr = response.getJSONObject("data")
            val desc = distr.getString("mensagem")

            val txtMsg = findViewById<TextView>(R.id.textViewMsg)
            if(desc != "") txtMsg.text = desc
            else txtMsg.text = getString(R.string.desativacao_sala)

            val waitResponse = findViewById<LinearProgressIndicator>(R.id.waitResponse)
            val textViewMsg = findViewById<TextView>(R.id.textViewMsg)
            waitResponse.visibility = View.GONE
            textViewMsg.visibility = View.VISIBLE

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace() })
        requestQueue.add(request)
    }

    /* Sistema de relogio, atualiza a cada 10 segundos */
    fun ClockSystem(){
        /* Atualizar horas cada 10 segundos */
        val dataHora = findViewById<TextView>(R.id.dataHora)
        clockHandler = Handler(mainLooper)
        clockHandler.postDelayed(object : Runnable {
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
                clockHandler.postDelayed(this, 10000)
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

                if(estado == 1){
                    clockHandler.removeCallbacksAndMessages(null);
                    val i = Intent(this, redirActivities::class.java)
                    startActivity(i)
                }

            }

            val swipe : SwipeRefreshLayout = findViewById(R.id.swipeRefresh)
            swipe.isRefreshing = false

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
        Log.i("Teste", "Desativado-Pause: Reloader Destruido")
        clockHandler.removeCallbacksAndMessages(null);
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("Teste", "Desativado-Destroy: Reloader Destruido")
        clockHandler.removeCallbacksAndMessages(null);

    }

    override fun onResume() {
        super.onResume()
        Log.i("Teste", "Desativado-Resume: Reloader Reiniciado")
        ClockSystem()
        updateHeader()
        callBackend()
    }

}