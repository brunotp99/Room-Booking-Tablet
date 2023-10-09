package com.example.apptablet

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.compose.ui.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import org.apache.http.conn.ConnectTimeoutException
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParserException
import java.net.ConnectException
import java.net.MalformedURLException
import java.net.SocketException
import java.net.SocketTimeoutException
import com.tapadoo.alerter.Alerter

class redirActivities : AppCompatActivity() {

    lateinit var session : SessionManager
    lateinit var pageHandler : Handler
    var onDesativado = false
    var onNormal = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_redir_activities)

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        ReloadSystem()

        val btnReload = findViewById<Button>(R.id.btnReload)
        btnReload.setOnClickListener {
            val i = Intent(applicationContext, redirActivities::class.java)
            startActivity(i)
        }

    }

    /* Verificar o estado de uma sala, se esta nao existir apaga a session */
    fun checkDisponibilidade() {
        if(session.getSala() == 0){
            session.LogoutUser()
            return
        }
        val url = appSettings.URL_get_sala + session.getSala() //resources.getString(R.string.obterSalaId) + session.getSala()
        Log.i("redir", url)
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

            if(response.getBoolean("success")){
                val sala = response.getJSONObject("data")
                val estado = sala.getInt("estadosala")
                if(estado != 0 && !onNormal){
                    Log.i("Redir", "Sala Ativada")
                    onNormal = true
                    onDesativado = false
                    var i = Intent(this, MainActivity::class.java)
                    startActivity(i)
                }else if(estado == 0 && !onDesativado){
                    Log.i("Redir", "Sala Desativada")
                    onDesativado = true
                    onNormal = false
                    var i = Intent(this, desativadoActivity::class.java)
                    startActivity(i)
                }
            }else session.LogoutUser()

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error ->
            alertaDanger(this@redirActivities, "Atenção", getVolleyError(error))
        })
        requestQueue.add(request)
    }

    fun tabletExists(){
        val url = appSettings.URL_tablet_exists + session.getTablet()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

            if(!response.getBoolean("success") && session.isLoggedIn()){
                session.LogoutUser()
            }else getDispositivo()

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error ->
            alertaDanger(this@redirActivities, "Atenção", getVolleyError(error))
        })
        requestQueue.add(request)
    }

    fun getSala(nsala : String) {
        val url = appSettings.URL_get_sala + nsala
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

            if(response.getBoolean("success")){
                val sala = response.getJSONObject("data")
                session.setSala(
                    sala.getInt("nsala"),
                    sala.getString("sala"),
                    sala.getInt("lugares"),
                    sala.getInt("alocacao"),
                    sala.getInt("intervalolimpeza"),
                    sala.getInt("nestado"),
                    sala.getInt("estadosala"),
                )
                session.setLugares(session.getLugaresDefault())
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace() })
        requestQueue.add(request)
    }

    fun getDispositivo() {
        val url = appSettings.URL_get_tablet + session.getTablet()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

            if(response.getBoolean("success")){
                val tablet = response.getJSONObject("data")
                getSala(tablet.getInt("nsala").toString())
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace().toString() })
        requestQueue.add(request)
    }

    /* Analisar problemas de conexão com a backend e apresentar erro ao utilizador */
    fun getVolleyError(error: VolleyError): String {
        val normalLayout = findViewById<LinearLayout>(R.id.normalLayout)
        val wave = findViewById<ImageView>(R.id.bottom_wave)
        val errorLayout = findViewById<LinearLayout>(R.id.errorLayout)
        val errorTV = findViewById<TextView>(R.id.errorMsg)
        normalLayout.visibility = View.GONE;
        wave.visibility = View.GONE;
        errorLayout.visibility = View.VISIBLE;
        pageHandler.removeCallbacksAndMessages(null);
        var errorMsg = ""
        if (error is NoConnectionError) {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var activeNetwork: NetworkInfo? = null
            activeNetwork = cm.activeNetworkInfo
            errorMsg = if (activeNetwork != null && activeNetwork.isConnectedOrConnecting) {
                "O servidor não está conectado à internet. Por favor, tente novamente"
            } else {
                "O seu dispositivo não está conectado à Internet. Tente novamente com uma conexão de Internet ativa"
            }
        } else if (error is NetworkError || error.cause is ConnectException) {
            errorMsg = "O seu dispositivo não está conectado à Internet. Tente novamente com uma conexão de Internet ativa"
        } else if (error.cause is MalformedURLException) {
            errorMsg = "Occoreu um problema a executar o seu pedido, porfavor tente novamente..."
        } else if (error is ParseError || error.cause is IllegalStateException || error.cause is JSONException || error.cause is XmlPullParserException) {
            errorMsg = "Ocorreu um erro ao analisar os dados…"
        } else if (error.cause is OutOfMemoryError) {
            errorMsg = "O despositivo esta sem mémoria"
        } else if (error is AuthFailureError) {
            errorMsg = "Falha ao autenticar o usuário no servidor, entre em contato com o suporte"
        } else if (error is ServerError || error.cause is ServerError) {
            errorMsg = "Ocorreu um erro interno no servidor, porfavor tente novamente...."
        } else if (error is TimeoutError || error.cause is SocketTimeoutException || error.cause is ConnectTimeoutException || error.cause is SocketException || (error.cause!!.message != null && error.cause!!.message!!.contains(
                "A sua conexão expirou, por favor tente novamente!"
            ))
        ) {
            errorMsg = "A sua conexão expirou, por favor tente novamente!"
        } else {
            errorMsg = "Ocorreu um erro desconhecido durante a operação, porfavor tente novamente"
        }
        errorTV.text = errorMsg
        return errorMsg
    }

    /* Gerir autorefresh */
    fun ReloadSystem(){
        pageHandler = Handler(mainLooper)
        pageHandler.postDelayed(object : Runnable {
            override fun run() {
                checkDisponibilidade()
                tabletExists()
                pageHandler.postDelayed(this, appSettings.appReload)
            }
        }, 10)
    }

    override fun onResume() {
        super.onResume()
        pageHandler.removeCallbacksAndMessages(null);
        ReloadSystem()
    }
}