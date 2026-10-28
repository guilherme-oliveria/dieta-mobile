package org.br.dietamais

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.br.dietamais.plataforma.ReceptorPush

/**
 * Push com `notification` + `data {tipo, sync, rota}` (integracao.md). Com o app fora da tela o sistema mostra a notificação e o
 * toque chega pela intent da [MainActivity]; com o app aberto este serviço só pede um sync (o contador de avisos atualiza sozinho).
 */
class ServicoPushDieta : FirebaseMessagingService() {
    override fun onNewToken(token: String) = ReceptorPush.novoToken(token)
    override fun onMessageReceived(mensagem: RemoteMessage) = ReceptorPush.chegou()
}
