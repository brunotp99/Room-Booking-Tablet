package com.example.apptablet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.apptablet.databinding.ActivityRegistroDispositivoBinding
import com.example.apptablet.databinding.ActivitySettingsBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.json.JSONException
import org.json.JSONObject

class Settings : AppCompatActivity(), AdapterView.OnItemClickListener {

    lateinit var session : SessionManager
    private lateinit var binding : ActivitySettingsBinding

    data class Salas(val nsala: Int, val nestabelecimento: Int, val sala: String, val lugares: Int, val alocacao: Int, val intervalo: Int, val estado: Int, val estadoatual: Int, val descricao: String)
    var listSalas = ArrayList<Salas>()
    var singleSala = ArrayList<Salas>()

    data class Tablet(val ntablet: Int, val nsala: Int, val marca: String, val modelo: String, val pin: Int)
    var listTablet = ArrayList<Tablet>()

    data class Estabelecimentos(val nestabelecimento: Int, val nome: String)
    var listEstabelecimentos = ArrayList<Estabelecimentos>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* Verificar Session */
        session = SessionManager(this)
        session.checkLogin()

        /* Obter dados da Session */
        try{
            val salas: java.util.HashMap<String, String> = session.getSalaDetails()
            val nomesala = salas[SessionManager.KEY_NOMESALA]
            val lugares = salas[SessionManager.KEY_LUGARES]

            val topBarSala = findViewById<TextView>(R.id.textViewSala)
            val topBarLugares = findViewById<TextView>(R.id.textViewLugares)
            topBarSala.text = nomesala.toString()
            topBarLugares.text = lugares.toString() + " Lugares"

        }catch (e:Exception){
            Log.e("Session", "Não foi possivel obter os dados na sessao")
        }

        /* Gerir eventos do menu superior */
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                if(tab?.position  == 0){
                    binding.containerDispositivo.visibility = View.VISIBLE
                    binding.containerSala.visibility = View.GONE
                }else if(tab?.position  == 1){
                    binding.containerDispositivo.visibility = View.GONE
                    binding.containerSala.visibility = View.VISIBLE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                //
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                //
            }

        })

        /* Carregar estabelecimentos para o dropdown */
        getEstabelecimentos(object: VolleyCallBack {
            override fun onSuccess() {
                var estabelecimentos = ArrayList<String>()
                for(item in listEstabelecimentos)
                    estabelecimentos.add(item.nome)

                val adapter = ArrayAdapter(applicationContext, R.layout.dropdown_item, estabelecimentos)
                with(binding.autoCompleteTextView){
                    setAdapter(adapter)
                    onItemClickListener = this@Settings
                }

            }

            override fun onFailure(error: String) {
                Log.i("Settings", error)
            }
        })

        /* Carregar inputs do dispositivo */
        getDispositivo(object: VolleyCallBack {
            override fun onSuccess() {
                binding.defineMarca.setText(listTablet[0].marca)
                binding.defineModelo.setText(listTablet[0].modelo)
                binding.pinText.setText(listTablet[0].pin.toString())

            }

            override fun onFailure(error: String) {
                Log.i("Settings", error)
            }
        })

        /* Carregar inputs da sala */
        getSala(object: VolleyCallBack {
            override fun onSuccess() {
                binding.defineSala.setText(singleSala[0].sala)
                binding.defineLugares.setText(singleSala[0].lugares.toString())
                binding.txtAlocacao.text = singleSala[0].alocacao.toString() + "%"
                binding.sliderAlocacao.value = singleSala[0].alocacao.toFloat()
                binding.txtIntervalo.text = singleSala[0].intervalo.toString() + " min"
                binding.sliderIntervalo.value = singleSala[0].intervalo.toFloat()
                binding.defineDescricao.setText(singleSala[0].descricao)
                if(singleSala[0].estadoatual == 1){
                    binding.estadoSala.isChecked = true
                    binding.txtEstadoSala.text = "Disponível"
                }
                else{
                    binding.estadoSala.isChecked = false
                    binding.txtEstadoSala.text = "Bloqueada"
                }

            }

            override fun onFailure(error: String) {
                Log.i("Settings", error)
            }
        })

        /* Guardar as alterações verificando o que foi trocado e o que foi mantido */
        binding.btnGuardarDispositivo.setOnClickListener {

            var flagError = false

            when {
                TextUtils.isEmpty(binding.defineMarca.text.toString()) -> flagError = true
                TextUtils.isEmpty(binding.defineModelo.text.toString()) -> flagError = true
                TextUtils.isEmpty(binding.pinText.text.toString()) -> flagError = true
            }

            if(flagError){
                alertaDanger(this@Settings, "Atenção", getString(R.string.erro_campos_vazios))
                return@setOnClickListener
            }

            /* Se não for vazio é porque foi definida uma nova sala */
            if(binding.autoCompleteTextView.text.toString() != "" && binding.autoCompleteTextView.text.toString() != null){
                var nestabelecimento = 0

                for(item in listEstabelecimentos) {
                    if (item.nome == binding.autoCompleteTextView.text.toString()){
                        nestabelecimento = item.nestabelecimento
                    }
                }

                for(item in listSalas) {
                    if (item.sala == binding.autoCompleteSalas.text.toString() && item.nestabelecimento == nestabelecimento){
                        session.setSala(item.nsala, item.sala, item.lugares, item.alocacao, item.intervalo, item.estado, item.estadoatual)
                        updateTablet(session.getTablet().toString(), item.nsala.toString(), binding.defineMarca.text.toString(), binding.defineModelo.text.toString(), binding.pinText.text.toString(), item.sala, item.lugares.toString())
                        break
                    }
                }
            }else updateTablet(session.getTablet().toString(), "0", binding.defineMarca.text.toString(), binding.defineModelo.text.toString(), binding.pinText.text.toString(), "", "")

        }

        /* Ao apagar o tablet removemos a sessão */
        binding.btnApagar.setOnClickListener {
            session.LogoutUser()
        }

        binding.btnArrow.setOnClickListener{
            finish()
        }

        /* Mostrar valor numerico do slider da alocação */
        binding.txtAlocacao.text = session.getAlocacao().toString() + "%"
        binding.sliderAlocacao.value = session.getAlocacao().toFloat()
        binding.sliderAlocacao.addOnChangeListener { slider, value, fromUser ->
            binding.txtAlocacao.text = value.toInt().toString() + "%"
        }

        /* Mostrar valor numerico do slider do intervalo de limpeza */
        binding.txtIntervalo.text = session.getIntervalo().toString() + " min"
        binding.sliderIntervalo.value = session.getIntervalo().toFloat()
        binding.sliderIntervalo.addOnChangeListener { slider, value, fromUser ->
            binding.txtIntervalo.text = value.toInt().toString() + " min"
        }

        binding.estadoSala.setOnClickListener {
            if(binding.estadoSala.isChecked){
                infoDialog("Atenção, tem a certeza que pretende reativar a sala?", true)
            }else{
                infoDialog("Atenção, ao desativar a sala, todas as reservas associadas a mesma serão removidas!", false)
            }
        }

        /* Guardar dados da sala e verificar o que foi atualizado e mantido */
        binding.btnGuardarSala.setOnClickListener {

            var flagError = false

            when {
                TextUtils.isEmpty(binding.defineSala.text.toString()) -> flagError = true
                TextUtils.isEmpty(binding.defineLugares.text.toString()) -> flagError = true
            }

            if(flagError){
                Snackbar.make(binding.paginaSettings, getString(R.string.erro_campos_vazios), Snackbar.LENGTH_SHORT)
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setBackgroundTint(ContextCompat.getColor(applicationContext, R.color.danger_bg))
                    .show()
                return@setOnClickListener
            }

            updateSala(
                session.getSala().toString(),
                binding.defineSala.text.toString(),
                binding.defineLugares.text.toString(),
                binding.defineDescricao.text.toString(),
                binding.sliderAlocacao.value.toInt().toString(),
                binding.sliderIntervalo.value.toInt().toString()
            )

        }

    }

    private fun infoDialog(message: String, estado: Boolean){
        val dialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_info, null)
        val dialogAceitar = dialogView.findViewById<Button>(R.id.dialogAceitar)
        val dialogCancelar = dialogView.findViewById<Button>(R.id.dialogCancelar)
        val txtMotivacao = dialogView.findViewById<TextView>(R.id.txtMotivacao)
        txtMotivacao.text = message
        dialog.setView(dialogView)
        dialog.setCancelable(true)
        val customDialog = dialog.create()
        customDialog.show()
        dialogAceitar.setOnClickListener {
            customDialog.dismiss()
            if(estado){
                binding.txtEstadoSala.text = "Disponível"
                updateEstado(session.getSala().toString(), "", "A sala foi reativada atráves do dispositivo")
                val i = Intent(applicationContext, redirActivities::class.java)
                startActivity(i)
            }else{
                binding.txtEstadoSala.text = "Bloqueada"

                val dialog = AlertDialog.Builder(this)
                val dialogView = layoutInflater.inflate(R.layout.dialog_nome, null)
                val btnContinuar = dialogView.findViewById<Button>(R.id.dialogAceitar)
                val editNome = dialogView.findViewById<EditText>(R.id.editNome)

                dialog.setView(dialogView)
                dialog.setCancelable(true)
                val customDialog = dialog.create()
                customDialog.show()
                btnContinuar.setOnClickListener {
                    var setMessage = getString(R.string.desativacao_sala)
                    if(editNome.text.toString() != "") setMessage = editNome.text.toString()

                    updateEstado(session.getSala().toString(), "delete", setMessage)

                    val i = Intent(applicationContext, desativadoActivity::class.java)
                    startActivity(i)
                    customDialog.dismiss()
                }

            }
        }
        dialogCancelar.setOnClickListener {
            customDialog.dismiss()
            binding.estadoSala.isChecked = !estado
        }
    }

    interface VolleyCallBack {
        fun onSuccess()
        fun onFailure(error: String)
    }

    /* Envio de alterações do tablet para a backend */
    fun updateTablet(ntablet: String, nsala: String, marca: String, modelo: String, pin: String, sala: String, lugares: String){
        val requestQueue = Volley.newRequestQueue(this)
        val postURL = appSettings.URLupdateTablet
        val strRequest: StringRequest = object : StringRequest(
            Method.POST, postURL,
            Response.Listener { response ->
                if(response.toBoolean()){
                    session.replacePinCode(pin.toInt())
                    if(nsala != ""){
                        binding.textViewSala.text = sala
                        binding.textViewLugares.text = lugares
                    }
                    alertaSuccess(this@Settings, "Sucesso", getString(R.string.sucesso_atualizar_dispositivo))
                }else{
                    alertaDanger(this@Settings, "Atenção", getString(R.string.erro_atualizar_dispositivo))
                }
            },
            Response.ErrorListener { error ->
                alertaDanger(this@Settings, "Atenção", getString(R.string.erro_geral))
            }) {
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["ntablet"] = ntablet
                params["marca"] = marca
                params["modelo"] = modelo
                params["nsala"] = nsala
                params["pin"] = pin
                return JSONObject(params as Map<String, String>).toString().toByteArray()
            }
        }
        requestQueue.add(strRequest)
    }

    /* Envio de alterações da sala para a backend */
    fun updateSala(nsala: String, sala: String, lugares: String, descricao: String, alocacao: String, intervalolimpeza: String){
        val url = appSettings.URL_atualizar_dados_sala + nsala
        val params = java.util.HashMap<String, String>()
        params["sala"] = sala
        params["lugares"] = lugares
        params["alocacao"] = alocacao
        params["intervalolimpeza"] = intervalolimpeza
        params["descricao"] = descricao
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.PUT, url, JSONObject(params as Map<String, String>), { response ->try {
            if(response.getBoolean("success")){

                session.setNomeSala(sala)
                binding.textViewSala.text = sala
                session.setLugares(lugares.toInt())
                session.setIntervalo(intervalolimpeza.toInt())
                session.setAlocacao(alocacao.toInt())
                session.setLugares(session.getLugaresDefault())
                binding.textViewLugares.text = session.getLugares().toString() +  " Lugares"

                alertaSuccess(this@Settings, "Sucesso", getString(R.string.sucesso_atualizar_sala))
            }else{
                alertaDanger(this@Settings, "Atenção", getString(R.string.erro_atualizar_sala))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            alertaDanger(this@Settings, "Atenção", getString(R.string.erro_geral))
        }
        }, { error -> error.printStackTrace().toString() })
        requestQueue.add(request)
    }

    /* Enviar informações da reserva e verificar disponibilidade */
    fun updateEstado(nsala : String, option: String, message: String){
        val url = appSettings.URL_atualizar_estado_sala + nsala
        val params = java.util.HashMap<String, String>()
        params["option"] = option
        params["message"] = message
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.PUT, url, JSONObject(params as Map<String, String>), { response ->try {
            if(response.getBoolean("success")){

                if(binding.estadoSala.isChecked) session.setEstado(1)
                else session.setEstado(0)

                alertaSuccess(this@Settings, "Sucesso", getString(R.string.sucesso_atualizar_estado))
            }else{
                alertaDanger(this@Settings, "Atenção", getString(R.string.erro_atualizar_estado))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            alertaDanger(this@Settings, "Atenção", getString(R.string.erro_geral))
        }
        }, { error -> error.printStackTrace().toString() })
        requestQueue.add(request)
    }

    /* Obter lista de salas */
    fun getSalas(callback: VolleyCallBack?, id : Int) {
        val url = appSettings.URL_salas_estabelecimento + id.toString()
        listSalas.clear()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, {
                response ->try {
            val jsonArray = response.getJSONArray("" + "data")
            Log.i("Array", jsonArray.toString())
            Log.i("Array", jsonArray.length().toString())
            for (i in 0 until jsonArray.length()) {
                val distr = jsonArray.getJSONObject(i)

                val getId = distr.getString("nsala").toString().toInt()
                val getEst = distr.getString("nestabelecimento").toString().toInt()
                val getSala = distr.getString("sala").toString()
                val getLugares = distr.getString("lugares").toString().toInt()
                val getAlocacao = distr.getString("alocacao").toString().toInt()
                val getIntervalo = distr.getString("intervalolimpeza").toString().toInt()
                val getEstado = distr.getString("nestado").toString().toInt()
                val getEstadoAtual = distr.getString("estadosala").toString().toInt()
                val descricao = distr.getString("descricao")
                listSalas.add(Salas(getId, getEst, getSala, getLugares, getAlocacao, getIntervalo, getEstado, getEstadoAtual, descricao))

            }
            callback?.onSuccess();
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> error.printStackTrace() })
        requestQueue.add(request)
    }

    /* Obter lista de estabelecimentos */
    fun getEstabelecimentos(callback: VolleyCallBack?) {
        val url = appSettings.URLestabelecimentos
        Log.i("Teste", url)
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, {
                response ->try {
            val jsonArray = response.getJSONArray("" + "data")
            Log.i("Array", jsonArray.toString())
            Log.i("Array", jsonArray.length().toString())
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

    fun getDispositivo(callback: VolleyCallBack?) {
        val url = appSettings.URL_get_tablet + session.getTablet()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

            if(response.getBoolean("success")){
                val tablet = response.getJSONObject("data")
                listTablet.add(Tablet(
                    tablet.getInt("ntablet"),
                    tablet.getInt("nsala"),
                    tablet.getString("modelo"),
                    tablet.getString("marca"),
                    tablet.getInt("pin")
                ))
                callback?.onSuccess()
            }else callback?.onFailure("Tablet não encontrado!")

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()) })
        requestQueue.add(request)
    }

    fun getSala(callback: VolleyCallBack?) {
        val url = appSettings.URL_get_sala + session.getSala()
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.GET, url, null, { response ->try {

            if(response.getBoolean("success")){
                val sala = response.getJSONObject("data")
                singleSala.add(Salas(
                    sala.getInt("nsala"),
                    sala.getInt("nestabelecimento"),
                    sala.getString("sala"),
                    sala.getInt("lugares"),
                    sala.getInt("alocacao"),
                    sala.getInt("intervalolimpeza"),
                    sala.getInt("nestado"),
                    sala.getInt("estadosala"),
                    sala.getString("descricao")
                ))
                callback?.onSuccess()
            }else callback?.onFailure("Sala não encontrada!")

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        }, { error -> callback?.onFailure(error.printStackTrace().toString()) })
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

    /* Carregar lista de salas quando um estabelecimento é selecionado */
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val item = parent?.getItemAtPosition(position).toString()
        getSalas(object: VolleyCallBack {
            val selectedSala = findViewById<TextInputLayout>(R.id.defineSalaDrop)
            override fun onSuccess() {
                val salas : MutableList<String> = mutableListOf()
                salas.clear()
                for(item in listSalas)
                    salas.add(item.sala)
                var adapter = ArrayAdapter(applicationContext, R.layout.dropdown_item, salas)
                (selectedSala.editText as? AutoCompleteTextView)?.setAdapter(adapter)
                adapter.notifyDataSetChanged();

            }
            override fun onFailure(error: String) {
                Log.i("Settings", error)
            }
        }, listEstabelecimentos[position].nestabelecimento)
    }

    override fun onResume() {
        super.onResume()
        callBackend()
    }

}