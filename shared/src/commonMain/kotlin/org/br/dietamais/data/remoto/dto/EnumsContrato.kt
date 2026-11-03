package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

abstract class EnumComOutro<T : Enum<T>>(nome: String, private val valores: Array<T>, private val outro: T) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(nome, PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): T = decoder.decodeString().let { texto -> valores.firstOrNull { it.name == texto } ?: outro }
    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.name)
}

@Serializable(with = PerfilSerializer::class)
enum class Perfil { ADMIN, NUTRICIONISTA, PACIENTE, OUTRO }
object PerfilSerializer : EnumComOutro<Perfil>("Perfil", Perfil.entries.toTypedArray(), Perfil.OUTRO)

@Serializable(with = PlataformaSerializer::class)
enum class Plataforma { ANDROID, IOS, DESKTOP, WEB, OUTRO }
object PlataformaSerializer : EnumComOutro<Plataforma>("Plataforma", Plataforma.entries.toTypedArray(), Plataforma.OUTRO)

@Serializable(with = StatusAssinaturaSerializer::class)
enum class StatusAssinatura { TESTE, ATIVA, EM_ATRASO, CANCELADA, EXPIRADA, OUTRO }
object StatusAssinaturaSerializer : EnumComOutro<StatusAssinatura>("StatusAssinatura", StatusAssinatura.entries.toTypedArray(), StatusAssinatura.OUTRO)

@Serializable(with = StatusVinculoSerializer::class)
enum class StatusVinculo { CONVIDADO, ATIVO, ENCERRADO, OUTRO }
object StatusVinculoSerializer : EnumComOutro<StatusVinculo>("StatusVinculo", StatusVinculo.entries.toTypedArray(), StatusVinculo.OUTRO)

@Serializable(with = SexoSerializer::class)
enum class Sexo { FEMININO, MASCULINO, OUTRO, NAO_INFORMADO }
object SexoSerializer : EnumComOutro<Sexo>("Sexo", Sexo.entries.toTypedArray(), Sexo.OUTRO)
