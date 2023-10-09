package com.example.apptablet

public class appSettings {

    companion object {
        private val ipAddress = "https://softinsa-backend.herokuapp.com/"
        //private val ipAddress = "http://192.168.1.130:24023/"

        val appReload : Long = 180000 //3 min = 180000 - 5 min = 300000
        val reloadQR : Long = 30000 //30 segundos

        val URL_get_sala = ipAddress + "salas/get/"
        val URL_salas_estabelecimento = ipAddress + "salas/estabelecimento/"
        val URLestabelecimentos = ipAddress + "estabelecimentos"
        val URL_reservas_sala = ipAddress + "reservas/list/sala/"
        val URL_adicionar_tablet = ipAddress + "tablets/adicionar/"
        val URL_estado_sala = ipAddress + "salas/state/"
        val URLupdateTablet = ipAddress + "tablets/update"
        val URL_validar_sobreposicao = ipAddress + "reservas/inserir/validar"
        val URL_validar_pin = ipAddress + "tablets/validar/pin/"
        val URL_get_tablet = ipAddress + "tablets/get/"
        val URL_minutos_disponiveis = ipAddress + "algoritmos/minutos/"
        val URL_atualizar_estado_sala = ipAddress + "salas/atualizar/estado/"
        val URL_atualizar_dados_sala = ipAddress + "salas/atualizar/sala/"
        val URL_tablet_exists = ipAddress + "tablets/exists/"
        val URL_reservas_sala_hoje = ipAddress + "reservas/list/hoje/sala/"
        val callBackend = ipAddress
    }

}