package no.nav.helse.spedisjon

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

internal class Inntektsmeldinger(
    rapidsConnection: RapidsConnection,
    private val meldingDao: MeldingDao
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(Inntektsmeldinger::class.java)
    }

    init {
        River(rapidsConnection).apply {
            validate { it.forbid("@event_name") }
            validate { it.requireKey(
                "inntektsmeldingId", "arbeidstakerFnr",
                "arbeidstakerAktorId", "virksomhetsnummer",
                "arbeidsgivertype", "beregnetInntekt",
                "endringIRefusjoner", "arbeidsgiverperioder",
                "status", "arkivreferanse", "ferieperioder",
                "foersteFravaersdag", "mottattDato"
            ) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val mottattDato = packet["mottattDato"].asLocalDateTime()
        packet["@event_name"] = "inntektsmelding"
        packet["@opprettet"] = mottattDato

        meldingDao.leggInn(packet.toJson(), mottattDato)
        //context.send(packet["arbeidstakerFnr"].asText(), packet.toJson())
    }

}
