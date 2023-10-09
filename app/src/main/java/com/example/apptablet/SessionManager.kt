package com.example.apptablet

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import kotlin.math.roundToInt

public class SessionManager {

    lateinit var pref : SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    lateinit var con: Context
    var PRIVATE_MODE: Int = 0

    constructor(con: Context){
        this.con = con
        pref = con.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = pref.edit()
    }

    companion object {
        /* nomeSala, Lugares, Alocacao, IntervaloLimpeza, Estado, EstadoAtual  */
        val PREF_NAME: String = "Login_Preferences"
        val IS_LOGIN: String = "isLoggedIn"
        val KEY_NTABLET: String = "ntablet"
        val KEY_IDSALA: String = "idsala"
        val KEY_NOMESALA: String = "nomesala"
        val KEY_LUGARES: String = "lugares"
        val KEY_LUGARES_DEFAULT : String = "lugares_def"
        val KEY_ALOCACAO : String = "alocacao"
        val KEY_INTERVALO : String = "intervalo"
        val KEY_ESTADO : String = "estado"
        val KEY_ESTADOATUAL : String = "estadoatual"
        val KEY_PINCODE : String = "pincode"
    }

    fun createLoginSession(idsala: Int, nomesala: String, lugares: Int, alocacao: Int, intervalo: Int, estado: Int, estadoatual: Int, pin : Int){
        editor.putBoolean(IS_LOGIN, true)
        editor.putInt(KEY_IDSALA, idsala)
        editor.putString(KEY_NOMESALA, nomesala)
        editor.putInt(KEY_LUGARES_DEFAULT, lugares)
        val lugaresFinal = Math.floor( (lugares * alocacao / 100).toDouble() ).roundToInt()
        editor.putInt(KEY_LUGARES, lugaresFinal)
        editor.putInt(KEY_ALOCACAO, alocacao)
        editor.putInt(KEY_INTERVALO, intervalo)
        editor.putInt(KEY_ESTADO, estado)
        editor.putInt(KEY_ESTADOATUAL, estadoatual)
        editor.putInt(KEY_PINCODE, pin)
        editor.commit()
    }

    fun checkLogin(){
        if(!this.isLoggedIn()){
            var i: Intent = Intent(con, registroDispositivo::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            con.startActivity(i)
        }
    }

    fun getSalaDetails() : HashMap<String, String>{
        var user: Map<String, String> = HashMap<String, String>()
        (user as HashMap).put(KEY_NOMESALA, pref.getString(KEY_NOMESALA, null)!!)
        (user as HashMap).put(KEY_LUGARES, pref.getInt(KEY_LUGARES, 0).toString()!!)
        return user
    }

    fun getSala() : Int{
        return pref.getInt(KEY_IDSALA, 0)
    }

    fun getIntervalo() : Int{
        return pref.getInt(KEY_INTERVALO, 0)
    }

    fun setIntervalo(n : Int){
        editor.putInt(KEY_INTERVALO, n)
        editor.commit()
    }

    fun getAlocacao() : Int{
        return pref.getInt(KEY_ALOCACAO, 0)
    }

    fun setAlocacao(n : Int){
        editor.putInt(KEY_ALOCACAO, n)
        editor.commit()
    }

    fun getPinCode() : Int{
        return pref.getInt(KEY_PINCODE, 0)
    }

    fun replacePinCode(pin : Int){
        editor.putInt(KEY_PINCODE, pin)
        editor.commit()
    }

    fun setNTablet(ntablet : Int){
        editor.putInt(KEY_NTABLET, ntablet)
        editor.commit()
    }

    fun getTablet() : Int{
        return pref.getInt(KEY_NTABLET, 0)
    }

    fun getEstado() : Int{
        return pref.getInt(KEY_ESTADOATUAL, 0)
    }

    fun setEstado(n : Int){
        editor.putInt(KEY_ESTADOATUAL, n)
        editor.commit()
    }

    fun setNomeSala(nomesala : String){
        editor.putString(KEY_NOMESALA, nomesala)
        editor.commit()
    }

    fun setLugares(n : Int){
        val lugaresFinal = Math.floor( (n * pref.getInt(KEY_ALOCACAO, 0) / 100).toDouble() ).roundToInt()
        editor.putInt(KEY_LUGARES, lugaresFinal)
        editor.putInt(KEY_LUGARES_DEFAULT, n)
        editor.commit()
    }

    fun getLugaresDefault() : Int{
        return pref.getInt(KEY_LUGARES_DEFAULT, 0)
    }

    fun getLugares() : Int{
        return pref.getInt(KEY_LUGARES, 0)
    }

    fun setSala(idsala: Int, nomesala: String, lugares: Int, alocacao: Int, intervalo: Int, estado: Int, estadoatual: Int){
        editor.putInt(KEY_IDSALA, idsala)
        editor.putString(KEY_NOMESALA, nomesala)
        editor.putInt(KEY_LUGARES, lugares)
        editor.putInt(KEY_LUGARES_DEFAULT, lugares)
        editor.putInt(KEY_ALOCACAO, alocacao)
        editor.putInt(KEY_INTERVALO, intervalo)
        editor.putInt(KEY_ESTADO, estado)
        editor.putInt(KEY_ESTADOATUAL, estadoatual)
        editor.commit()
    }

    fun LogoutUser(){
        editor.clear()
        editor.commit()
        var i: Intent = Intent(con, registroDispositivo::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        con.startActivity(i)
    }

    fun isLoggedIn() : Boolean {
        return pref.getBoolean(IS_LOGIN, false)
    }


}