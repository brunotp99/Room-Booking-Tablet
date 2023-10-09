package com.example.apptablet

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.apptablet.databinding.ActivityRegistroDispositivoBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONException
import org.json.JSONObject


class  registroDispositivo : AppCompatActivity(), OnItemClickListener {

    lateinit var session : SessionManager
    data class Salas(val nsala: Int, val nestabelecimento: Int, val sala: String, val lugares: Int, val alocacao: Int, val intervalo: Int, val estado: Int, val estadoatual: Int)
    var listSalas = ArrayList<Salas>()

    data class Estabelecimentos(val nestabelecimento: Int, val nome: String)
    var listEstabelecimentos = ArrayList<Estabelecimentos>()

    private lateinit var binding : ActivityRegistroDispositivoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroDispositivoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* Verificar Sessao */
        session = SessionManager(this)
        if(session.isLoggedIn()){
            var i: Intent = Intent(applicationContext, MainActivity::class.java)
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }

        val btnCfg = findViewById<Button>(R.id.btnCfg)

        /* Carregar estabeleciementos para o dropdown */
        getEstabelecimentos(object: VolleyCallBack {
            override fun onSuccess() {
                var estabelecimentos = ArrayList<String>()
                for(item in listEstabelecimentos)
                    estabelecimentos.add(item.nome)

                val adapter = ArrayAdapter(applicationContext, R.layout.dropdown_item, estabelecimentos)
                with(binding.autoCompleteTextView){
                    setAdapter(adapter)
                    onItemClickListener = this@registroDispositivo
                }
            }
        })

        /* Finalizar configuração, verificar dados introduzidos e fazer pedido de criação */
        btnCfg.setOnClickListener {

            val selectedSala = findViewById<TextInputLayout>(R.id.textField)
            val defineEst = findViewById<TextInputLayout>(R.id.defineEst)
            val selectedEst = (defineEst.editText as AutoCompleteTextView)
            val defineMarca = findViewById<EditText>(R.id.defineMarca)
            val defineModelo = findViewById<EditText>(R.id.defineModelo)
            val defineSala = (selectedSala.editText as AutoCompleteTextView)
            val definePin = findViewById<EditText>(R.id.definePin)

            when {
                TextUtils.isEmpty(defineMarca.text.toString()) -> defineMarca.error = "Preencha o campo Marca do Dispositivo"
                TextUtils.isEmpty(defineModelo.text.toString()) -> defineModelo.error = "Preencha o campo Modelo do Dispositivo"
                TextUtils.isEmpty(defineSala.text.toString()) -> defineSala.error = "Escolha uma sala a associar"
                TextUtils.isEmpty(definePin.text.toString()) -> definePin.error = "Defina um PIN de 4 digitos"
                else -> {

                    var nestabelecimento = 0

                    for(item in listEstabelecimentos) {
                        if (item.nome == selectedEst.text.toString()){
                            nestabelecimento = item.nestabelecimento
                        }
                    }

                    for(item in listSalas) {
                        Log.i("Settings", item.sala + " == " + defineSala.text.toString() + " && " + item.nestabelecimento.toString() + " == " + nestabelecimento.toString())
                        if (item.sala == defineSala.text.toString() && item.nestabelecimento == nestabelecimento){
                            registoTablet(item.nsala, item.sala, item.lugares, item.alocacao, item.intervalo, item.estado, item.estadoatual, defineMarca.text.toString(), defineModelo.text.toString(), definePin.text.toString())
                            break
                        }
                    }


                }
            }

        }

    }

    interface VolleyCallBack {
        fun onSuccess()
    }

    /* Envio dos dados do dispositivo para a backend */
    fun registoTablet(nsala: Int, sala: String, lugares: Int, alocacao: Int, intervalo: Int, estado: Int, estadoatual: Int, marca: String, modelo: String, pin : String){
        val requestQueue = Volley.newRequestQueue(this)
        val postURL = appSettings.URL_adicionar_tablet + nsala
        val strRequest: StringRequest = object : StringRequest(
            Method.POST, postURL,
            Response.Listener { response ->
                Log.i("Registo", response)
                session.setNTablet(response.toInt())
                session.createLoginSession(nsala, sala, lugares, alocacao, intervalo, estado, estadoatual, pin.toInt())
                var i = Intent(applicationContext, MainActivity::class.java)
                startActivity(i)
            },
            Response.ErrorListener { error ->
                alertaDanger(this@registroDispositivo, "Atenção", getString(R.string.erro_criar_dispositivo))
            }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["marca"] = marca
                params["modelo"] = modelo
                params["pin"] = pin
                return JSONObject(params as Map<String, String>).toString().toByteArray()
            }
        }
        requestQueue.add(strRequest)
    }

    /* Obter salas por estabelecimento */
    fun getSalas(callback: VolleyCallBack?, id : Int) {
        val url = appSettings.URL_salas_estabelecimento + id.toString()
        listSalas.clear()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {
            val jsonArray = response.getJSONArray("" + "data")
            for (i in 0 until jsonArray.length()) {
                val distr = jsonArray.getJSONObject(i)

                val getId = distr.getString("nsala").toString().toInt()
                val getSala = distr.getString("sala").toString()
                val getEst = distr.getInt("nestabelecimento")
                val getLugares = distr.getString("lugares").toString().toInt()
                val getAlocacao = distr.getString("alocacao").toString().toInt()
                val getIntervalo = distr.getString("intervalolimpeza").toString().toInt()
                val getEstado = distr.getString("nestado").toString().toInt()
                val getEstadoAtual = distr.getString("estadosala").toString().toInt()
                listSalas.add(Salas(getId, getEst, getSala, getLugares, getAlocacao, getIntervalo, getEstado, getEstadoAtual))

            }
            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace() })
        requestQueue.add(request)
    }

    /* Obter lista de estabelecimentos - Ok */
    fun getEstabelecimentos(callback: VolleyCallBack?) {
        val url = appSettings.URLestabelecimentos
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, {
                response ->try {
            val jsonArray = response.getJSONArray("" + "data")
            for (i in 0 until jsonArray.length()) {
                val distr = jsonArray.getJSONObject(i)

                val getId = distr.getString("nestabelecimento").toString().toInt()
                val geNome = distr.getString("estabelecimento").toString()
                listEstabelecimentos.add(Estabelecimentos(getId, geNome))

            }
            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace() })
        requestQueue.add(request)
    }

    /* Carregar lista de salas quando um estabelecimento é selecionado */
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val item = parent?.getItemAtPosition(position).toString()
        getSalas(object: VolleyCallBack {
            val selectedSala = findViewById<TextInputLayout>(R.id.textField)
            override fun onSuccess() {
                val salas : MutableList<String> = mutableListOf()
                salas.clear()
                for(item in listSalas)
                    salas.add(item.sala)
                var adapter = ArrayAdapter(applicationContext, R.layout.dropdown_item, salas)
                (selectedSala.editText as? AutoCompleteTextView)?.setAdapter(adapter)
                adapter.notifyDataSetChanged();
            }
        }, listEstabelecimentos[position].nestabelecimento)
    }

}